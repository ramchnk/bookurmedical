/**
 * 
 */
package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
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
		JSONObject orderMessage = new JSONObject(exchange.getProperty("orderMessage", String.class));
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		
		BasicDBObject updateInventoryQuantity = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", inventoryDBRecord.getString("SKU"));
		JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
		
		int quantity = orderItemMessage.getInt("quantity");
		if ( hasOrderInDB ) {
			BasicDBObject orderDBObject = exchange.getProperty("orderDBObject", BasicDBObject.class);
			List<BasicDBObject> orderItemsInDB = (List<BasicDBObject>) orderDBObject.get("orderItems");
			for (BasicDBObject orderItemInDB : orderItemsInDB) {
				if (orderItemInDB.getString("SKU").equals(inventoryDBRecord.getString("SKU"))) {
					quantity = orderItemInDB.getInt("quantity") - orderItemMessage.getInt("quantity");
				}
			}
		}
		
		List<String> syncSites = new ArrayList<String>();
		BasicDBObject quantityModifier = new BasicDBObject();

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
				if (siteSpecific.getString("nickNameID").equals(orderMessage.getString("nickNameID"))) {
					siteSpecificIndex = index;
					hasSiteSpecificIndex = true;
				}
				// TODO need re-factor here
				if ( inventoryDBRecord.getBoolean("sync")) {  // Update other sites only if sync true
					if ( notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING) ||
							notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ) {
						incrementSetter(quantityModifier, siteName+"."+index+".noOfItem", -quantity, updateInventoryQuantity);	
					} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED) ) {
						incrementSetter(quantityModifier, siteName+"."+index+".noOfItem", quantity, updateInventoryQuantity);
					}
				}
			}
			if ( notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING) ||
				 notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ) {							
				incrementSetter(quantityModifier, "noOfItem", -quantity, updateInventoryQuantity);
				incrementSetter(quantityModifier, "noOfItemsold", quantity, updateInventoryQuantity);			
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityModifier, siteName+"."+siteSpecificIndex+".noOfItem", -quantity, updateInventoryQuantity);	
					incrementSetter(quantityModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", quantity, updateInventoryQuantity);	
				}
			} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED) ) {
				incrementSetter(quantityModifier, "noOfItem", quantity, updateInventoryQuantity);
				incrementSetter(quantityModifier, "noOfItemsold", -quantity, updateInventoryQuantity);			
				if (hasSiteSpecificIndex) {
					incrementSetter(quantityModifier, siteName+"."+siteSpecificIndex+".noOfItem", quantity, updateInventoryQuantity);	
					incrementSetter(quantityModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", -quantity, updateInventoryQuantity);	
				}
			} 
		}
		
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		log.debug("searchQuery:"+searchQuery);
		log.debug("updateInventoryRecord: Quantity: "+updateInventoryQuantity);
		if ( quantityModifier.isEmpty() ) {
			syncSites.clear();
		} else {
			table.update(searchQuery, new BasicDBObject("$inc", quantityModifier));
		}
		exchange.getOut().setBody(syncSites);
	}
	
	private void incrementSetter(BasicDBObject modifier, String key, int value, BasicDBObject updateInventoryQuantity) {
		modifier.append(key, value);
		updateInventoryQuantity.put("$inc", modifier);
	}
}