package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.enums.OrderFulfilledBy;

public class InitOrderItemRoute implements Processor {
	static Logger log = Logger.getLogger(InitOrderItemRoute.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderItemMessage = OrderUtil
				.parseToJsonObject(Document.parse(exchange.getIn().getBody(String.class)));
		exchange.setProperty("orderItemMessage", orderItemMessage);
		exchange.setProperty("hasSKU", false);
		if (orderItemMessage.has("SKU")) {
			exchange.setProperty("hasSKU", true);
		}
		exchange.setProperty("isEligibleToStockSync", true);
		// In Blibli "orderFulfilledBy" flag maintained by order item level
		if (orderItemMessage.has("orderFulfilledBy")
				&& orderItemMessage.getString("orderFulfilledBy").equals(OrderFulfilledBy.CHANNEL.toString())) {
			log.info("This " + orderItemMessage.getString("orderItemID") + " item is fulfilled by channel in Order : " + exchange.getProperty("orderID", String.class) + ". So stock sync not required to SIA system.");
			exchange.setProperty("isEligibleToStockSync", false);
		}
		exchange.getOut().setBody(orderItemMessage);
	}
}
