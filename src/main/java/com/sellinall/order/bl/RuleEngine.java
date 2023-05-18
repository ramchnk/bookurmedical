package com.sellinall.order.bl;

import java.io.IOException;
import java.math.BigDecimal;
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

import com.mongodb.client.MongoCollection;
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
	public static boolean setGiftItems(Document order, Document rule, List<Document> freeGiftOrderItems,
			String selectedWMS, List<String> giftItemSKUs) throws JSONException {
		List<Document> conditions = (List<Document>) rule.get("conditions");
		List<Document> orderItems = (List<Document>) order.get("orderItems");
		List<Document> newOrderItemList = new LinkedList<>();
		for (Document orderItem : orderItems) {
			newOrderItemList.add((Document) orderItem);
		}
		Boolean isConditionSatisfied = checkConditionSatisfied(newOrderItemList, order, conditions);
		if (isConditionSatisfied) {
			Map<String, Integer> sellerSKUAndQuantityMap = new LinkedHashMap<>();
			List<Document> freeGiftInventoryListFromDb = getFreeGiftInvetnoryFromDB(rule,
					order.get("accountNumber").toString(), sellerSKUAndQuantityMap, giftItemSKUs);
			constructFreeGiftOrderItems(order, freeGiftInventoryListFromDb, sellerSKUAndQuantityMap, freeGiftOrderItems,
					selectedWMS);
		} else {
			log.info("Free gift rule not satisfied for orderID : " + order.get("orderID").toString() + ", accountNumber : "
					+ order.get("accountNumber").toString() + ", gift doc id : " + rule.getObjectId("_id").toString());
		}
		return isConditionSatisfied;
	}

	public static boolean removeGiftItems(Document order, Document rule, List<Document> freeGiftOrderItems,
			String selectedWMS, List<String> giftItemSKUs) throws JSONException {
		List<Document> conditions = (List<Document>) rule.get("conditions");
		List<Document> orderItems = (List<Document>) order.get("orderItems");
		List<Document> newOrderItemList = new LinkedList<>();
		for (Document orderItem : orderItems) {
			newOrderItemList.add((Document) orderItem);
		}
		return checkConditionSatisfied(newOrderItemList, order, conditions);
	}

	private static void constructFreeGiftOrderItems(Document order, List<Document> freeGiftInevntoryListFromDb,
			Map<String, Integer> sellerSKUAndQuantityMap, List<Document> freeGiftOrderItems, String selectedWMS)
			throws JSONException {
		Document orderAmount = (Document) order.get("orderAmount");
		String currencyCode = orderAmount.getString("currencyCode");
		for (Document freeGift : freeGiftInevntoryListFromDb) {
			String sellerSKU = freeGift.getString("sellerSKU");
			int availableFreeGiftQty = getAvailableQuantityFromProductMaster(freeGift, selectedWMS);
			int orderedFreeGiftQty = sellerSKUAndQuantityMap.get(sellerSKU);
			if (availableFreeGiftQty >= orderedFreeGiftQty) {
				boolean isFreeGiftHandled = false;
				for (Document object : freeGiftOrderItems) {
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
				Document freeGiftOrderItem = new Document();
				freeGiftOrderItem.put("orderItemID", order.get("orderID").toString() + "-gwp" + giftItemCount);
				freeGiftOrderItem.put("siaOrderItemID", order.get("orderID").toString() + "-gwp" + giftItemCount);
				freeGiftOrderItem.put("customSKU", sellerSKU);
				if (freeGift.containsKey("SKU") && freeGift.get("SKU") != null) {
					freeGiftOrderItem.put("SKU", freeGift.getString("SKU"));
				}
				if (freeGift.containsKey("itemTitle") && freeGift.get("itemTitle") != null) {
					freeGiftOrderItem.put("itemTitle", freeGift.getString("itemTitle"));
				}
				Document itemAmountObject = new Document();
				if (selectedWMS != null && !selectedWMS.isEmpty()) {
					freeGiftOrderItem.put("wmsID", selectedWMS);
				}
				itemAmountObject.put("amount", 0);
				itemAmountObject.put("currencyCode", currencyCode);
				freeGiftOrderItem.put("itemAmount", itemAmountObject);
				Document itemSoldAmountObject = new Document();
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

	private static int getAvailableQuantityFromProductMaster(Document freeGift, String selectedWMS) {
		int quantity = 0;
		List<Document> quantities = (List<Document>) freeGift.get("quantities");
		for (Document quantityObj : quantities) {
			/* Note: Handled only for single wms */
			if (quantityObj.getString("warehouseID").equals(selectedWMS)) {
				quantity += quantityObj.getInteger("quantity");
			}
		}
		if (quantity == 0) {
			log.warn("quantity not available for wms : " + selectedWMS + ", doc : " + freeGift);
		}
		return quantity;
	}

	private static void decrementQuantityForGiftItem(Document order, String sellerSKU, int freeGiftQuantity,
			String selectedWMS) throws JSONException {
		try {
			String accountNumber = order.get("accountNumber").toString();
			Document siteObj = (Document) order.get("site");
			JSONObject quantityObj = new JSONObject();
			quantityObj.put("warehouseID", selectedWMS);
			quantityObj.put("quantityDiff", -freeGiftQuantity);

			JSONArray quantityArray = new JSONArray();
			quantityArray.put(quantityObj);

			JSONObject addendum = new JSONObject();
			addendum.put("orderID", order.get("orderID").toString());
			addendum.put("nickNameID", siteObj.getString("nickNameID"));
			addendum.put("quantitySold", freeGiftQuantity);
			addendum.put("timeOrderCreated", new BigDecimal(order.get("timeOrderCreated").toString()).longValue());
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

	private static List<Document> getFreeGiftInvetnoryFromDB(Document rule, String accountNumber,
			Map<String, Integer> sellerSKUAndQuantityMap, List<String> giftItemSKUs) {
		Document action = (Document) rule.get("action");
		List<Document> itemList = (List<Document>) action.get("itemList");
		boolean isAvailableStockFoundInDB = false;
		boolean isGiftItemAdded = false;
		String actionType = RuleActionTypes.ALL.toString();
		if (action.containsKey("type")) {
			actionType = action.getString("type");
		}
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("productMaster");
		Document searchQuery = new Document();
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
			Document item = itemList.get(i);
			boolean isStockAvailableForFreeGift = true;
			if (item.containsKey("availableStock")) {
				isAvailableStockFoundInDB = true;
				if (item.getInteger("availableStock") <= 0) {
					isStockAvailableForFreeGift = false;
				}
			}
			if (isStockAvailableForFreeGift) {
				if (giftItemSKUs.contains(item.getString("sellerSKU"))) {
					isGiftItemAdded = true;
					continue;
				}
				sellerSKUList.add(item.getString("sellerSKU"));
				sellerSKUAndQuantityMap.put(item.getString("sellerSKU"), item.getInteger("quantity"));
				if (isAvailableStockFoundInDB) {
					updateAvailableStockInRule(rule.getObjectId("_id").toString(), item.getString("sellerSKU"),
							item.getInteger("quantity"));
					isGiftItemAdded = true;
				}
			}
		}
		if (sellerSKUList.isEmpty()) {
			new ArrayList<Document>();
		}
		searchQuery.put("sellerSKU", new Document("$in", sellerSKUList));
		Document projection = new Document();
		projection.put("_id", 0);
		projection.put("itemTitle", 1);
		projection.put("sellerSKU", 1);
		projection.put("quantities", 1);
		List<Document> documentList = table.find(searchQuery).projection(projection).into(new ArrayList<Document>());
		List<Document> freeGiftInventoryList = new ArrayList<Document>();
		for (Document document : documentList) {
			freeGiftInventoryList.add(Document.parse((document).toJson()));
		}
		return freeGiftInventoryList;
	}

	private static void updateAvailableStockInRule(String docId, String sellerSKU, int quantity) {
		Document query = new Document();
		query.put("_id", new ObjectId(docId));
		query.put("action.itemList.sellerSKU", sellerSKU);

		Document updateObj = new Document();
		updateObj.put("action.itemList.$.availableStock", -quantity);

		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("ruleOrder");
		table.updateOne(query, new Document("$inc", updateObj));
	}

	private static Boolean checkConditionSatisfied(List<Document> orderItems, Document order,
			List<Document> conditions) {
		for (Document condition : conditions) {
			if (condition.getString("leftOperand").equals("SKU")) {
				if (!processCondition(condition, orderItems, "SKU")) {
					return false;
				}
			} else if (condition.getString("leftOperand").equals("customSKU")) {
				if (!processCondition(condition, orderItems, "customSKU")) {
					return false;
				}
			} else if (condition.getString("leftOperand").equals("orderSoldAmount")) {
				if (!processCondition(condition, order, "orderSoldAmount")) {
					return false;
				}
			} else if (condition.getString("leftOperand").equals("orderAmount")) {
				if (!processCondition(condition, order, "orderAmount")) {
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

	private static boolean processCondition(Document condition, Object data, String fieldName) {
		if (data instanceof List) {
			List<Document> orderItems = (List<Document>) data;
			for (Document orderItem : orderItems) {
				boolean isConditionSatisfied = false;
				String value = orderItem.get(fieldName).toString();
				if (processOperands(value, condition.get("rightOperand"), condition.getString("operator"))) {
					isConditionSatisfied = true;
					if (!orderItem.containsKey("isConditionSatisfied")) {
						orderItem.put("isConditionSatisfied", isConditionSatisfied);
					}
				} else {
					orderItem.put("isConditionSatisfied", isConditionSatisfied);
				}
			}
			return orderItems.stream().filter(x -> x.getBoolean("isConditionSatisfied")).count() > 0;
		} else if (data instanceof Document
				&& (fieldName.equals("orderSoldAmount") || fieldName.equals("orderAmount"))) {
			Document order = (Document) data;
			Document orderSoldAmountObj = (Document) order.get(fieldName);
			long amount = new BigDecimal(orderSoldAmountObj.get("amount").toString()).longValue();
			return processOperands(amount, condition.get("rightOperand"), condition.getString("operator"));
		} else if (data instanceof Document && fieldName.equals("paymentStatus")) {
			Document order = (Document) data;
			return processOperands(order.getString("paymentStatus"), condition.get("rightOperand"),
					condition.getString("operator"));
		} else if (data instanceof Document && fieldName.equals("timeOrderCreated")) {
			Document order = (Document) data;
			return processOperands(new BigDecimal(order.get("timeOrderCreated").toString()).longValue(), condition.get("rightOperand"),
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
		case "NOT_CONTAINS":
			if (!((List) rightOperand).contains(leftOperand)) {
				return true;
			}
			return false;
		}
		return false;
	}
}
