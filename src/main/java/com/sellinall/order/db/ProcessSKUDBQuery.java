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
		JSONArray inventoryList = new JSONArray(inventoryString);
		String SKU = exchange.getProperty("SKU", String.class);
		JSONObject inventory = getInventoryBySKU(inventoryList, SKU);
		if (inventory != null) {
			String itemTitle = "";
			JSONObject parentInventory = new JSONObject();
			if (inventoryList.length() > 1) {
				parentInventory = getInventoryBySKU(inventoryList, SKU.split("-")[0]);
				itemTitle = parentInventory.getString("itemTitle");
			}

			if (inventory.has("itemTitle")) {
				itemTitle = inventory.getString("itemTitle");
			}
			exchange.setProperty("hasInventoryInDB", true);
			exchange.setProperty("inventory", inventory.toString());
			extractInventoryValues(exchange, inventory, itemTitle, parentInventory);
		}
		if (exchange.getProperty("messageType", String.class).equals("order")) {
			JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
			exchange.setProperty("quantity", orderItemMessage.getInt("quantity"));
		}
	}

	@SuppressWarnings("unchecked")
	private void extractInventoryValues(Exchange exchange, JSONObject inventory, String itemTitle, JSONObject parentInventory) throws JSONException {
		Map<String, BasicDBObject> inventoryDetailsMap = new HashMap<String, BasicDBObject>();
		if (exchange.getProperties().containsKey("inventoryDetailsMap")) {
			inventoryDetailsMap = (Map<String, BasicDBObject>) exchange.getProperty("inventoryDetailsMap");
		}
		String siteName = exchange.getProperty("siteName", String.class);
		BasicDBObject inventoryValues = new BasicDBObject();
		inventoryValues.put("itemTitle", itemTitle);
		if (inventory.has("imageURL")) {
			inventoryValues.put("imageURL", inventory.getString("imageURL"));
		}
		if (inventory.has("customSKU")) {
			String customSKU = inventory.getString("customSKU");
			inventoryValues.put("customSKU", customSKU);
			exchange.setProperty("customSKU", customSKU);
		}
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		JSONArray siteSpecificList = inventory.getJSONArray(siteName);
		BasicDBObject site = null;
		for (int index = 0; index < siteSpecificList.length(); index++) {
			JSONObject siteJSON = siteSpecificList.getJSONObject(index);
			if (siteJSON.getString("nickNameID").equals(orderMessage.getString("nickNameID"))) {
				// If variant record has no image, get the parent image.
				site = BasicDBObject.parse(siteJSON.toString());
				if ((site.containsField("imageURI") && siteJSON.getJSONArray("imageURI").length() == 0)
						|| !site.containsField("imageURI")) {
					if (parentInventory.has("imageURI")) {
						site.put("imageURI", JSON.parse(parentInventory.getJSONArray("imageURI").toString()));
					}
				}
				break;
			}
		}
		if (site != null && parentInventory.length() != 0) {
			JSONArray parentSiteSpecificList = parentInventory.getJSONArray(siteName);
			BasicDBObject parentSite = null;
			for (int index = 0; index < parentSiteSpecificList.length(); index++) {
				JSONObject parentSiteJSON = parentSiteSpecificList.getJSONObject(index);
				if (parentSiteJSON.getString("nickNameID").equals(orderMessage.getString("nickNameID"))) {
					parentSite = (BasicDBObject) JSON.parse(parentSiteJSON.toString());
					if (parentSite.containsField("categoryName")) {
						site.put("categoryName", parentSite.get("categoryName"));
					}
					if (parentSite.containsField("categoryID")) {
						site.put("categoryID", parentSite.get("categoryID"));
					}
					break;
				}
			}
		}
		inventoryValues.put(siteName, site);
		for (int i = 0; i < siteSpecificList.length(); i++) {
			JSONObject channelObj = siteSpecificList.getJSONObject(i);
			if (channelObj.getString("nickNameID").equals(orderMessage.getString("nickNameID"))) {
				BasicDBList variants = new BasicDBList();
				JSONArray invVariants = new JSONArray();
				if (channelObj.has("variantDetails")) {
					invVariants = channelObj.getJSONArray("variantDetails");
				} else if (inventory.has("variantDetails")) {
					invVariants = inventory.getJSONArray("variantDetails");
				}
				for (int j = 0; j < invVariants.length(); j++) {
					JSONObject variant = invVariants.getJSONObject(j);
					BasicDBObject bVariant = new BasicDBObject();
					bVariant.put("title", variant.getString("title"));
					bVariant.put("name", variant.getString("name"));
					variants.add(bVariant);
				}
				if (variants.size() > 0) {
					inventoryValues.put("variantDetails", variants);
				}
				break;
			}
		}
		inventoryDetailsMap.put(inventory.getString("SKU"), inventoryValues);
		exchange.setProperty("inventoryDetailsMap", inventoryDetailsMap);
	}

	private JSONObject getInventoryBySKU(JSONArray inventoryList, String SKU) throws Exception {
		for (int i = 0; i < inventoryList.length(); i++) {
			JSONObject inventory = inventoryList.getJSONObject(i);
			if (inventory.isNull("SKU")) {
				throw new Exception("Inventory record doesn't exists for this SKU : " + SKU);
			}
			if (inventory.getString("SKU").equals(SKU)) {
				return inventory;
			}
		}
		return null;
	}
}