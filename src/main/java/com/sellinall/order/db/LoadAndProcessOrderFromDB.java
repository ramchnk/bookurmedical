package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
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
		String orderID = orderMessage.getString("orderID");
		exchange.setProperty("orderID", orderID);
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		searchQuery.put("orderID", orderID);
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));
		searchQuery.put("site.name", orderMessage.getString("site"));
		Document dbResult = table.find(searchQuery).first();
		exchange.setProperty("hasOrderInDB", false);
		if (dbResult == null) {
			log.debug("Order Record - not exists in our DB Result: " + dbResult);
			return;
		}
		if (orderMessage.has("needToGenerateAirwayBill")) {
			exchange.setProperty("needToGenerateAirwayBill", orderMessage.getBoolean("needToGenerateAirwayBill"));
		}
		exchange.setProperty("hasOrderInDB", true);
		exchange.setProperty("orderDBObject", BasicDBObject.parse(dbResult.toJson()));
	}
}