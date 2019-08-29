package com.sellinall.order.bl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;

public class RuleEngine {
	@SuppressWarnings("unchecked")
	public static void setGiftItems(BasicDBObject order, BasicDBObject rule, List<BasicDBObject> freeGiftOrderItems) {
		List<BasicDBObject> conditions = (List<BasicDBObject>) rule.get("conditions");
		List<BasicDBObject> orderItems = (List<BasicDBObject>) order.get("orderItems");
		List<BasicDBObject> newOrderItemList = new LinkedList<>();
		for (BasicDBObject orderItem : orderItems) {
			newOrderItemList.add((BasicDBObject) orderItem.clone());
		}
		Boolean isConditionSatisfied = checkConditionSatisfied(newOrderItemList, order, conditions);
		if (isConditionSatisfied) {
			BasicDBObject action = (BasicDBObject) rule.get("action");
			Map<String, Integer> skuAndQuantityMap = new LinkedHashMap<>();
			List<BasicDBObject> freeGiftInventoryListFromDb = getFreeGiftInvetnoryFromDB(
					(List<BasicDBObject>) action.get("itemList"), order.getString("accountNumber"), skuAndQuantityMap);
			constructFreeGiftOrderItems(order, freeGiftInventoryListFromDb, skuAndQuantityMap, freeGiftOrderItems);
		}
	}

	private static void constructFreeGiftOrderItems(BasicDBObject order,
			List<BasicDBObject> freeGiftInevntoryListFromDb, Map<String, Integer> skuAndQuantityMap,
			List<BasicDBObject> freeGiftOrderItems) {
		BasicDBObject orderAmount = (BasicDBObject) order.get("orderAmount");
		String currencyCode = orderAmount.getString("currencyCode");
		for (BasicDBObject freeGift : freeGiftInevntoryListFromDb) {
			String SKU = freeGift.getString("SKU");
			for (BasicDBObject object : freeGiftOrderItems) {
				if (object.getString("SKU").equals(SKU)) {
					int newQuantity = object.getInt("quantity") + skuAndQuantityMap.get(SKU);
					object.put("quantity", newQuantity);
					return;
				}
			}
			BasicDBObject freeGiftOrderItem = new BasicDBObject();
			freeGiftOrderItem.put("SKU", SKU);
			if (freeGift.containsField("customSKU") && freeGift.get("customSKU") != null) {
				freeGiftOrderItem.put("customSKU", freeGift.getString("customSKU"));
			}
			if (freeGift.containsField("itemTitle") && freeGift.get("itemTitle") != null) {
				freeGiftOrderItem.put("itemTitle", freeGift.getString("itemTitle"));
			}
			BasicDBObject itemAmountObject = new BasicDBObject();
			itemAmountObject.put("amount", 0);
			itemAmountObject.put("currencyCode", currencyCode);
			freeGiftOrderItem.put("itemAmount", itemAmountObject);
			BasicDBObject itemSoldAmountObject = new BasicDBObject();
			itemSoldAmountObject.put("amount", 0);
			itemSoldAmountObject.put("currencyCode", currencyCode);
			freeGiftOrderItem.put("itemSoldAmount", itemSoldAmountObject);
			freeGiftOrderItem.put("quantity", skuAndQuantityMap.get(SKU));
			freeGiftOrderItem.put("isFreeGift", true);
			freeGiftOrderItems.add(freeGiftOrderItem);
		}
	}

	private static List<BasicDBObject> getFreeGiftInvetnoryFromDB(List<BasicDBObject> itemList, String accountNumber,
			Map<String, Integer> skuAndQuantityMap) {
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("inventory");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		List<String> skuList = new LinkedList<>();
		for (int i = 0; i < itemList.size(); i++) {
			BasicDBObject item = itemList.get(i);
			skuList.add(item.getString("SKU"));
			skuAndQuantityMap.put(item.getString("SKU"), item.getInt("quantity"));
		}
		searchQuery.put("SKU", new BasicDBObject("$in", skuList));
		BasicDBObject projection = new BasicDBObject();
		projection.put("_id", 0);
		projection.put("itemTitle", 1);
		projection.put("customSKU", 1);
		projection.put("SKU", 1);
		List<Document> inventoryList = table.find(searchQuery).projection(projection).into(new ArrayList<Document>());
		List<BasicDBObject> freeGiftInventoryList = new ArrayList<BasicDBObject>();
		for (Document inventory : inventoryList) {
			BasicDBObject orderObj = (BasicDBObject) JSON.parse((inventory).toJson());
			freeGiftInventoryList.add(orderObj);
		}
		return freeGiftInventoryList;
	}

	private static Boolean checkConditionSatisfied(List<BasicDBObject> orderItems, BasicDBObject order,
			List<BasicDBObject> conditions) {
		for (BasicDBObject condition : conditions) {
			if (condition.getString("leftOperand").equals("SKU")) {
				if (!processCondition(condition, orderItems, "SKU")) {
					return false;
				}
			} else if (condition.getString("leftOperand").equals("orderSoldAmount")) {
				if (!processCondition(condition, order, "orderSoldAmount")) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean processCondition(BasicDBObject condition, Object data, String fieldName) {

		if (data instanceof List) {
			List<BasicDBObject> orderItems = (List<BasicDBObject>) data;
			for (BasicDBObject orderItem : orderItems) {
				boolean isConditionSatisfied = false;
				String value = orderItem.getString(fieldName);
				if (processOperands(value, condition.get("rightOperand"), condition.getString("operator"))) {
					isConditionSatisfied = true;
					if (!orderItem.containsField("isConditionSatisfied")) {
						orderItem.put("isConditionSatisfied", isConditionSatisfied);
					}
				} else {
					orderItem.put("isConditionSatisfied", isConditionSatisfied);
				}
			}
			return orderItems.stream().filter(x -> x.getBoolean("isConditionSatisfied")).count() > 0;
		} else if (data instanceof BasicDBObject) {
			BasicDBObject order = (BasicDBObject) data;
			BasicDBObject orderSoldAmountObj = (BasicDBObject) order.get(fieldName);
			long amount = orderSoldAmountObj.getLong("amount");
			return processOperands(amount, condition.get("rightOperand"), condition.getString("operator"));
		}
		return false;
	}

	private static boolean processOperands(Object leftOperand, Object rightOperand, String operator) {
		switch (operator) {
		case "EQUAL":
			if (rightOperand instanceof String && leftOperand.equals(rightOperand)) {
				return true;
			} else if (leftOperand == rightOperand) {
				return true;
			}
			return false;
		case "GREATER_THAN":
			if (Long.parseLong(leftOperand.toString()) > Long.parseLong(rightOperand.toString())) {
				return true;
			}
			return false;
		case "LESS_THAN":
			if (Long.parseLong(leftOperand.toString()) < Long.parseLong(rightOperand.toString())) {
				return true;
			}
			return false;
		}
		return false;
	}
}
