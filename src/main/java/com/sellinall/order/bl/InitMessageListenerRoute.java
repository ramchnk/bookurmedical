package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

public class InitMessageListenerRoute implements Processor {
	private static final String ORDER = "order";
	static Logger log = Logger.getLogger(InitMessageListenerRoute.class.getName());
	public void process(Exchange exchange) throws Exception {
		JSONObject message = new JSONObject(exchange.getIn().getBody(String.class));
		String messageType = ORDER;
		if ( message.has("type")) {
			messageType = message.getString("type");
		}
		exchange.setProperty("messageType", messageType);
		exchange.setProperty("message", message);
		exchange.getOut().setBody(message);
	}
}
