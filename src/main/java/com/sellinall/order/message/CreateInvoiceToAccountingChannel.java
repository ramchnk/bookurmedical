package com.sellinall.order.message;

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
		BasicDBObject orderRecord = exchange.getProperty("orderRecord", BasicDBObject.class);
		BasicDBObject accounts = loadAccountDetails(orderRecord.getString("accountNumber"));
		String accountingChannel = Config.getConfig().getSIAAccountingChannels();
		String[] channels = accountingChannel.split("-");
		for (String site : channels) {
			if (accounts.containsKey(site)) {
				exchange.getOut().setHeader(site, true);
			}
		}
		JSONObject publishMessage = new JSONObject(orderRecord.toString());
		publishMessage.put("requestType", "createInvoice");
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
