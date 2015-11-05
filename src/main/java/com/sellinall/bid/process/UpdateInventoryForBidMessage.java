/**
 * 
 */
package com.sellinall.bid.process;

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
import com.mudra.sellinall.util.DateUtil;
import com.sellinall.database.DbUtilities;
import com.sellinall.enums.SIAInventoryStatus;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateInventoryForBidMessage implements Processor {
	private static final int BID_QUANTITY = 1;
	static Logger log = Logger.getLogger(UpdateInventoryForBidMessage.class.getName());
	static String siteNames[] = PostingSites.getConfig().getSitesList() ;

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		
		JSONObject inventoryDBRecordJSON = new JSONObject(exchange.getIn().getBody(String.class));	
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		JSONObject bidMessage = exchange.getProperty("message", JSONObject.class);
		BasicDBObject updateInventoryQuantity = new BasicDBObject();
		
		List<String> syncSites = new ArrayList<String>();
		BasicDBObject quantityModifier = new BasicDBObject();
		BasicDBObject updateFields = new BasicDBObject();

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
				
				// This case may happen for auction and buy it now sync with different quantity
				if ( siteSpecific.getInt("noOfItem") <= 0) {
					continue; 
				}
				
				if (siteSpecific.getString("nickNameID").equals(bidMessage.getString("nickNameID"))) {
					siteSpecificIndex = index;
					hasSiteSpecificIndex = true;
				}
				// TODO need re-factor here
				if ( inventoryDBRecord.getBoolean("sync")) {  // Update other sites only if sync true
					incrementSetter(quantityModifier, siteName+"."+index+".noOfItem", -BID_QUANTITY, updateInventoryQuantity);
				}
			}
			incrementSetter(quantityModifier, "noOfItem", -BID_QUANTITY, updateInventoryQuantity);
			incrementSetter(quantityModifier, "noOfItemsold", BID_QUANTITY, updateInventoryQuantity);			
			if (hasSiteSpecificIndex) {
				incrementSetter(quantityModifier, siteName+"."+siteSpecificIndex+".noOfItem", -BID_QUANTITY, updateInventoryQuantity);	
				incrementSetter(quantityModifier, siteName+"."+siteSpecificIndex+".noOfItemsold", BID_QUANTITY, updateInventoryQuantity);
				BasicDBObject valuesSet = new BasicDBObject(siteName+"."+siteSpecificIndex+".status", SIAInventoryStatus.BIDDING);
				valuesSet.put(siteName+"."+siteSpecificIndex+".failureReason", "");
				valuesSet.put(siteName+"."+siteSpecificIndex+".timeLastUpdated", DateUtil.getSIADateFormat());
				updateFields.put("$set", valuesSet);
			}
		}

		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", exchange.getProperty("SKU", String.class));
		DBCollection table = DbUtilities.getInventoryDBCollection("inventory");
		log.debug("searchQuery:"+searchQuery);
		if ( quantityModifier.isEmpty() ) {
			syncSites.clear();
		} else {
			updateFields.put("$inc", quantityModifier);
			table.update(searchQuery, updateFields);
		}
		exchange.getOut().setBody(syncSites);
	}
	
	private void incrementSetter(BasicDBObject modifier, String key, int value, BasicDBObject updateInventoryQuantity) {
		modifier.append(key, value);
		updateInventoryQuantity.put("$inc", modifier);
	}
}