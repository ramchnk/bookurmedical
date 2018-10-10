package com.sellinall.order.bl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.util.enums.SIAOrderStatus;

public class ProcessOrderStatus implements Processor {
	static Logger log = Logger.getLogger(ProcessOrderStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
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
		NotificationOrderActionStatus notificationOrderActionStatus = NotificationOrderActionStatus.NO_ACTION;
		exchange.setProperty("hasCombinedOrderIds", false);
		if ( orderMessage.has("combinedOrderIds") && !orderMessage.isNull("combinedOrderIds")) {
			exchange.setProperty("hasCombinedOrderIds", true);
		}
		if ( ( SIAOrderStatus.UNKNOWN.equals(notificationOrderStatus) ||
			  SIAOrderStatus.UNSUPPORTED.equals(notificationOrderStatus) ) ) {
			log.warn("Notification Order Status : " + notificationOrderStatus);
			throw new Exception("Unknown Notification Order Status"); 
		}
		notificationOrderActionStatus = NotificationOrderActionStatus.valueOf(orderMessage.getString("orderStatus"));
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		if (hasOrderInDB) {
			BasicDBObject orderDBObject = exchange.getProperty("orderDBObject", BasicDBObject.class);
			SIAOrderStatus orderDBStatus = SIAOrderStatus.valueOf(orderDBObject.getString("orderStatus"));
			notificationOrderActionStatus = handleExistingOrderStatus(exchange, notificationOrderStatus, orderDBStatus, orderMessage);
		} 
		log.debug("OrderActionStatus	: " + notificationOrderActionStatus);
		exchange.setProperty("notificationOrderActionStatus", notificationOrderActionStatus);

		// inventoryDetailsMap is set empty as this is used inside the splitter
		Map<String, BasicDBObject> inventoryDetailsMap = new HashMap<String, BasicDBObject>();
		exchange.setProperty("inventoryDetailsMap", inventoryDetailsMap);

		exchange.getOut().setBody(orderMessage);
	}
	
	private NotificationOrderActionStatus handleExistingOrderStatus(
			Exchange exchange, SIAOrderStatus notificationOrderStatus, 
			SIAOrderStatus orderDBStatus, JSONObject orderMessage) throws Exception {
		if (notificationOrderStatus.equals(orderDBStatus)) {
			return NotificationOrderActionStatus.NO_ACTION;
		}
		String orderStateTransition = orderDBStatus+"_TO_"+notificationOrderStatus;	
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
								.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_ACCEPTED.toString())) {
					log.warn(" The backward transistion came for orderID is " + orderMessage.getString("orderID")
							+ " and orderStateTransistion " + orderStateTransition);
				}
				return NotificationOrderActionStatus.valueOf(orderStateTransition);
			}
		} catch ( Exception e) {
		//   TODO Activity logging for Invalid State Transitions
			String errMsg = "Some Invalid Order state transition : "+ orderStateTransition + " Exception Message : " + e.getMessage() + " orderMessage: " + orderMessage;
			log.warn(errMsg);
			throw new Exception(errMsg);
		}
		return NotificationOrderActionStatus.NO_ACTION;
	}
}
