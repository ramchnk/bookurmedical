package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.CurrencyUtil;

public class GroupOrderByCartNumber implements Processor {

	public void process(Exchange exchange) throws Exception {
		int totalOrderItemsInCart = exchange.getProperty("totalOrderItemsInCart", Integer.class);
		String cartNumber = exchange.getProperty("cartNumber", String.class);
		List<BasicDBObject> orderList = getOrderList(cartNumber, exchange.getProperty("accountNumber", String.class));
		exchange.setProperty("isEligibleToProceed", false);
		if (totalOrderItemsInCart == orderList.size()) {
			exchange.setProperty("isEligibleToProceed", true);
			JSONObject orderMessage = groupOrderBycart(orderList);
			orderMessage.put("requestType", exchange.getProperty("requestType"));
			exchange.getOut().setBody(orderMessage);
		}
	}

	private JSONObject groupOrderBycart(List<BasicDBObject> orderList) throws JSONException {
		DBObject order = orderList.get(0);
		String orderNumber = "";
		String orderID = "";
		ArrayList<DBObject> orderItems = new ArrayList<DBObject>();
		BasicDBObject orderAmount = (BasicDBObject) order.get("orderAmount");
		String currencyCode = orderAmount.getString("currencyCode");
		JSONObject orderAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		JSONObject sellerDiscountAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		JSONObject channelDiscountAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		JSONObject orderSoldAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		BasicDBObject orderAmountInUSD = (BasicDBObject) order.get("orderAmountInUSD");
		JSONObject orderSoldAmountInUSDObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0),
				orderAmountInUSD.getString("currencyCode"));
		JSONObject orderAmountInUSDObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0),
				orderAmountInUSD.getString("currencyCode"));
		JSONObject shippingAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);

		for (DBObject relatedOrder : orderList) {
			orderItems.addAll((ArrayList<DBObject>) relatedOrder.get("orderItems"));
			if (relatedOrder.containsField("orderNumber")) {
				orderNumber += (orderNumber.isEmpty() ? "" : ", ") + relatedOrder.get("orderNumber");
			}
			if (relatedOrder.containsField("orderID")) {
				orderID += (orderID.isEmpty() ? "" : ", ") + relatedOrder.get("orderID");
			}
			if (relatedOrder.containsField("orderAmount")) {
				orderAmountObj = CurrencyUtil.addAmountObject(orderAmountObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("orderAmount").toString())));
			}
			if (relatedOrder.containsField("orderAmountInUSD")) {
				orderAmountInUSDObj = CurrencyUtil.addAmountObject(orderAmountInUSDObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("orderAmountInUSD").toString())));
			}
			if (relatedOrder.containsField("orderSoldAmount")) {
				orderSoldAmountObj = CurrencyUtil.addAmountObject(orderSoldAmountObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("orderSoldAmount").toString())));
			}
			if (relatedOrder.containsField("orderSoldAmountInUSD")) {
				orderSoldAmountInUSDObj = CurrencyUtil.addAmountObject(orderSoldAmountInUSDObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("orderSoldAmountInUSD").toString())));
			}
			if (relatedOrder.containsField("sellerDiscountAmount")) {
				sellerDiscountAmountObj = CurrencyUtil.addAmountObject(sellerDiscountAmountObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("sellerDiscountAmount").toString())));
			}
			if (relatedOrder.containsField("shippingAmount")) {
				shippingAmountObj = CurrencyUtil.addAmountObject(shippingAmountObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("shippingAmount").toString())));
			}
			if (relatedOrder.containsField("channelDiscountAmount")) {
				channelDiscountAmountObj = CurrencyUtil.addAmountObject(channelDiscountAmountObj,
						OrderUtil.parseToJsonObject((DBObject) JSON.parse(relatedOrder.get("channelDiscountAmount").toString())));
			}
		}
		order.put("orderNumber", orderNumber);
		order.put("orderID", orderID);
		order.put("orderItems", orderItems);
		order.put("orderAmount", BasicDBObject.parse(orderAmountObj.toString()));
		order.put("orderAmountInUSD", BasicDBObject.parse(orderAmountInUSDObj.toString()));
		order.put("orderSoldAmount", BasicDBObject.parse(orderSoldAmountObj.toString()));
		order.put("orderSoldAmountInUSD", BasicDBObject.parse(orderSoldAmountInUSDObj.toString()));
		order.put("sellerDiscountAmount", BasicDBObject.parse(sellerDiscountAmountObj.toString()));
		order.put("channelDiscountAmount", BasicDBObject.parse(channelDiscountAmountObj.toString()));
		order.put("shippingAmount", BasicDBObject.parse(shippingAmountObj.toString()));
		return OrderUtil.parseToJsonObject(order);
	}	

	private List<BasicDBObject> getOrderList(String cartNumber, String accountNumber) {
		List<BasicDBObject> orderList = new ArrayList<BasicDBObject>();
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("cartNumber", cartNumber);
		List<Document> result = table.find(searchQuery).into(new ArrayList<Document>());
		for (Document order : result) {
			BasicDBObject orderObj = (BasicDBObject) JSON.parse((order).toJson());
			orderList.add(orderObj);
		}
		return orderList;
	}
}
