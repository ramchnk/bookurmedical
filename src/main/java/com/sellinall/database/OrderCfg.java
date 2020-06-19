package com.sellinall.database;

import org.springframework.context.annotation.Bean;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mudra.sellinall.config.Config;

public class OrderCfg {

	public @Bean MongoDatabase db() throws Exception {
		MongoClient mongoClient = null;
		try {
			MongoClientURI uri = new MongoClientURI(Config.getConfig().getOrderConfigDBURI());
			mongoClient = new MongoClient(uri);
			MongoDatabase db = mongoClient.getDatabase(Config.getConfig().getOrderConfigDBName());
			return db;
		} finally {
			if (mongoClient != null) {
				mongoClient.close();
			}
		}
	}

}