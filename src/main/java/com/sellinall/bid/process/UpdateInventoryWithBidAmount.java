package com.sellinall.bid.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.mudra.sellinall.util.DateUtil;
import com.sellinall.database.DbUtilities;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateInventoryWithBidAmount implements Processor {
	static Logger log = Logger.getLogger(UpdateInventoryWithBidAmount.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = new JSONObject(exchange.getIn().getBody(String.class));
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		JSONObject bidMessage = exchange.getProperty("message", JSONObject.class);
		processBidAmountUpdateQuery(exchange, inventoryDBRecord, bidMessage);
	}

	private void processBidAmountUpdateQuery(Exchange exchange, BasicDBObject inventoryDBRecord, JSONObject bidMessage)
			throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", exchange.getProperty("SKU", String.class));
		searchQuery.put("userId", bidMessage.getString("userId"));
		String siteName = bidMessage.getString("site");
		searchQuery.put(siteName + ".nickNameID", bidMessage.getString("nickNameID"));
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		BasicDBObject updateFields = new BasicDBObject(siteName + ".$.highBidAmount",
				(BasicDBObject) JSON.parse(bidMessage.getJSONObject("bidAmount").toString()));
		updateFields.put(siteName + ".$.failureReason", "");
		updateFields.put(siteName + ".$.timeLastUpdated", DateUtil.getSIADateFormat());
		table.update(searchQuery, new BasicDBObject("$set",updateFields));
	}
}