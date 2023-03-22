/**
 * 
 */
package com.sellinall.bid.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.client.MongoCollection;
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
		Document bidMessage = (Document) Document.parse(bidMessageJSON.toString());
		insertBidRecord(bidMessage);
		exchange.getOut().setBody(bidMessageJSON);
	}

	private void insertBidRecord(Document bidMessage) throws JSONException {
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("bid");
		table.insertOne(new Document(bidMessage));
	}
}