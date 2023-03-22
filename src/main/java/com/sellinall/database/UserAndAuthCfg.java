package com.sellinall.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mudra.sellinall.config.Config;

@Configuration
public class UserAndAuthCfg {
	public @Bean MongoDatabase db() throws Exception {
		MongoClient uri = (MongoClient) MongoClients.create(Config.getConfig().getUserConfigDBURI());
		MongoDatabase db = uri.getDatabase(Config.getConfig().getUserConfigDBName());
		return db;
	}
}