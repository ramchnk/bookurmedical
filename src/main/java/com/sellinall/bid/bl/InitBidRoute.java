package com.sellinall.bid.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitBidRoute implements Processor {
	static Logger log = Logger.getLogger(InitBidRoute.class.getName());
	public void process(Exchange exchange) throws Exception {
		JSONObject message = exchange.getProperty("message", JSONObject.class);
		boolean isSyncRequired = message.getBoolean("sync");
		exchange.setProperty("isSyncRequired", isSyncRequired);
		exchange.getOut().setBody(message);
	}
}
