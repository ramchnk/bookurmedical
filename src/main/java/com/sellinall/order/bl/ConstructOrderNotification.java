package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mudra.sellinall.config.Config;

public class ConstructOrderNotification implements Processor {

	static Logger log = Logger.getLogger(ConstructOrderNotification.class.getName());

	public void process(Exchange exchange) {

		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		JSONObject outBody = new JSONObject();

		try {
			JSONObject message = new JSONObject();
			BasicDBObject userSiteObject = exchange.getProperty("userSiteSpecificObject", BasicDBObject.class);
			BasicDBObject nickName = (BasicDBObject) userSiteObject.get("nickName");
			String nickNameID = nickName.getString("id");
			String orderPageUrl = Config.getConfig().getSIAOrderPageURL() + inBody.get("orderID") + "&site="
					+ nickNameID;
			String siteNickname = nickNameID.split("-")[0] + "-" + nickName.getString("value");
			message.put("siteNickname", siteNickname);
			JSONArray orderItems = inBody.getJSONArray("orderItems");
			JSONArray items = new JSONArray();
			for (int i = 0; i < orderItems.length(); i++) {
				JSONObject itemDetails = new JSONObject();
				itemDetails.put("title", ((JSONObject) orderItems.get(i)).get("itemTitle"));
				itemDetails.put("quantity", ((JSONObject) orderItems.get(i)).get("quantity"));
				items.put(itemDetails);
			}
			message.put("items", items);
			String buyerId = "-";
			if (inBody.has("buyerDetails")) {
				JSONObject buyerDetails = inBody.getJSONObject("buyerDetails");
				if (buyerDetails.has("buyerID")) {
					message.put("buyerId", buyerDetails.getString("buyerID"));
				}
			}
			message.put("buyerId", buyerId);
			message.put("orderPageUrl", orderPageUrl);
			// For amazon cancel orders, orderAmount is not returned
			if (inBody.has("orderAmount")) {
				message.put("orderAmount", inBody.getJSONObject("orderAmount"));
			}
			message.put("orderId", inBody.get("orderID"));
			message.put("orderNumber", inBody.get("orderID"));
			outBody.put("accountNumber", inBody.get("userId"));
			outBody.put("userMessageName", (String) exchange.getIn().getHeader("userMessageName"));
			outBody.put("message", message);
			exchange.getOut().setBody(outBody);
		} catch (Exception exception) {
			log.error("Error occured while constructing the email-notification payload");
			exception.printStackTrace();
			exchange.getOut().setBody(null);
		}

	}
}