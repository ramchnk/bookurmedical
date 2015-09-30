package com.sellinall.order.bl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

public class InitOrderRoute implements Processor {
	static Logger log = Logger.getLogger(InitOrderRoute.class.getName());
	public void process(Exchange exchange) throws Exception {
		String orderMessage = exchange.getIn().getBody(String.class);
		exchange.setProperty("orderMessage", orderMessage);
		exchange.getOut().setBody(orderMessage);
	}
}
