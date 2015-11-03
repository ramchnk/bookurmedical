/**
 * 
 */
package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

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
			insertOrderRecord(notificationOrderActionStatus, orderMessage, inBody);
			return;
		}
		updateOrderRecord(notificationOrderActionStatus, orderMessage);
	}

	private void insertOrderRecord(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderMessage, JSONObject inBody) throws JSONException {
		BasicDBObject site = new BasicDBObject();
		site.put("name", orderMessage.getString("site"));
		site.put("nickNameID", orderMessage.getString("nickNameID"));
		BasicDBObject orderRecord = new BasicDBObject();
		orderRecord.put("site", site);
		orderRecord.put("orderID", orderMessage.getString("orderID"));
		orderRecord.put("userId", inBody.getString("userID"));
		fillOrderRecord (notificationOrderActionStatus, orderRecord, orderMessage);
		List<String> notificationIDList = new ArrayList<String>();
		notificationIDList.add(orderMessage.getString("notificationID"));
		orderRecord.put("notificationID", notificationIDList);
		orderRecord.put("timeCreated", DateUtil.getSIADateFormat());
		log.debug("Order Document Inserting : "+orderRecord);
		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		table.insert(orderRecord);
	}
	
	private void updateOrderRecord(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderMessage) throws JSONException {
		
		BasicDBObject orderRecord = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("orderID", orderMessage.getString("orderID"));
		searchQuery.put("site.name", orderMessage.getString("site"));
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));
		
		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		// Append the OrderNotificationID to the database
		table.update(searchQuery, new BasicDBObject("$push", new BasicDBObject("notificationID", orderMessage.get("notificationID"))));
		fillOrderRecord (notificationOrderActionStatus, orderRecord, orderMessage);
		orderRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		log.debug("Order Document Updating : "+orderRecord);
		table.update(searchQuery, new BasicDBObject("$set", orderRecord));
	}
	
	private void fillOrderRecord (NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord, BasicDBObject orderMessage) {
		fillTransactionKeyValuePair(orderRecord, "buyerDetails", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentMethods", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderItems", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "paymentStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingStatus", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "orderAmount", orderMessage);
		fillTransactionKeyValuePair(orderRecord, "shippingDetails", orderMessage);
		fillOrderTime(notificationOrderActionStatus, orderRecord);
	}

	private void fillTransactionKeyValuePair (BasicDBObject orderRecord, String key, BasicDBObject orderMessage) {
		if (orderMessage.containsField(key)) {
			orderRecord.put(key, orderMessage.get(key));
		}
	}
	
	private void fillOrderTime(NotificationOrderActionStatus notificationOrderActionStatus, BasicDBObject orderRecord){
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