package com.sellinall.order.db;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.SIAInventoryStatus;

public class UpdateSoldInventoryDetails implements Processor {

	static Logger log = Logger.getLogger(UpdateSoldInventoryDetails.class.getName());
	static String siteNames[] = PostingSites.getConfig().getSitesList();

	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = OrderUtil
				.parseToJsonObject((DBObject) JSON.parse(exchange.getProperty("inventory", String.class)));
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange
				.getProperty("notificationOrderActionStatus");
		BasicDBObject inventoryDBRecord = (BasicDBObject) JSON.parse(inventoryDBRecordJSON.toString());
		String SKU = inventoryDBRecord.getString("SKU");
		int quantity = exchange.getProperty("quantity", Integer.class);
		BasicDBObject quantityIncDecModifier = new BasicDBObject();
		BasicDBObject quantitySetModifier = new BasicDBObject();
		processSoldQuantityUpdates(notificationOrderActionStatus, inventoryDBRecord, quantity, quantityIncDecModifier,
				quantitySetModifier);
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("inventory");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("SKU", SKU);

		BasicDBObject queryToDB = new BasicDBObject();
		queryToDB.put("$inc", quantityIncDecModifier);
		if(!quantitySetModifier.isEmpty()){
			queryToDB.put("$set", quantitySetModifier);
		}
		log.debug("searchQuery: " + searchQuery + " queryToDB: " + queryToDB);
		table.updateOne(searchQuery, queryToDB);
	}

	@SuppressWarnings("unchecked")
	private void processSoldQuantityUpdates(NotificationOrderActionStatus notificationOrderActionStatus,
			BasicDBObject inventoryDBRecord, int quantitySold, BasicDBObject quantityIncDecModifier,
			BasicDBObject quantitySetModifier) throws JSONException {
		boolean newOrder = notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERY_FAILED);
		boolean cancelledOrder = notificationOrderActionStatus
				.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_PENDING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_RETURNED);
		for (String siteName : siteNames) {
			if (!inventoryDBRecord.containsField(siteName)) {
				continue;
			}
			ArrayList<BasicDBObject> siteSpecificList = (ArrayList<BasicDBObject>) inventoryDBRecord.get(siteName);
			for (int index = 0; index < siteSpecificList.size(); index++) {
				BasicDBObject siteSpecific = siteSpecificList.get(index);
				if (siteSpecific.containsField("status")
						&& !siteSpecific.getString("status").equals(SIAInventoryStatus.ACTIVE.toString())) {
					continue;
				}
				if (newOrder) {

					incrementSetter(quantityIncDecModifier, siteName + "." + index + ".noOfItemsold", quantitySold);
					quantitySetModifier.append(siteName + "." + index + ".lastSoldTime",
							System.currentTimeMillis() / 1000L);
				} else if (cancelledOrder) {
					incrementSetter(quantityIncDecModifier, siteName + "." + index + ".noOfItemsold", -quantitySold);

				}
			}

		}
		if (newOrder) {
			incrementSetter(quantityIncDecModifier, "noOfItemsold", quantitySold);
		} else if (cancelledOrder) {
			incrementSetter(quantityIncDecModifier, "noOfItemsold", -quantitySold);
		}
	}

	private void incrementSetter(BasicDBObject modifier, String key, int value) {
		modifier.append(key, value);
	}

}
