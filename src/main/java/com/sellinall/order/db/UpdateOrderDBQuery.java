/**
 * 
 */
package com.sellinall.order.db;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.HashUtil;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.InvoiceSequence;
import com.sellinall.util.enums.OrderFulfilledBy;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAErpUpdateStatuses;
import com.sellinall.util.enums.SIAOmsUpdateStatuses;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAShippingCarrierUpdateStatuses;
import com.sellinall.util.enums.SIAWmsUpdateStatuses;
import com.sellinall.util.enums.UserMessageName;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateOrderDBQuery implements Processor {

	static Logger log = Logger.getLogger(UpdateOrderDBQuery.class.getName());
	public static final int MC_MAX_EXPIRE_TIME = 15 * 60;

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = OrderUtil.parseToJsonObject(Document.parse(exchange.getIn().getBody(String.class)));
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
		exchange.setProperty("isFreeGiftAddedToOrder", true);
		Document orderMessage = Document.parse(orderMessageJSON.toString());
		String nickNameID = orderMessage.getString("nickNameID");
		String channel = nickNameID.split("-")[0];
		if (!channel.equalsIgnoreCase("offline")) {
			boolean isValidDocUrl = checkValidDocumentUrl(orderMessage);
			if (!isValidDocUrl) {
				exchange.setProperty("stopProcess", true);
				return;
			}
		}
		exchange.setProperty("isNewOrder", false);
		if (!hasOrderInDB) {
			insertOrderRecord(exchange, notificationOrderActionStatus, orderMessage, inBody);
			checkIsOrderFulfilledByChannel(exchange, orderMessageJSON);
			exchange.setProperty("isNewOrder", true);
			exchange.setProperty("isFreeGiftAddedToOrder", false);
			return;
		}
		updateOrderRecord(exchange, notificationOrderActionStatus, orderMessage);
		checkIsOrderFulfilledByChannel(exchange, orderMessageJSON);
	}

	private boolean checkValidDocumentUrl(Document orderMessage) {
		if (orderMessage.containsKey("documents")
				&& ((Document) orderMessage.get("documents")).containsKey("shippingLabelUrl")) {
			String url = ((Document) orderMessage.get("documents")).getString("shippingLabelUrl");
			if (url.contains(Config.getConfig().getDocUploadPath())) {
				String orderID = orderMessage.get("orderID").toString();
				List<String> values = Arrays.asList(url.split("/"));
				String fileName = values.get(values.size() - 1);
				if (fileName.split("[.]")[0].isEmpty()) {
					log.warn("shipping label url not matched with orderID : " + orderID + ", url : " + url);
					return true;
				}
				if (!fileName.split("[.]")[0].equals(orderID)) {
					log.error("shipping label url not matched with orderID : " + orderID + ", url : " + url);
					return false;
				}
			}
		}
		return true;
	}

	private void insertOrderRecord(Exchange exchange, NotificationOrderActionStatus notificationOrderActionStatus,
			Document orderMessage, JSONObject inBody) throws Exception {
		Document site = new Document();
		String nickNameID = orderMessage.getString("nickNameID");
		String accountNumber = orderMessage.get("accountNumber").toString();
		String orderID = orderMessage.get("orderID").toString();
		String siteName = orderMessage.getString("site");
		site.put("name", siteName);
		site.put("nickNameID", nickNameID);
		Document orderRecord = new Document();
		orderRecord.put("site", site);
		orderRecord.put("orderID", orderID);
		if (orderMessage.containsKey("invoiceNumber")) {
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
		if (Config.getConfig().getIsEligibleToUpdateBrandID()) {
			updateBradIDInOrderItem(exchange, orderMessage);
		}
		fillOrderRecord(exchange, notificationOrderActionStatus, orderRecord, orderMessage);
		// TODO: need to remove isWhatsAppEnabled after whatsapp approval
		if (orderRecord.containsKey("isNotifyOrderUpdates") && Config.getConfig().getWhatsAppEnabled()) {
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
		if (orderMessage.containsKey("timeOrderCreated")) {
			orderRecord.put("timeOrderCreated", new BigDecimal(orderMessage.get("timeOrderCreated").toString()).longValue());
		} else {
			orderRecord.put("timeOrderCreated", System.currentTimeMillis() / 1000);
		}
		if (orderMessage.containsKey("timeOrderCancelled")) {
			orderRecord.put("timeOrderCancelled", new BigDecimal(orderMessage.get("timeOrderCancelled").toString()).longValue());
		}
		if (orderMessage.containsKey("timeOrderUpdated")) {
			orderRecord.put("timeOrderUpdated", new BigDecimal(orderMessage.get("timeOrderUpdated").toString()).longValue());
		}
		if (orderMessage.containsKey("timeOrderReturnRequested")) {
			
			orderRecord.put("timeOrderReturnRequested", new BigDecimal(orderMessage.get("timeOrderReturnRequested").toString()).longValue());
		}
		if (orderMessage.containsKey("shippingAmount")) {
			orderRecord.put("shippingAmount", orderMessage.get("shippingAmount"));
		}
		if (orderMessage.containsKey("isPosOrder")) {
			orderRecord.put("isPosOrder", orderMessage.getBoolean("isPosOrder"));
		}
		if (orderMessage.containsKey("sourceChannel")) {
			orderRecord.put("sourceChannel", orderMessage.get("sourceChannel"));
		}
		if (orderMessage.containsKey("channelApiVersion")) {
			orderRecord.put("channelApiVersion", orderMessage.get("channelApiVersion"));
		}
		if (orderMessage.containsKey("packageWeight")) {
			orderRecord.put("packageWeight", orderMessage.get("packageWeight"));
		}
		if (orderMessage.containsKey("orderTypes")) {
			orderRecord.put("orderTypes", orderMessage.get("orderTypes"));
		}
		if (orderMessage.containsKey("shippingTypes")) {
			orderRecord.put("shippingTypes", orderMessage.get("shippingTypes"));
		}
		if (orderMessage.containsKey("buyerOwedAmount")) {
			orderRecord.put("buyerOwedAmount", orderMessage.get("buyerOwedAmount"));
		}
		if (orderMessage.containsKey("salesPerson")) {
			orderRecord.put("salesPerson", orderMessage.get("salesPerson"));
		}
		if (orderMessage.containsKey("totalRefundAmount")) {
			orderRecord.put("totalRefundAmount", orderMessage.get("totalRefundAmount"));
		}
		if (orderMessage.containsKey("totalTaxAmount")) {
			orderRecord.put("totalTaxAmount", orderMessage.get("totalTaxAmount"));
		}
		if (orderMessage.containsKey("vouchers")) {
			orderRecord.put("vouchers", orderMessage.get("vouchers"));
		}
		if (orderMessage.containsKey("landingURL")) {
			orderRecord.put("landingURL", orderMessage.get("landingURL"));
		}
		if (orderMessage.containsKey("paymentChannels")) {
			orderRecord.put("paymentChannels", orderMessage.get("paymentChannels"));
		}
		if (orderMessage.containsKey("billingDetails")) {
			orderRecord.put("billingDetails", orderMessage.get("billingDetails"));
		}
		fillTransactionKeyValuePair(orderRecord, "finalShippingFeePaidToChannel", orderMessage);
		exchange.setProperty("accountNumber", orderRecord.get("accountNumber").toString());
		exchange.setProperty("groupOrderByCartNumber", false);
		if (orderMessage.containsKey("cartNumber")) {
			String cartNumber = (String) orderMessage.get("cartNumber");
			orderRecord.put("cartNumber", cartNumber);
			int totalOrderItemsInCart = 0;
			if (orderMessage.containsKey("totalOrderItemsInCart")) {
				totalOrderItemsInCart = orderMessage.getInteger("totalOrderItemsInCart");
				orderRecord.put("totalOrderItemsInCart", totalOrderItemsInCart);
			}
			checkIfgroupOrderByCartNumberNeeded(exchange, totalOrderItemsInCart, cartNumber);
		}
		caculateAndStoreOrderSoldAmount(orderMessage, orderRecord);
		fillAdditionDetails(exchange, orderRecord, siteName, orderMessage);
		if (!checkIsValidOrderForAccount(orderRecord)) {
			exchange.setProperty("stopProcess", true);
			return;
		}
		fillMaatramIntegratedDetails(exchange, orderRecord);
		if (exchange.getProperties().containsKey("isPartnerLogistics")
				&& exchange.getProperties().containsKey("airwayBillExists")) {
			orderRecord.put("isPartnerLogistics", exchange.getProperty("isPartnerLogistics"));
		}
		if (orderRecord.containsKey("orderItems")) {
			List<Document> orderItems = (List<Document>) orderRecord.get("orderItems");
			if (orderItems.size() == 0) {
				log.error("Insert - orderItems List is Empty for this orderId: " + orderMessage.get("orderID").toString());
			}
		}
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("orderID", orderID);
		searchQuery.put("site.nickNameID", nickNameID);
		searchQuery.put("site.name", siteName);
		UpdateOptions options = new UpdateOptions();
		options.upsert(true);
		try {
			if (exchange.getProperties().containsKey("merchantID")) {
				orderRecord.append("merchantID", exchange.getProperty("merchantID"));
			}
			table.insertOne(orderRecord);
			orderRecord.put("_id", orderRecord.getObjectId("_id"));
		} catch (MongoWriteException e) {
			log.info("Order Insert - Duplicate message received for orderID: " + orderID);
			exchange.setProperty("stopProcess", true);
			return;

		}
		exchange.setProperty("orderRecord", orderRecord);
		exchange.getOut().setBody(orderMessage);
	}

	private void updateBradIDInOrderItem(Exchange exchange, Document orderMessage) {
		Map<String, String> brandIDMap = exchange.getProperty("brandIDMap", HashMap.class);
		List<Document> orderItems = (List<Document>) orderMessage.get("orderItems");
		for (Document orderItem : orderItems) {
			if (orderItem.containsKey("customSKU") && brandIDMap.containsKey(orderItem.getString("customSKU"))) {
				orderItem.put("graasBrandID", brandIDMap.get(orderItem.getString("customSKU")));
			}
		}
	}

	private void fillMaatramIntegratedDetails(Exchange exchange, Document orderRecord) {
		if (exchange.getProperties().containsKey("isMaatramIntegratedErp")
				&& exchange.getProperty("isMaatramIntegratedErp", Boolean.class)) {
			orderRecord.put("erpStatus", SIAErpUpdateStatuses.NOT_INITIATED.toString());
		}
		if (exchange.getProperties().containsKey("isMaatramIntegratedWms")
				&& exchange.getProperty("isMaatramIntegratedWms", Boolean.class)) {
			orderRecord.put("wmsStatus", SIAWmsUpdateStatuses.NOT_INITIATED.toString());
		}
		if (exchange.getProperties().containsKey("isMaatramIntegratedShippingCarrier")
				&& exchange.getProperty("isMaatramIntegratedShippingCarrier", Boolean.class)) {
			orderRecord.put("shippingCarrierStatus", SIAShippingCarrierUpdateStatuses.NOT_INITIATED.toString());
		}
		if (exchange.getProperties().containsKey("isMaatramIntegratedOms")
				&& exchange.getProperty("isMaatramIntegratedOms", Boolean.class)) {
			orderRecord.put("omsStatus", SIAOmsUpdateStatuses.NOT_INITIATED.toString());
		}
	}

	private void checkIfgroupOrderByCartNumberNeeded(Exchange exchange, int totalOrderItemsInCart, String cartNumber) {
		exchange.setProperty("groupOrderByCartNumber", false);
		if (totalOrderItemsInCart > 1) {
			exchange.setProperty("groupOrderByCartNumber", true);
		}
		exchange.setProperty("totalOrderItemsInCart", totalOrderItemsInCart);
		exchange.setProperty("cartNumber", cartNumber);
	}

	private void fillOrderAmountInUSD(Document orderRecord) {
		if (orderRecord.containsKey("orderAmount")) {
			Document orderAmount = (Document) orderRecord.get("orderAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderAmount.getString("currencyCode"), "USD");
				if (exchangeRate == 0) {
					log.error("orderAmountInUSD field is not set for the orderID: " + orderRecord.get("orderID").toString());
					return;
				}
				long amount = Math.round(new BigDecimal(orderAmount.get("amount").toString()).longValue() * exchangeRate);
				Document orderAmountInUSD = CurrencyUtil.getAmountObject(amount, "USD");
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
		String MCKey = OrderUtil.getMCkeyforGcStatusWaitingSKUS(fromCurrency, toCurrency);
		Object mcValue = OrderUtil.getValueFromMemcache(MCKey, true);
		if (mcValue != null) {
			return (double) mcValue;
		} else {
			String url = Config.getConfig().getSIAFinopsServerURL() + "/exchange?fromCurrency=" + fromCurrency
					+ "&toCurrency=" + toCurrency;
			Map<String, String> header = new HashMap<String, String>();
			header.put("Content-Type", "application/json");
			header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
			JSONObject response = HttpsURLConnectionUtil.doGet(url, header);
			log.debug("exchange rate:" + response);
			int httpCode = response.getInt("httpCode");
			if (httpCode == HttpStatus.OK_200) {
				JSONObject payload = new JSONObject(response.getString("payload"));
				double exchangeRate = payload.getDouble("exchangeRate");
				OrderUtil.updateMemcache(MCKey, MC_MAX_EXPIRE_TIME, exchangeRate);
				return exchangeRate;
			} else {
				log.error("Get " + url + " failed with status code " + httpCode + " and the response is: " + response);
				return 0;
			}
		}
	}

	private Boolean checkIsValidOrderForAccount(Document orderRecord) {
		if (!orderRecord.containsKey("orderItems")) {
			return false;
		}
		List<Document> orderItems = (List<Document>) orderRecord.get("orderItems");
		if (orderItems.size() == 0 || orderItems == null) {
			return false;
		}
		return true;
	}

	private void updateOrderRecord(Exchange exchange, NotificationOrderActionStatus notificationOrderActionStatus,
			Document orderMessage) throws Exception {
		Document orderRecord = new Document();
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", orderMessage.get("accountNumber").toString());
		searchQuery.put("orderID", orderMessage.get("orderID").toString());
		String siteName = orderMessage.getString("site");
		searchQuery.put("site.name", siteName);
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));

		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		if (exchange.getProperties().containsKey("isManaged") && exchange.getProperty("isManaged", Boolean.class)) {
			orderRecord.put("isManaged", exchange.getProperty("isManaged", Boolean.class));
		}

		String updateStatus = OrderUpdateStatus.COMPLETE.toString();
		if (orderMessage.containsKey("updateStatus")) {
			updateStatus = orderMessage.getString("updateStatus");
		}
		// TODO: need to remove isReconciliation check after disable the finops1.0
		if (orderMessage.containsKey("isReconciliation")) {
			exchange.setProperty("isReconciliation", orderMessage.getBoolean("isReconciliation"));
		}
		if (orderMessage.containsKey("timeSettled")) {
			orderRecord.put("timeSettled", new BigDecimal(orderMessage.get("timeSettled").toString()).longValue());
		}
		if (orderMessage.containsKey("timeSettlementProcessed")) {
			orderRecord.put("timeSettlementProcessed", new BigDecimal(orderMessage.get("timeSettlementProcessed").toString()).longValue());
		}
		if (orderMessage.containsKey("settlementStatus")) {
			orderRecord.put("settlementStatus", orderMessage.getString("settlementStatus"));
		}
		if (orderMessage.containsKey("returnSettlementStatus")) {
			orderRecord.put("returnSettlementStatus", orderMessage.getString("returnSettlementStatus"));
		}
		if (orderMessage.containsKey("transactionPeriod")) {
			orderRecord.put("transactionPeriod", orderMessage.getString("transactionPeriod"));
		}
		if (orderMessage.containsKey("timeOrderUpdated")) {
			orderRecord.put("timeOrderUpdated", new BigDecimal(orderMessage.get("timeOrderUpdated").toString()).longValue());
		}
		if (orderMessage.containsKey("timeOrderCancelled")) {
			orderRecord.put("timeOrderCancelled", new BigDecimal(orderMessage.get("timeOrderCancelled").toString()).longValue());
		}
		if (orderMessage.containsKey("timeOrderReturnRequested")) {
			orderRecord.put("timeOrderReturnRequested", new BigDecimal(orderMessage.get("timeOrderReturnRequested").toString()).longValue());
		}
		if (exchange.getProperties().containsKey("isPartnerLogistics")
				&& exchange.getProperties().containsKey("airwayBillExists")) {
			orderRecord.put("isPartnerLogistics", exchange.getProperty("isPartnerLogistics"));
		}
		if (orderMessage.containsKey("isPosOrder")) {
			orderRecord.put("isPosOrder", orderMessage.getBoolean("isPosOrder"));
		}
		if (orderMessage.containsKey("sourceChannel")) {
			orderRecord.put("sourceChannel", orderMessage.get("sourceChannel"));
		}
		if (orderMessage.containsKey("isPreOrder")) {
			orderRecord.put("isPreOrder", orderMessage.getBoolean("isPreOrder"));
		}
		if (orderMessage.containsKey("channelApiVersion")) {
			orderRecord.put("channelApiVersion", orderMessage.get("channelApiVersion"));
		}
		if (orderMessage.containsKey("packageWeight")) {
			orderRecord.put("packageWeight", orderMessage.get("packageWeight"));
		}
		if (orderMessage.containsKey("orderTypes")) {
			orderRecord.put("orderTypes", orderMessage.get("orderTypes"));
		}
		if (orderMessage.containsKey("shippingTypes")) {
			orderRecord.put("shippingTypes", orderMessage.get("shippingTypes"));
		}
		if (orderMessage.containsKey("buyerOwedAmount")) {
			orderRecord.put("buyerOwedAmount", orderMessage.get("buyerOwedAmount"));
		}
		if (orderMessage.containsKey("salesPerson")) {
			orderRecord.put("salesPerson", orderMessage.get("salesPerson"));
		}
		if (orderMessage.containsKey("totalRefundAmount")) {
			orderRecord.put("totalRefundAmount", orderMessage.get("totalRefundAmount"));
		}
		if (orderMessage.containsKey("totalTaxAmount")) {
			orderRecord.put("totalTaxAmount", orderMessage.get("totalTaxAmount"));
		}
		if (orderMessage.containsKey("vouchers")) {
			orderRecord.put("vouchers", orderMessage.get("vouchers"));
		}
		if (orderMessage.containsKey("landingURL")) {
			orderRecord.put("landingURL", orderMessage.get("landingURL"));
		}
		if (orderMessage.containsKey("paymentChannels")) {
			orderRecord.put("paymentChannels", orderMessage.get("paymentChannels"));
		}
		if (orderMessage.containsKey("billingDetails")) {
			orderRecord.put("billingDetails", orderMessage.get("billingDetails"));
		}
		/*
		 * Need to set this flag in exchange and in out going message for re-pushing
		 * infor orders again
		 */
		if (isItemsReAllocatedNeeded(orderMessage, exchange)) {
			exchange.setProperty("isItemsReAllocated", true);
		}
		if (Config.getConfig().getIsEligibleToUpdateBrandID()) {
			updateBradIDInOrderItem(exchange, orderMessage);
		}
		// update order data only when the update is complete
		if (OrderUpdateStatus.COMPLETE.toString().equals(updateStatus)) {
			if (orderMessage.containsKey("orderStatus")
					&& (orderMessage.get("orderStatus").equals(SIAOrderStatus.ACCEPTED.toString())
							|| orderMessage.get("orderStatus").equals(SIAOrderStatus.PROCESSING.toString())
							|| orderMessage.get("orderStatus").equals(SIAOrderStatus.DISPATCHED.toString()))
					&& orderMessage.containsKey("shippingDetails")) {
				Document shippingDetails = (Document) orderMessage.get("shippingDetails");
				if (shippingDetails.containsKey("shippingTrackingDetails")) {
					Document shippingTrackingDetails = (Document) shippingDetails.get("shippingTrackingDetails");
					if (shippingTrackingDetails.containsKey("airwayBill")
							&& !shippingTrackingDetails.getString("airwayBill").isEmpty()) {
						orderMessage.put("shippingCarrierStatus",
								SIAShippingCarrierUpdateStatuses.SHIPMENT_CREATED.toString());
						orderMessage.put("isAWBCreated", true);
					}
				}
			}
			fillOrderRecord(exchange, notificationOrderActionStatus, orderRecord, orderMessage);
			// TODO: need to remove isWhatsAppEnabled after whatsapp approval
			if (orderRecord.containsKey("isNotifyOrderUpdates") && Config.getConfig().getWhatsAppEnabled()) {
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
			fillAdditionDetails(exchange, orderRecord, siteName, orderMessage);
			fillOrderAmountInUSD(orderRecord);
		}
		caculateAndStoreOrderSoldAmount(orderMessage, orderRecord);
		orderRecord.put("updateStatus", updateStatus);
		if (updateStatus.equals(OrderUpdateStatus.FAILED.toString()) && orderMessage.containsKey("failureReason")) {
			orderRecord.put("failureReason", orderMessage.getString("failureReason"));
			if (orderMessage.containsKey("orderStatus")
					&& orderMessage.get("orderStatus").equals(SIAOrderStatus.ACCEPTED.toString())
					&& orderMessage.containsKey("shippingDetails")) {
				Document shippingDetails = (Document) orderMessage.get("shippingDetails");
				if (shippingDetails.containsKey("shippingTrackingDetails")) {
					Document shippingTrackingDetails = (Document) shippingDetails.get("shippingTrackingDetails");
					String courierName = shippingTrackingDetails.getString("courierName");
					if (courierName.equals("janio")
							|| (courierName.toLowerCase().replaceAll(" ", "")).contains("ninjavan")) {
						if ((!shippingTrackingDetails.containsKey("airwayBill")
								|| (shippingTrackingDetails.containsKey("airwayBill")
										&& shippingTrackingDetails.getString("airwayBill").isEmpty()))) {
							orderRecord.put("shippingCarrierStatus",
									SIAShippingCarrierUpdateStatuses.SHIPMENT_CREATE_FAILED.toString());
							orderRecord.put("shippingCarrierFailureReason", orderMessage.getString("failureReason"));
						}
					}
				}
			}
		}
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		if (orderRecord.containsKey("orderItems")) {
			if (orderRecord.get("orderItems") != null) {
				List<Document> orderItems = (List<Document>) orderRecord.get("orderItems");
				if (orderItems.size() == 0) {
					log.error(
							"Update - orderItems List is Empty for this orderId: " + orderMessage.get("orderID").toString());
				}
			} else {
				log.error("Null orderItem came for orderID : " + orderMessage.get("orderID").toString());
			}
		}
		// if we pass true then will modified data
		UpdateResult result = table.updateOne(searchQuery, new Document("$set", orderRecord));
		if (result.getModifiedCount() == 0) {
			log.info("Order :" + orderMessage.get("orderID").toString() + " is already updated. this is duplicate message.");
			exchange.setProperty("stopProcess", true);
			return;
		}
		Document order = updateAndGetLatestUpdatedOrder(searchQuery, orderMessage);
		if (order.containsKey("totalOrderItemsInCart") && order.containsKey("cartNumber")) {
			checkIfgroupOrderByCartNumberNeeded(exchange, order.getInteger("totalOrderItemsInCart"),
					order.getString("cartNumber"));
		}
		exchange.setProperty("orderRecord", order);
		exchange.getOut().setBody(orderMessage);
	}

	private void caculateAndStoreOrderSoldAmount(Document orderMessage, Document orderRecord) {
		if (orderMessage.containsKey("orderAmount")) {
			Document orderAmount = (Document) orderMessage.get("orderAmount");
			String currencyCode = orderAmount.getString("currencyCode");
			long orderSoldAmount = new BigDecimal(orderAmount.get("amount").toString()).longValue();
			if (orderMessage.containsKey("voucherAmount")) {
				Document voucherAmount = (Document) orderMessage.get("voucherAmount");
				orderSoldAmount = orderSoldAmount - new BigDecimal(voucherAmount.get("amount").toString()).longValue();
			} else if (orderMessage.containsKey("sellerDiscountAmount")) {
				Document sellerDiscountAmount = (Document) orderMessage.get("sellerDiscountAmount");
				orderSoldAmount = orderSoldAmount - new BigDecimal(sellerDiscountAmount.get("amount").toString()).longValue();
			}
			orderRecord.put("orderSoldAmount", CurrencyUtil.getAmountObject(orderSoldAmount, currencyCode));
			fillOrderSoldAmountInUSD(orderRecord);
		}
	}

	private void fillOrderSoldAmountInUSD(Document orderRecord) {
		if (orderRecord.containsKey("orderSoldAmount")) {
			Document orderSoldAmount = (Document) orderRecord.get("orderSoldAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderSoldAmount.getString("currencyCode"), "USD");
				if (exchangeRate == 0) {
					log.error("orderSoldAmountInUSD field is not set for the orderID: "
							+ orderRecord.get("orderID").toString());
					return;
				}
				long amount = Math.round(new BigDecimal(orderSoldAmount.get("amount").toString()).longValue() * exchangeRate);
				Document orderSoldAmountInUSD = CurrencyUtil.getAmountObject(amount, "USD");
				orderRecord.put("orderSoldAmountInUSD", orderSoldAmountInUSD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Document updateAndGetLatestUpdatedOrder(Document searchQuery, Document orderMessage) {
		Document updateObject = new Document();
		if (orderMessage.containsKey("notificationID")) {
			// Append the OrderNotificationID to the database
			updateObject.put("$push", new Document("notificationID", orderMessage.get("notificationID")));
		}
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.returnDocument(ReturnDocument.AFTER);
		Document update = new Document("timeLastUpdated", DateUtil.getSIADateFormat());
		updateObject.put("$set", update);
		Document order = table.findOneAndUpdate(searchQuery, updateObject, options);
		return order;
	}

	private void fillOrderRecord(Exchange exchange, NotificationOrderActionStatus notificationOrderActionStatus,
			Document orderRecord, Document orderMessage) {
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
		fillTransactionKeyValuePair(orderRecord, "marketplaceInvoiceNumber", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "channelVoucherCodes", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "bundledPromotionItems", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "finalShippingFeePaidToChannel", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "sellerRemarks", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "buyerRemarks", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "gstAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "giftMessage", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "isPreOrder", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderFulfilledBy", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "packageList", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "isAWBCreated", orderMessage);
		fillOrderTime(notificationOrderActionStatus, orderRecord, orderMessage);
		hashRequiredFields(exchange, orderRecord, orderMessage);
	}

	private void hashRequiredFields(Exchange exchange, Document orderRecord, Document orderMessage) {
		HashUtil hashUtil = new HashUtil();
		List<String> sites = Arrays.asList(Config.getConfig().getRemoveBuyerDetailChannels().split("-"));
		String siteName = orderMessage.getString("site");
		boolean isHashedBuyerDetailsFound = false, isHashedBuyerAddressFound = false, isBuyerDetailsHashed = false;
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		Document addressHashed = new Document();
		if (hasOrderInDB) {
			Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
			Document shippingDetailsFromDB = new Document();
			if (orderDBObject.containsKey("shippingDetails")) {
				shippingDetailsFromDB = (Document) orderDBObject.get("shippingDetails");
			}
			if (sites.contains(siteName) && orderDBObject.containsKey("isPIIRemoved")) {
				if (orderDBObject.getBoolean("isPIIRemoved") || orderDBObject.containsKey("buyerDetailsHashed")) {
					isHashedBuyerDetailsFound = true;
				}
				if (orderDBObject.getBoolean("isPIIRemoved")) {
					isHashedBuyerAddressFound = true;
				} else if (shippingDetailsFromDB.containsKey("addressHashed")) {
					isHashedBuyerAddressFound = true;
					addressHashed = (Document) shippingDetailsFromDB.get("addressHashed");
				}
			} else if (orderDBObject.containsKey("isPIIAnonymized")) {
				if (orderDBObject.getBoolean("isPIIAnonymized") || orderDBObject.containsKey("buyerDetailsHashed")) {
					isHashedBuyerDetailsFound = true;
				}
				if (orderDBObject.getBoolean("isPIIAnonymized")) {
					isHashedBuyerAddressFound = true;
				} else if (shippingDetailsFromDB.containsKey("addressHashed")) {
					isHashedBuyerAddressFound = true;
					addressHashed = (Document) shippingDetailsFromDB.get("addressHashed");
				}
			}
		}
		if (isHashedBuyerDetailsFound) {
			fillTransactionKeyValuePair(orderRecord, "buyerDetailsHashed", orderMessage);
		} else {
			Document buyerDetailsHashed = hashObjectFields("buyerDetails", orderMessage, hashUtil);
			if (buyerDetailsHashed != null) {
				isBuyerDetailsHashed = true;
				orderRecord.put("buyerDetailsHashed", buyerDetailsHashed);
			}
		}
		if (isHashedBuyerAddressFound) {
			if (!addressHashed.isEmpty()) {
				Document shippingDetailsFromDB = new Document();
				if (orderRecord.containsKey("shippingDetails")) {
					shippingDetailsFromDB = (Document) orderRecord.get("shippingDetails");
				}
				shippingDetailsFromDB.put("addressHashed", addressHashed);
				orderRecord.put("shippingDetails", shippingDetailsFromDB);
			}
		} else {
			if (orderMessage.containsKey("shippingDetails")) {
				Document shippingDetails = (Document) orderMessage.get("shippingDetails");
				Document shippingDetailsFromDB = orderRecord.containsKey("shippingDetails")
						? (Document) orderRecord.get("shippingDetails")
						: new Document();
				addressHashed = hashObjectFields("address", shippingDetails, hashUtil);
				if (addressHashed != null) {
					isBuyerDetailsHashed = true;
					shippingDetailsFromDB.put("addressHashed", addressHashed);
					orderRecord.put("shippingDetails", shippingDetailsFromDB);
				}
			}
		}

		if (isBuyerDetailsHashed || isHashedBuyerDetailsFound || isHashedBuyerAddressFound) {
			if (sites.contains(siteName)) {
				orderRecord.put("isPIIRemoved", false);
			} else {
				orderRecord.put("isPIIAnonymized", false);
			}
		}
	}

	private Document hashObjectFields(String key, Document orderMessage, HashUtil hashUtil) {
		if (orderMessage.containsKey(key)) {
			Document obj = (Document) orderMessage.get(key);
			Set<String> keys = obj.keySet();

			Document hashedObj = new Document();
			for (String objKey : keys) {
				if (key.equals("address")
						&& (objKey.equals("city") || objKey.equals("country") || objKey.equals("postalCode"))) {
					// Note: we are skipping city, country & postalCode in buyer address
					hashedObj.put(objKey, obj.get(objKey).toString());
				} else {
					hashedObj.put(objKey, hashUtil.hash(obj.get(objKey).toString().toCharArray(), false));
				}
			}
			return hashedObj;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void fillAdditionDetails(Exchange exchange, Document orderRecord, String siteName, Document orderMessage)
			throws Exception {
		Map<String, Document> inventoryDetailsMap = (Map<String, Document>) exchange.getProperty("inventoryDetailsMap");
		String orderID = exchange.getProperty("orderID", String.class);
		String appType = "";
		boolean isMaatramIntegratedExternalAPIUpdate = false;
		boolean addOrderItemLocation = false;
		boolean processOrdersWithSKUOnly = processOrdersWithtSKUOnly(exchange);
		List<Document> newOrderItems = new ArrayList<Document>();
		List<Document> freeGiftItems = new ArrayList<Document>();
		List<String> orderItemIDListFromDB = new ArrayList<String>();
		Map<String, String> orderItemsStatusMap = new HashMap<String, String>();
		int orderDBObjectSize = 0;
		if ((Boolean) exchange.getProperty("hasOrderInDB")) {
			JSONObject orderDBObject = OrderUtil.parseToJsonObject((Document) exchange.getProperty("orderDBObject"));
			JSONArray items = orderDBObject.getJSONArray("orderItems");
			orderDBObjectSize = items.length();
			for (int i = 0; i < items.length(); i++) {
				if (items.getJSONObject(i).has("orderItemID")) {
					orderItemIDListFromDB.add(items.getJSONObject(i).get("orderItemID").toString());
				}
				if (items.getJSONObject(i).has("isFreeGift") && items.getJSONObject(i).getBoolean("isFreeGift")) {
					freeGiftItems.add(Document.parse(items.getJSONObject(i).toString()));
				}
				if (exchange.getProperties().containsKey("isStatusHandledInOrderItem")
						&& exchange.getProperty("isStatusHandledInOrderItem", Boolean.class)
						&& items.getJSONObject(i).has("orderStatus") && items.getJSONObject(i).has("orderItemID")) {
					orderItemsStatusMap.put(items.getJSONObject(i).getString("orderItemID"),
							items.getJSONObject(i).getString("orderStatus"));
				}
			}
			Document addendum = orderMessage.containsKey("addendum") ? (Document) orderMessage.get("addendum")
					: new Document();
			String eventType = addendum.containsKey("eventType") ? addendum.getString("eventType") : "";
			appType = addendum.containsKey("appType") ? addendum.getString("appType") : "";
			if (eventType.equals("API_UPDATE")
					&& (appType.equals("ERP") || appType.equals("WMS") || appType.equals("OMS"))) {
				// set maatram order status when it was an maatram integrated external API order
				// update
				setExternalAPIMaatramOrderStatus(orderRecord, exchange, orderMessage, appType);
				isMaatramIntegratedExternalAPIUpdate = true;
			}
		}

		int giftItemsSize = freeGiftItems.size();
		if (orderRecord.containsKey("orderItems")) {
			List<Document> orderItems = (ArrayList<Document>) orderRecord.get("orderItems");
			int requestOrderItemSize = orderItems.size();
			for (int i = 0; i < orderItems.size(); i++) {
				Document orderItem = orderItems.get(i);
				if (exchange.getProperties().containsKey("isStatusHandledInOrderItem")
						&& exchange.getProperty("isStatusHandledInOrderItem", Boolean.class)
						&& orderItem.containsKey("orderStatus") && orderItem.containsKey("orderItemID")) {
					String orderItemID = orderItem.get("orderItemID").toString();
					if (orderItemsStatusMap.containsKey(orderItemID)) {
						SIAOrderStatus orderItemDBStatus = SIAOrderStatus.valueOf(orderItemsStatusMap.get(orderItemID));
						SIAOrderStatus notificationOrderStatus = SIAOrderStatus
								.valueOf(orderItem.getString("orderStatus"));
						NotificationOrderActionStatus notificationOrderActionStatus = OrderUtil
								.handleExistingOrderStatus(notificationOrderStatus, orderItemDBStatus,
										OrderUtil.parseToJsonObject((Document) orderItem), orderID, "orderItem");
						if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.NO_ACTION)) {
							orderItem.put("orderStatus", orderItemDBStatus.toString());
						}
					}
				}
				if (!orderItem.containsKey("settlementAmount")
						&& requestOrderItemSize == orderDBObjectSize - giftItemsSize) {
					processSettlementAmountOrderItem(orderItem, i, exchange);
				}
				boolean orderHasInventory = false;
				if (orderItem.containsKey("isFreeGift") && orderItem.getBoolean("isFreeGift")) {
					// Free gift only set and handled in PNQ.so can be removed if incoming message
					// has freeGift items.
					// From DB existing free gift items will be retained.
					continue;
				}
				if (orderItem.containsKey("SKU")) {
					String SKU = orderItem.getString("SKU");
					Document inventoryValue = inventoryDetailsMap.get(SKU);
					if (inventoryValue != null) {
						orderHasInventory = true;
						if (!orderItem.containsKey("itemTitle")) {
							orderItem.put("itemTitle", inventoryValue.getString("itemTitle"));
						}
						if (!getImageURL(inventoryValue, siteName).isEmpty()) {
							orderItem.put("imageURL", getImageURL(inventoryValue, siteName));
						}
						if (inventoryValue.containsKey("variantDetails")) {
							orderItem.put("variantDetails", inventoryValue.get("variantDetails"));
						}
						if (inventoryValue.containsKey("customSKU")) {
							orderItem.put("customSKU", inventoryValue.get("customSKU"));
						}
						if (inventoryValue.containsKey("hsnCode")) {
							orderItem.put("hsnCode", inventoryValue.get("hsnCode"));
						}
						Document site = (Document) inventoryValue.get(siteName);
						if (site.containsKey("isOption")) {
							orderItem.put("isOption", site.getBoolean("isOption"));
						} else {
							orderItem.put("isOption", false);
						}
						if (site.containsKey("categoryName")) {
							orderItem.put("categoryName", site.get("categoryName"));
						}
						if (site.containsKey("categoryID")) {
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
				if (orderItem.containsKey("itemAmount")) {
					orderItem.put("itemSoldAmount", (Document) orderItem.get("itemAmount"));
					if (orderItem.containsKey("sellerDiscountAmount")) {
						Document itemAmountObject = (Document) orderItem.get("itemAmount");
						long itemAmount = new BigDecimal(itemAmountObject.get("amount").toString()).longValue();
						Document sellerDiscountAmountObject = (Document) orderItem.get("sellerDiscountAmount");
						long sellerDiscountAmount = new BigDecimal(sellerDiscountAmountObject.get("amount").toString()).longValue();
						String currencyCode = itemAmountObject.getString("currencyCode");
						long itemSoldAmount = itemAmount - sellerDiscountAmount;
						orderItem.put("itemSoldAmount", Document
								.parse(CurrencyUtil.getJSONAmountObject(itemSoldAmount, currencyCode).toString()));
					}
				}
				if (orderItem.containsKey("totalItemAmount")) {
					orderItem.put("totalItemSoldAmount", (Document) orderItem.get("totalItemAmount"));
					if (orderItem.containsKey("totalSellerDiscountAmount")) {
						Document totalItemAmount = (Document) orderItem.get("totalItemAmount");
						long itemAmount = new BigDecimal(totalItemAmount.get("amount").toString()).longValue();
						Document totalSellerDiscountAmount = (Document) orderItem.get("totalSellerDiscountAmount");
						long sellerDiscountAmount = new BigDecimal(totalSellerDiscountAmount.get("amount").toString()).longValue();
						String currencyCode = totalItemAmount.getString("currencyCode");
						long itemSoldAmount = itemAmount - sellerDiscountAmount;
						orderItem.put("totalItemSoldAmount", Document
								.parse(CurrencyUtil.getJSONAmountObject(itemSoldAmount, currencyCode).toString()));
					}
				}
				// Set maatram orderItem statuses
				setMaatramItemStatusFromDbOrderItem(orderItem, i, exchange);
				if (isMaatramIntegratedExternalAPIUpdate) {
					// set maatram order status when it was an maatram integrated external API order
					// update
					setExternalAPIMaatramItemStatus(orderItem, i, exchange, orderMessage, appType);
				}
				if (processOrdersWithSKUOnly) {
					// For managed accounts, add orderItem to list, only it has
					// SKU
					if (orderHasInventory || orderItemIDListFromDB.contains(orderItem.get("orderItemID").toString())) {
						newOrderItems.add(orderItem);
					}
				} else {
					newOrderItems.add(orderItem);
				}
				if (orderItem.containsKey("settlementDetails")) {
					Document settlementDetails = (Document) orderItem.get("settlementDetails");
					if (settlementDetails.containsKey("refunded")) {
						Document refunded = (Document) settlementDetails.get("refunded");
						removeFeesFields(refunded);
					}
				}
				removeFeesFields(orderItem);
			}
			if (freeGiftItems.size() > 0) {
				// Retain freegift item from DB.
				newOrderItems.addAll(freeGiftItems);
			} else {
				exchange.setProperty("isFreeGiftAddedToOrder", false);
			}
			orderRecord.put("orderItems", newOrderItems);
		}
	}

	private void removeFeesFields(Document refunded) {
		refunded.remove("expectedMarketPlaceCommission");
		refunded.remove("feesFieldsToUpdate");
	}

	private void processSettlementAmountOrderItem(Document orderItem, int orderItemIndex, Exchange exchange) {
		if (exchange.getProperty("hasOrderInDB", Boolean.class)) {
			Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
			if (orderDBObject.containsKey("orderItems")) {
				List<Document> orderItems = (List<Document>) orderDBObject.get("orderItems");
				Document orderItemDB = (Document) orderItems.get(orderItemIndex);
				if (orderItemDB.containsKey("settlementAmount")) {
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
		Document userSiteSpecificObject = exchange.getProperty("userSiteSpecificObject", Document.class);
		if (!userSiteSpecificObject.containsKey("processOrdersWithSKUOnly")) {
			return false;
		}
		return userSiteSpecificObject.getBoolean("processOrdersWithSKUOnly");
	}

	@SuppressWarnings("unchecked")
	private String getImageURL(Document inventoryValues, String siteName) {
		String imageURL = inventoryValues.getString("imageURL");
		Document site = (Document) inventoryValues.get(siteName);
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
	private boolean getItemLocation(Document inventoryValues, String siteName, Document orderRecord) {
		Document site = (Document) inventoryValues.get(siteName);
		if (site.containsKey("itemLocation") && site.get("itemLocation") != null) {
			orderRecord.put("itemLocation", site.get("itemLocation"));
			return true;
		}
		return false;
	}

	private void fillTransactionKeyValuePair(Document orderRecord, String key, Document orderMessage) {
		if (orderMessage.containsKey(key)) {
			orderRecord.put(key, orderMessage.get(key));
		}
	}

	private void fillOrderTime(NotificationOrderActionStatus notificationOrderActionStatus, Document orderRecord,
			Document orderMessage) {
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
			if (orderMessage.containsKey("timeDispatched")) {
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
			// for offline orders update timeDelivered by user.
			if (orderMessage.containsKey("timeDelivered")) {
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
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PARTIALLY_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURN_REQUESTED_TO_RETURNED)
				|| notificationOrderActionStatus
						.equals(NotificationOrderActionStatus.PARTIAL_RETURN_REQUESTED_TO_PARTIALLY_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_RETURNED)) {
			orderRecord.put("timeReturned", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.LOST_BY_3PL)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_LOST_BY_3PL)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURN_SHIPPED_TO_LOST_BY_3PL)) {
			orderRecord.put("timeLostBy3PL", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.DAMAGE_BY_3PL)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_DAMAGE_BY_3PL)) {
			orderRecord.put("timeDamageBy3PL", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_DELIVERY_FAILED)) {
			orderRecord.put("timeDeliveryFailed", DateUtil.getSIADateFormat());
		}
	}

	private void checkIsOrderFulfilledByChannel(Exchange exchange, JSONObject orderMessageJSON) throws JSONException {
		// If order fulfilled by channel, no need sync inventory.
		// That will be done by respective channel itself.
		// So should stop inventory sync.
		if (orderMessageJSON.has("orderFulfilledBy")
				&& orderMessageJSON.getString("orderFulfilledBy").equals(OrderFulfilledBy.CHANNEL.toString())) {
			log.info("Order : " + orderMessageJSON.get("orderID").toString()
					+ " is fulfilled by channel. So stock sync not required to SIA system.");
			exchange.setProperty("stopProcess", true);
		}
	}

	private boolean isItemsReAllocatedNeeded(Document orderMessage, Exchange exchange) throws JSONException {
		JSONObject orderRecord = OrderUtil.parseToJsonObject((Document) exchange.getProperty("orderDBObject"));
		if (orderRecord.has("orderItems") && orderMessage.containsKey("orderItems")) {
			JSONArray orderItemsFromDB = orderRecord.getJSONArray("orderItems");
			// When size mismatches need to set the isItemReAllocated flag
			List<Document> orderItemsMessage = (ArrayList<Document>) orderMessage.get("orderItems");
			if (orderItemsMessage.size() > 0 && (orderItemsMessage.size() != orderItemsFromDB.length())) {
				return true;
			}
			HashSet<String> orderItemIDs = new HashSet<String>();
			for (int i = 0; i < orderItemsMessage.size(); i++) {
				Document orderItemMessage = orderItemsMessage.get(i);
				if (orderItemMessage.containsKey("orderItemID")) {
					orderItemIDs.add(orderItemMessage.get("orderItemID").toString());
				}
			}
			// When orderedItemID mismatches means need to set the isItemReAllocated
			for (int j = 0; j < orderItemsFromDB.length(); j++) {
				JSONObject orderItemFromDB = orderItemsFromDB.getJSONObject(j);
				if (orderItemFromDB.has("orderItemID")
						&& !orderItemIDs.contains(orderItemFromDB.getString("orderItemID"))) {
					return true;
				}
			}
			// When wmsID not in orders data but in message then vice versa to set the
			// isItemReAllocated
			for (int k = 0; k < orderItemsFromDB.length(); k++) {
				JSONObject orderItemFromDB = orderItemsFromDB.getJSONObject(k);
				for (int l = 0; l < orderItemsMessage.size(); l++) {
					Document orderItemMessage = orderItemsMessage.get(l);
					if (orderItemMessage.containsKey("orderItemID") && orderItemFromDB.has("orderItemID")) {
						String orderItemIDFromMessage = orderItemMessage.get("orderItemID").toString();
						if (orderItemMessage.containsKey("wmsID")) {
							String wmsIDFromMessage = orderItemMessage.getString("wmsID");
							if (orderItemFromDB.getString("orderItemID").equals(orderItemIDFromMessage)) {
								if (deceideToSetReAllocationFlag(orderItemFromDB, wmsIDFromMessage)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean deceideToSetReAllocationFlag(JSONObject orderItemFromDB, String wmsIDFromMessage)
			throws JSONException {
		if (!wmsIDFromMessage.isEmpty() && !orderItemFromDB.has("wmsID")) {
			return true;
		}
		if (orderItemFromDB.has("wmsID") && !orderItemFromDB.getString("wmsID").equals(wmsIDFromMessage)) {
			return true;
		}
		return false;
	}

	private void setMaatramItemStatusFromDbOrderItem(Document orderItem, int orderItemIndex, Exchange exchange) {
		if (exchange.getProperty("hasOrderInDB", Boolean.class)) {
			Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
			if (orderDBObject.containsKey("orderItems")) {
				List<Document> orderItems = (List<Document>) orderDBObject.get("orderItems");
				if (orderItems.size() > orderItemIndex) {
					Document orderItemDB = (Document) orderItems.get(orderItemIndex);
					if (orderItemDB.containsKey("omsStatus")) {
						fillTransactionKeyValuePair(orderItem, "omsStatus", orderItemDB);
					}
					if (orderItemDB.containsKey("wmsStatus")) {
						fillTransactionKeyValuePair(orderItem, "wmsStatus", orderItemDB);
					}
					if (orderItemDB.containsKey("erpStatus")) {
						fillTransactionKeyValuePair(orderItem, "erpStatus", orderItemDB);
					}
				}
			}
		}
	}

	private void setExternalAPIMaatramItemStatus(Document orderItem, int orderItemIndex, Exchange exchange,
			Document orderMessage, String appType) {
		String incomingOrderStatus = orderItem.containsKey("orderStatus") ? orderItem.getString("orderStatus") : "";
		Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
		if (orderDBObject.containsKey("orderItems")) {
			List<Document> orderItems = (List<Document>) orderDBObject.get("orderItems");
			if (orderItems.size() > orderItemIndex) {
				Document orderItemDB = (Document) orderItems.get(orderItemIndex);
				if (!orderItemDB.getString("orderStatus").equals(incomingOrderStatus)) {
					String integrateType = appType.toLowerCase();
					if (incomingOrderStatus.equals(SIAOrderStatus.CANCELLED.toString())) {
						orderItem.put(integrateType + "Status", SIAErpUpdateStatuses.ORDER_CANCELLED.toString());
					} else if (incomingOrderStatus.equals(SIAOrderStatus.RETURNED.toString())) {
						orderItem.put(integrateType + "Status", SIAErpUpdateStatuses.ORDER_RETURNED.toString());
					}
				}
			}
		}
	}

	private void setExternalAPIMaatramOrderStatus(Document orderRecord, Exchange exchange, Document orderMessage,
			String appType) {
		String incomingOrderStatus = orderRecord.containsKey("orderStatus") ? orderRecord.getString("orderStatus") : "";
		Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
		if (orderDBObject.containsKey("orderStatus")
				&& !orderDBObject.getString("orderStatus").equals(incomingOrderStatus)) {
			String integrateType = appType.toLowerCase();
			if (incomingOrderStatus.equals(SIAOrderStatus.CANCELLED.toString())) {
				orderRecord.put(integrateType + "Status", SIAErpUpdateStatuses.ORDER_CANCELLED.toString());
				List<String> integrateUpdateStatuses = new ArrayList<String>();
				if (orderDBObject.containsKey(integrateType + "UpdateStatuses")
						&& orderDBObject.get(integrateType + "UpdateStatuses") instanceof List<?>) {
					integrateUpdateStatuses = (List<String>) orderDBObject.get(integrateType + "UpdateStatuses");
				}
				integrateUpdateStatuses.add(SIAErpUpdateStatuses.ORDER_CANCELLED.toString());
				orderRecord.put(integrateType + "UpdateStatuses", integrateUpdateStatuses);
			} else if (incomingOrderStatus.equals(SIAOrderStatus.RETURNED.toString())) {
				orderRecord.put(integrateType + "Status", SIAErpUpdateStatuses.ORDER_RETURNED.toString());
				List<String> integrateUpdateStatuses = new ArrayList<String>();
				if (orderDBObject.containsKey(integrateType + "UpdateStatuses")
						&& orderDBObject.get(integrateType + "UpdateStatuses") instanceof List<?>) {
					integrateUpdateStatuses = (List<String>) orderDBObject.get(integrateType + "UpdateStatuses");
				}
				integrateUpdateStatuses.add(SIAErpUpdateStatuses.ORDER_RETURNED.toString());
				orderRecord.put(integrateType + "UpdateStatuses", integrateUpdateStatuses);
			}
		}
	}

}
