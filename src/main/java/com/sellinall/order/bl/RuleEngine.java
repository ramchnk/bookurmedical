package com.sellinall.order.bl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.RuleActionTypes;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.Actor;
import com.sellinall.util.enums.StockEventType;

public class RuleEngine {

	static Logger log = Logger.getLogger(RuleEngine.class.getName());

	@SuppressWarnings("unchecked")
	public static void setGiftItems(BasicDBObject order, BasicDBObject rule, List<BasicDBObject> freeGiftOrderItems,
			String selectedWMS, List<String> giftItemSKUs) throws JSONException {
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
					order.getString("accountNumber"), sellerSKUAndQuantityMap, giftItemSKUs);
			constructFreeGiftOrderItems(order, freeGiftInventoryListFromDb, sellerSKUAndQuantityMap, freeGiftOrderItems,
					selectedWMS);
		} else {
			log.info("Free gift rule not satisfied for orderID : " + order.getString("orderID") + ", accountNumber : "
					+ order.getString("accountNumber") + ", gift doc id : " + rule.getString("_id"));
		}
	}

	private static void constructFreeGiftOrderItems(BasicDBObject order,
			List<BasicDBObject> freeGiftInevntoryListFromDb, Map<String, Integer> sellerSKUAndQuantityMap,
			List<BasicDBObject> freeGiftOrderItems, String selectedWMS) throws JSONException {
		BasicDBObject orderAmount = (BasicDBObject) order.get("orderAmount");
		String currencyCode = orderAmount.getString("currencyCode");
		for (BasicDBObject freeGift : freeGiftInevntoryListFromDb) {
			String sellerSKU = freeGift.getString("sellerSKU");
			int availableFreeGiftQty = getAvailableQuantityFromProductMaster(freeGift, selectedWMS);
			int orderedFreeGiftQty = sellerSKUAndQuantityMap.get(sellerSKU);
			if (availableFreeGiftQty >= orderedFreeGiftQty) {
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
				decrementQuantityForGiftItem(order, sellerSKU, orderedFreeGiftQty, selectedWMS);
				int giftItemCount = freeGiftOrderItems.size() + 1;
				BasicDBObject freeGiftOrderItem = new BasicDBObject();
				freeGiftOrderItem.put("orderItemID", order.getString("orderID") + "-gwp" + giftItemCount);
				freeGiftOrderItem.put("siaOrderItemID", order.getString("orderID") + "-gwp" + giftItemCount);
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
		if (quantity==0) {
			log.warn("quantity not available for wms : "+ selectedWMS+", doc : "+ freeGift);
		}
		return quantity;
	}

	private static void decrementQuantityForGiftItem(BasicDBObject order, String sellerSKU, int freeGiftQuantity,
			String selectedWMS) throws JSONException {
		try {
			String accountNumber = order.getString("accountNumber");
			BasicDBObject siteObj = (BasicDBObject) order.get("site");
			JSONObject quantityObj = new JSONObject();
			quantityObj.put("warehouseID", selectedWMS);
			quantityObj.put("quantityDiff", -freeGiftQuantity);

			JSONArray quantityArray = new JSONArray();
			quantityArray.put(quantityObj);

			JSONObject addendum = new JSONObject();
			addendum.put("orderID", order.getString("orderID"));
			addendum.put("nickNameID", siteObj.getString("nickNameID"));
			addendum.put("quantitySold", freeGiftQuantity);
			addendum.put("timeOrderCreated", order.getLong("timeOrderCreated"));
			JSONObject payload = new JSONObject();
			payload.put("sellerSKU", sellerSKU);
			payload.put("quantityDiffs", quantityArray);
			payload.put("actor", Actor.SALES_CHANNEL.toString());
			payload.put("stockEventType", StockEventType.NEW_ORDER.toString());
			payload.put("isPromotionItem", false);
			payload.put("addendum", addendum);

			updateProductMaster(payload, accountNumber);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void updateProductMaster(JSONObject payload, String accountNumber) {
		String url = Config.getConfig().getSIAInventoryManagementServerURL() + "/productMaster/quantityDiffs";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		headers.put("accountNumber", accountNumber);
		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doPut(url, payload.toString(), headers);
			if (response.getInt("httpCode") != HttpStatus.OK_200) {
				if (response.getInt("httpCode") == HttpStatus.NOT_FOUND_404) {
					log.info("The sellerSKU: " + payload.getString("sellerSKU")
							+ " not found in ProductMaster for accountNumber : " + accountNumber);
				} else {
					log.error("SyncProductMaster failed with HttpStatus code : " + response.getInt("httpCode")
							+ " for accountNumber : " + accountNumber + ", sellerSKU : "
							+ payload.getString("sellerSKU") + " and response payload: " + response.get("payload"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error(response);
		} catch (JSONException e) {
			e.printStackTrace();
			log.error(response);
		}
	}

	private static List<BasicDBObject> getFreeGiftInvetnoryFromDB(BasicDBObject rule, String accountNumber,
			Map<String, Integer> sellerSKUAndQuantityMap, List<String> giftItemSKUs) {
		BasicDBObject action = (BasicDBObject) rule.get("action");
		List<BasicDBObject> itemList = (List<BasicDBObject>) action.get("itemList");
		boolean isAvailableStockFoundInDB = false;
		boolean isGiftItemAdded = false;
		String actionType = RuleActionTypes.ALL.toString();
		if (action.containsField("type")) {
			actionType = action.getString("type");
		}
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("productMaster");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		List<String> sellerSKUList = new LinkedList<>();
		for (int i = 0; i < itemList.size(); i++) {
			if (isGiftItemAdded && actionType.equals(RuleActionTypes.TIERED.toString())) {
				/*
				 * Note: we are adding only 1 gift product based on availableStock & itemList
				 * index for tier case
				 */
				break;
			}
			BasicDBObject item = itemList.get(i);
			boolean isStockAvailableForFreeGift = true;
			if (item.containsField("availableStock")) {
				isAvailableStockFoundInDB = true;
				if (item.getInt("availableStock") <= 0) {
					isStockAvailableForFreeGift = false;
				}
			}
			if (isStockAvailableForFreeGift) {
				if (giftItemSKUs.contains(item.getString("sellerSKU"))) {
					isGiftItemAdded = true;
					continue;
				}
				sellerSKUList.add(item.getString("sellerSKU"));
				sellerSKUAndQuantityMap.put(item.getString("sellerSKU"), item.getInt("quantity"));
				if (isAvailableStockFoundInDB) {
					updateAvailableStockInRule(rule.getString("_id"), item.getString("sellerSKU"),
							item.getInt("quantity"));
					isGiftItemAdded = true;
				}
			}
		}
		if (sellerSKUList.isEmpty()) {
			new ArrayList<BasicDBObject>();
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
			} else if (condition.getString("leftOperand").equals("quantity")) {
				if (!processCondition(condition, orderItems, "quantity")) {
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
