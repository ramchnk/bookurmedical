package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

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
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", checkoutMessage.get("accountNumber").toString());
		searchQuery.put("site.nickNameID", checkoutMessage.getString("nickNameID"));
		searchQuery.put("checkoutID", checkoutID);
		Document dbResult = table.find(searchQuery).first();
		exchange.setProperty("hasCheckoutInDB", false);
		if (dbResult == null) {
			log.debug("Checkout Record - not exists in our DB Result: " + dbResult);
			return;
		}
		exchange.setProperty("hasCheckoutInDB", true);
		exchange.setProperty("checkoutDBObject", Document.parse(dbResult.toJson()));
	}
}