package com.sellinall.database;

import org.bson.Document;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DbUtilities {
	static ApplicationContext userContext = new AnnotationConfigApplicationContext(UserAndAuthCfg.class);
	static MongoDatabase userDB = (MongoDatabase) userContext.getBean("db");
	static ApplicationContext inventoryContext = new AnnotationConfigApplicationContext(InventoryCfg.class);
	static MongoDatabase inventoryDB = (MongoDatabase) inventoryContext.getBean("db");
	static ApplicationContext orderContext = new AnnotationConfigApplicationContext(OrderCfg.class);
	static MongoDatabase orderDB = (MongoDatabase) orderContext.getBean("db");

	public static MongoCollection<Document> getDBCollection(String collectionName) {
		return userDB.getCollection(collectionName);
	}

	public static MongoCollection<Document> getInventoryDBCollection(String collectionName) {
		return inventoryDB.getCollection(collectionName);
	}

	public static MongoCollection<Document> getOrderDBCollection(String collectionName) {
		return orderDB.getCollection(collectionName);
	}

}
