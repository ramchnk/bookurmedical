/**
 * 
 */
package com.sellinall.order.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.InvoiceSequence;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.UserMessageName;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateOrderDBQuery implements Processor {

	static Logger log = Logger.getLogger(UpdateOrderDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = new JSONObject(exchange.getIn().getBody(String.class));
		exchange.setProperty("stopProcess", false);
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange
				.getProperty("notificationOrderActionStatus");
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		JSONObject orderMessageJSON = exchange.getProperty("message", JSONObject.class);
		exchange.setProperty("isOrderUpdatedByShippingCarrier", false);
		if (orderMessageJSON.has("isOrderUpdatedByShippingCarrier")
				&& orderMessageJSON.getBoolean("isOrderUpdatedByShippingCarrier")) {
			exchange.setProperty("isOrderUpdatedByShippingCarrier", true);
		}
		BasicDBObject orderMessage = (BasicDBObject) JSON.parse(orderMessageJSON.toString());
		exchange.setProperty("isNewOrder", false);
		if (!hasOrderInDB) {
			insertOrderRecord(exchange, notificationOrderActionStatus, orderMessage, inBody);
			exchange.setProperty("isNewOrder", true);
			return;
		}
		updateOrderRecord(exchange, notificationOrderActionStatus, orderMessage);
	}

	private void insertOrderRecord(Exchange exchange, NotificationOrderActionStatus notificationOrderActionStatus,
			BasicDBObject orderMessage, JSONObject inBody) throws Exception {
		BasicDBObject site = new BasicDBObject();
		String nickNameID = orderMessage.getString("nickNameID");
		String accountNumber = orderMessage.getString("accountNumber");
		String orderID = orderMessage.getString("orderID");
		String siteName = orderMessage.getString("site");
		site.put("name", siteName);
		site.put("nickNameID", nickNameID);
		BasicDBObject orderRecord = new BasicDBObject();
		orderRecord.put("site", site);
		orderRecord.put("orderID", orderID);
		if (orderMessage.containsField("invoiceNumber")) {
			orderRecord.put("invoiceNumber", orderMessage.get("invoiceNumber"));
		} else if (exchange.getProperties().containsKey("profileID")) {
			String profileID = exchange.getProperty("profileID", String.class);
			String merchantID = exchange.getProperty("merchantID", String.class);
			String invoiceNumberPrefix = exchange.getProperty("invoiceNumberPrefix", String.class);
			String invoiceNumber = invoiceNumberPrefix + InvoiceSequence.getNextInvoiceSequence(merchantID, profileID);
			orderRecord.put("invoiceNumber", invoiceNumber);
		}
		// TODO: remove the condition after all publishers start publishing user
		// id.
		orderRecord.put("accountNumber", accountNumber);
		if (exchange.getProperties().containsKey("isManaged") && exchange.getProperty("isManaged", Boolean.class)) {
			orderRecord.put("isManaged", exchange.getProperty("isManaged", Boolean.class));
		}
		if (exchange.getProperties().containsKey("isTransactionFee")
				&& exchange.getProperty("isTransactionFee", boolean.class)) {
			orderRecord.put("isTransactionFee", exchange.getProperty("isTransactionFee", boolean.class));
		}
		fillOrderRecord(notificationOrderActionStatus, orderRecord, orderMessage);
		//TODO: need to remove isWhatsAppEnabled after whatsapp approval
		if (orderRecord.containsField("isNotifyOrderUpdates") && Config.getConfig().getWhatsAppEnabled()) {
			exchange.setProperty("isNotifyOrderUpdates", orderRecord.getBoolean("isNotifyOrderUpdates"));
			orderRecord.remove("isNotifyOrderUpdates");
			if (orderRecord.getString("orderStatus").equals(SIAOrderStatus.CANCELLED.toString())) {
				exchange.setProperty("userMessageName", UserMessageName.ORDER_CANCELLED.toString());
			} else if (orderRecord.getString("orderStatus").equals(SIAOrderStatus.DELIVERED.toString())) {
				exchange.setProperty("userMessageName", UserMessageName.ORDER_DELIVERED.toString());
			} else if (orderRecord.getString("orderStatus").equals(SIAOrderStatus.ACCEPTED.toString())
					|| orderRecord.getString("orderStatus").equals(SIAOrderStatus.PROCESSING.toString())) {
				exchange.setProperty("userMessageName", UserMessageName.ORDER_ACCEPTED.toString());
			}
		}
		fillOrderAmountInUSD(orderRecord);
		List<String> notificationIDList = new ArrayList<String>();
		if (orderMessage.containsKey("notificationID")) {
			notificationIDList.add(orderMessage.getString("notificationID"));
			orderRecord.put("notificationID", notificationIDList);
		}
		orderRecord.put("timeCreated", DateUtil.getSIADateFormat());
		orderRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		if (orderMessage.containsField("timeOrderCreated")) {
			orderRecord.put("timeOrderCreated", orderMessage.getLong("timeOrderCreated"));
		} else {
			orderRecord.put("timeOrderCreated", System.currentTimeMillis() / 1000);
		}
		if (orderMessage.containsField("timeOrderUpdated")) {
			orderRecord.put("timeOrderUpdated",  orderMessage.getLong("timeOrderUpdated"));
		}
		if (orderMessage.containsField("timeOrderReturnRequested")) {
			orderRecord.put("timeOrderReturnRequested",  orderMessage.getLong("timeOrderReturnRequested"));
		}
		if (orderMessage.containsField("shippingAmount")) {
			orderRecord.put("shippingAmount", orderMessage.get("shippingAmount"));
		}
		fillTransactionKeyValuePair(orderRecord, "finalShippingFeePaidToChannel", orderMessage);
		exchange.setProperty("accountNumber", orderRecord.getString("accountNumber"));
		exchange.setProperty("groupOrderByCartNumber", false);
		if (orderMessage.containsField("cartNumber")) {
			String cartNumber = (String) orderMessage.get("cartNumber");
			orderRecord.put("cartNumber", cartNumber);
			int totalOrderItemsInCart = 0;
			if (orderMessage.containsField("totalOrderItemsInCart")) {
				totalOrderItemsInCart = orderMessage.getInt("totalOrderItemsInCart");
				orderRecord.put("totalOrderItemsInCart", totalOrderItemsInCart);
			}
			checkIfgroupOrderByCartNumberNeeded(exchange,totalOrderItemsInCart,cartNumber);
		}		
		caculateAndStoreOrderSoldAmount(orderMessage, orderRecord);
		fillAdditionDetails(exchange, orderRecord, siteName);
		if (!checkIsValidOrderForAccount(orderRecord)) {
			exchange.setProperty("stopProcess", true);
			return;
		}
		if (exchange.getProperties().containsKey("isPartnerLogistics")
				&& exchange.getProperties().containsKey("airwayBillExists")) {
			orderRecord.put("isPartnerLogistics", exchange.getProperty("isPartnerLogistics"));
		}
		if (orderRecord.containsField("orderItems")) {
			List<BasicDBObject> orderItems = (List<BasicDBObject>) orderRecord.get("orderItems");
			if (orderItems.size() == 0) {
				log.error("Insert - orderItems List is Empty for this orderId: " + orderMessage.getString("orderID"));
			}
		}
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("orderID", orderID);
		searchQuery.put("site.nickNameID", nickNameID);
		searchQuery.put("site.name", siteName);
		UpdateOptions options = new UpdateOptions();
		options.upsert(true);
		try {
			Document orderDocument = getDocument(orderRecord);
			table.insertOne(orderDocument);
			orderRecord.put("_id", orderDocument.getObjectId("_id"));
		} catch (MongoWriteException e) {
			log.info("Order Insert - Duplicate message received for orderID: " + orderID);
			exchange.setProperty("stopProcess", true);
			return;

		}
		exchange.setProperty("orderRecord", orderRecord);
		exchange.getOut().setBody(orderMessage);
	}

	private void checkIfgroupOrderByCartNumberNeeded(Exchange exchange, int totalOrderItemsInCart, String cartNumber) {
		exchange.setProperty("groupOrderByCartNumber", false);
		if (totalOrderItemsInCart > 1) {
			exchange.setProperty("groupOrderByCartNumber", true);
		}
		exchange.setProperty("totalOrderItemsInCart", totalOrderItemsInCart);
		exchange.setProperty("cartNumber", cartNumber);
	}

	private void fillOrderAmountInUSD(BasicDBObject orderRecord) {
		if (orderRecord.containsField("orderAmount")) {
			BasicDBObject orderAmount = (BasicDBObject) orderRecord.get("orderAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderAmount.getString("currencyCode"), "USD");
				if (exchangeRate == 0) {
					log.error("orderAmountInUSD field is not set for the orderID: " + orderRecord.getString("orderID"));
					return;
				}
				long amount = Math.round(orderAmount.getLong("amount") * exchangeRate);
				DBObject orderAmountInUSD = CurrencyUtil.getAmountObject(amount, "USD");
				orderRecord.put("orderAmountInUSD", orderAmountInUSD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static double getExchangeRateFromApi(String fromCurrency, String toCurrency)
			throws JSONException, IOException {
		if (fromCurrency.equals(toCurrency)) {
			return 1;
		}
		String url = Config.getConfig().getSIAfeeManagementServerURL() + "/exchange?fromCurrency=" + fromCurrency
				+ "&toCurrency=" + toCurrency;
		JSONObject response = HttpsURLConnectionUtil.doGet(url, null);
		log.debug("exchange rate:" + response);
		int httpCode = response.getInt("httpCode");
		if (httpCode == HttpStatus.OK_200) {
			JSONObject payload = new JSONObject(response.getString("payload"));
			double exchangeRate = payload.getDouble("exchangeRate");
			return exchangeRate;
		} else {
			log.error("Get " + url + " failed with status code " + httpCode + " and the response is: " + response);
			return 0;
		}
	}

	private Boolean checkIsValidOrderForAccount(BasicDBObject orderRecord) {
		if (!orderRecord.containsField("orderItems")) {
			return false;
		}
		List<BasicDBObject> orderItems = (List<BasicDBObject>) orderRecord.get("orderItems");
		if (orderItems.size() == 0 || orderItems == null) {
			return false;
		}
		return true;
	}

	private void updateOrderRecord(Exchange exchange, NotificationOrderActionStatus notificationOrderActionStatus,
			BasicDBObject orderMessage) throws Exception {
		BasicDBObject orderRecord = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		searchQuery.put("orderID", orderMessage.getString("orderID"));
		String siteName = orderMessage.getString("site");
		searchQuery.put("site.name", siteName);
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));

		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		if (exchange.getProperties().containsKey("isManaged") && exchange.getProperty("isManaged", Boolean.class)) {
			orderRecord.put("isManaged", exchange.getProperty("isManaged", Boolean.class));
		}

		String updateStatus = OrderUpdateStatus.COMPLETE.toString();
		if (orderMessage.containsField("updateStatus")) {
			updateStatus = orderMessage.getString("updateStatus");
		}
		if (orderMessage.getBoolean("isReconciliation")) {
			exchange.setProperty("isReconciliation", orderMessage.getBoolean("isReconciliation"));
			exchange.setProperty("orderItemList", new JSONArray(orderMessage.get("orderItems").toString()));
		}
		if (orderMessage.containsField("timeSettled")) {
			orderRecord.put("timeSettled", orderMessage.getLong("timeSettled"));
		}
		if (orderMessage.containsField("timeSettlementProcessed")) {
			orderRecord.put("timeSettlementProcessed", orderMessage.getLong("timeSettlementProcessed"));
		}
		if (orderMessage.containsField("settlementStatus")) {
			orderRecord.put("settlementStatus", orderMessage.getString("settlementStatus"));
		}
		if (orderMessage.containsField("returnSettlementStatus")) {
			orderRecord.put("returnSettlementStatus", orderMessage.getString("returnSettlementStatus"));
		}
		if (orderMessage.containsField("transactionPeriod")) {
			orderRecord.put("transactionPeriod", orderMessage.getString("transactionPeriod"));
		}
		if (orderMessage.containsField("timeOrderUpdated")) {
			orderRecord.put("timeOrderUpdated", orderMessage.getLong("timeOrderUpdated"));
		}
		if (orderMessage.containsField("timeOrderReturnRequested")) {
			orderRecord.put("timeOrderReturnRequested", orderMessage.getLong("timeOrderReturnRequested"));
		}
		if (exchange.getProperties().containsKey("isPartnerLogistics")
				&& exchange.getProperties().containsKey("airwayBillExists")) {
			orderRecord.put("isPartnerLogistics", exchange.getProperty("isPartnerLogistics"));
		}
		// update order data only when the update is complete
		if (OrderUpdateStatus.COMPLETE.toString().equals(updateStatus)) {
			fillOrderRecord(notificationOrderActionStatus, orderRecord, orderMessage);
			//TODO: need to remove isWhatsAppEnabled after whatsapp approval
			if (orderRecord.containsField("isNotifyOrderUpdates") && Config.getConfig().getWhatsAppEnabled()) {
				exchange.setProperty("isNotifyOrderUpdates", orderRecord.getBoolean("isNotifyOrderUpdates"));
				orderRecord.remove("isNotifyOrderUpdates");
				if (orderRecord.getString("orderStatus").equals(SIAOrderStatus.CANCELLED.toString())) {
					exchange.setProperty("userMessageName", UserMessageName.ORDER_CANCELLED.toString());
				} else if (orderRecord.getString("orderStatus").equals(SIAOrderStatus.DELIVERED.toString())) {
					exchange.setProperty("userMessageName", UserMessageName.ORDER_DELIVERED.toString());
				} else if (orderRecord.getString("orderStatus").equals(SIAOrderStatus.ACCEPTED.toString())
						|| orderRecord.getString("orderStatus").equals(SIAOrderStatus.PROCESSING.toString())) {
					exchange.setProperty("userMessageName", UserMessageName.ORDER_ACCEPTED.toString());
				}
			}
			fillAdditionDetails(exchange, orderRecord, siteName);
			fillOrderAmountInUSD(orderRecord);
		}
		caculateAndStoreOrderSoldAmount(orderMessage, orderRecord);
		orderRecord.put("updateStatus", updateStatus);
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		if (orderRecord.containsField("orderItems")) {
			if (orderRecord.get("orderItems") != null) {
				List<BasicDBObject> orderItems = (List<BasicDBObject>) orderRecord.get("orderItems");
				if (orderItems.size() == 0) {
					log.error("Update - orderItems List is Empty for this orderId: "
						+ orderMessage.getString("orderID"));
				}
			} else {
				log.error("Null orderItem came for orderID : " + orderMessage.getString("orderID"));
			}
		}
		// if we pass true then will modified data
		UpdateResult result = table.updateOne(searchQuery, new BasicDBObject("$set", orderRecord));
		if (result.getModifiedCount() == 0) {
			log.info("Order :" + orderMessage.getString("orderID") + " is already updated. this is duplicate message.");
			exchange.setProperty("stopProcess", true);
			return;
		}
		BasicDBObject order = updateAndGetLatestUpdatedOrder(searchQuery, orderMessage);
		if (order.containsField("totalOrderItemsInCart") && order.containsField("cartNumber")) {
			checkIfgroupOrderByCartNumberNeeded(exchange, order.getInt("totalOrderItemsInCart"), order.getString("cartNumber"));
		}
		exchange.setProperty("orderRecord", order);
		exchange.getOut().setBody(orderMessage);
	}

	private void caculateAndStoreOrderSoldAmount(BasicDBObject orderMessage, BasicDBObject orderRecord) {
		if (orderMessage.containsField("orderAmount")) {
			BasicDBObject orderAmount = (BasicDBObject) orderMessage.get("orderAmount");
			String currencyCode = orderAmount.getString("currencyCode");
			long orderSoldAmount = orderAmount.getLong("amount");
			if (orderMessage.containsField("voucherAmount")) {
				BasicDBObject voucherAmount = (BasicDBObject) orderMessage.get("voucherAmount");
				orderSoldAmount = orderSoldAmount - voucherAmount.getLong("amount");
			} else if (orderMessage.containsField("sellerDiscountAmount")) {
				BasicDBObject sellerDiscountAmount = (BasicDBObject) orderMessage.get("sellerDiscountAmount");
				orderSoldAmount = orderSoldAmount - sellerDiscountAmount.getLong("amount");
			}
			orderRecord.put("orderSoldAmount", CurrencyUtil.getAmountObject(orderSoldAmount, currencyCode));
			fillOrderSoldAmountInUSD(orderRecord);
		}
	}

	private void fillOrderSoldAmountInUSD(BasicDBObject orderRecord) {
		if (orderRecord.containsField("orderSoldAmount")) {
			BasicDBObject orderSoldAmount = (BasicDBObject) orderRecord.get("orderSoldAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderSoldAmount.getString("currencyCode"), "USD");
				if (exchangeRate == 0) {
					log.error("orderSoldAmountInUSD field is not set for the orderID: "
							+ orderRecord.getString("orderID"));
					return;
				}
				long amount = Math.round(orderSoldAmount.getLong("amount") * exchangeRate);
				DBObject orderSoldAmountInUSD = CurrencyUtil.getAmountObject(amount, "USD");
				orderRecord.put("orderSoldAmountInUSD", orderSoldAmountInUSD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Document getDocument(BasicDBObject doc) {
		if (doc == null)
			return null;
		return new Document(doc.toMap());
	}
	private BasicDBObject updateAndGetLatestUpdatedOrder(BasicDBObject searchQuery, BasicDBObject orderMessage) {
		BasicDBObject updateObject = new BasicDBObject();
		if (orderMessage.containsKey("notificationID")) {
			// Append the OrderNotificationID to the database
			updateObject.put("$push", new BasicDBObject("notificationID", orderMessage.get("notificationID")));
		}
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.returnDocument(ReturnDocument.AFTER);
		BasicDBObject update = new BasicDBObject("timeLastUpdated", DateUtil.getSIADateFormat());
		updateObject.put("$set", update);
		Document orderDoc = table.findOneAndUpdate(searchQuery, updateObject, options);
		BasicDBObject order = (BasicDBObject) JSON.parse(orderDoc.toJson());
		return order;
	}

	private void fillOrderRecord(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord,
			BasicDBObject orderMessage) {
		fillTransactionKeyValuePair(orderRecord, "buyerDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderNumber", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "returnOrderID", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentMethods", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentType", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderItems", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingCarrierStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderStatuses", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentStatuses", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingStatuses", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "pickUpDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "dropoffBranchList", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "refundDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "returnDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "cancelDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "sellerDiscountAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "channelDiscountAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "sellerCartDiscount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "channelCartDiscount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "voucherAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "documents", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "sellerVoucherAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "sellerVoucherCodes", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "channelVoucherCodes", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "bundledPromotionItems", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "finalShippingFeePaidToChannel", orderMessage);
		fillOrderTime(notificationOrderActionStatus, orderRecord, orderMessage);
	}

	@SuppressWarnings("unchecked")
	private void fillAdditionDetails(Exchange exchange, BasicDBObject orderRecord, String siteName)
			throws Exception {
		Map<String, BasicDBObject> inventoryDetailsMap = (Map<String, BasicDBObject>) exchange
				.getProperty("inventoryDetailsMap");
		String orderID = exchange.getProperty("orderID", String.class);
		boolean addOrderItemLocation = false;
		boolean processOrdersWithSKUOnly = processOrdersWithtSKUOnly(exchange);
		List<BasicDBObject> newOrderItems = new ArrayList<BasicDBObject>();
		List<BasicDBObject> freeGiftItems = new ArrayList<BasicDBObject>();
		List<String> orderItemIDListFromDB = new ArrayList<String>();
		Map<String, String> orderItemsStatusMap = new HashMap<String, String>();
		int orderDBObjectSize = 0;
		if ((Boolean) exchange.getProperty("hasOrderInDB")) {
			JSONObject orderDBObject = new JSONObject(exchange.getProperty("orderDBObject").toString());
			JSONArray items = orderDBObject.getJSONArray("orderItems");
			orderDBObjectSize = items.length();
			for (int i = 0; i < items.length(); i++) {
				if (items.getJSONObject(i).has("orderItemID")) {
					orderItemIDListFromDB.add(items.getJSONObject(i).getString("orderItemID"));
				}
				if(items.getJSONObject(i).has("isFreeGift") && items.getJSONObject(i).getBoolean("isFreeGift")) {
					freeGiftItems.add(BasicDBObject.parse(items.getJSONObject(i).toString()));
				}
				if (exchange.getProperties().containsKey("isStatusHandledInOrderItem")
						&& exchange.getProperty("isStatusHandledInOrderItem", Boolean.class)
						&& items.getJSONObject(i).has("orderStatus") && items.getJSONObject(i).has("orderItemID")) {
					orderItemsStatusMap.put(items.getJSONObject(i).getString("orderItemID"),
							items.getJSONObject(i).getString("orderStatus"));
				}
			}
		}

		if (orderRecord.containsField("orderItems")) {
			List<BasicDBObject> orderItems = (ArrayList<BasicDBObject>) orderRecord.get("orderItems");
			if (orderRecord.containsField("orderStatus")
					&& orderRecord.getString("orderStatus").equals(SIAOrderStatus.INITIATED.toString())
					&& exchange.getProperty("hasOrderInDB", Boolean.class) && orderDBObjectSize != orderItems.size()) {
				return;
			}
			for (int i = 0; i < orderItems.size(); i++) {
				BasicDBObject orderItem = orderItems.get(i);
				if (exchange.getProperties().containsKey("isStatusHandledInOrderItem")
						&& exchange.getProperty("isStatusHandledInOrderItem", Boolean.class)
						&& orderItem.containsField("orderStatus") && orderItem.containsField("orderItemID")) {
					String orderItemID = orderItem.getString("orderItemID");
					if (orderItemsStatusMap.containsKey(orderItemID)) {
						SIAOrderStatus orderItemDBStatus = SIAOrderStatus.valueOf(orderItemsStatusMap.get(orderItemID));
						SIAOrderStatus notificationOrderStatus = SIAOrderStatus
								.valueOf(orderItem.getString("orderStatus"));
						NotificationOrderActionStatus notificationOrderActionStatus = OrderUtil
								.handleExistingOrderStatus(notificationOrderStatus, orderItemDBStatus,
										new JSONObject(orderItem.toString()), orderID, "orderItem");
						if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.NO_ACTION)) {
							orderItem.put("orderStatus", orderItemDBStatus.toString());
						}
					}
				}
				if (!orderItem.containsField("settlementAmount")) {
					processSettlementAmountOrderItem(orderItem, i, exchange);
				}
				boolean orderHasInventory = false;
				if(orderItem.containsField("isFreeGift") && orderItem.getBoolean("isFreeGift")) {
					//Free gift only set and handled in PNQ.so can be removed if incoming message has freeGift items.
					//From DB existing free gift items will be retained.
					continue;
				}
				if (orderItem.containsField("SKU")) {
					String SKU = orderItem.getString("SKU");
					BasicDBObject inventoryValue = inventoryDetailsMap.get(SKU);
					if (inventoryValue != null) {
						orderHasInventory = true;
						if (!orderItem.containsField("itemTitle")) {
							orderItem.put("itemTitle", inventoryValue.getString("itemTitle"));
						}
						if (!getImageURL(inventoryValue, siteName).isEmpty()) {
							orderItem.put("imageURL", getImageURL(inventoryValue, siteName));
						}
						if (inventoryValue.containsField("variantDetails")) {
							orderItem.put("variantDetails", inventoryValue.get("variantDetails"));
						}
						if (inventoryValue.containsField("customSKU")) {
							orderItem.put("customSKU", inventoryValue.get("customSKU"));
						}
						if (inventoryValue.containsField("hsnCode")) {
							orderItem.put("hsnCode", inventoryValue.get("hsnCode"));
						}
						BasicDBObject site = (BasicDBObject) inventoryValue.get(siteName);
						if (site.containsKey("isOption")) {
							orderItem.put("isOption", site.getBoolean("isOption"));
						} else {
							orderItem.put("isOption", false);
						}
						if (site.containsField("categoryName")) {
							orderItem.put("categoryName", site.get("categoryName"));
						}
						if (site.containsField("categoryID")) {
							orderItem.put("categoryID", site.get("categoryID"));
						}
						if (siteName.equals("eBay") && !addOrderItemLocation) {
							addOrderItemLocation = getItemLocation(inventoryValue, siteName, orderRecord);
						}
					} else {
						// If inventory deleted then
						orderItem.remove("SKU");
						orderItem.remove("imageURL");
					}
				}
				if (orderItem.containsField("itemAmount")) {
					orderItem.put("itemSoldAmount", (BasicDBObject) orderItem.get("itemAmount"));
					if (orderItem.containsField("sellerDiscountAmount")) {
						BasicDBObject itemAmountObject = (BasicDBObject) orderItem.get("itemAmount");
						long itemAmount = itemAmountObject.getLong("amount");
						BasicDBObject sellerDiscountAmountObject = (BasicDBObject) orderItem
								.get("sellerDiscountAmount");
						long sellerDiscountAmount = sellerDiscountAmountObject.getLong("amount");
						String currencyCode = itemAmountObject.getString("currencyCode");
						long itemSoldAmount = itemAmount - sellerDiscountAmount;
						orderItem.put("itemSoldAmount",
								JSON.parse(CurrencyUtil.getJSONAmountObject(itemSoldAmount, currencyCode).toString()));
					}
				}
				if (orderItem.containsField("totalItemAmount")) {
					orderItem.put("totalItemSoldAmount", (BasicDBObject) orderItem.get("totalItemAmount"));
					if (orderItem.containsField("totalSellerDiscountAmount")) {
						BasicDBObject totalItemAmount = (BasicDBObject) orderItem.get("totalItemAmount");
						long itemAmount = totalItemAmount.getLong("amount");
						BasicDBObject totalSellerDiscountAmount = (BasicDBObject) orderItem
								.get("totalSellerDiscountAmount");
						long sellerDiscountAmount = totalSellerDiscountAmount.getLong("amount");
						String currencyCode = totalItemAmount.getString("currencyCode");
						long itemSoldAmount = itemAmount - sellerDiscountAmount;
						orderItem.put("totalItemSoldAmount",
								JSON.parse(CurrencyUtil.getJSONAmountObject(itemSoldAmount, currencyCode).toString()));
					}
				}
				if (processOrdersWithSKUOnly) {
					// For managed accounts, add orderItem to list, only it has
					// SKU
					if (orderHasInventory || orderItemIDListFromDB.contains(orderItem.getString("orderItemID"))) {
						newOrderItems.add(orderItem);
					}
				} else {
					newOrderItems.add(orderItem);
				}
				if(orderItem.containsField("settlementDetails"))
				{
					BasicDBObject settlementDetails = (BasicDBObject) orderItem.get("settlementDetails");
					if (settlementDetails.containsField("refunded")) {
						BasicDBObject refunded = (BasicDBObject) settlementDetails.get("refunded");
						removeFeesFields(refunded);
					}
				}
				removeFeesFields(orderItem);
			}
			if(freeGiftItems.size()>0) {
				//Retain freegift item from DB.
				newOrderItems.addAll(freeGiftItems);
			}
			orderRecord.put("orderItems", newOrderItems);
		}
	}

	private void removeFeesFields(BasicDBObject refunded) {
		refunded.remove("expectedMarketPlaceCommission");
		refunded.remove("feesFieldsToUpdate");
		refunded.remove("shippingFeePaidToChannelVAT");
		refunded.remove("sponsoredAffiliatesFee");
		refunded.remove("sponsoredAffiliatesFeeVAT");
		refunded.remove("refundSponsoredAffiliatesFee");
		refunded.remove("refundSponsoredAffiliatesFeeVAT");
	}

	private void processSettlementAmountOrderItem(BasicDBObject orderItem, int orderItemIndex, Exchange exchange) {
		if (exchange.getProperty("hasOrderInDB", Boolean.class)) {
			BasicDBObject orderDBObject = exchange.getProperty("orderDBObject", BasicDBObject.class);
			if (orderDBObject.containsField("orderItems")) {
				BasicDBList orderItems = (BasicDBList) orderDBObject.get("orderItems");
				BasicDBObject orderItemDB = (BasicDBObject) orderItems.get(orderItemIndex);
				if (orderItemDB.containsField("settlementAmount")) {
					fillTransactionKeyValuePair(orderItem, "settlementAmount", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "settlementStatus", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "returnSettlementStatus", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "transactionPeriod", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "timeSettled", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "timeSettlementProcessed", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "shippingFeeRebateFromChannel", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "buyerPaidAmount", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "shippingFeePaidToChannel", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "shippingFeeRebateFromChannel", orderItemDB);
					fillTransactionKeyValuePair(orderItem, "settlementDetails", orderItemDB);
				}
			}
		}
	}

	private boolean processOrdersWithtSKUOnly(Exchange exchange) {
		BasicDBObject userSiteSpecificObject = exchange.getProperty("userSiteSpecificObject", BasicDBObject.class);
		if (!userSiteSpecificObject.containsField("processOrdersWithSKUOnly")) {
			return false;
		}
		return userSiteSpecificObject.getBoolean("processOrdersWithSKUOnly");
	}

	@SuppressWarnings("unchecked")
	private String getImageURL(BasicDBObject inventoryValues, String siteName) {
		String imageURL = inventoryValues.getString("imageURL");
		BasicDBObject site = (BasicDBObject) inventoryValues.get(siteName);
		List<String> imageURIs = (List<String>) site.get("imageURI");

		if (imageURIs != null && imageURIs.size() > 0) {
			String imageURI = imageURIs.get(0);
			String[] splitImageURI = imageURI.split("/");
			return imageURL + splitImageURI[0] + "/thumbnail/" + splitImageURI[1];
		} else {
			return "";
		}
	}

	@SuppressWarnings("unchecked")
	private boolean getItemLocation(BasicDBObject inventoryValues, String siteName, BasicDBObject orderRecord) {
		BasicDBObject site = (BasicDBObject) inventoryValues.get(siteName);
		if (site.containsField("itemLocation") && site.get("itemLocation") != null) {
			orderRecord.put("itemLocation", site.get("itemLocation"));
			return true;
		}
		return false;
	}

	private void fillTransactionKeyValuePair(BasicDBObject orderRecord, String key, BasicDBObject orderMessage) {
		if (orderMessage.containsField(key)) {
			orderRecord.put(key, orderMessage.get(key));
		}
	}

	private void fillOrderTime(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord, BasicDBObject orderMessage) {
		// TODO: need to get more insights on how these dates can be used, so as
		// of now ignoring other state transition timestamps
		if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_PROCESSING)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_PROCESSING)) {
			orderRecord.put("timeProcessing", DateUtil.getSIADateFormat());
			orderRecord.put("isNotifyOrderUpdates", true);
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_ACCEPTED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_ACCEPTED)) {
			orderRecord.put("timeAccepted", DateUtil.getSIADateFormat());
			orderRecord.put("isNotifyOrderUpdates", true);
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_DISPATCHED)) {
			// for offline orders update timeDispatched by user.
			if (orderMessage.containsField("timeDispatched")) {
				orderRecord.put("timeDispatched", orderMessage.get("timeDispatched"));
			} else {
				orderRecord.put("timeDispatched", DateUtil.getSIADateFormat());
			}
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERY_FAILED_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_DELIVERED)) {
			//for offline orders update timeDelivered by user.
			if (orderMessage.containsField("timeDelivered")) {
				orderRecord.put("timeDelivered", orderMessage.get("timeDelivered"));
			} else {
				orderRecord.put("timeDelivered", DateUtil.getSIADateFormat());
			}
			orderRecord.put("isNotifyOrderUpdates", true);
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_PENDING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_CANCELLED)) {
			orderRecord.put("timeCancelled", DateUtil.getSIADateFormat());
			orderRecord.put("isNotifyOrderUpdates", true);
		}  else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURN_REQUESTED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_RETURNED)) {
			orderRecord.put("timeReturned", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.LOST_BY_3PL)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_LOST_BY_3PL)) {
			orderRecord.put("timeLostBy3PL", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.DAMAGE_BY_3PL)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_DAMAGE_BY_3PL)) {
			orderRecord.put("timeDamageBy3PL", DateUtil.getSIADateFormat());
		}
	}

}