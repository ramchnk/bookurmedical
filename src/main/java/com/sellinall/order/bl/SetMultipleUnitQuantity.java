package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

public class SetMultipleUnitQuantity implements Processor {

	static Logger log = Logger.getLogger(SetMultipleUnitQuantity.class.getName());

	public void process(Exchange exchange) {
		BasicDBObject inventory = exchange.getIn().getBody(BasicDBObject.class);
		String customSKU = inventory.getString("customSKU");
		String[] splitCustomSKU = customSKU.split("[xX]");
		int lotSize = Integer.parseInt(splitCustomSKU[splitCustomSKU.length - 1]);
		int noOfItem = inventory.getInt("noOfItem");
		exchange.setProperty("inventory", inventory.toString());
		int finalQuantity = exchange.getProperty("basicUnitQuantity", Integer.class) / lotSize;
		exchange.setProperty("quantity", noOfItem - finalQuantity);
		exchange.setProperty("processBasicUnitSKU", false);
	}
}