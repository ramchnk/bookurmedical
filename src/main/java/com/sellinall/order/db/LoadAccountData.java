package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;

public class LoadAccountData implements Processor{

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String[] sitesList = PostingSites.getConfig().getSitesList();
		Document accountDetails = getAccountDetails(accountNumber, sitesList);
		Map<String, Document> nickNameObjectMap = new HashMap<String, Document>();
		for (String site : sitesList) {
			if(accountDetails.containsKey(site)){
				ArrayList<Document> siteAccountList = (ArrayList<Document>) accountDetails.get(site);
				for (Document siteAccountObject: siteAccountList){
					Document siteNickNameObject = (Document) siteAccountObject.get("nickName");
					nickNameObjectMap.put(siteNickNameObject.getString("id"), siteAccountObject);
				}
			}
		}
		exchange.setProperty("nickNameObjectMap", nickNameObjectMap);
		exchange.setProperty("accountDetails", accountDetails);
	}
	private Document getAccountDetails(String accountNumber, String[] sitesName) {
		Document searchQuery = new Document();
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);

		Document projection = new Document();
		projection.put("merchantID", 1);
		for (String site : sitesName) {
			projection.put(site + ".nickName", 1);
			projection.put(site + ".enableLowQuantityNotification", 1);
			projection.put(site + ".lowQuantityThreshold", 1);
		}
		MongoCollection<Document> table = DbUtilities.getDBCollection("accounts");
		Document accountDocument = table.find(searchQuery).projection(projection).first();
		Document accountDetails = Document.parse(accountDocument.toJson());
		return accountDetails;
	}

}
