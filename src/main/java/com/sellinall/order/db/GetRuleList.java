package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

public class GetRuleList implements Processor {

	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("nickNameID", nickNameID);
		searchQuery.put("status", SIAInventoryStatus.ACTIVE.toString());
		exchange.getOut().setBody(searchQuery);
	}

}
