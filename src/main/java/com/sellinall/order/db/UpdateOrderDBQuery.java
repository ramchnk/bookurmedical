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
import com.mudra.sellinall.util.DateUtil;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;

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
		
		if (!hasOrderInDB) {
			insertOrderRecord(exchange, notificationOrderActionStatus, orderMessage, inBody);
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
		//TODO: remove the condition after all publishers start publishing user id.
		if (orderMessage.containsField("userId")) {
			orderRecord.put("userId", orderMessage.getString("userId"));
		} else {
			orderRecord.put("userId", inBody.getString("userID"));
		}
		fillOrderRecord (notificationOrderActionStatus, orderRecord, orderMessage);
		List<String> notificationIDList = new ArrayList<String>();
		notificationIDList.add(orderMessage.getString("notificationID"));
		orderRecord.put("notificationID", notificationIDList);
		orderRecord.put("timeCreated", DateUtil.getSIADateFormat());
		fillAdditionDetails(exchange, orderRecord, siteName);
		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		table.insert(orderRecord);
		exchange.getOut().setBody(orderRecord);
	}
	
	private void updateOrderRecord(Exchange exchange,
			NotificationOrderActionStatus notificationOrderActionStatus,
			BasicDBObject orderMessage) throws JSONException {
		BasicDBObject orderRecord = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("orderID", orderMessage.getString("orderID"));
		String siteName = orderMessage.getString("site");
		searchQuery.put("site.name", siteName);
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));
		
		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		// Append the OrderNotificationID to the database
		table.update(searchQuery, new BasicDBObject("$push", new BasicDBObject("notificationID", orderMessage.get("notificationID"))));
		fillOrderRecord (notificationOrderActionStatus, orderRecord, orderMessage);
		fillAdditionDetails(exchange, orderRecord, siteName);
		orderRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		if (orderMessage.containsField("updateStatus")) {
			orderRecord.put("updateStatus", orderMessage.getString("updateStatus"));
		} else {
			orderRecord.put("updateStatus", "success");
		}
		table.update(searchQuery, new BasicDBObject("$set", orderRecord));
		exchange.getOut().setBody(orderMessage);
	}
	
	private void fillOrderRecord (NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord, BasicDBObject orderMessage) {
		fillTransactionKeyValuePair(orderRecord, "buyerDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentMethods", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentType", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderItems", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "failureMessage", orderMessage);
		fillOrderTime(notificationOrderActionStatus, orderRecord);
	}

	@SuppressWarnings("unchecked")
	private void fillAdditionDetails(Exchange exchange, BasicDBObject orderRecord, String siteName)
			throws JSONException {
		Map<String, BasicDBObject> inventoryDetailsMap = (Map<String, BasicDBObject>) exchange
				.getProperty("inventoryDetailsMap");
		if (orderRecord.containsField("orderItems")) {
			List<BasicDBObject> orderItems = (ArrayList<BasicDBObject>) orderRecord.get("orderItems");
			for (int i = 0; i < orderItems.size(); i++) {
				BasicDBObject orderItem = orderItems.get(i);
				if (orderItem.containsField("SKU")) {
					String SKU = orderItem.getString("SKU");
					BasicDBObject inventoryValues = inventoryDetailsMap.get(SKU);
					orderItem.put("itemTitle", inventoryValues.getString("itemTitle"));
					if (!getImageURL(inventoryValues, siteName).isEmpty()) {
						orderItem.put("imageURL", getImageURL(inventoryValues, siteName));
					}
					if (inventoryValues.containsField("variantDetails")) {
						orderItem.put("variantDetails", inventoryValues.get("variantDetails"));
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

	private void fillTransactionKeyValuePair (BasicDBObject orderRecord, String key, BasicDBObject orderMessage) {
		if (orderMessage.containsField(key)) {
			orderRecord.put(key, orderMessage.get(key));
		}
	}
	
	private void fillOrderTime(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord){
		// TODO: need to get more insights on how these dates can be used, so as of now ignoring other state transition timestamps
		if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING)) {
			orderRecord.put("timeProcessing", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.COMPLETED) ||
				notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_COMPLETED)) {
			orderRecord.put("timeCompleted", DateUtil.getSIADateFormat());
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCELLED) ||
				notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED) ) {
			orderRecord.put("timeCancelled", DateUtil.getSIADateFormat());			
		}
	}
}