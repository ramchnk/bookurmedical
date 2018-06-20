package com.sellinall.database;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mudra.sellinall.config.Config;

@Configuration
public class UserAndAuthCfg {
	public @Bean MongoDatabase db() throws Exception {
		List<ServerAddress> seeds = new ArrayList<ServerAddress>();
		String[] hostNames = Config.getConfig().getUserCollectionHostName().split(",");
		String[] ports = Config.getConfig().getUserCollectionPort().split(",");
		for(int i = 0; i < hostNames.length; i++) {
			seeds.add(new ServerAddress(hostNames[i], Integer.parseInt(ports[i])));
		}

		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(MongoCredential.createScramSha1Credential(Config.getConfig().getDbUserName(), Config
				.getConfig().getUserCollectionDBName(), Config.getConfig().getDbPassword().toCharArray()));

		MongoClient mongoClient = new MongoClient(seeds, credentials);
		MongoDatabase db = mongoClient.getDatabase(Config.getConfig().getUserCollectionDBName());
		return db;
	}
}