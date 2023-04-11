package com.sellinall.order.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBCursor;
import com.mudra.sellinall.config.Config;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.enums.NotificationPaymentActionStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

public class OrderUtil {
	static Logger log = Logger.getLogger(OrderUtil.class.getName());
	private static MemcachedClient memcachedClient;

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
								.equals(NotificationOrderActionStatus.PROCESSING_TO_PARTIALLY_RETURNED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.DISPATCHED_TO_PROCESSING.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.DISPATCHED_TO_PARTIALLY_RETURNED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DELIVERED_TO_DISPATCHED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED.toString())
						|| orderStateTransition.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.DELIVERED_TO_PARTIALLY_RETURNED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_ACCEPTED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.DELIVERY_FAILED_TO_RETURN_SHIPPED.toString())
						|| orderStateTransition
								.equals(NotificationOrderActionStatus.RETURN_REQUESTED_TO_RETURN_SHIPPED.toString())
						|| orderStateTransition.equals(
								NotificationOrderActionStatus.PARTIAL_RETURN_REQUESTED_TO_RETURN_SHIPPED.toString())) {
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

	public static NotificationPaymentActionStatus handleExistingPaymentStatus(
			SIAPaymentStatus notificationPaymentStatus, SIAPaymentStatus orderDBPaymentStatus, JSONObject orderMessage)
			throws Exception {
		if (notificationPaymentStatus.equals(orderDBPaymentStatus)) {
			return NotificationPaymentActionStatus.NO_ACTION;
		}
		String orderPaymentStateTransition = orderDBPaymentStatus + "_TO_" + notificationPaymentStatus;
		try {
			if (orderPaymentStateTransition
					.equals(NotificationPaymentActionStatus.valueOf(orderPaymentStateTransition).toString())) {
				return NotificationPaymentActionStatus.valueOf(orderPaymentStateTransition);
			}
		} catch (Exception e) {
			// TODO Activity logging for Invalid State Transitions
			String errMsg = "Some Invalid payment state transition : " + orderPaymentStateTransition
					+ " Exception Message : " + e.getMessage() + " orderMessage: " + orderMessage;
			log.debug(errMsg);
		}
		return NotificationPaymentActionStatus.NO_ACTION;
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
			Document doc = (Document) myList.next();
			JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
			jsonArray.put(new JSONObject(doc.toJson(writerSettings)));
		}
		return jsonArray;
	}

	public static JSONObject parseToJsonObject(Document findOneObject) throws JSONException {
		if (findOneObject == null) {
			return new JSONObject();
		}
		Document doc = (Document) findOneObject;
		JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
		return new JSONObject(doc.toJson(writerSettings));
	}

	public static void initMemoryCached() {
		try {
			AuthDescriptor ad = new AuthDescriptor(new String[] { "PLAIN" }, new PlainCallbackHandler(
					Config.getConfig().getMemcachedCloudUsername(), Config.getConfig().getMemcachedCloudPassword()));
			MemcachedClient mc = new MemcachedClient(
					new ConnectionFactoryBuilder().setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
							.setAuthDescriptor(ad).build(),
					AddrUtil.getAddresses(Config.getConfig().getMemcachedCloudServers()));
			memcachedClient = mc;
		} catch (IOException ex) {
			log.error("Memcached client could not be initialized");
			ex.printStackTrace();
		}
	}

	public static MemcachedClient getMemcachedClient() {
		return memcachedClient;
	}

	public static String getMCkeyforGcStatusWaitingSKUS(String fromCurrency, String toCurrency) {
		return fromCurrency + "-" + toCurrency + "-ExchangeRate";
	}

	public static Object getValueFromMemcache(String MCkey, boolean retry) {
		try {
			Object mcValue = memcachedClient.get(MCkey);
			if (mcValue != null) {
				return mcValue;
			}
		} catch (Exception e) {
			if (retry) {
				try {
					log.error("Retrying to read value from  memcache, key:" + MCkey);
					memcachedClient.shutdown();
					initMemoryCached();
					return getValueFromMemcache(MCkey, false);
				} catch (Exception i) {
					i.printStackTrace();
				}
			} else {
				log.error("unable to get value from memcache, key :" + MCkey);
				e.printStackTrace();
			}
		}
		return null;
	}

	public static void updateMemcache(String MCKey, int MC_MAX_EXPIRE_TIME, double exchangeRate) {
		memcachedClient.set(MCKey, MC_MAX_EXPIRE_TIME, exchangeRate);
	}
	
	public static List<Document> parseDocumentListFromArray(JSONArray arrayObject) throws JSONException {
		List<Document> documentList = new ArrayList<Document>();
		for (int iterator = 0; iterator < arrayObject.length(); iterator++) {
			documentList.add(Document.parse(arrayObject.get(iterator).toString()));
		}
		return documentList;
	}

}
