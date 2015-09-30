package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.sellinall.enums.SIAOrderStatus;
import com.sellinall.order.enums.NotificationOrderActionStatus;

public class ProcessOrderStatus implements Processor {
	static Logger log = Logger.getLogger(ProcessOrderStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = new JSONObject(exchange.getProperty("orderMessage", String.class));
		SIAOrderStatus notificationOrderStatus = SIAOrderStatus.valueOf(orderMessage.getString("orderStatus"));
		NotificationOrderActionStatus notificationOrderActionStatus = NotificationOrderActionStatus.NO_ACTION;
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
			if ( orderStateTransition.equals(NotificationOrderActionStatus.valueOf(orderStateTransition).toString())) {
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
