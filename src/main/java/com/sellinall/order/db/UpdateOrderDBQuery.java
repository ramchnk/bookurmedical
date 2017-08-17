/**
 * 
 */
package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.util.DateUtil;
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
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange.getProperty("notificationOrderActionStatus");
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		JSONObject orderMessageJSON = exchange.getProperty("message", JSONObject.class);
		BasicDBObject orderMessage = (BasicDBObject) JSON.parse(orderMessageJSON.toString());
		exchange.setProperty("isNewOrder", false);
		if (!hasOrderInDB) {
			insertOrderRecord(exchange, notificationOrderActionStatus, orderMessage, inBody);
			exchange.setProperty("isNewOrder", true);
			return;
		}
		updateOrderRecord(exchange, notificationOrderActionStatus, orderMessage);
	}

	private void insertOrderRecord(Exchange exchange, 
			NotificationOrderActionStatus notificationOrderActionStatus, 
			BasicDBObject orderMessage, 
			JSONObject inBody) throws JSONException {
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
		//TODO: remove the condition after all publishers start publishing user id.
		if (orderMessage.containsField("accountNumber")) {
			orderRecord.put("accountNumber", orderMessage.getString("accountNumber"));
		} else {
			orderRecord.put("accountNumber", inBody.getString("accountNumber"));
		}
		fillOrderRecord (notificationOrderActionStatus, orderRecord, orderMessage);
		List<String> notificationIDList = new ArrayList<String>();
		if(orderMessage.containsKey("notificationID")){
			notificationIDList.add(orderMessage.getString("notificationID"));
			orderRecord.put("notificationID", notificationIDList);
		}
		orderRecord.put("timeCreated", DateUtil.getSIADateFormat());
		orderRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		if (orderMessage.containsField("timeOrderCreated")) {
			orderRecord.put("timeOrderCreated", orderMessage.getLong("timeOrderCreated"));
		}
		if (orderMessage.containsField("shippingAmount")) {
			orderRecord.put("shippingAmount", orderMessage.get("shippingAmount"));
		}
		if (orderMessage.containsField("voucherAmount")) {
			orderRecord.put("voucherAmount", orderMessage.get("voucherAmount"));
		}
		exchange.setProperty("accountNumber", orderRecord.getString("accountNumber"));
		if (orderMessage.containsField("cartNumber")) {
			orderRecord.put("cartNumber", orderMessage.get("cartNumber"));
		}
		fillAdditionDetails(exchange, orderRecord, siteName);
		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		table.insert(orderRecord);
		//for accounting channel
		exchange.setProperty("orderRecord", orderRecord);
		exchange.getOut().setBody(orderMessage);
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

		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		if(orderMessage.containsKey("notificationID")){
			// Append the OrderNotificationID to the database
			table.update(searchQuery,
				new BasicDBObject("$push", new BasicDBObject("notificationID", orderMessage.get("notificationID"))));
		}

		String updateStatus = OrderUpdateStatus.COMPLETE.toString();
		if (orderMessage.containsField("updateStatus")) {
			updateStatus = orderMessage.getString("updateStatus");
		}
		if(orderMessage.containsField("timeSettled")){
			orderRecord.put("timeSettled", orderMessage.getLong("timeSettled"));
		}
		if(orderMessage.containsField("settlementStatus")){
			orderRecord.put("settlementStatus", orderMessage.getString("settlementStatus"));
		}
		if(orderMessage.containsField("transactionPeriod")){
			orderRecord.put("transactionPeriod", orderMessage.getString("transactionPeriod"));
		}

		//update order data only when the update is complete
		if (OrderUpdateStatus.COMPLETE.toString().equals(updateStatus)) {
			fillOrderRecord(notificationOrderActionStatus, orderRecord, orderMessage);
			fillAdditionDetails(exchange, orderRecord, siteName);
		}
		orderRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		orderRecord.put("updateStatus", updateStatus);
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		table.update(searchQuery, new BasicDBObject("$set", orderRecord));
		exchange.getOut().setBody(orderMessage);
	}
	
	private void fillOrderRecord (NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord, BasicDBObject orderMessage) {
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
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "refundDetails", orderMessage);
		fillOrderTime(notificationOrderActionStatus, orderRecord);
	}

	@SuppressWarnings("unchecked")
	private void fillAdditionDetails(Exchange exchange, BasicDBObject orderRecord, String siteName)
			throws JSONException {
		Map<String, BasicDBObject> inventoryDetailsMap = (Map<String, BasicDBObject>) exchange
				.getProperty("inventoryDetailsMap");
		boolean addOrderItemLocation = false;
		if (orderRecord.containsField("orderItems")) {
			List<BasicDBObject> orderItems = (ArrayList<BasicDBObject>) orderRecord.get("orderItems");
			for (int i = 0; i < orderItems.size(); i++) {
				BasicDBObject orderItem = orderItems.get(i);
				if (orderItem.containsField("SKU")) {
					String SKU = orderItem.getString("SKU");
					BasicDBObject inventoryValue = inventoryDetailsMap.get(SKU);
					if (inventoryValue != null) {
						orderItem.put("itemTitle", inventoryValue.getString("itemTitle"));
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
					}else{
						//If inventory deleted then 
						orderItem.remove("SKU");
						orderItem.remove("imageURL");
					}
					orderItems.set(i, orderItem);
				}
			}
			orderRecord.put("orderItems", orderItems);
		}
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
	
	private void fillTransactionKeyValuePair (BasicDBObject orderRecord, String key, BasicDBObject orderMessage) {
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
