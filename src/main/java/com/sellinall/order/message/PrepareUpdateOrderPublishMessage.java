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

public class PrepareUpdateOrderPublishMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareUpdateOrderPublishMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject publishMessage = new JSONObject(exchange.getProperty("orderRecord", BasicDBObject.class).toString());
		if (!publishMessage.has("site")) {
			log.error("siteName not found in order message");
			return;
		}
		String site = publishMessage.getJSONObject("site").getString("name");
		exchange.getOut().setHeader(site, true);
		String nickNameId = publishMessage.getJSONObject("site").getString("nickNameID");
		publishMessage.put("site", site);
		publishMessage.put("nickNameID", nickNameId);
		publishMessage.put("needToUpdateOrder", true);
		publishMessage.put("needToUpdateShipping", true);
		publishMessage.put("needToUpdatePayment", false);
		publishMessage.put("needToUpdateFeedBack", false);
		publishMessage.put("requestType", "updateOrder");
		exchange.getOut().setBody(publishMessage);
	}
}
