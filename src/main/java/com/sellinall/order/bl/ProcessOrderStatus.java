package com.sellinall.order.bl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.SIAOrderStatus;

public class ProcessOrderStatus implements Processor {
	static Logger log = Logger.getLogger(ProcessOrderStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		String orderID = exchange.getProperty("orderID", String.class);
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
		boolean isStatusHandledInOrderItem = false;
		if (orderMessage.has("orderStatuses")) {
			isStatusHandledInOrderItem = true;
		}
		exchange.setProperty("isStatusHandledInOrderItem", isStatusHandledInOrderItem);
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
			notificationOrderActionStatus = OrderUtil.handleExistingOrderStatus(notificationOrderStatus, orderDBStatus,
					orderMessage, orderID, "order");
		} 
		log.debug("OrderActionStatus	: " + notificationOrderActionStatus);
		exchange.setProperty("notificationOrderActionStatus", notificationOrderActionStatus);

		// inventoryDetailsMap is set empty as this is used inside the splitter
		Map<String, BasicDBObject> inventoryDetailsMap = new HashMap<String, BasicDBObject>();
		exchange.setProperty("inventoryDetailsMap", inventoryDetailsMap);

		exchange.getOut().setBody(orderMessage);
	}
}
