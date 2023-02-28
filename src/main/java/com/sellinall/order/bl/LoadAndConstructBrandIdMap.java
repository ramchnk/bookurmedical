package com.sellinall.order.bl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
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
				BasicDBObject searchQuery = new BasicDBObject();
				searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
				searchQuery.put("sellerSKU", sellerSKU);
				BasicDBObject projection = new BasicDBObject("graasBrandID", 1);
				Document pmDocument = productMasterTable.find(searchQuery).projection(projection).first();
				BasicDBObject productMaster = (BasicDBObject) BasicDBObject.parse(pmDocument.toJson());
				if (productMaster.containsKey("graasBrandID")) {
					brandIDMap.put(sellerSKU, productMaster.getString("graasBrandID"));
				}
			} else {
				log.error("CustomSKU not available for orderID :" + exchange.getProperty("orderID", String.class)
						+ ", nicknameID: " + orderMessage.getString("nickNameID") + ", accountNumber: "
						+ orderMessage.getString("accountNumber"));
			}
		} catch (Exception e) {
			log.error("Some exception occured while get brandID from PM for orderID :"
					+ exchange.getProperty("orderID", String.class) + ", nicknameID: "
					+ orderMessage.getString("nickNameID") + ", accountNumber: "
					+ orderMessage.getString("accountNumber"));
			e.printStackTrace();
		}
	}
}
