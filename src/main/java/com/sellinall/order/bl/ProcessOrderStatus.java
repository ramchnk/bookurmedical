package com.sellinall.order.bl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.enums.NotificationPaymentActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.OrderUpdateStatus;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;
import com.sellinall.util.enums.UserMessageName;

public class ProcessOrderStatus implements Processor {
	static Logger log = Logger.getLogger(ProcessOrderStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		String orderID = exchange.getProperty("orderID", String.class);
		exchange.setProperty("isOldOrder", false);
		if (exchange.getProperties().containsKey("timeLinked")) {
			long timeLinked = exchange.getProperty("timeLinked", Long.class);
			long timeOrderCreated = new BigDecimal(orderMessage.get("timeOrderCreated").toString()).longValue();
			if (timeOrderCreated < timeLinked) {
				exchange.setProperty("isOldOrder", true);
			}
		}
		if (orderMessage.has("shippingDetails")) {
			JSONObject shippingObj = orderMessage.getJSONObject("shippingDetails");
			if (shippingObj.has("shippingTrackingDetails")) {
				JSONObject shippingTrackingObj = shippingObj.getJSONObject("shippingTrackingDetails");
				if (shippingTrackingObj.has("airwayBill")) {
					String trackingId = shippingTrackingObj.getString("airwayBill");
					if (!trackingId.isEmpty() && trackingId != null) {
						exchange.setProperty("airwayBillExists", true);
					}
				}
			}
		}
		SIAOrderStatus notificationOrderStatus = SIAOrderStatus.valueOf(orderMessage.getString("orderStatus"));
		SIAPaymentStatus notificationPaymentStatus = SIAPaymentStatus.valueOf(orderMessage.getString("paymentStatus"));
		boolean isStatusHandledInOrderItem = false;
		if (orderMessage.has("orderStatuses")) {
			isStatusHandledInOrderItem = true;
		}
		exchange.setProperty("isStatusHandledInOrderItem", isStatusHandledInOrderItem);
		NotificationOrderActionStatus notificationOrderActionStatus = NotificationOrderActionStatus.NO_ACTION;
		NotificationPaymentActionStatus notificationPaymentActionStatus = NotificationPaymentActionStatus.NO_ACTION;
		exchange.setProperty("hasCombinedOrderIds", false);
		if (orderMessage.has("combinedOrderIds") && !orderMessage.isNull("combinedOrderIds")) {
			exchange.setProperty("hasCombinedOrderIds", true);
		}
		if ((SIAOrderStatus.UNKNOWN.equals(notificationOrderStatus)
				|| SIAOrderStatus.UNSUPPORTED.equals(notificationOrderStatus))) {
			log.warn("Notification Order Status : " + notificationOrderStatus);
			throw new Exception("Unknown Notification Order Status");
		}
		notificationOrderActionStatus = NotificationOrderActionStatus.valueOf(orderMessage.getString("orderStatus"));
		try {
			notificationPaymentActionStatus = NotificationPaymentActionStatus
					.valueOf(orderMessage.getString("paymentStatus"));
		} catch (Exception e) {
			log.debug("Unknow payement action status recevied" + orderMessage.getString("paymentStatus"));
		}
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		if (hasOrderInDB) {
			Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
			SIAOrderStatus orderDBStatus = SIAOrderStatus.valueOf(orderDBObject.getString("orderStatus"));
			notificationOrderActionStatus = OrderUtil.handleExistingOrderStatus(notificationOrderStatus, orderDBStatus,
					orderMessage, orderID, "order");
			SIAPaymentStatus orderDBPayementStatus = SIAPaymentStatus.valueOf(orderDBObject.getString("paymentStatus"));
			notificationPaymentActionStatus = OrderUtil.handleExistingPaymentStatus(notificationPaymentStatus,
					orderDBPayementStatus, orderMessage);
			//Note : handled payment status for Cash On Delivery(COD) cancelled/returned orders
			if (notificationOrderStatus.equals(SIAOrderStatus.CANCELLED.toString())
					|| notificationOrderStatus.equals(SIAOrderStatus.RETURNED.toString())) {
				orderMessage.put("paymentStatus", orderDBPayementStatus.equals(SIAPaymentStatus.COMPLETED.toString())
								? SIAPaymentStatus.REFUNDED.toString() : orderDBPayementStatus.toString());
			}
			buildOrderUpdateJournal(exchange, hasOrderInDB, orderDBObject, orderMessage);
		} else {
			buildOrderUpdateJournal(exchange, hasOrderInDB, null, orderMessage);
		}
		String userMessageName = null;
		if (!hasOrderInDB) {
			userMessageName = UserMessageName.ORDER_CREATED.toString();
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_DELIVERED)) {
			userMessageName = UserMessageName.ORDER_DELIVERED.toString();
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_PENDING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_CANCELLED)) {
			userMessageName = UserMessageName.ORDER_CANCELLED.toString();
		} else if (notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERY_FAILED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURN_REQUESTED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.RETURN_SHIPPED_TO_RETURNED)) {
			userMessageName = UserMessageName.ORDER_RETURNED.toString();
		} else if (notificationPaymentActionStatus.equals(NotificationPaymentActionStatus.NOT_INITIATED_TO_COMPLETED)) {
			userMessageName = UserMessageName.ORDER_PAYMENT_RECEIVED.toString();
		}
		exchange.setProperty("userMessageName", userMessageName);
		log.debug("OrderActionStatus	: " + notificationOrderActionStatus);
		exchange.setProperty("notificationOrderActionStatus", notificationOrderActionStatus);

		// inventoryDetailsMap is set empty as this is used inside the splitter
		Map<String, Document> inventoryDetailsMap = new HashMap<String, Document>();
		exchange.setProperty("inventoryDetailsMap", inventoryDetailsMap);

		// For qoo10 orders orderItems doesn't exist in update polling msg so pushed
		// orderItems from existing db object to satisfy free gift rule cases
		if (exchange.getProperty("processRule", Boolean.class) && exchange.getProperty("hasOrderInDB", Boolean.class)
				&& exchange.getProperties().containsKey("siteName")
				&& exchange.getProperty("siteName", String.class).equals("qoo10") && !orderMessage.has("orderItems")) {
			Document orderFromDB = exchange.getProperty("orderDBObject", Document.class);
			if (orderFromDB.containsKey("orderItems")) {
				orderMessage.put("orderItems", new JSONArray(orderFromDB.get("orderItems").toString()));
			}
		}
		exchange.getOut().setBody(orderMessage);
	}

	public void buildOrderUpdateJournal(Exchange exchange, Boolean hasOrderInDB, Document orderDBObject,
			JSONObject orderMessage) throws JSONException {
		JSONArray journalMessage = new JSONArray();
		setJournalMessage(journalMessage, "orderStatus", hasOrderInDB ? orderDBObject.getString("orderStatus") : null,
				orderMessage.getString("orderStatus"));
		if (orderMessage.has("shippingStatus")
				&& (!hasOrderInDB || (hasOrderInDB && orderDBObject.containsKey("shippingStatus")))) {
			setJournalMessage(journalMessage, "shippingStatus",
					hasOrderInDB ? orderDBObject.getString("shippingStatus") : null,
					orderMessage.getString("shippingStatus"));
		}
		exchange.setProperty("isEligiblePublishToJournal", false);

		String updateStatus = OrderUpdateStatus.COMPLETE.toString();
		if (orderMessage.has("updateStatus")) {
			updateStatus = orderMessage.getString("updateStatus");
		}
		if (journalMessage.length() > 0 && orderMessage.has("addendum")
				&& (updateStatus.equals(OrderUpdateStatus.COMPLETE.toString()) || !hasOrderInDB)) {
			exchange.setProperty("isEligiblePublishToJournal", true);
			exchange.setProperty("journalMessage", journalMessage);
		}
	}

	public void setJournalMessage(JSONArray journalMessage, String fieldName, String oldValue, String newValue)
			throws JSONException {
		if (oldValue != null) {
			if (!oldValue.equals(newValue)) {
				JSONObject statusChange = new JSONObject();
				statusChange.put("fieldName", fieldName);
				statusChange.put("oldValue", oldValue);
				statusChange.put("newValue", newValue);
				journalMessage.put(statusChange);
			}
		} else {
			JSONObject statusChange = new JSONObject();
			statusChange.put("fieldName", fieldName);
			statusChange.put("newValue", newValue);
			journalMessage.put(statusChange);
		}
	}
}
