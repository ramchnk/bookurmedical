package com.sellinall.order.bl;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.util.ParserUtil;

public class ProcessHistoricalOrder implements Processor {
	static Logger log = Logger.getLogger(ProcessHistoricalOrder.class.getName());

	@Override
	public void process(Exchange exchange) throws Exception {
		String orderID = exchange.getProperty("orderID", String.class);
		boolean hasOrderInDB = exchange.getProperty("hasOrderInDB", Boolean.class);
		boolean isEligibleToProcessOrder = true;
		if (hasOrderInDB) {
			Document orderFromDB = exchange.getProperty("orderDBObject", Document.class);
			if (!orderFromDB.containsKey("isHistoricalOrder") || !orderFromDB.getBoolean("isHistoricalOrder")) {
				isEligibleToProcessOrder = false;
				log.info("Skipping historical order :" + orderID + ", because order polled through normal polling.");
			}
		}
		exchange.setProperty("isEligibleToProcessOrder", isEligibleToProcessOrder);
		exchange.setProperty("notificationOrderActionStatus", NotificationOrderActionStatus.NO_ACTION);
		exchange.setProperty("inventoryDetailsMap", new HashMap<String, Document>());
	}

}
