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
public class UserAndAuthCfg {
	public @Bean DB db() throws Exception {
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
		DB db = mongoClient.getDB(Config.getConfig().getUserCollectionDBName());
		return db;
	}
}