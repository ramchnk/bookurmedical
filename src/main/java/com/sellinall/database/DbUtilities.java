package com.sellinall.database;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoOperations;
import com.mongodb.DBCollection;
import com.mudra.sellinall.config.Config;

public class DbUtilities {
	static ApplicationContext ctx = new AnnotationConfigApplicationContext(
			UserAndAuthCfg.class);
	static MongoOperations mongoOperation = (MongoOperations) ctx
			.getBean("mongoTemplate");
	static ApplicationContext ctx1 = new AnnotationConfigApplicationContext(
			InventoryCfg.class);
	static MongoOperations mongoOperation1 = (MongoOperations) ctx1
			.getBean("mongoTemplate");

	public static DBCollection getDBCollection(String collectionName) {
		DBCollection table = mongoOperation.getCollection(collectionName);		
		Boolean boo = table.getDB().authenticate(
				Config.getConfig().getDbUserName(),
				Config.getConfig().getDbPassword().toCharArray());
		if (boo) {
			return table;
		}
		return table;
	}

	public static DBCollection getInventoryDBCollection(String collectionName) {
		DBCollection table = mongoOperation1.getCollection(collectionName);
		Boolean boo = table.getDB().authenticate(
				Config.getConfig().getDbUserName(),
				Config.getConfig().getDbPassword().toCharArray());
		if (boo) {
			return table;
		}
		return table;
	}

}
