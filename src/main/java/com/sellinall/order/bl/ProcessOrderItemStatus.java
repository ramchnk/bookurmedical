package com.sellinall.order.bl;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;

public class ProcessOrderItemStatus implements Processor {
	static Logger log = Logger.getLogger(ProcessOrderItemStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderItemMessage = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(orderItemMessage);
		SIAOrderStatus notificationOrderStatus = SIAOrderStatus.valueOf(orderItemMessage.getString("orderStatus"));
		NotificationOrderActionStatus notificationOrderActionStatus = NotificationOrderActionStatus.NO_ACTION;
		notificationOrderActionStatus = NotificationOrderActionStatus
				.valueOf(orderItemMessage.getString("orderStatus"));
		String orderID = exchange.getProperty("orderID", String.class);
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		if (hasOrderInDB) {
			String orderItemID = orderItemMessage.getString("orderItemID");
			BasicDBObject orderDBObject = exchange.getProperty("orderDBObject", BasicDBObject.class);
			if (orderDBObject.containsField("orderItems")) {
				List<BasicDBObject> orderItems = (ArrayList<BasicDBObject>) orderDBObject.get("orderItems");
				for (int i = 0; i < orderItems.size(); i++) {
					BasicDBObject orderItem = orderItems.get(i);
					if (orderItem.containsField("orderStatus")) {
						if (orderItem.getString("orderItemID").equals(orderItemID)) {
							SIAOrderStatus orderDBStatus = SIAOrderStatus.valueOf(orderItem.getString("orderStatus"));
							notificationOrderActionStatus = OrderUtil.handleExistingOrderStatus(notificationOrderStatus,
									orderDBStatus, orderItemMessage, orderID, "orderItem");
							// Note : handled payment status in orderItem level for Cash On Delivery(COD)
							// cancelled/returned orders
							if (orderItem.containsField("paymentStatus") && (notificationOrderStatus.equals(SIAOrderStatus.CANCELLED.toString())
											|| notificationOrderStatus.equals(SIAOrderStatus.RETURNED.toString()))) {
								SIAPaymentStatus paymentDBStatus = SIAPaymentStatus
										.valueOf(orderItem.getString("paymentStatus"));
								orderItemMessage.put("paymentStatus",
										paymentDBStatus.equals(SIAPaymentStatus.COMPLETED.toString())
												? SIAPaymentStatus.REFUNDED.toString()
												: paymentDBStatus.toString());
							}
							break;
						}
					} else {
						break;
					}
				}
			}

		}
		log.debug("OrderActionStatus	: " + notificationOrderActionStatus);
		exchange.setProperty("notificationOrderActionStatus", notificationOrderActionStatus);
	}
}
