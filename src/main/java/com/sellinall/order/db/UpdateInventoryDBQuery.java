/**
 * 
 */
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
import com.sellinall.order.enums.NotificationOrderActionStatus;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateInventoryDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateInventoryDBQuery.class.getName());
	static String siteNames[] = PostingSites.getConfig().getSitesList() ;
	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = new JSONObject(exchange.getIn().getBody(String.class));
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange.getProperty("notificationOrderActionStatus");
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", inventoryDBRecord.getString("SKU"));
		JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
		
		int quantity = orderItemMessage.getInt("quantity");
		
		List<String> syncSites = new ArrayList<String>();
		BasicDBObject quantityModifier = new BasicDBObject();
		BasicDBObject quantitySetModifier = new BasicDBObject();
		processQuantityUpdates(notificationOrderActionStatus, orderMessage,
				inventoryDBRecord, quantity,
				syncSites, quantityModifier, quantitySetModifier);

		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		log.debug("searchQuery:"+searchQuery);
		log.debug("updateInventoryRecord: Quantity: "+quantityModifier);
		if ( quantityModifier.isEmpty()) {
			syncSites.clear();
		} else {
			BasicDBObject queryToDB = new BasicDBObject();
			queryToDB.put("$inc", quantityModifier);
			if(!quantitySetModifier.isEmpty()){
				queryToDB.put("$set", quantitySetModifier);
			}
			table.update(searchQuery, queryToDB);
		}
		exchange.getOut().setBody(syncSites);
	}

	@SuppressWarnings("unchecked")
	private void processQuantityUpdates(
			NotificationOrderActionStatus notificationOrderActionStatus,
			JSONObject orderMessage, BasicDBObject inventoryDBRecord,
			int quantity, List<String> syncSites, BasicDBObject quantityIncModifier,
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
					if ( inventoryDBRecord.getBoolean("sync")) {  // Update other sites only if sync true
						// skip auction site quantity update, if we have more than one quantity
						if (siteSpecific.containsField("auction") && siteSpecific.getBoolean("auction") &&
							inventoryDBRecord.getInt("noOfItem") > quantity ) {
							continue;
						}
						if ( notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING) ||
								notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ) {
							if(siteSpecific.containsField("noOfItem") && siteSpecific.getInt("noOfItem") > quantity){
								incrementSetter(quantityIncModifier, siteName+"."+index+".noOfItem", -quantity);
							} else {
								incrementSetter(quantitySetModifier, siteName+"."+index+".noOfItem", 0);
							}
						} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED) ) {
							incrementSetter(quantityIncModifier, siteName+"."+index+".noOfItem", quantity);
						}
					}
				} else {
					siteSpecificIndex = index;
					hasSiteSpecificIndex = true;
				}
			}
			if ( notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING) ||
					notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ) {
				incrementSetter(quantityIncModifier, "noOfItem", -quantity);
				incrementSetter(quantityIncModifier, "noOfItemsold", quantity);
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityIncModifier, siteName+"."+siteSpecificIndex+".noOfItem", -quantity);
					incrementSetter(quantityIncModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", quantity);
				}
			} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED) ) {
				incrementSetter(quantityIncModifier, "noOfItem", quantity);
				incrementSetter(quantityIncModifier, "noOfItemsold", -quantity);
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityIncModifier, siteName+"."+siteSpecificIndex+".noOfItem", quantity);
					incrementSetter(quantityIncModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", -quantity);
				}
			}
		}
	}
	
	private void incrementSetter(BasicDBObject modifier, String key, int value) {
		modifier.append(key, value);
	}
}