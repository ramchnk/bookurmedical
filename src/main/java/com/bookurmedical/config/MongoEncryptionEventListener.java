package com.bookurmedical.config;

import com.bookurmedical.annotation.Encrypted;
import com.bookurmedical.service.FieldEncryptionService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transparent field-level encryption/decryption for MongoDB entities.
 *
 * How it works:
 * - BeforeSaveEvent : encrypts every @Encrypted String field in the BSON
 * Document
 * just before it is written to MongoDB.
 * - AfterLoadEvent : decrypts every @Encrypted String field in the raw BSON
 * Document
 * immediately after it is read from MongoDB (before mapping).
 *
 * This means the application always works with plain-text values in memory,
 * and MongoDB only ever stores the encrypted form.
 */
@Component
public class MongoEncryptionEventListener extends AbstractMongoEventListener<Object> {

    private static final Logger log = LoggerFactory.getLogger(MongoEncryptionEventListener.class);

    @Autowired
    private FieldEncryptionService encryptionService;

    // ── Encrypt before save ───────────────────────────────────────────────────

    @Override
    public void onBeforeSave(@NonNull BeforeSaveEvent<Object> event) {
        Object source = event.getSource();
        Document document = event.getDocument();
        if (document == null)
            return;

        processFields(source.getClass(), source, document, true);
    }

    // ── Decrypt after load ────────────────────────────────────────────────────

    @Override
    public void onAfterLoad(@NonNull AfterLoadEvent<Object> event) {
        Document document = event.getDocument();
        if (document == null)
            return;

        Class<?> type = event.getType();
        processDocumentFields(type, document, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Walk all fields (including inherited) of the entity class.
     * For BeforeSave: read plain value from object → encrypt → write into BSON
     * document.
     */
    private void processFields(Class<?> clazz, Object source, Document document, boolean encrypt) {
        if (clazz == null || clazz == Object.class)
            return;

        // recurse into superclass first
        processFields(clazz.getSuperclass(), source, document, encrypt);

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Encrypted.class))
                continue;
            if (field.getType() != String.class)
                continue;

            field.setAccessible(true);
            try {
                String mongoFieldName = resolveMongoFieldName(field);
                String value = (String) field.get(source);
                if (value == null)
                    continue;

                String processed = encrypt
                        ? encryptionService.encrypt(value)
                        : encryptionService.decrypt(value);

                document.put(mongoFieldName, processed);
            } catch (Exception e) {
                log.warn("[Encryption] Skipping field '{}' on {}: {}",
                        field.getName(), clazz.getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * For AfterLoad: read the raw BSON document → decrypt → write back into
     * document
     * (Spring Data will then map the decrypted document into the entity object).
     */
    private void processDocumentFields(Class<?> clazz, Document document, boolean encrypt) {
        if (clazz == null || clazz == Object.class)
            return;

        processDocumentFields(clazz.getSuperclass(), document, encrypt);

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Encrypted.class))
                continue;
            if (field.getType() != String.class)
                continue;

            try {
                String mongoFieldName = resolveMongoFieldName(field);
                Object rawValue = document.get(mongoFieldName);
                if (!(rawValue instanceof String value))
                    continue;
                if (value.isBlank())
                    continue;

                String processed = encrypt
                        ? encryptionService.encrypt(value)
                        : encryptionService.decrypt(value);

                document.put(mongoFieldName, processed);
            } catch (Exception e) {
                log.warn("[Encryption] Skipping field '{}' on {} during load: {}",
                        field.getName(), clazz.getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Resolves the MongoDB field name: honours @Field annotation if present,
     * otherwise uses the Java field name as-is.
     */
    private String resolveMongoFieldName(Field field) {
        org.springframework.data.mongodb.core.mapping.Field mongoField = field
                .getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class);
        if (mongoField != null && !mongoField.value().isEmpty()) {
            return mongoField.value();
        }
        return field.getName();
    }
}
