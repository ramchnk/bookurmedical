package com.sellinall.bid.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateInventoryForBidMessage implements Processor {
	private static final int BID_QUANTITY = 1;
	static Logger log = Logger.getLogger(UpdateInventoryForBidMessage.class.getName());
	static String siteNames[] = PostingSites.getConfig().getSitesList();

	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = new JSONObject(exchange.getProperty("inventory", String.class));
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		JSONObject bidMessage = exchange.getProperty("message", JSONObject.class);
		BasicDBObject updateInventoryQuantity = new BasicDBObject();

		List<String> syncSites = new ArrayList<String>();
		BasicDBObject quantityModifier = new BasicDBObject();
		BasicDBObject updateFields = new BasicDBObject();
		Map<String,List<String>> siteMap = new HashMap<String,List<String>>();

		processQuantityUpdates(inventoryDBRecord, bidMessage, updateInventoryQuantity, syncSites, quantityModifier,
				updateFields, siteMap);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", exchange.getProperty("SKU", String.class));
		searchQuery.put("accountNumber", bidMessage.getString("accountNumber"));
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("inventory");
		if (quantityModifier.isEmpty()) {
			syncSites.clear();
		} else {
			exchange.setProperty("isPublishSyncMsgToBatch", true);
			exchange.setProperty("siteMap", siteMap);
			updateFields.put("$inc", quantityModifier);
			table.updateOne(searchQuery, updateFields);
		}
		log.debug("searchQuery:" + searchQuery + " updateFields: " + updateFields);
		exchange.getOut().setBody(syncSites);
	}

	@SuppressWarnings("unchecked")
	private void processQuantityUpdates(BasicDBObject inventoryDBRecord, JSONObject bidMessage,
			BasicDBObject updateInventoryQuantity, List<String> syncSites, BasicDBObject quantityModifier,
			BasicDBObject updateFields, Map<String, List<String>> siteMap) throws JSONException {
		for (String siteName : siteNames) {
			List<String> nickNameList = new ArrayList<String>();
			if (!inventoryDBRecord.containsField(siteName)) {
				continue;
			}
			ArrayList<BasicDBObject> siteSpecificList = (ArrayList<BasicDBObject>) inventoryDBRecord.get(siteName);
			Boolean hasSiteSpecificIndex = false;
			int siteSpecificIndex = 0;
			for (int index = 0; index < siteSpecificList.size(); index++) {
				BasicDBObject siteSpecific = siteSpecificList.get(index);
				Boolean isNotificationFromThisNickNameID = false;

				// This case may happen for auction and buy it now sync with
				// different quantity
				if (siteSpecific.getInt("noOfItem") <= 0) {
					continue;
				}

				if (siteSpecific.getString("nickNameID").equals(bidMessage.getString("nickNameID"))) {
					siteSpecificIndex = index;
					hasSiteSpecificIndex = true;
					isNotificationFromThisNickNameID = true;
				}

				if (inventoryDBRecord.getBoolean("sync") && !isNotificationFromThisNickNameID) {
					// Update other sites only if sync is true. Skip if the site
					// specific quantity is lesser than (overall quantity - bid
					// quantity).
					int invNoOfItem = inventoryDBRecord.getInt("noOfItem");
					int siteNoOfItem = siteSpecific.getInt("noOfItem");
					if ((invNoOfItem - BID_QUANTITY) >= siteNoOfItem) {
						continue;
					}
					int quantityDiff = BID_QUANTITY - (invNoOfItem - siteNoOfItem);
					incrementSetter(quantityModifier, siteName + "." + index + ".noOfItem", -quantityDiff,
							updateInventoryQuantity);
					nickNameList.add(siteSpecific.getString("nickNameID"));
				}
			}
			if (hasSiteSpecificIndex) {
				incrementSetter(quantityModifier, siteName + "." + siteSpecificIndex + ".noOfItem", -BID_QUANTITY,
						updateInventoryQuantity);
				incrementSetter(quantityModifier, siteName + "." + siteSpecificIndex + ".noOfItemsold", BID_QUANTITY,
						updateInventoryQuantity);
				BasicDBObject valuesSet = new BasicDBObject(siteName + "." + siteSpecificIndex + ".status",
						SIAInventoryStatus.BIDDING.toString());
				valuesSet.put(siteName + "." + siteSpecificIndex + ".highBidAmount",
						(BasicDBObject) JSON.parse(bidMessage.getJSONObject("bidAmount").toString()));
				if (bidMessage.has("bidder")) {
					String highBidderEmailId = "";
					String highBidderUserId = "";
					JSONObject bidder = bidMessage.getJSONObject("bidder");
					if (bidder.has("Email")) {
						highBidderEmailId = bidder.getString("Email");
						valuesSet.put(siteName + "." + siteSpecificIndex + ".highBidderEmailId", highBidderEmailId);
					}
					if (bidder.has("UserID")) {
						highBidderUserId = bidder.getString("UserID");
						valuesSet.put(siteName + "." + siteSpecificIndex + ".highBidderUserId", highBidderUserId);
					}
				}
				valuesSet.put(siteName + "." + siteSpecificIndex + ".failureReason", "");
				valuesSet.put(siteName + "." + siteSpecificIndex + ".timeLastUpdated", DateUtil.getSIADateFormat());
				updateFields.put("$set", valuesSet);
			}
			if (inventoryDBRecord.getBoolean("sync") && !nickNameList.isEmpty()) {
				syncSites.add(siteName);
				siteMap.put(siteName, nickNameList);
			}

		}
		incrementSetter(quantityModifier, "noOfItem", -BID_QUANTITY, updateInventoryQuantity);
		incrementSetter(quantityModifier, "noOfItemsold", BID_QUANTITY, updateInventoryQuantity);
	}

	private void incrementSetter(BasicDBObject modifier, String key, int value, BasicDBObject updateInventoryQuantity) {
		modifier.append(key, value);
		updateInventoryQuantity.put("$inc", modifier);
	}
}