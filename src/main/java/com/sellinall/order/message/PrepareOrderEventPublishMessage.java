package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raju
 *
 */
public class PrepareOrderEventPublishMessage implements Processor {

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		exchange.getOut().setBody(inBody);
		exchange.getOut().setHeader("feeManagement", true);
	}
}
