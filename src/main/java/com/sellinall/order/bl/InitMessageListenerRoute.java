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
		log.info(message);
		String messageType = ORDER;
		if (message.has("type")) {
			messageType = message.getString("type");
		}
		exchange.setProperty("messageType", messageType);
		exchange.setProperty("message", message);
		String nickNameID = message.getString("nickNameID");
		String siteName = nickNameID.split("-")[0];
		exchange.setProperty("siteName", siteName);
		exchange.setProperty("nickNameID", nickNameID);
		exchange.setProperty("accountNumber", message.get("accountNumber").toString());
		exchange.setProperty("updateStock", true);
		if (message.has("updateStock")) {
			exchange.setProperty("updateStock", message.getBoolean("updateStock"));
		}
		exchange.getOut().setBody(message);
	}
}
