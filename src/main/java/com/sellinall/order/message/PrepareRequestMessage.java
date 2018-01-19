package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

/**
 * 
 * @author Raguvaran
 *
 */

public class PrepareRequestMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareRequestMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderRecord = new JSONObject(exchange.getProperty("orderRecord", BasicDBObject.class).toString());

		orderRecord.put("merchantID", exchange.getProperty("merchantID", String.class));
		if(exchange.getProperties().containsKey("countryCode")){
			orderRecord.put("countryCode", exchange.getProperty("countryCode"));
		}
		// prepare publish message to fee management server
		exchange.setProperty("publishMessage", orderRecord);		
		// prepare publish message for create in quickbooks server
		exchange.setProperty("publishToQuickBooks", false);
		if (exchange.getProperty("isNewOrder", boolean.class)) {
			Boolean isAccountingChannel = exchange.getProperty("isAccountingChannel", Boolean.class);
			if (isAccountingChannel) {
				exchange.setProperty("publishToQuickBooks", true);
			}
		}
		// prepare publish message for create & update in ninjaVan server
		Boolean isNinjaVanShippingCarrier = exchange.getProperty("isNinjaVanShippingCarrier", Boolean.class);
		exchange.setProperty("publishToNinjaVan", false);
		if (isNinjaVanShippingCarrier) {
			exchange.setProperty("publishToNinjaVan", true);
		}
	}
}