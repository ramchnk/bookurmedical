package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raju
 *
 */

public class PrepareOrderEventPublishMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareOrderEventPublishMessage.class.getName());
	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		inBody.put("feeType", "order");
		exchange.getOut().setBody(inBody);
		log.debug(inBody);
		exchange.getOut().setHeader("feeManagement", true);
		log.debug("header set already");
	}
}
