package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.util.enums.SIAOrderStatus;

public class ValidateOrderItemStatus implements Processor {
	static Logger log = Logger.getLogger(ValidateOrderItemStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderItemMessage = exchange.getIn().getBody(JSONObject.class);
		exchange.getOut().setBody(orderItemMessage);
		SIAOrderStatus notificationOrderStatus = SIAOrderStatus.valueOf(orderItemMessage.getString("orderStatus"));
		if ((SIAOrderStatus.UNKNOWN.equals(notificationOrderStatus)
				|| SIAOrderStatus.UNSUPPORTED.equals(notificationOrderStatus))) {
			log.warn("Notification Order Status : " + notificationOrderStatus);
			throw new Exception("Unknown Notification Order Status");
		}
	}
}
