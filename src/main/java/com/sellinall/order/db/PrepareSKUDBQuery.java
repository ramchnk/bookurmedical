/**
 * 
 */
package com.sellinall.order.db;

import java.util.ArrayList;

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
		ArrayList<String> in = new ArrayList<String>();
		in.add(SKU);
		if (SKU.split("-").length > 1) {
			in.add(SKU.split("-")[0]);
		}
		DBObject searchQuery = new BasicDBObject("SKU", new BasicDBObject("$in", in));
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String siteName = exchange.getProperty("siteName", String.class);
		BasicDBObject elemMatch = new BasicDBObject("nickNameID", nickNameID);
		BasicDBObject searchSite = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put(siteName, searchSite);
		exchange.getOut().setBody(searchQuery);
	}
}