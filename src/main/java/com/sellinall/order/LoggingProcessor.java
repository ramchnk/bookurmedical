package com.sellinall.order;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

public class LoggingProcessor implements Processor {
	static Logger log = Logger.getLogger(LoggingProcessor.class.getName());
	static int si = 0;

	public void process(Exchange exchange) throws Exception {

		log.debug("LoggingProcessor " + ++si + " {");
		log.debug("Received Bodyr: " +

		exchange.getIn().getBody().toString());

		log.debug("Received header: " +

		exchange.getIn().getHeaders().toString());
		try {
			log.debug("Received Exception: " + exchange.getException() != null ? (exchange.getException().getMessage() != null ? exchange
					.getException().getMessage() : "")
					: "no exception");
		} catch (Exception e) {
			log.debug("Exception" + e);
		}
		log.debug(si + "} done.");
	}
}