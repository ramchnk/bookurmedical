package com.sellinall.order.db;

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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.SIAInventoryStatus;
import com.sellinall.util.enums.SIAOrderCancelReasons;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateInventoryDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateInventoryDBQuery.class.getName());
	static String siteNames[] = PostingSites.getConfig().getSitesList() ;
	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = new JSONObject(exchange.getProperty("inventory", String.class));
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange.getProperty("notificationOrderActionStatus");
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		String SKU = inventoryDBRecord.getString("SKU");
		exchange.setProperty("SKU", SKU);

		boolean isMultipleUnitSKUUpdate = (exchange.getProperties().containsKey("isMultipleUnitSKUUpdate")
				&& exchange.getProperty("isMultipleUnitSKUUpdate", Boolean.class));
		int quantity = exchange.getProperty("quantity", Integer.class);
		boolean syncInventory = exchange.getProperty("syncInventory", Boolean.class);
		List<String> syncSites = new ArrayList<String>();
		BasicDBObject quantityIncDecModifier = new BasicDBObject();
		BasicDBObject quantitySetModifier = new BasicDBObject();
		Map<String,List<String>> siteMap = new HashMap<String,List<String>>();
		processQuantityUpdates(notificationOrderActionStatus, orderMessage, inventoryDBRecord, quantity, syncSites,
				quantityIncDecModifier, quantitySetModifier, syncInventory, siteMap, exchange, isMultipleUnitSKUUpdate);
		log.debug("updateInventoryRecord: Quantity: "+quantityIncDecModifier);
		if ( quantityIncDecModifier.isEmpty()) {
			syncSites.clear();
		} else {
			exchange.setProperty("quantityModified", true);
			exchange.setProperty("isPublishSyncMsgToBatch", true);
			exchange.setProperty("siteMap", siteMap);
			MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("inventory");
			BasicDBObject searchQuery = new BasicDBObject();
			searchQuery.put("SKU", SKU);

			BasicDBObject queryToDB = new BasicDBObject();
			queryToDB.put("$inc", quantityIncDecModifier);
			if(!quantitySetModifier.isEmpty()){
				queryToDB.put("$set", quantitySetModifier);
			}
			log.debug("searchQuery: " + searchQuery + " queryToDB: " + queryToDB);
			FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
			options.projection(new BasicDBObject("noOfItem", 1));
			options.returnDocument(ReturnDocument.AFTER);
			Document inventoryDoc = table.findOneAndUpdate(searchQuery, queryToDB, options);
			BasicDBObject result = (BasicDBObject) JSON.parse(inventoryDoc.toJson());
			if (exchange.getProperties().containsKey("processBasicUnitSKU")
					&& exchange.getProperty("processBasicUnitSKU", Boolean.class) && result != null) {
				exchange.setProperty("basicUnitQuantity", result.getInt("noOfItem"));
			}

		}
		exchange.getOut().setBody(syncSites);
	}



	@SuppressWarnings("unchecked")
	private void processQuantityUpdates(NotificationOrderActionStatus notificationOrderActionStatus,
			JSONObject orderMessage, BasicDBObject inventoryDBRecord, int quantitySold, List<String> syncSites,
			BasicDBObject quantityIncDecModifier, BasicDBObject quantitySetModifier, boolean syncInventory,
			Map<String, List<String>> siteMap, Exchange exchange, boolean isMultipleUnitSKUUpdate) throws JSONException {
		boolean isOutOfStock = false;
		boolean newOrder = OrderUtil.checkIsNewOrder(notificationOrderActionStatus);
		boolean cancelledOrder = OrderUtil.checkIsCancelledOrder(notificationOrderActionStatus);
		if (cancelledOrder && orderMessage.has("cancelDetails")) {
			JSONObject cancelDetails = orderMessage.getJSONObject("cancelDetails");
			//SELLER_UNABLE_TO_RESERVE_STOCK usually came from lazada for which no need to decrement the stock.
			if (cancelDetails.has("cancelReason") && !cancelDetails.getString("cancelReason").isEmpty()
					&& (cancelDetails.getString("cancelReason").equals(SIAOrderCancelReasons.OUT_OF_STOCK.toString())
							|| cancelDetails.getString("cancelReason")
							.equals(SIAOrderCancelReasons.SELLER_UNABLE_TO_RESERVE_STOCK.toString()))) {
				isOutOfStock = true;
			}
		}
		boolean isPublishDuplicatSKUS = false;
		if (exchange.getProperties().containsKey("publishDuplicatSKUS")) {
			isPublishDuplicatSKUS = exchange.getProperty("publishDuplicatSKUS", boolean.class);
		}
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
				if (siteSpecific.containsField("status")
						&& !siteSpecific.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())) {
					continue;
				}
				boolean isSameSite = siteSpecific.getString("nickNameID").equals(orderMessage.getString("nickNameID"));
				if (isPublishDuplicatSKUS || !isSameSite || isMultipleUnitSKUUpdate) {
					nickNameList.add(siteSpecific.getString("nickNameID"));
				}
				if (!isSameSite) {
					if ((inventoryDBRecord.getBoolean("sync") && syncInventory) || isMultipleUnitSKUUpdate) {
						// Update other sites only if sync is true. Skip if the
						// site specific quantity is lesser than (overall
						// quantity - quantity sold).
						int invNoOfItem = inventoryDBRecord.getInt("noOfItem");
						int siteNoOfItem = siteSpecific.getInt("noOfItem");
						if ((invNoOfItem - quantitySold) >= siteNoOfItem) {
							continue;
						}
						int quantityDiff = quantitySold - (invNoOfItem - siteNoOfItem);
						if (newOrder) {
							if (siteSpecific.containsField("noOfItem") && siteNoOfItem > quantityDiff) {
								incrementSetter(quantityIncDecModifier, siteName + "." + index + ".noOfItem",
										-quantityDiff);
							} else {
								incrementSetter(quantitySetModifier, siteName + "." + index + ".noOfItem", 0);
							}
						} else if (cancelledOrder) {
							// in case of cancel, ideally we should compare with
							// max allolwed quantity and decide whether to
							// increment or not. To be done in future.
							if (isOutOfStock) {
								incrementSetter(quantitySetModifier, siteName + "." + index + ".noOfItem", 0);
							} else {
								incrementSetter(quantityIncDecModifier, siteName + "." + index + ".noOfItem",
										quantitySold);
							}
						}
					}
				} else { // notification from this site
					// For already Bided inventory, the quantity(noOfItem) is
					// updated for the first bid without a order been created.
					if (SIAInventoryStatus.BIDDING.equalsName(siteSpecific.getString("status"))) {
						// skip the inventory update
						quantityIncDecModifier.clear();
						return;
					}
					siteSpecificIndex = index;
					hasSiteSpecificIndex = true;
				}
				quantitySetModifier.append(siteName + "." + index + ".lastSoldTime", System.currentTimeMillis()/1000L);
			}
			if (newOrder) {
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityIncDecModifier, siteName + "." + siteSpecificIndex + ".noOfItem",
							-quantitySold);
					incrementSetter(quantityIncDecModifier, siteName + "." + siteSpecificIndex + ".noOfItemsold",
							quantitySold);
				}
			} else if (cancelledOrder) {
				if (hasSiteSpecificIndex) {
					if (isOutOfStock) {
						incrementSetter(quantitySetModifier, siteName + "." + siteSpecificIndex + ".noOfItem", 0);
					} else {
						incrementSetter(quantityIncDecModifier, siteName + "." + siteSpecificIndex + ".noOfItem",
								quantitySold);
					}
					incrementSetter(quantityIncDecModifier, siteName + "." + siteSpecificIndex + ".noOfItemsold",
							-quantitySold);
				}
			}
			if (inventoryDBRecord.getBoolean("sync") && syncInventory && !nickNameList.isEmpty()) {
				syncSites.add(siteName);
				siteMap.put(siteName, nickNameList);
			}
		}
		if (newOrder) {
			incrementSetter(quantityIncDecModifier, "noOfItem", -quantitySold);
			incrementSetter(quantityIncDecModifier, "noOfItemsold", quantitySold);
		} else if (cancelledOrder) {
			if (isOutOfStock) {
				incrementSetter(quantitySetModifier, "noOfItem", 0);
			} else {
				incrementSetter(quantityIncDecModifier, "noOfItem", quantitySold);
			}
			incrementSetter(quantityIncDecModifier, "noOfItemsold", -quantitySold);
		}
	}
	
	private void incrementSetter(BasicDBObject modifier, String key, int value) {
		modifier.append(key, value);
	}
}