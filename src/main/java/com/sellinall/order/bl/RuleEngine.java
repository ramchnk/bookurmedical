package com.sellinall.order.bl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.RuleActionTypes;

public class RuleEngine {
	@SuppressWarnings("unchecked")
	public static void setGiftItems(BasicDBObject order, BasicDBObject rule, List<BasicDBObject> freeGiftOrderItems,
			String selectedWMS) {
		List<BasicDBObject> conditions = (List<BasicDBObject>) rule.get("conditions");
		List<BasicDBObject> orderItems = (List<BasicDBObject>) order.get("orderItems");
		List<BasicDBObject> newOrderItemList = new LinkedList<>();
		for (BasicDBObject orderItem : orderItems) {
			newOrderItemList.add((BasicDBObject) orderItem.clone());
		}
		Boolean isConditionSatisfied = checkConditionSatisfied(newOrderItemList, order, conditions);
		if (isConditionSatisfied) {
			Map<String, Integer> sellerSKUAndQuantityMap = new LinkedHashMap<>();
			List<BasicDBObject> freeGiftInventoryListFromDb = getFreeGiftInvetnoryFromDB(rule,
					order.getString("accountNumber"), sellerSKUAndQuantityMap);
			constructFreeGiftOrderItems(order, freeGiftInventoryListFromDb, sellerSKUAndQuantityMap, freeGiftOrderItems,
					selectedWMS);
		}
	}

	private static void constructFreeGiftOrderItems(BasicDBObject order,
			List<BasicDBObject> freeGiftInevntoryListFromDb, Map<String, Integer> sellerSKUAndQuantityMap,
			List<BasicDBObject> freeGiftOrderItems, String selectedWMS) {
		BasicDBObject orderAmount = (BasicDBObject) order.get("orderAmount");
		String currencyCode = orderAmount.getString("currencyCode");
		for (BasicDBObject freeGift : freeGiftInevntoryListFromDb) {
			String sellerSKU = freeGift.getString("sellerSKU");
			int availableFreeGiftQty = getAvailableQuantityFromProductMaster(freeGift, selectedWMS);
			int orderedFreeGiftQty = sellerSKUAndQuantityMap.get(sellerSKU);
			if (availableFreeGiftQty >= orderedFreeGiftQty) {
				decrementQuantityForGiftItem(order.getString("accountNumber"), sellerSKU, orderedFreeGiftQty,
						selectedWMS);
				boolean isFreeGiftHandled = false;
				for (BasicDBObject object : freeGiftOrderItems) {
					if (object.getString("customSKU").equals(sellerSKU)) {
						/* Note: Added condition to restrict multiple gift of same product */
						/*
						 * int newQuantity = object.getInt("quantity") + orderedFreeGiftQty;
						 * object.put("quantity", newQuantity);
						 */
						isFreeGiftHandled = true;
						break;
					}
				}
				if (isFreeGiftHandled) {
					continue;
				}
				BasicDBObject freeGiftOrderItem = new BasicDBObject();
				freeGiftOrderItem.put("customSKU", sellerSKU);
				if (freeGift.containsField("SKU") && freeGift.get("SKU") != null) {
					freeGiftOrderItem.put("SKU", freeGift.getString("SKU"));
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
				freeGiftOrderItem.put("quantity", orderedFreeGiftQty);
				freeGiftOrderItem.put("isFreeGift", true);
				freeGiftOrderItem.put("orderStatus", order.getString("orderStatus"));
				freeGiftOrderItem.put("paymentStatus", order.getString("paymentStatus"));
				freeGiftOrderItem.put("shippingStatus", order.getString("shippingStatus"));
				freeGiftOrderItems.add(freeGiftOrderItem);
			}
		}
	}

	private static int getAvailableQuantityFromProductMaster(BasicDBObject freeGift, String selectedWMS) {
		int quantity = 0;
		List<BasicDBObject> quantities = (List<BasicDBObject>) freeGift.get("quantities");
		for (BasicDBObject quantityObj : quantities) {
			/* Note: Handled only for single wms */
			if (quantityObj.getString("warehouseID").equals(selectedWMS)) {
				quantity += quantityObj.getInt("quantity");
			}
		}
		return quantity;
	}

	private static void decrementQuantityForGiftItem(String accountNumber, String sellerSKU, int freeGiftQuantity,
			String selectedWMS) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("sellerSKU", sellerSKU);
		searchQuery.put("quantities.warehouseID", selectedWMS);

		BasicDBObject update = new BasicDBObject();
		update.put("$inc", new BasicDBObject("quantities.$.quantity", -freeGiftQuantity));

		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("productMaster");
		table.updateOne(searchQuery, update);
	}

