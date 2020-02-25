package com.sellinall.order.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class PrepareSyncPublishMessage implements Processor {

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecord = new JSONObject(exchange.getProperty("inventory", String.class));
		JSONObject publishMessage = new JSONObject();
		Object body = exchange.getIn().getBody();
		// publish updateItem msg to site
		if (body instanceof String) {
			String site = (String) body;
			Map<String, List<String>> siteMap = (HashMap<String, List<String>>) exchange.getProperty("siteMap");
			exchange.getOut().setHeader(site, true);
			publishMessage.put("siteNicknames", siteMap.get(site));
		} else {
			// publish updateItem msg to batch
			exchange.getOut().setHeader("batchProcessor", true);
			exchange.setProperty("batchDelayKey", "batchDelay0secKey");
		}
		publishMessage.put("requestType", "updateItem");
		List<String> syncFieldsToUpdate = new ArrayList<String>();
		syncFieldsToUpdate.add("quantity");
		publishMessage.put("isQuantityUpdateByNewOrder", true);
		publishMessage.put("fieldsToUpdate", syncFieldsToUpdate);
		publishMessage.put("SKU", inventoryDBRecord.getString("SKU"));
		publishMessage.put("accountNumber", inventoryDBRecord.getString("accountNumber"));
		exchange.setProperty("accountNumber", inventoryDBRecord.getString("accountNumber"));
		exchange.getOut().setBody(publishMessage);
	}
}
