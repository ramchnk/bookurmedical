package com.sellinall.database;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public class DbUtilities {
	static ApplicationContext userContext = new AnnotationConfigApplicationContext(UserAndAuthCfg.class);
	static DB userDB = (DB) userContext.getBean("db");
	static ApplicationContext inventoryContext = new AnnotationConfigApplicationContext(InventoryCfg.class);
	static DB inventoryDB = (DB) inventoryContext.getBean("db");

	public static DBCollection getDBCollection(String collectionName) {
		return userDB.getCollection(collectionName);
	}

	public static DBCollection getInventoryDBCollection(String collectionName) {
		return inventoryDB.getCollection(collectionName);
	}

}
