package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class InitOrderNotification implements Processor {

	static Logger log = Logger.getLogger(InitOrderNotification.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		JSONObject outputdata = new JSONObject();
		JSONObject itemAmount = new JSONObject();
		JSONObject messageData = new JSONObject();
		String itemTitle = "";
		String imageUrl = "";
		messageData.put("siteNickname", inBody.get("nickNameID"));
		org.json.JSONArray orderDetails = inBody.getJSONArray("orderItems");
		for (int i = 0; i < orderDetails.length(); i++) {
			itemTitle += ((JSONObject) orderDetails.get(i)).get("itemTitle")
					+ ((orderDetails.length() == (i + 1)) ? "" : ",");
			if (((JSONObject) orderDetails.get(i)).has("imageUrl")) {
				imageUrl += ((JSONObject) orderDetails.get(i)).get("imageUrl")
						+ ((orderDetails.length() == (i + 1)) ? "" : ",");
			}
		}
		messageData.put("itemTitle", itemTitle);
		messageData.put("buyerId", ((JSONObject) inBody.get("buyerDetails")).get("buyerID"));
		messageData.put("orderPageUrl", imageUrl);
		itemAmount = (JSONObject) inBody.get("orderAmount");
		messageData.put("itemAmount", itemAmount);
		messageData.put("orderId", inBody.get("orderID"));
		messageData.put("orderNumber", inBody.get("orderID"));
		outputdata.put("accountNumber", inBody.get("userId"));
		outputdata.put("userMessageName", "ORDER_CREATED");
		outputdata.put("message", messageData);
		exchange.getOut().setBody(outputdata);
	}
}
