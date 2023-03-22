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

import com.mongodb.client.MongoCollection;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.util.OrderUtil;
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
		JSONObject inventoryDBRecordJSON = OrderUtil.parseToJsonObject(Document.parse(exchange.getProperty("inventory", String.class)));
		Document inventoryDBRecord = Document.parse(inventoryDBRecordJSON.toString());
		JSONObject bidMessage = exchange.getProperty("message", JSONObject.class);
		Document updateInventoryQuantity = new Document();

		List<String> syncSites = new ArrayList<String>();
		Document quantityModifier = new Document();
		Document updateFields = new Document();
		Map<String, List<String>> siteMap = new HashMap<String, List<String>>();

		processQuantityUpdates(inventoryDBRecord, bidMessage, updateInventoryQuantity, syncSites, quantityModifier,
				updateFields, siteMap);
		Document searchQuery = new Document();
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
	private void processQuantityUpdates(Document inventoryDBRecord, JSONObject bidMessage,
			Document updateInventoryQuantity, List<String> syncSites, Document quantityModifier, Document updateFields,
			Map<String, List<String>> siteMap) throws JSONException {
		for (String siteName : siteNames) {
			List<String> nickNameList = new ArrayList<String>();
			if (!inventoryDBRecord.containsKey(siteName)) {
				continue;
			}
			ArrayList<Document> siteSpecificList = (ArrayList<Document>) inventoryDBRecord.get(siteName);
			Boolean hasSiteSpecificIndex = false;
			int siteSpecificIndex = 0;
			for (int index = 0; index < siteSpecificList.size(); index++) {
				Document siteSpecific = siteSpecificList.get(index);
				if (siteSpecific.containsKey("status")
						&& !siteSpecific.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())) {
					continue;
				}
				Boolean isNotificationFromThisNickNameID = false;

				// This case may happen for auction and buy it now sync with
				// different quantity
				if (siteSpecific.getInteger("noOfItem") <= 0) {
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
					int invNoOfItem = inventoryDBRecord.getInteger("noOfItem");
					int siteNoOfItem = siteSpecific.getInteger("noOfItem");
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
				Document valuesSet = new Document(siteName + "." + siteSpecificIndex + ".status",
						SIAInventoryStatus.BIDDING.toString());
				valuesSet.put(siteName + "." + siteSpecificIndex + ".highBidAmount",
						Document.parse(bidMessage.getJSONObject("bidAmount").toString()));
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

	private void incrementSetter(Document modifier, String key, int value, Document updateInventoryQuantity) {
		modifier.append(key, value);
		updateInventoryQuantity.put("$inc", modifier);
	}
}