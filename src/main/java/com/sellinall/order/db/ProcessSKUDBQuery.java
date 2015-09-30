package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class ProcessSKUDBQuery implements Processor {

	static Logger log = Logger.getLogger(ProcessSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String inventoryString = exchange.getIn().getBody(String.class);
		JSONObject inventory = new JSONObject(inventoryString);
		if (inventory.isNull("SKU")) {
			// TODO handle the error here
			throw new Exception("Inventory Record not found");
		}
		exchange.setProperty("inventory", inventoryString);
	}
}
