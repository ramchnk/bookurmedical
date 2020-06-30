package com.sellinall.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mudra.sellinall.config.Config;

@Configuration
public class InventoryCfg {

	public @Bean MongoDatabase db() throws Exception {
		MongoClientURI uri = new MongoClientURI(Config.getConfig().getInventoryConfigDBURI());
		MongoClient mongoClient = new MongoClient(uri);
		MongoDatabase db = mongoClient.getDatabase(Config.getConfig().getInventoryConfigDBName());
		return db;
}
}
