package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.bson.Document;

import com.sellinall.util.enums.SIAInventoryStatus;

public class GetRuleList implements Processor {

	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		List<String> nicknameIdList = new ArrayList<String>();
		nicknameIdList.add("DEFAULT");
		nicknameIdList.add(nickNameID);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", accountNumber);
		searchQuery.put("nickNameID", new Document("$in", nicknameIdList));

		Document elemMatch = new Document();
		elemMatch.put("leftOperand", "timeOrderCreated");
		elemMatch.put("operator", "LESS_THAN");
		elemMatch.put("rightOperand", new Document("$gt", System.currentTimeMillis() / 1000));

		Document conditions = new Document();
		conditions.put("$elemMatch", elemMatch);
		searchQuery.put("conditions", conditions);
		searchQuery.put("status", SIAInventoryStatus.ACTIVE.toString());
		Document sort = new Document();
		sort.put("priority", -1);
		exchange.getOut().setHeader(MongoDbConstants.SORT_BY, sort);
		exchange.getOut().setBody(searchQuery);
	}

}
