/**
 * 
 */
package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Raju
 *
 */
public class PrepareSKUDBQuery implements Processor {

	static Logger log = Logger.getLogger(PrepareSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getIn().getBody(JSONObject.class);
		String SKU = inBody.getString("SKU");
		exchange.setProperty("SKU", SKU);
		DBObject searchQuery = new BasicDBObject("SKU", SKU);
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		if (orderMessage.has("userId")) {
			searchQuery.put("userId", orderMessage.getString("userId"));
		}
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String siteName = exchange.getProperty("siteName", String.class);
		BasicDBObject elemMatch = new BasicDBObject("nickNameID", nickNameID);
		BasicDBObject searchSite = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put(siteName, searchSite);
		exchange.getOut().setBody(searchQuery);
	}
}