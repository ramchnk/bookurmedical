package com.sellinall.database;

import org.springframework.context.annotation.Bean;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mudra.sellinall.config.Config;

public class OrderCfg {

	public @Bean MongoDatabase db() throws Exception {
			MongoClient uri = (MongoClient) MongoClients.create(Config.getConfig().getOrderConfigDBURI());
			MongoDatabase db = uri.getDatabase(Config.getConfig().getOrderConfigDBName());
			return db;
	}

}