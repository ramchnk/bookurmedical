package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.CurrencyUtil;

public class GroupOrderByCartNumber implements Processor {

	public void process(Exchange exchange) throws Exception {
		int totalOrderItemsInCart = exchange.getProperty("totalOrderItemsInCart", Integer.class);
		String cartNumber = exchange.getProperty("cartNumber", String.class);
		List<Document> orderList = getOrderList(cartNumber, exchange.getProperty("accountNumber", String.class));
		exchange.setProperty("isEligibleToProceed", false);
		if (totalOrderItemsInCart == orderList.size()) {
			exchange.setProperty("isEligibleToProceed", true);
			JSONObject orderMessage = groupOrderBycart(orderList);
			orderMessage.put("requestType", exchange.getProperty("requestType"));
			exchange.getOut().setBody(orderMessage);
		}
	}

	private JSONObject groupOrderBycart(List<Document> orderList) throws JSONException {
		Document order = orderList.get(0);
		String orderNumber = "";
		String orderID = "";
		ArrayList<Document> orderItems = new ArrayList<Document>();
		Document orderAmount = (Document) order.get("orderAmount");
		String currencyCode = orderAmount.getString("currencyCode");
		JSONObject orderAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		JSONObject sellerDiscountAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		JSONObject channelDiscountAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		JSONObject orderSoldAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);
		Document orderAmountInUSD = (Document) order.get("orderAmountInUSD");
		JSONObject orderSoldAmountInUSDObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0),
				orderAmountInUSD.getString("currencyCode"));
		JSONObject orderAmountInUSDObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0),
				orderAmountInUSD.getString("currencyCode"));
		JSONObject shippingAmountObj = CurrencyUtil.getJSONAmountObject(Long.valueOf(0), currencyCode);

		for (Document relatedOrder : orderList) {
			orderItems.addAll((ArrayList<Document>) relatedOrder.get("orderItems"));
			if (relatedOrder.containsKey("orderNumber")) {
				orderNumber += (orderNumber.isEmpty() ? "" : ", ") + relatedOrder.get("orderNumber");
			}
			if (relatedOrder.containsKey("orderID")) {
				orderID += (orderID.isEmpty() ? "" : ", ") + relatedOrder.get("orderID");
			}
			if (relatedOrder.containsKey("orderAmount")) {
				orderAmountObj = CurrencyUtil.addAmountObject(orderAmountObj,
						OrderUtil.parseToJsonObject((Document) relatedOrder.get("orderAmount")));
			}
			if (relatedOrder.containsKey("orderAmountInUSD")) {
				orderAmountInUSDObj = CurrencyUtil.addAmountObject(orderAmountInUSDObj,
						OrderUtil.parseToJsonObject((Document) relatedOrder.get("orderAmountInUSD")));
			}
			if (relatedOrder.containsKey("orderSoldAmount")) {
				orderSoldAmountObj = CurrencyUtil.addAmountObject(orderSoldAmountObj,
						OrderUtil.parseToJsonObject((Document) relatedOrder.get("orderSoldAmount")));
			}
			if (relatedOrder.containsKey("orderSoldAmountInUSD")) {
				orderSoldAmountInUSDObj = CurrencyUtil.addAmountObject(orderSoldAmountInUSDObj, OrderUtil
						.parseToJsonObject((Document) (relatedOrder.get("orderSoldAmountInUSD"))));
			}
			if (relatedOrder.containsKey("sellerDiscountAmount")) {
				sellerDiscountAmountObj = CurrencyUtil.addAmountObject(sellerDiscountAmountObj, OrderUtil
						.parseToJsonObject((Document) relatedOrder.get("sellerDiscountAmount")));
			}
			if (relatedOrder.containsKey("shippingAmount")) {
				shippingAmountObj = CurrencyUtil.addAmountObject(shippingAmountObj,
						OrderUtil.parseToJsonObject((Document) relatedOrder.get("shippingAmount")));
			}
			if (relatedOrder.containsKey("channelDiscountAmount")) {
				channelDiscountAmountObj = CurrencyUtil.addAmountObject(channelDiscountAmountObj, OrderUtil
						.parseToJsonObject((Document) relatedOrder.get("channelDiscountAmount")));
			}
		}
		order.put("orderNumber", orderNumber);
		order.put("orderID", orderID);
		order.put("orderItems", orderItems);
		order.put("orderAmount", Document.parse(orderAmountObj.toString()));
		order.put("orderAmountInUSD", Document.parse(orderAmountInUSDObj.toString()));
		order.put("orderSoldAmount", Document.parse(orderSoldAmountObj.toString()));
		order.put("orderSoldAmountInUSD", Document.parse(orderSoldAmountInUSDObj.toString()));
		order.put("sellerDiscountAmount", Document.parse(sellerDiscountAmountObj.toString()));
		order.put("channelDiscountAmount", Document.parse(channelDiscountAmountObj.toString()));
		order.put("shippingAmount", Document.parse(shippingAmountObj.toString()));
		return OrderUtil.parseToJsonObject(order);
	}

	private List<Document> getOrderList(String cartNumber, String accountNumber) {
		List<Document> orderList = new ArrayList<Document>();
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("cartNumber", cartNumber);
		List<Document> result = table.find(searchQuery).into(new ArrayList<Document>());
		for (Document order : result) {
			Document orderObj = Document.parse((order).toJson());
			orderList.add(orderObj);
		}
		return orderList;
	}
}
