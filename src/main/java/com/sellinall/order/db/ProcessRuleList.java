package com.sellinall.order.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.bl.RuleActionsMapping;
import com.sellinall.order.bl.RuleEngine;
import com.sellinall.order.enums.RuleActionEnum;

public class ProcessRuleList implements Processor {

	public void process(Exchange exchange) throws Exception {
		List<Document> ruleList = (List<Document>) exchange.getIn().getBody();
		Document orderDetails = (Document) exchange.getProperty("orderRecord");
		String selectedWMS = exchange.getProperty("selectedWMS", String.class);
		List<String> giftItemSKUs = new ArrayList<String>();
		List<Document> freeGiftOrderItems = new LinkedList<>();
		for (Document rule : ruleList) {
			Document action = (Document) rule.get("action");
			String ruleEngineMethod = RuleActionsMapping
					.getRuleEngineMethod(RuleActionEnum.valueOf(action.getString("name")));
			Method method = RuleEngine.class.getMethod(ruleEngineMethod, Document.class, Document.class, List.class,
					String.class, List.class);
			boolean isRuleSatisfied = (boolean) method.invoke(new RuleEngine(), orderDetails, rule, freeGiftOrderItems,
					selectedWMS, giftItemSKUs);
			if (action.getString("name").equals(RuleActionEnum.REMOVE_ALL_ITEM.toString()) && isRuleSatisfied) {
				break;
			}
		}
		if (freeGiftOrderItems.size() > 0) {
			List<Document> orderItems = (List<Document>) orderDetails.get("orderItems");
			orderItems.addAll(freeGiftOrderItems);
			addFreeGiftItemsToOrder(freeGiftOrderItems, orderDetails.getObjectId("_id").toString());
		}
	}

	private static void addFreeGiftItemsToOrder(List<Document> freeGiftOrderItems, String id) {
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		Document searchQuery = new Document();
		searchQuery.put("_id", new ObjectId(id));
		Document updateObject = new Document();
		Document addToset = new Document();
		addToset.put("orderItems", new Document("$each", freeGiftOrderItems));
		updateObject.put("$addToSet", addToset);
		table.updateOne(searchQuery, updateObject);
	}
}
