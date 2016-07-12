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

public class constructOrderNotification implements Processor {

	static Logger log = Logger.getLogger(constructOrderNotification.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		JSONObject outputdata = new JSONObject();
		JSONObject itemAmount = new JSONObject();
		JSONObject messageData = new JSONObject();
		String itemTitle = "";
		String orderPageUrl=Config.getConfig().getSIAOrderPageURL()+inBody.get("orderID")+"&site="+inBody.get("nickNameID");
		messageData.put("siteNickname", inBody.get("nickNameID"));
		org.json.JSONArray orderDetails = inBody.getJSONArray("orderItems");
		for (int i = 0; i < orderDetails.length(); i++) {
			itemTitle += ((JSONObject) orderDetails.get(i)).get("itemTitle")
					+ ((orderDetails.length() == (i + 1)) ? "" : ",");			
		}
		messageData.put("itemTitle", itemTitle);
		if(((JSONObject) inBody.get("buyerDetails")).has("buyerID"))
		{
		messageData.put("buyerId", ((JSONObject) inBody.get("buyerDetails")).get("buyerID"));
		}
		else
		{
			messageData.put("buyerId","-");
		}
		messageData.put("orderPageUrl",orderPageUrl);
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
