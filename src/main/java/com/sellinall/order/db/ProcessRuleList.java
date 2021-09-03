package com.sellinall.order.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.bl.RuleActionsMapping;
import com.sellinall.order.bl.RuleEngine;
import com.sellinall.order.enums.RuleActionEnum;

public class ProcessRuleList implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<BasicDBObject> ruleList = (List<BasicDBObject>) exchange.getIn().getBody();
		BasicDBObject orderDetails = (BasicDBObject) exchange.getProperty("orderRecord");
		String selectedWMS = exchange.getProperty("selectedWMS", String.class);
		List<String> giftItemSKUs = new ArrayList<String>();
		List<BasicDBObject> freeGiftOrderItems = new LinkedList<>();
		for (BasicDBObject rule : ruleList) {
			BasicDBObject action = (BasicDBObject) rule.get("action");
			String ruleEngineMethod = RuleActionsMapping
					.getRuleEngineMethod(RuleActionEnum.valueOf(action.getString("name")));
			Method method = RuleEngine.class.getMethod(ruleEngineMethod, BasicDBObject.class, BasicDBObject.class,
					List.class, String.class, List.class);
			method.invoke(new RuleEngine(), orderDetails, rule, freeGiftOrderItems, selectedWMS, giftItemSKUs);
		}
		if (freeGiftOrderItems.size() > 0) {
			List<BasicDBObject> orderItems = (List<BasicDBObject>) orderDetails.get("orderItems");
			orderItems.addAll(freeGiftOrderItems);
			addFreeGiftItemsToOrder(freeGiftOrderItems, orderDetails.getString("_id"));
		}
	}

	private static void addFreeGiftItemsToOrder(List<BasicDBObject> freeGiftOrderItems, String id) {
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(id));
		BasicDBObject updateObject = new BasicDBObject();
		BasicDBObject addToset = new BasicDBObject();
		addToset.put("orderItems", new BasicDBObject("$each", freeGiftOrderItems));
		updateObject.put("$addToSet", addToset);
		table.updateOne(searchQuery, updateObject);
	}
}
