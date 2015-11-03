/**
 * 
 */
package com.sellinall.bid.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;

/**
 * @author Mallikarjun
 * 
 */
public class InsertBidNotification implements Processor {
	static Logger log = Logger.getLogger(InsertBidNotification.class.getName());

	public void process(Exchange exchange) throws Exception {

		JSONObject bidMessageJSON = exchange.getProperty("message",JSONObject.class);
		bidMessageJSON.remove("type");
		BasicDBObject bidMessage = (BasicDBObject) JSON.parse(bidMessageJSON.toString());
		insertBidRecord(bidMessage);
		exchange.getOut().setBody(bidMessageJSON);
	}

	private void insertBidRecord(BasicDBObject bidMessage) throws JSONException {
		DBCollection table = DbUtilities.getInventoryDBCollection("bid");
		table.insert(bidMessage);
	}
}