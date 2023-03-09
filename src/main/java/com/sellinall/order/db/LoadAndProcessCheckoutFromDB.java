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
 * @author ManI
 *
 */
public class LoadAndProcessCheckoutFromDB implements Processor {
	static Logger log = Logger.getLogger(LoadAndProcessCheckoutFromDB.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject checkoutMessage = exchange.getProperty("message", JSONObject.class);
		String checkoutID = checkoutMessage.getString("checkoutID");
		exchange.setProperty("checkoutID", checkoutID);
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("abandonedCheckouts");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", checkoutMessage.getString("accountNumber"));
		searchQuery.put("checkoutID", checkoutID);
		searchQuery.put("site.nickNameID", checkoutMessage.getString("nickNameID"));
		searchQuery.put("site.name", checkoutMessage.getString("site"));
		Document dbResult = table.find(searchQuery).first();
		exchange.setProperty("hasCheckoutInDB", false);
		if (dbResult == null) {
			log.debug("Checkout Record - not exists in our DB Result: " + dbResult);
			return;
		}
		exchange.setProperty("hasCheckoutInDB", true);
		exchange.setProperty("checkoutDBObject", BasicDBObject.parse(dbResult.toJson()));
	}
}