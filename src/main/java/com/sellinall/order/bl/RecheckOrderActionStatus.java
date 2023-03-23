package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.SIAOrderStatus;

public class RecheckOrderActionStatus implements Processor {
	static Logger log = Logger.getLogger(RecheckOrderActionStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		String orderID = exchange.getProperty("orderID", String.class);
		SIAOrderStatus notificationOrderStatus = SIAOrderStatus.valueOf(orderMessage.getString("orderStatus"));
		NotificationOrderActionStatus notificationOrderActionStatus = NotificationOrderActionStatus.NO_ACTION;
		notificationOrderActionStatus = NotificationOrderActionStatus.valueOf(orderMessage.getString("orderStatus"));
		Boolean hasOrderInDB = (Boolean) exchange.getProperty("hasOrderInDB");
		if (hasOrderInDB) {
			Document orderDBObject = exchange.getProperty("orderDBObject", Document.class);
			SIAOrderStatus orderDBStatus = SIAOrderStatus.valueOf(orderDBObject.getString("orderStatus"));
			notificationOrderActionStatus = OrderUtil.handleExistingOrderStatus(notificationOrderStatus, orderDBStatus,
					orderMessage, orderID, "order");
		}
		log.debug("OrderActionStatus	: " + notificationOrderActionStatus);
		exchange.setProperty("notificationOrderActionStatus", notificationOrderActionStatus);

	}

}
