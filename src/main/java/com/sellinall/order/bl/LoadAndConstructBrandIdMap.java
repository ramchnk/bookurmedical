package com.sellinall.order.bl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;

public class LoadAndConstructBrandIdMap implements Processor {
	static Logger log = Logger.getLogger(LoadAndConstructBrandIdMap.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		try {
			Map<String, String> brandIDMap = exchange.getProperty("brandIDMap", HashMap.class);
			JSONObject orderItemMessage = exchange.getProperty("orderItemMessage", JSONObject.class);
			if (orderItemMessage.has("customSKU") && !orderItemMessage.getString("customSKU").isEmpty()) {
				String sellerSKU = orderItemMessage.getString("customSKU");
				MongoCollection<Document> productMasterTable = DbUtilities.getInventoryDBCollection("productMaster");
				Document searchQuery = new Document();
				searchQuery.put("accountNumber", orderMessage.get("accountNumber").toString());
				searchQuery.put("sellerSKU", sellerSKU);
				Document projection = new Document("graasBrandID", 1);
				Document pmDocument = productMasterTable.find(searchQuery).projection(projection).first();
				if (pmDocument != null && !pmDocument.isEmpty()) {
					Document productMaster = Document.parse(pmDocument.toJson());
					if (productMaster.containsKey("graasBrandID")) {
						brandIDMap.put(sellerSKU, productMaster.getString("graasBrandID"));
					}
				}
			} else {
				log.error("CustomSKU not available for orderID :" + exchange.getProperty("orderID", String.class)
						+ ", nicknameID: " + orderMessage.getString("nickNameID") + ", accountNumber: "
						+ orderMessage.get("accountNumber").toString());
			}
		} catch (Exception e) {
			log.error("Some exception occured while get brandID from PM for orderID :"
					+ exchange.getProperty("orderID", String.class) + ", nicknameID: "
					+ orderMessage.getString("nickNameID") + ", accountNumber: "
					+ orderMessage.get("accountNumber").toString());
			e.printStackTrace();
		}
	}
}
