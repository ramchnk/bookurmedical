package com.sellinall.order.db;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class ProcessSKUDBQuery implements Processor {

	static Logger log = Logger.getLogger(ProcessSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String inventoryString = exchange.getIn().getBody(String.class);
		exchange.setProperty("hasInventoryInDB", false);
		if (inventoryString == null) {
			log.debug("Inventory Record - may be deleted in our DB : " + inventoryString);
			return;
		}
		JSONObject inventory = new JSONObject(inventoryString);
		if (inventory.isNull("SKU")) {
			throw new Exception("Inventory record doesn't exists for this SKU : "+ 
				exchange.getProperty("SKU", String.class));
		}
		exchange.setProperty("hasInventoryInDB", true);
		exchange.setProperty("inventory", inventoryString);
		extractInventoryValues(exchange, inventory);
	}

	@SuppressWarnings("unchecked")
	private void extractInventoryValues(Exchange exchange, JSONObject inventory)
			throws JSONException {
		Map<String, BasicDBObject> inventoryDetailsMap = new HashMap<String, BasicDBObject>();
		if (exchange.getProperties().containsKey("inventoryDetailsMap")) {
			inventoryDetailsMap = (Map<String, BasicDBObject>) exchange.getProperty("inventoryDetailsMap");
		}
		String siteName = exchange.getProperty("siteName", String.class);
		BasicDBObject inventoryValues = new BasicDBObject();
		inventoryValues.put("itemTitle", inventory.getString("itemTitle"));
		inventoryValues.put("imageURL", inventory.getString("imageURL"));

		if (inventory.has("customSKU")) {
			inventoryValues.put("customSKU", inventory.getString("customSKU"));
		}
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		JSONArray siteSpecificList = inventory.getJSONArray(siteName);
		BasicDBObject site = null;
		for (int index = 0; index < siteSpecificList.length(); index++) {
			JSONObject siteJSON = siteSpecificList.getJSONObject(index);
			if (siteJSON.getString("nickNameID").equals(orderMessage.getString("nickNameID"))){
				site = (BasicDBObject) JSON.parse(siteJSON.toString());
				break;
			}
		}
		inventoryValues.put(siteName, site);
		if ( inventory.has("variantDetails") ) {
			BasicDBList variants = new BasicDBList();
			JSONArray invVariants = inventory.getJSONArray("variantDetails");
			for (int i = 0 ; i < invVariants.length(); i ++) {
				JSONObject variant = invVariants.getJSONObject(i);
				BasicDBObject bVariant = new BasicDBObject();
				bVariant.put("title", variant.getString("title"));
				bVariant.put("name", variant.getString("name"));
				variants.add(bVariant);
			}
			inventoryValues.put("variantDetails", variants);
		}
		inventoryDetailsMap.put(inventory.getString("SKU"), inventoryValues);
		exchange.setProperty("inventoryDetailsMap", inventoryDetailsMap);
	}
}
