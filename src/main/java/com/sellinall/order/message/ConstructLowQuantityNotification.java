package com.sellinall.order.message;

import java.util.ArrayList;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.PostingSites;

public class ConstructLowQuantityNotification implements Processor {

	public void process(Exchange exchange) throws Exception {
		Map<String, JSONObject> skuDetailMap = (Map<String, JSONObject>) exchange.getProperty("skuDetailMap");
		ArrayList<BasicDBObject> inventoryListFromDB = (ArrayList<BasicDBObject>) exchange.getIn().getBody();
		exchange.getOut().setBody(null);
		JSONObject outBody = new JSONObject();
		JSONObject message = new JSONObject();
		JSONArray items = new JSONArray();
		for (BasicDBObject inventory : inventoryListFromDB) {
			ArrayList<String> nickNameIDs = getNickNameIDs(inventory, exchange);

			if (!nickNameIDs.isEmpty()) {
				JSONObject itemDetail = new JSONObject();
				String SKU = (String) inventory.get("SKU");
				if (skuDetailMap.containsKey(SKU)) {
					JSONObject skuDetail = skuDetailMap.get(SKU);
					itemDetail.put("SKU", SKU);
					if (inventory.containsField("customSKU")) {
						itemDetail.put("customSKU", inventory.get("customSKU"));
					}

					if (skuDetail.has("imageUrl")) {
						itemDetail.put("imageUrl", skuDetail.getString("imageUrl"));
					}
					itemDetail.put("title", skuDetail.getString("title"));
					itemDetail.put("nickNameIDs", nickNameIDs);
					items.put(itemDetail);
				}
			}
		}
		if (items.length() != 0) {
			message.put("items", items);
			message.put("isManaged", exchange.getProperty("isManaged"));
			outBody.put("accountNumber", exchange.getProperty("accountNumber"));
			outBody.put("merchantID", exchange.getProperty("merchantID"));
			outBody.put("userMessageName", (String) exchange.getIn().getHeader("userMessageName"));
			outBody.put("message", message);
			exchange.getOut().setBody(outBody);
		}
	}

	private ArrayList<String> getNickNameIDs(BasicDBObject inventory, Exchange exchange) {
		Map<String, BasicDBObject> nickNameObjectMap = (Map<String, BasicDBObject>) exchange
				.getProperty("nickNameObjectMap");
		String[] sitesList = PostingSites.getConfig().getSitesList();
		ArrayList<String> nickNameIDs = new ArrayList<String>();
		for (String site : sitesList) {
			if (inventory.containsField(site)) {
				ArrayList<BasicDBObject> siteInventoryList = (ArrayList<BasicDBObject>) inventory.get(site);
				for (BasicDBObject siteInventoryObject : siteInventoryList) {
					String nickNameID = siteInventoryObject.getString("nickNameID");
					if (nickNameObjectMap.containsKey(nickNameID)) {
						BasicDBObject siteAccountObject = (BasicDBObject) nickNameObjectMap.get(nickNameID);
						if (siteAccountObject.containsField("enableLowQuantityNotification")
								&& siteAccountObject.getBoolean("enableLowQuantityNotification")) {
							int thresholdQuantity = (siteAccountObject.containsField("lowQuantityThreshold"))
									? siteAccountObject.getInt("lowQuantityThreshold") : 0;
							if (siteInventoryObject.getInt("noOfItem") < thresholdQuantity) {
								nickNameIDs.add(siteInventoryObject.getString("nickNameID"));
							}
						}
					}
				}
			}
		}
		return nickNameIDs;
	}
}
