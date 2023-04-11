package com.sellinall.order.bl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.config.Config;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.SIAOrderStatus;

public class ConstructOrderNotification implements Processor {

	static Logger log = Logger.getLogger(ConstructOrderNotification.class.getName());

	public void process(Exchange exchange) throws JSONException {

		JSONObject orderRecord = OrderUtil
				.parseToJsonObject((Document) exchange.getProperty("orderRecord", Document.class));
		JSONObject outBody = new JSONObject();

		try {
			JSONObject message = new JSONObject();
			Document userSiteObject = exchange.getProperty("userSiteSpecificObject", Document.class);
			Document nickName = (Document) userSiteObject.get("nickName");
			String nickNameID = nickName.getString("id");
			String orderPageUrl = Config.getConfig().getSIAOrderPageURL() + orderRecord.get("orderID") + "&site="
					+ nickNameID;
			String siteNickname = nickNameID.split("-")[0] + "-" + nickName.get("value").toString();
			message.put("siteNickname", siteNickname);
			message.put("nicknameId", nickNameID);
			JSONArray orderItems = orderRecord.getJSONArray("orderItems");
			if (orderItems.length() == 0) {
				exchange.getOut().setBody(null);
				return;
			}
			JSONArray items = new JSONArray();
			ArrayList<String> SKUListInOrder = new ArrayList<String>();
			Map<String, JSONObject> skuDetailMap = new HashMap<String, JSONObject>();
			exchange.setProperty("isSKUListEmpty", true);
			for (int i = 0; i < orderItems.length(); i++) {
				JSONObject itemDetails = new JSONObject();
				JSONObject orderItem = orderItems.getJSONObject(i);
				itemDetails.put("title", orderItem.getString("itemTitle"));
				itemDetails.put("quantity", orderItem.getInt("quantity"));
				if (orderItem.has("customSKU")) {
					itemDetails.put("customSKU", orderItem.getString("customSKU"));
				}
				if (orderItem.has("imageURL")) {
					itemDetails.put("imageUrl", orderItem.getString("imageURL"));
				}
				if (orderItem.has("SKU")) {
					SKUListInOrder.add(orderItem.get("SKU").toString());
					skuDetailMap.put(orderItem.get("SKU").toString(), itemDetails);
				}
				items.put(itemDetails);
			}
			exchange.setProperty("SKUListInOrder", SKUListInOrder);
			exchange.setProperty("skuDetailMap", skuDetailMap);
			if (!SKUListInOrder.isEmpty()) {
				exchange.setProperty("isSKUListEmpty", false);
			}
			message.put("items", items);
			if (orderRecord.has("buyerDetails")) {
				JSONObject buyerDetails = orderRecord.getJSONObject("buyerDetails");
				// for shopee only having buyerID
				if (buyerDetails.has("buyerID")) {
					message.put("buyerId", buyerDetails.get("buyerID").toString());
				} else if (buyerDetails.has("name")) {
					// for rest of all channels has name only.
					message.put("buyerId", buyerDetails.getString("name"));
				}
			}
			message.put("orderPageUrl", orderPageUrl);
			// For amazon cancel orders, orderAmount is not returned
			if (orderRecord.has("orderAmount")) {
				message.put("orderAmount", orderRecord.getJSONObject("orderAmount"));
			}
			message.put("orderId", orderRecord.get("orderID"));
			message.put("isManaged", exchange.getProperty("isManaged"));
			if (exchange.getProperties().containsKey("showOnlyManagedOrders")) {
				message.put("showOnlyManagedOrders", exchange.getProperty("showOnlyManagedOrders", Boolean.class));
			}
			if (orderRecord.has("paymentStatus")) {
				message.put("paymentStatus", orderRecord.get("paymentStatus"));
			}
			message.put("orderNumber", orderRecord.get("orderID"));
			String orderStatus = orderRecord.getString("orderStatus");
			if ((orderStatus.equals(SIAOrderStatus.ACCEPTED.toString())
					|| orderStatus.equals(SIAOrderStatus.PROCESSING.toString())) && orderRecord.has("documents")) {
				message.put("documents", orderRecord.get("documents"));
			}
			if (orderRecord.has("cancelDetails") && orderRecord.getJSONObject("cancelDetails").has("cancelReason")) {
				message.put("cancelReason",
						orderRecord.getJSONObject("cancelDetails").getString("cancelReason").replaceAll("_", " "));
			}
			outBody.put("accountNumber", orderRecord.get("accountNumber"));
			outBody.put("merchantID", exchange.getProperty("merchantID"));
			if (orderRecord.has("invoiceNumber")) {
				message.put("invoiceNumber", orderRecord.get("invoiceNumber"));
			}
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