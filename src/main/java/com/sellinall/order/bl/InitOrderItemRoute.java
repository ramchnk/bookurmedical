package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitOrderItemRoute implements Processor {
	static Logger log = Logger.getLogger(InitOrderItemRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderItemMessage = new JSONObject(exchange.getIn().getBody(String.class));
		exchange.setProperty("orderItemMessage", orderItemMessage);
		exchange.setProperty("hasSKU", false);
		if (orderItemMessage.has("SKU")) {
			exchange.setProperty("hasSKU", true);
		}
		exchange.getOut().setBody(orderItemMessage);
	}
}
