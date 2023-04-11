package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;

public class SetMultipleUnitQuantity implements Processor {

	static Logger log = Logger.getLogger(SetMultipleUnitQuantity.class.getName());

	public void process(Exchange exchange) {
		Document inventory = exchange.getIn().getBody(Document.class);
		String customSKU = inventory.getString("customSKU");
		String[] splitCustomSKU = customSKU.split("[xX]");
		int lotSize = Integer.parseInt(splitCustomSKU[splitCustomSKU.length - 1]);
		int noOfItem = inventory.getInteger("noOfItem");
		exchange.setProperty("inventory", inventory.toString());
		int finalQuantity = exchange.getProperty("basicUnitQuantity", Integer.class) / lotSize;
		exchange.setProperty("quantity", noOfItem - finalQuantity);
		exchange.setProperty("processBasicUnitSKU", false);
	}
}