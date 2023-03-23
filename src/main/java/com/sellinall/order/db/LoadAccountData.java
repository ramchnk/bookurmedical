package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;

public class LoadAccountData implements Processor{

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String[] sitesList = PostingSites.getConfig().getSitesList();
		BasicDBObject accountDetails = getAccountDetails(accountNumber, sitesList);
		Map<String, BasicDBObject> nickNameObjectMap = new HashMap<String, BasicDBObject>();
		for (String site : sitesList) {
			if(accountDetails.containsField(site)){
				ArrayList<BasicDBObject> siteAccountList = (ArrayList<BasicDBObject>) accountDetails.get(site);
				for (BasicDBObject siteAccountObject: siteAccountList){
					BasicDBObject siteNickNameObject = (BasicDBObject) siteAccountObject.get("nickName");
					nickNameObjectMap.put(siteNickNameObject.getString("id"), siteAccountObject);
				}
			}
		}
		exchange.setProperty("nickNameObjectMap", nickNameObjectMap);
		exchange.setProperty("accountDetails", accountDetails);
	}
	private BasicDBObject getAccountDetails(String accountNumber, String[] sitesName) {
		BasicDBObject searchQuery = new BasicDBObject();
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);

		BasicDBObject projection = new BasicDBObject();
		projection.put("merchantID", 1);
		for (String site : sitesName) {
			projection.put(site + ".nickName", 1);
			projection.put(site + ".enableLowQuantityNotification", 1);
			projection.put(site + ".lowQuantityThreshold", 1);
		}
		MongoCollection<Document> table = DbUtilities.getDBCollection("accounts");
		Document accountDocument = table.find(searchQuery).projection(projection).first();
		BasicDBObject accountDetails = (BasicDBObject) JSON.parse(accountDocument.toJson());
		return accountDetails;
	}

}
