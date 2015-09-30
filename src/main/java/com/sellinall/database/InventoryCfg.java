package com.sellinall.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import com.mongodb.MongoClient;
import com.mudra.sellinall.config.Config;

@Configuration
public class InventoryCfg {
	public @Bean
	MongoDbFactory dbFactory() throws Exception {
		return new SimpleMongoDbFactory(new MongoClient(
				Config.getConfig().getInventoryCollectionHostName(),
				Integer.parseInt(Config.getConfig().getInventoryCollectionPort())),
				Config.getConfig().getInventoryCollectionDBName());
	}

	public @Bean
	MongoTemplate mongoTemplate() throws Exception {
		MongoTemplate mongoTemplate = new MongoTemplate(dbFactory());
		return mongoTemplate;
	}
}
