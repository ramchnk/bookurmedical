package com.sellinall.order.db;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;

/**
 * @author Mallikarjun
 *
 */
public class LoadAndProcessOrderFromDB implements Processor {
	static Logger log = Logger.getLogger(LoadAndProcessOrderFromDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		exchange.setProperty("orderStatus", orderMessage.getString("orderStatus"));
		String orderID = orderMessage.get("orderID").toString();
		exchange.setProperty("orderID", orderID);
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", orderMessage.get("accountNumber").toString());
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));
		searchQuery.put("orderID", orderID);
		Document dbResult = table.find(searchQuery).first();
		exchange.setProperty("hasOrderInDB", false);
		exchange.setProperty("isEligibleToUpdateBrandID", Config.getConfig().getIsEligibleToUpdateBrandID());
		if (Config.getConfig().getIsEligibleToUpdateBrandID()) {
			exchange.setProperty("brandIDMap", new HashMap<String, String>());
		}
		if (orderMessage.has("needToGenerateAirwayBill")) {
			exchange.setProperty("needToGenerateAirwayBill", orderMessage.getBoolean("needToGenerateAirwayBill"));
		}
		if (dbResult == null) {
			log.debug("Order Record - not exists in our DB Result: " + dbResult);
			return;
		}
		exchange.setProperty("hasOrderInDB", true);
		exchange.setProperty("orderDBObject", Document.parse(dbResult.toJson()));
	}
}