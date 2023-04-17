package com.sellinall.order.message;

import java.math.BigDecimal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InitializeJournalMessage implements Processor {
	static Logger log = Logger.getLogger(InitializeJournalMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		JSONArray eventMessagesArray = new JSONArray();
		JSONObject eventMessages = new JSONObject();
		eventMessages.put("orderID", orderMessage.get("orderID").toString());
		eventMessages.put("accountNumber", orderMessage.get("accountNumber").toString());
		eventMessages.put("nickNameID", orderMessage.getString("nickNameID"));
		eventMessages.put("channel", orderMessage.getString("site"));
		if (orderMessage.has("addendum")) {
			JSONObject addendum = orderMessage.getJSONObject("addendum");
			if (addendum.has("eventTime")) {
				eventMessages.put("eventTime", new BigDecimal(addendum.get("eventTime").toString()).longValue());
			}
			if (addendum.has("adjustedBy")) {
				eventMessages.put("adjustedBy", addendum.getString("adjustedBy"));
			}
			if (addendum.has("eventType")) {
				eventMessages.put("orderEventType", addendum.getString("eventType"));
			}
		}
		JSONArray journalMessageArray = exchange.getProperty("journalMessage", JSONArray.class);
		eventMessages.put("addendum", journalMessageArray);
		eventMessagesArray.put(eventMessages);
		exchange.getOut().setBody(eventMessagesArray);
	}

}