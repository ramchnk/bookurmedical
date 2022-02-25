package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mongodb.BasicDBObject;
import com.sellinall.util.enums.SIAInventoryStatus;

public class GetRuleList implements Processor {

	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		List<String> nicknameIdList = new ArrayList<String>();
		nicknameIdList.add("DEFAULT");
		nicknameIdList.add(nickNameID);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("nickNameID", new BasicDBObject("$in", nicknameIdList));

		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("leftOperand", "timeOrderCreated");
		elemMatch.put("operator", "LESS_THAN");
		elemMatch.put("rightOperand", new BasicDBObject("$gt", System.currentTimeMillis() / 1000));

		BasicDBObject conditions = new BasicDBObject();
		conditions.put("$elemMatch", elemMatch);
		searchQuery.put("conditions",conditions);
		searchQuery.put("status", SIAInventoryStatus.ACTIVE.toString());
		exchange.getOut().setBody(searchQuery);
	}

}
