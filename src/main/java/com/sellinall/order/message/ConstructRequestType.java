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
		exchange.setProperty("isEligibleToProceed", true);
		if (exchange.getProperties().containsKey("publishTo")) {
			String publishTo = exchange.getProperty("publishTo", String.class);
			// for feemanagement createOrder & updateOrder
			if (publishTo.equals("feeManagement")) {
				publishMessage.put("feeType", "order");
			}
			// for quickBooks createInvoice
			if (publishTo.equals("quickbooks")) {
				publishMessage.put("requestType", "createInvoice");
				publishMessage.remove("feeType");
			}
			// for ninjaVan createOrder & updateOrder
			// for infor createOrder & updateOrder
			if (publishTo.equals("ninjaVan") || publishTo.equals("infor") || publishTo.equals("satsaco")) {
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
