package com.sellinall.bid.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateInventoryWithBidAmount implements Processor {
	static Logger log = Logger.getLogger(UpdateInventoryWithBidAmount.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = OrderUtil
				.parseToJsonObject(Document.parse(exchange.getProperty("inventory", String.class)));
		Document inventoryDBRecord = Document.parse(inventoryDBRecordJSON.toString());
		JSONObject bidMessage = exchange.getProperty("message", JSONObject.class);
		processBidAmountUpdateQuery(exchange, inventoryDBRecord, bidMessage);
	}

	private void processBidAmountUpdateQuery(Exchange exchange, Document inventoryDBRecord, JSONObject bidMessage)
			throws JSONException {
		Document searchQuery = new Document();
		searchQuery.put("SKU", exchange.getProperty("SKU", String.class));
		searchQuery.put("accountNumber", bidMessage.getString("accountNumber"));
		String siteName = bidMessage.getString("site");
		searchQuery.put(siteName + ".nickNameID", bidMessage.getString("nickNameID"));
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("inventory");
		Document updateFields = new Document(siteName + ".$.highBidAmount",
				Document.parse(bidMessage.getJSONObject("bidAmount").toString()));
		if (bidMessage.has("bidder")) {
			String highBidderEmailId = "";
			String highBidderUserId = "";
			JSONObject bidder = bidMessage.getJSONObject("bidder");
			if (bidder.has("Email")) {
				highBidderEmailId = bidder.getString("Email");
				updateFields.put(siteName + ".$.highBidderEmailId", highBidderEmailId);
			}
			if (bidder.has("UserID")) {
				highBidderUserId = bidder.getString("UserID");
				updateFields.put(siteName + ".$.highBidderUserId", highBidderUserId);
			}
		}
		updateFields.put(siteName + ".$.status", SIAInventoryStatus.BIDDING.toString());
		updateFields.put(siteName + ".$.failureReason", "");
		updateFields.put(siteName + ".$.timeLastUpdated", DateUtil.getSIADateFormat());
		table.updateOne(searchQuery, new Document("$set", updateFields));
	}
}