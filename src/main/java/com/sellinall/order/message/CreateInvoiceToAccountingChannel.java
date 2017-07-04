package com.sellinall.order.message;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;

public class CreateInvoiceToAccountingChannel implements Processor {

	public void process(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		ArrayList<String> customSKUs = exchange.getProperty("customSKUs", ArrayList.class);
		if (customSKUs == null || customSKUs.size() <= 0) {
			return;
		}
		BasicDBObject accounts = loadAccountDetails(accountNumber);
		String accountingChannel = Config.getConfig().getSIAAccountingChannels();
		String[] channels = accountingChannel.split("-");
		for (String site : channels) {
			if (accounts.containsKey(site)) {
				exchange.getOut().setHeader(site, true);
			}
		}
		JSONObject publishMessage = new JSONObject();
		publishMessage.put("requestType", "createInvoice");
		publishMessage.put("customSKUs", customSKUs);
		publishMessage.put("accountNumber", accountNumber);
		exchange.getOut().setBody(publishMessage);
	}

	private BasicDBObject loadAccountDetails(String accountNumber) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", new ObjectId(accountNumber));
		String accountingChannel = Config.getConfig().getSIAAccountingChannels();
		String[] channels = accountingChannel.split("-");
		BasicDBObject fields = new BasicDBObject();
		for (String channel : channels) {
			fields.put(channel, 1);
		}
		DBCollection table = DbUtilities.getDBCollection("accounts");
		return (BasicDBObject) table.findOne(searchQuery, fields);
	}
}
