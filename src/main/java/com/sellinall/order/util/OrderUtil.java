package com.sellinall.order.util;

import org.apache.log4j.Logger;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.util.enums.SIAOrderStatus;

public class OrderUtil {
	static Logger log = Logger.getLogger(OrderUtil.class.getName());

	public static NotificationOrderActionStatus handleExistingOrderStatus(SIAOrderStatus notificationOrderStatus,
			SIAOrderStatus orderDBStatus, JSONObject orderMessage, String orderID, String type) throws Exception {
		if (notificationOrderStatus.equals(orderDBStatus)) {
			return NotificationOrderActionStatus.NO_ACTION;
		}
		String orderStateTransition = orderDBStatus + "_TO_" + notificationOrderStatus;
		try {
			if (orderStateTransition.equals(NotificationOrderActionStatus.valueOf(orderStateTransition).toString())) {
				if (orderStateTransition.equals(NotificationOrderActionStatus.PROCESSING_TO_INITIATED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.PROCESSING_TO_COMBINED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.PROCESSING_TO_RETURNED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.DISPATCHED_TO_PROCESSING.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DELIVERED_TO_DISPATCHED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_ACCEPTED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.DELIVERY_FAILED_TO_RETURN_SHIPPED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.RETURN_REQUESTED_TO_RETURN_SHIPPED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.RETURN_SHIPPED_TO_RETURNED.toString())) {
					if (type.equals("order")) {
						log.warn(" The backward transistion came for orderID is " + orderID
								+ " and orderStateTransistion " + orderStateTransition);
					} else {
						log.warn(" The backward transistion came for orderID is " + orderID + "and orderItemID is "
								+ orderMessage.getString("orderItemID") + " and orderStateTransistion "
								+ orderStateTransition);
					}
				}
				return NotificationOrderActionStatus.valueOf(orderStateTransition);
			}
		} catch (Exception e) {
			// TODO Activity logging for Invalid State Transitions
			String errMsg = "Some Invalid Order state transition : " + orderStateTransition + " Exception Message : "
					+ e.getMessage() + " orderMessage: " + orderMessage;
			log.warn(errMsg);
			if (type.equals("order")) {
				throw new Exception(errMsg);
			}
		}
		return NotificationOrderActionStatus.NO_ACTION;
	}

	public static boolean checkIsNewOrder(NotificationOrderActionStatus notificationOrderActionStatus) {
		return notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERY_FAILED);
	}

	public static boolean checkIsCancelledOrder(NotificationOrderActionStatus notificationOrderActionStatus) {
		return notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_PENDING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED);
	}

	public static JSONArray parseToJsonArray(DBCursor myList) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		while (myList.hasNext()) {
			BasicDBObject doc = (BasicDBObject) myList.next();
			JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
			jsonArray.put(new JSONObject(doc.toJson(writerSettings)));
		}
		return jsonArray;
	}

	public static JSONObject parseToJsonObject(DBObject findOneObject) throws JSONException {
		if (findOneObject == null) {
			return new JSONObject();
		}
		BasicDBObject doc = (BasicDBObject) findOneObject;
		JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
		return new JSONObject(doc.toJson(writerSettings));
	}

}
