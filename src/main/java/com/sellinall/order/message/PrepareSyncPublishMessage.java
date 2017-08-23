package com.sellinall.order.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

public class PrepareSyncPublishMessage implements Processor {

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		List<String> syncSites = (ArrayList<String>) exchange.getIn().getBody();
		if (syncSites.isEmpty()) {
			return;
		}
		for ( String site : syncSites) {
			exchange.getOut().setHeader(site, true);
		}
		JSONObject inventoryDBRecord = new JSONObject(exchange.getProperty("inventory", String.class));
		JSONObject publishMessage = new JSONObject();
		publishMessage.put("requestType", "updateItem");
		List<String> syncFieldsToUpdate = new ArrayList<String>();
		syncFieldsToUpdate.add("quantity");
		publishMessage.put("fieldsToUpdate", syncFieldsToUpdate);
		publishMessage.put("SKU", inventoryDBRecord.getString("SKU"));
		publishMessage.put("accountNumber", inventoryDBRecord.getString("accountNumber"));
		exchange.setProperty("accountNumber", inventoryDBRecord.getString("accountNumber"));
		exchange.setProperty("batchDelayKey", null);
		//set the batch delay key only when sites need to be synced (that is, only for new orders)
		if (syncSites.size() > 0) {
			exchange.setProperty("batchDelayKey", "batchDelay0secKey");
		}
		exchange.getOut().setBody(publishMessage);
	}
}
