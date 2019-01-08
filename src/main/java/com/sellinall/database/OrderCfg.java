package com.sellinall.database;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mudra.sellinall.config.Config;

public class OrderCfg {

	public @Bean MongoDatabase db() throws Exception {
		List<ServerAddress> seeds = new ArrayList<ServerAddress>();
		String[] hostNames = Config.getConfig().getOrderCollectionHostName().split(",");
		String[] ports = Config.getConfig().getOrderCollectionPort().split(",");
		for (int i = 0; i < hostNames.length; i++) {
			seeds.add(new ServerAddress(hostNames[i], Integer.parseInt(ports[i])));
		}

		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(MongoCredential.createScramSha1Credential(Config.getConfig().getDbUserName(),
				Config.getConfig().getOrderCollectionDBName(), Config.getConfig().getDbPassword().toCharArray()));

		MongoClient mongoClient = new MongoClient(seeds, credentials);
		MongoDatabase db = mongoClient.getDatabase(Config.getConfig().getOrderCollectionDBName());
		return db;
	}

}