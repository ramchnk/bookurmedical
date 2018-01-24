package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran
 *
 */

public class ConstructRequestType implements Processor {
	static Logger log = Logger.getLogger(ConstructRequestType.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject publishMessage = exchange.getProperty("publishMessage", JSONObject.class);
		publishMessage.remove("_id");
		if (exchange.getProperties().containsKey("publishTo")) {
			// for feemanagement createOrder & updateOrder
			if (exchange.getProperty("publishTo", String.class).equals("feeManagement")) {
				publishMessage.put("feeType", "order");
			}
			// for quickBooks createInvoice
			if (exchange.getProperty("publishTo", String.class).equals("quickbooks")) {
				publishMessage.put("requestType", "createInvoice");
				publishMessage.remove("feeType");
			}
			// for ninjaVan createOrder & updateOrder
			// for infor createOrder & updateOrder
			if (exchange.getProperty("publishTo", String.class).equals("ninjaVan")
					|| exchange.getProperty("publishTo", String.class).equals("infor")) {
				if (exchange.getProperty("isNewOrder", boolean.class)) {
					publishMessage.put("requestType", "createOrder");
				}
				if (!exchange.getProperty("isNewOrder", boolean.class)) {
					publishMessage.put("requestType", "updateOrder");
				}
				publishMessage.remove("feeType");
			}
		}
		log.debug("publishMessage " + publishMessage);
		exchange.getOut().setBody(publishMessage);

	}
}
