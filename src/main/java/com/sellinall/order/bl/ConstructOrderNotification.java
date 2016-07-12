package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;

public class ConstructOrderNotification implements Processor {

	static Logger log = Logger.getLogger(ConstructOrderNotification.class.getName());

	public void process(Exchange exchange) {

		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		JSONObject outBody = new JSONObject();

		try {
			JSONObject message = new JSONObject();
			String itemTitle = "";
			String orderPageUrl = Config.getConfig().getSIAOrderPageURL() + inBody.get("orderID") + "&site="
					+ inBody.get("nickNameID");
			message.put("siteNickname", inBody.get("nickNameID"));
			org.json.JSONArray orderDetails = inBody.getJSONArray("orderItems");
			for (int i = 0; i < orderDetails.length(); i++) {
				itemTitle += ((JSONObject) orderDetails.get(i)).get("itemTitle")
						+ ((orderDetails.length() == (i + 1)) ? "" : ",");
			}
			message.put("itemTitle", itemTitle);
			JSONObject buyerDetails = inBody.getJSONObject("buyerDetails");
			if (buyerDetails.has("buyerID")) {
				message.put("buyerId", buyerDetails.getString("buyerID"));
			} else {
				message.put("buyerId", "-");
			}
			message.put("orderPageUrl", orderPageUrl);
			message.put("itemAmount", inBody.getJSONObject("orderAmount"));
			message.put("orderId", inBody.get("orderID"));
			message.put("orderNumber", inBody.get("orderID"));
			outBody.put("accountNumber", inBody.get("userId"));
			outBody.put("userMessageName", "ORDER_CREATED");
			outBody.put("message", message);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		exchange.getOut().setBody(outBody);
	}
}
