package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;
import com.sellinall.enums.SIAInventoryStatus;
import com.sellinall.order.enums.NotificationOrderActionStatus;

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
		
		JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
		
		int quantity = orderItemMessage.getInt("quantity");
		
		List<String> syncSites = new ArrayList<String>();
		BasicDBObject quantityIncDecModifier = new BasicDBObject();
		BasicDBObject quantitySetModifier = new BasicDBObject();
		processQuantityUpdates(notificationOrderActionStatus, orderMessage,
				inventoryDBRecord, quantity,
				syncSites, quantityIncDecModifier, quantitySetModifier);

		log.debug("updateInventoryRecord: Quantity: "+quantityIncDecModifier);
		if ( quantityIncDecModifier.isEmpty()) {
			syncSites.clear();
		} else {
			DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
			BasicDBObject searchQuery = new BasicDBObject();
			searchQuery.put("SKU", inventoryDBRecord.getString("SKU"));

			BasicDBObject queryToDB = new BasicDBObject();
			queryToDB.put("$inc", quantityIncDecModifier);
			if(!quantitySetModifier.isEmpty()){
				queryToDB.put("$set", quantitySetModifier);
			}
			table.update(searchQuery, queryToDB);
			log.debug("searchQuery: "+searchQuery+" queryToDB: "+queryToDB);
		}
		exchange.getOut().setBody(syncSites);
	}

	@SuppressWarnings("unchecked")
	private void processQuantityUpdates(
			NotificationOrderActionStatus notificationOrderActionStatus,
			JSONObject orderMessage, BasicDBObject inventoryDBRecord,
			int quantitySold, List<String> syncSites, BasicDBObject quantityIncDecModifier,
			BasicDBObject quantitySetModifier)
			throws JSONException {
		for (String siteName : siteNames ) {
			if (!inventoryDBRecord.containsField(siteName)) {
				continue;
			}
			if ( inventoryDBRecord.getBoolean("sync") ) {
				syncSites.add(siteName);
			}		
			ArrayList<BasicDBObject> siteSpecificList = (ArrayList<BasicDBObject>) inventoryDBRecord.get(siteName);
			Boolean hasSiteSpecificIndex = false;
			int siteSpecificIndex = 0;
			for (int index = 0; index < siteSpecificList.size(); index++) {
				BasicDBObject siteSpecific = siteSpecificList.get(index);
				if (!siteSpecific.getString("nickNameID").equals(orderMessage.getString("nickNameID"))) {
					if (inventoryDBRecord.getBoolean("sync")) {
						// Update other sites only if sync is true. Skip if the
						// site specific quantity is lesser than (overall
						// quantity - quantity sold).
						int invNoOfItem = inventoryDBRecord.getInt("noOfItem");
						int siteNoOfItem = siteSpecific.getInt("noOfItem");
						if ((invNoOfItem - quantitySold) >= siteNoOfItem) {
							continue;
						}
						int quantityDiff = quantitySold - (invNoOfItem - siteNoOfItem);
						if ( notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED)) {							
							if(siteSpecific.containsField("noOfItem") && siteNoOfItem > quantityDiff){
								incrementSetter(quantityIncDecModifier, siteName+"."+index+".noOfItem", -quantityDiff);
							} else {
								incrementSetter(quantitySetModifier, siteName+"."+index+".noOfItem", 0);
							}
						} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)) {
							// in case of cancel, ideally we should compare with
							// max allolwed quantity and decide whether to
							// increment or not. To be done in future.
							incrementSetter(quantityIncDecModifier, siteName+"."+index+".noOfItem", quantitySold);
						}
					}
				} else { // notification from this site
					// For already Bided inventory, the quantity(noOfItem) is updated for the first bid without a order been created.
					if (SIAInventoryStatus.BIDDING.equalsName(siteSpecific.getString("status"))
							|| SIAInventoryStatus.SOLDOUT.equalsName(siteSpecific.getString("status"))) {
						// skip the inventory update
						quantityIncDecModifier.clear();
						return;
					}
					siteSpecificIndex = index;
					hasSiteSpecificIndex = true;
				}
			}
			if ( notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED)) {
				incrementSetter(quantityIncDecModifier, "noOfItem", -quantitySold);
				incrementSetter(quantityIncDecModifier, "noOfItemsold", quantitySold);
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityIncDecModifier, siteName+"."+siteSpecificIndex+".noOfItem", -quantitySold);
					incrementSetter(quantityIncDecModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", quantitySold);
				}
			} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED)) {
				incrementSetter(quantityIncDecModifier, "noOfItem", quantitySold);
				incrementSetter(quantityIncDecModifier, "noOfItemsold", -quantitySold);
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityIncDecModifier, siteName+"."+siteSpecificIndex+".noOfItem", quantitySold);
					incrementSetter(quantityIncDecModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", -quantitySold);
				}
			}
		}
	}
	
	private void incrementSetter(BasicDBObject modifier, String key, int value) {
		modifier.append(key, value);
	}
}