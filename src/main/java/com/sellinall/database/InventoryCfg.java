package com.sellinall.database;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mudra.sellinall.config.Config;

@Configuration
public class InventoryCfg {

	public @Bean DB db() throws Exception {
		List<ServerAddress> seeds = new ArrayList<ServerAddress>();
		seeds.add(new ServerAddress(Config.getConfig().getInventoryCollectionHostName(), Integer.parseInt(Config
				.getConfig().getInventoryCollectionPort())));

		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(MongoCredential.createScramSha1Credential(Config.getConfig().getDbUserName(), Config
				.getConfig().getInventoryCollectionDBName(), Config.getConfig().getDbPassword().toCharArray()));

		MongoClient mongoClient = new MongoClient(seeds, credentials);
		DB db = mongoClient.getDB(Config.getConfig().getInventoryCollectionDBName());
		return db;
	}
}
