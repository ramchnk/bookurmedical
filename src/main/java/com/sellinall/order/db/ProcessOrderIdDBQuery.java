package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;

/**
 * @author Mallikarjun
 *
 */
public class ProcessOrderIdDBQuery implements Processor {
	static Logger log = Logger.getLogger(ProcessOrderIdDBQuery.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("hasOrderInDB", false);	
		BasicDBObject dbResult = exchange.getIn().getBody(BasicDBObject.class);
		
		if ( dbResult == null) {
			log.debug("New Notification Record - not exists in our DB Result: "+dbResult);			
			return;
		}
		exchange.setProperty("hasOrderInDB", true);
		exchange.setProperty("orderDBObject", dbResult);
	}
}