	private static List<BasicDBObject> getFreeGiftInvetnoryFromDB(BasicDBObject rule, String accountNumber,
			Map<String, Integer> sellerSKUAndQuantityMap) {
		BasicDBObject action = (BasicDBObject) rule.get("action");
		List<BasicDBObject> itemList = (List<BasicDBObject>) action.get("itemList");
		boolean isAvailableStockFoundInDB = false;
		String actionType = RuleActionTypes.ALL.toString();
		if (action.containsField("type")) {
			actionType = action.getString("type");
		}
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("productMaster");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		List<String> sellerSKUList = new LinkedList<>();
		for (int i = 0; i < itemList.size(); i++) {
			BasicDBObject item = itemList.get(i);
			boolean isStockAvailableForFreeGift = true;
			if (item.containsField("availableStock")) {
				isAvailableStockFoundInDB = true;
				if (item.getInt("availableStock") <= 0) {
					isStockAvailableForFreeGift = false;
				}
			}
			if (isStockAvailableForFreeGift) {
				sellerSKUList.add(item.getString("sellerSKU"));
				sellerSKUAndQuantityMap.put(item.getString("sellerSKU"), item.getInt("quantity"));
				if (isAvailableStockFoundInDB) {
					updateAvailableStockInRule(rule.getString("_id"), item.getString("sellerSKU"),
							item.getInt("quantity"));
					if (actionType.equals(RuleActionTypes.TIERED.toString())) {
						/*
						 * Note: we are adding only 1 gift product based on availableStock & itemList
						 * index for tier case
						 */
						break;
					}
				}
			}
		}
		searchQuery.put("sellerSKU", new BasicDBObject("$in", sellerSKUList));
		BasicDBObject projection = new BasicDBObject();
		projection.put("_id", 0);
		projection.put("itemTitle", 1);
		projection.put("sellerSKU", 1);
		projection.put("quantities", 1);
		List<Document> documentList = table.find(searchQuery).projection(projection).into(new ArrayList<Document>());
		List<BasicDBObject> freeGiftInventoryList = new ArrayList<BasicDBObject>();
		for (Document document : documentList) {
			freeGiftInventoryList.add((BasicDBObject) JSON.parse((document).toJson()));
		}
		return freeGiftInventoryList;
	}

	private static void updateAvailableStockInRule(String docId, String sellerSKU, int quantity) {
		BasicDBObject query = new BasicDBObject();
		query.put("_id", new ObjectId(docId));
		query.put("action.itemList.sellerSKU", sellerSKU);

		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("action.itemList.$.availableStock", -quantity);

		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("ruleOrder");
		table.updateOne(query, new BasicDBObject("$inc", updateObj));
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
			} else if (condition.getString("leftOperand").equals("paymentStatus")) {
				if (!processCondition(condition, order, "paymentStatus")) {
					return false;
				}
			} else if (condition.getString("leftOperand").equals("timeOrderCreated")) {
				if (!processCondition(condition, order, "timeOrderCreated")) {
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
		} else if (data instanceof BasicDBObject && fieldName.equals("orderSoldAmount")) {
			BasicDBObject order = (BasicDBObject) data;
			BasicDBObject orderSoldAmountObj = (BasicDBObject) order.get(fieldName);
			long amount = orderSoldAmountObj.getLong("amount");
			return processOperands(amount, condition.get("rightOperand"), condition.getString("operator"));
		} else if (data instanceof BasicDBObject && fieldName.equals("paymentStatus")) {
			BasicDBObject order = (BasicDBObject) data;
			return processOperands(order.getString("paymentStatus"), condition.get("rightOperand"),
					condition.getString("operator"));
		} else if (data instanceof BasicDBObject && fieldName.equals("timeOrderCreated")) {
			BasicDBObject order = (BasicDBObject) data;
			return processOperands(order.getLong("timeOrderCreated"), condition.get("rightOperand"),
					condition.getString("operator"));
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
		case "GREATER_THAN_OR_EQUAL":
			if (Long.parseLong(leftOperand.toString()) >= Long.parseLong(rightOperand.toString())) {
				return true;
			}
			return false;
		case "CONTAINS":
			if (((List) rightOperand).contains(leftOperand)) {
				return true;
			}
			return false;
		}
		return false;
	}
}
