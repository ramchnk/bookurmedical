package com.sellinall.order.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.enums.Actor;
import com.sellinall.util.enums.SIAOrderCancelReasons;
import com.sellinall.util.enums.StockEventType;

public class SyncProductMaster implements Processor {
	static Logger log = Logger.getLogger(SyncProductMaster.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderItemMessage = exchange.getProperty("orderItemMessage", JSONObject.class);
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		String orderID = orderMessage.getString("orderID");
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String siteName = nickNameID.split("-")[0];
		if (!exchange.getProperty("isWmsSelected", Boolean.class)) {
			log.error("productMaster is not synced, since no wms chosen for accountNumber : " + accountNumber
					+ ", nickNameID : " + exchange.getProperty("nickNameID", String.class));
			return;
		}
		if (exchange.getProperty("processOrdersWithSKUOnly", Boolean.class)
				&& !exchange.getProperty("hasSKU", Boolean.class)) {
			return;
		}
		int quantitySold = orderItemMessage.getInt("quantity");
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange
				.getProperty("notificationOrderActionStatus");
		boolean isOutOfStock = false;
		boolean isNewOrder = OrderUtil.checkIsNewOrder(notificationOrderActionStatus);
		boolean isCancelledOrder = OrderUtil.checkIsCancelledOrder(notificationOrderActionStatus);
		if (isCancelledOrder && orderMessage.has("cancelDetails")) {
			JSONObject cancelDetails = orderMessage.getJSONObject("cancelDetails");
			if (cancelDetails.has("cancelReason") && !cancelDetails.getString("cancelReason").isEmpty()
					&& cancelDetails.getString("cancelReason").equals(SIAOrderCancelReasons.OUT_OF_STOCK.toString())) {
				isOutOfStock = true;
			}
		}
		JSONObject inventoryDBRecordJSON = new JSONObject();
		if (exchange.getProperties().containsKey("inventory")) {
			inventoryDBRecordJSON = new JSONObject(exchange.getProperty("inventory", String.class));
		}
		String sellerSKU = "";
		if (orderItemMessage.has("customSKU")) {
			sellerSKU = orderItemMessage.getString("customSKU");
		} else if (inventoryDBRecordJSON.has("customSKU")) {
			sellerSKU = inventoryDBRecordJSON.getString("customSKU");
		}
		if (!sellerSKU.isEmpty()) {
			String selectedWMS = exchange.getProperty("selectedWMS", String.class);
			String urlPath = "";
			JSONObject payload = new JSONObject();
			String actor = Actor.SALES_CHANNEL.toString();
			String stockEventType = null;
			if (isNewOrder) {
				payload = constructPayload(sellerSKU, -quantitySold, selectedWMS, true);
				urlPath = "quantityDiffs";
				stockEventType = StockEventType.NEW_ORDER.toString();
			} else if (isCancelledOrder) {
				stockEventType = StockEventType.CANCELLED_ORDER.toString();
				if (isOutOfStock) {
					payload = constructPayload(sellerSKU, 0, selectedWMS, false);
					urlPath = "quantities";
				} else {
					payload = constructPayload(sellerSKU, quantitySold, selectedWMS, true);
					urlPath = "quantityDiffs";
				}
			}
			if (payload.length() != 0) {
				payload.put("actor", actor);
				payload.put("stockEventType", stockEventType);
				JSONObject addendum = new JSONObject();
				addendum.put("orderID", orderID);
				addendum.put("nickNameID", orderMessage.getString("nickNameID"));
				payload.put("addendum", addendum);
				String url = Config.getConfig().getSIAInventoryManagementServerURL() + "/productMaster/" + urlPath;
				updateProductMaster(payload, accountNumber, url);
			}
		} else {
			log.error("customSKU not found / empty for orderID : " + orderID + " and accountNumber : " + accountNumber
					+ ", nickNameID : " + nickNameID);
		}
		//to update Sold count
		if (orderItemMessage.has("SKU") && (isNewOrder || isCancelledOrder)) {
			BasicDBObject searchQuery = new BasicDBObject();
			searchQuery.put("accountNumber", accountNumber);
			searchQuery.put("SKU", orderItemMessage.getString("SKU"));
			searchQuery.put(siteName + ".nickNameID", nickNameID);
			BasicDBObject updateobject = new BasicDBObject();
			BasicDBObject incObject = new BasicDBObject();
			if (isNewOrder) {
				incObject.put("noOfItemsold", quantitySold);
				incObject.put(siteName + ".$.noOfItemsold", quantitySold);
			} else if (isCancelledOrder) {
				incObject.put("noOfItemsold", -quantitySold);
				incObject.put(siteName + ".$.noOfItemsold", -quantitySold);
			}
			if (!incObject.isEmpty()) {
				updateobject.put("$inc", incObject);
			}
			MongoCollection<Document> productMasterTable = DbUtilities.getInventoryDBCollection("inventory");
			productMasterTable.findOneAndUpdate(searchQuery, updateobject);
		}
	}

	private void updateProductMaster(JSONObject payload, String accountNumber, String url) {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		headers.put("accountNumber", accountNumber);
		JSONObject response = new JSONObject();
		try {
			response = HttpsURLConnectionUtil.doPut(url, payload.toString(), headers);
			if (response.getInt("httpCode") != HttpStatus.OK_200) {
				log.error("syncProductMaster failed for accountNumber : " + accountNumber + ", sellerSKU : "
						+ payload.getString("sellerSKU"));
				log.error(response.get("payload"));
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error(response);
		} catch (JSONException e) {
			e.printStackTrace();
			log.error(response);
		}
	}

	private JSONObject constructPayload(String sellerSKU, int quantitySold, String selectedWMS,
			boolean isUpdateByQtyDiff) throws JSONException {
		JSONObject payload = new JSONObject();
		JSONArray quantityArray = new JSONArray();
		JSONObject quantityObj = new JSONObject();
		quantityObj.put("warehouseID", selectedWMS);
		quantityObj.put(isUpdateByQtyDiff ? "quantityDiff" : "quantity" , quantitySold);
		quantityArray.put(quantityObj);
		payload.put("sellerSKU", sellerSKU);
		if (isUpdateByQtyDiff) {
			payload.put("quantityDiffs", quantityArray);
		} else {
			payload.put("quantities", quantityArray);
		}
		return payload;
	}

}
