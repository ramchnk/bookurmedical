package com.sellinall.database;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mudra.sellinall.config.Config;

@Configuration
public class InventoryCfg {

	public @Bean MongoDatabase db() throws Exception {
		List<ServerAddress> seeds = new ArrayList<ServerAddress>();
		String[] hostNames = Config.getConfig().getInventoryCollectionHostName().split(",");
		String[] ports = Config.getConfig().getInventoryCollectionPort().split(",");
		for (int i = 0; i < hostNames.length; i++) {
			seeds.add(new ServerAddress(hostNames[i], Integer.parseInt(ports[i])));
		}

		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(MongoCredential.createScramSha1Credential(Config.getConfig().getDbUserName(),
				Config.getConfig().getInventoryCollectionDBName(), Config.getConfig().getDbPassword().toCharArray()));

		MongoClient mongoClient = new MongoClient(seeds, credentials);
		MongoDatabase db = mongoClient.getDatabase(Config.getConfig().getInventoryCollectionDBName());
		return db;
	}
}
