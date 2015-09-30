package com.sellinall.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import com.mongodb.MongoClient;
import com.mudra.sellinall.config.Config;

@Configuration
public class UserAndAuthCfg {

	public @Bean
	MongoDbFactory DbFactory() throws Exception {
		return new SimpleMongoDbFactory(new MongoClient(
				Config.getConfig().getUserCollectionHostName(),
				Integer.parseInt(Config.getConfig().getUserCollectionPort())),
				Config.getConfig().getUserCollectionDBName());
	}

	public @Bean
	MongoTemplate mongoTemplate() throws Exception {
		MongoTemplate template = new MongoTemplate(DbFactory());
		return template;

	}

}