/**
 * 
 */
package com.sellinall.order.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.HttpsURLConnectionUtil;
import com.sellinall.util.InvoiceSequence;
import com.sellinall.util.enums.OrderUpdateStatus;

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
			BasicDBObject orderMessage, JSONObject inBody) throws JSONException {

		BasicDBObject site = new BasicDBObject();
		String siteName = orderMessage.getString("site");
		site.put("name", siteName);
		site.put("nickNameID", orderMessage.getString("nickNameID"));
		BasicDBObject orderRecord = new BasicDBObject();
		orderRecord.put("site", site);
		orderRecord.put("orderID", orderMessage.getString("orderID"));
		if (exchange.getProperties().containsKey("profileID")) {
			String profileID = exchange.getProperty("profileID", String.class);
			String merchantID = exchange.getProperty("merchantID", String.class);
			String invoiceNumberPrefix = exchange.getProperty("invoiceNumberPrefix", String.class);
			String invoiceNumber = invoiceNumberPrefix + InvoiceSequence.getNextInvoiceSequence(merchantID, profileID);
			orderRecord.put("invoiceNumber", invoiceNumber);
		}
		// TODO: remove the condition after all publishers start publishing user
		// id.
		orderRecord.put("accountNumber", orderMessage.getString("accountNumber"));
		if (exchange.getProperties().containsKey("isManaged") && exchange.getProperty("isManaged", Boolean.class)) {
			orderRecord.put("isManaged", exchange.getProperty("isManaged", Boolean.class));
		}
		if (exchange.getProperties().containsKey("isTransactionFee")
				&& exchange.getProperty("isTransactionFee", boolean.class)) {
			orderRecord.put("isTransactionFee", exchange.getProperty("isTransactionFee", boolean.class));
		}
		fillOrderRecord(notificationOrderActionStatus, orderRecord, orderMessage);
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
		if (orderMessage.containsField("shippingAmount")) {
			orderRecord.put("shippingAmount", orderMessage.get("shippingAmount"));
		}
		if (orderMessage.containsField("voucherAmount")) {
			orderRecord.put("voucherAmount", orderMessage.get("voucherAmount"));
		}
		if (orderMessage.containsField("sellerDiscountAmount")) {
			orderRecord.put("sellerDiscountAmount", orderMessage.get("sellerDiscountAmount"));
		}
		if (orderMessage.containsField("channelDiscountAmount")) {
			orderRecord.put("channelDiscountAmount", orderMessage.get("channelDiscountAmount"));
		}
		exchange.setProperty("accountNumber", orderRecord.getString("accountNumber"));
		if (orderMessage.containsField("cartNumber")) {
			orderRecord.put("cartNumber", orderMessage.get("cartNumber"));
		}
		fillAdditionDetails(exchange, orderRecord, siteName);
		if (!checkIsValidOrderForAccount(orderRecord)) {
			exchange.setProperty("stopProcess", true);
			return;
		}
		if (exchange.getProperties().containsKey("isPartnerLogistics")
				&& exchange.getProperties().containsKey("airwayBillExists")) {
			orderRecord.put("isPartnerLogistics", exchange.getProperty("isPartnerLogistics"));
		}
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("order");
		Document document = new Document(orderRecord);
		table.insertOne(document);
		// for accounting channel
		orderRecord.put("_id", document.get("_id"));
		exchange.setProperty("orderRecord", orderRecord);
		exchange.getOut().setBody(orderMessage);
	}

	private void fillOrderAmountInUSD(BasicDBObject orderRecord) {
		if (orderRecord.containsField("orderAmount")) {
			BasicDBObject orderAmount = (BasicDBObject) orderRecord.get("orderAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderAmount.getString("currencyCode"), "USD");
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
			log.error("Get " + url + " failed with status code " + httpCode);
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
			BasicDBObject orderMessage) throws JSONException {
		BasicDBObject orderRecord = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		searchQuery.put("orderID", orderMessage.getString("orderID"));
		String siteName = orderMessage.getString("site");
		searchQuery.put("site.name", siteName);
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));

		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("order");
		if (exchange.getProperties().containsKey("isManaged") && exchange.getProperty("isManaged", Boolean.class)) {
			orderRecord.put("isManaged", exchange.getProperty("isManaged", Boolean.class));
		}

		String updateStatus = OrderUpdateStatus.COMPLETE.toString();
		if (orderMessage.containsField("updateStatus")) {
			updateStatus = orderMessage.getString("updateStatus");
		}
		if (orderMessage.containsField("timeSettled")) {
			orderRecord.put("timeSettled", orderMessage.getLong("timeSettled"));
		}
		if (orderMessage.containsField("settlementStatus")) {
			orderRecord.put("settlementStatus", orderMessage.getString("settlementStatus"));
		}
		if (orderMessage.containsField("transactionPeriod")) {
			orderRecord.put("transactionPeriod", orderMessage.getString("transactionPeriod"));
		}
		if (orderMessage.containsField("timeOrderUpdated")) {
			orderRecord.put("timeOrderUpdated", orderMessage.getLong("timeOrderUpdated"));
		}
		if (exchange.getProperties().containsKey("isPartnerLogistics")
				&& exchange.getProperties().containsKey("airwayBillExists")) {
			orderRecord.put("isPartnerLogistics", exchange.getProperty("isPartnerLogistics"));
		}
		// update order data only when the update is complete
		if (OrderUpdateStatus.COMPLETE.toString().equals(updateStatus)) {
			fillOrderRecord(notificationOrderActionStatus, orderRecord, orderMessage);
			fillAdditionDetails(exchange, orderRecord, siteName);
			fillOrderAmountInUSD(orderRecord);
		}
		orderRecord.put("updateStatus", updateStatus);
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		// if we pass true then will modified data
		UpdateResult result = table.updateOne(searchQuery, new BasicDBObject("$set", orderRecord));
		if (result.getModifiedCount() == 0) {
			log.info("Order :"+orderMessage.getString("orderID")+" is already updated. this is duplicate message.");
			exchange.setProperty("stopProcess", true);
			return;
		}
		exchange.setProperty("orderRecord", updateAndGetLatestUpdatedOrder(searchQuery, orderMessage));
		exchange.getOut().setBody(orderMessage);
	}

	private BasicDBObject updateAndGetLatestUpdatedOrder(BasicDBObject searchQuery, BasicDBObject orderMessage) {
		BasicDBObject updateObject = new BasicDBObject();
		if (orderMessage.containsKey("notificationID")) {
			// Append the OrderNotificationID to the database
			updateObject.put("$push", new BasicDBObject("notificationID", orderMessage.get("notificationID")));
		}
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("order");
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
		fillTransactionKeyValuePair(orderRecord, "paymentMethods", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentType", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderItems", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "pickUpDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "refundDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "cancelDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "sellerDiscountAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "channelDiscountAmount", orderMessage);
		fillOrderTime(notificationOrderActionStatus, orderRecord);
	}

	@SuppressWarnings("unchecked")
	private void fillAdditionDetails(Exchange exchange, BasicDBObject orderRecord, String siteName)
			throws JSONException {
		Map<String, BasicDBObject> inventoryDetailsMap = (Map<String, BasicDBObject>) exchange
				.getProperty("inventoryDetailsMap");
		boolean addOrderItemLocation = false;
		boolean processOrdersWithSKUOnly = processOrdersWithtSKUOnly(exchange);
		List<BasicDBObject> newOrderItems = new ArrayList<BasicDBObject>();
		if (orderRecord.containsField("orderItems")) {
			List<BasicDBObject> orderItems = (ArrayList<BasicDBObject>) orderRecord.get("orderItems");
			for (int i = 0; i < orderItems.size(); i++) {
				BasicDBObject orderItem = orderItems.get(i);
				boolean orderHasInventory = false;
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
				if (processOrdersWithSKUOnly) {
					if (orderHasInventory) {
						newOrderItems.add(orderItem);
					}
				} else {
					newOrderItems.add(orderItem);
				}
			}
			orderRecord.put("orderItems", newOrderItems);
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
		// TODO if variant record has no image, get the parent image.
		// For quick fix, returning empty string
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

	private void fillOrderTime(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord) {
		// TODO: need to get more insights on how these dates can be used, so as
		// of now ignoring other state transition timestamps
		if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING)) {
			orderRecord.put("timeProcessing", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_COMPLETED)) {
			orderRecord.put("timeCompleted", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_PENDING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED_TO_CANCELLED)) {
			orderRecord.put("timeCancelled", DateUtil.getSIADateFormat());
		}
	}

}
