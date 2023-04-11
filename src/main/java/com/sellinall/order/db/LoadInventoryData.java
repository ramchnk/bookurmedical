package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;

public class LoadInventoryData implements Processor {
	static Logger log = Logger.getLogger(LoadInventoryData.class.getName());

	public void process(Exchange exchange) throws Exception {
		Document query = createQuery(exchange);
		MongoCollection<Document> table = DbUtilities.getInventoryDBCollection("inventory");
		List<Document> inventoryList = table.find(query).into(new ArrayList<Document>());
		ArrayList<Document> inventoryListFromDB = new ArrayList<Document>();
		for (Document inventory : inventoryList) {
			Document orderObj = Document.parse((inventory).toJson());
			inventoryListFromDB.add(orderObj);
		}
		exchange.setProperty("inventoryListFromDB", inventoryListFromDB);
		exchange.getOut().setBody(inventoryListFromDB);
	}

	private Document createQuery(Exchange exchange) throws JSONException {
		ArrayList<String> SKUListInOrder = (ArrayList<String>) exchange.getProperty("SKUListInOrder");
		String accountNumber = (String) exchange.getProperty("accountNumber");
		Document dbquery = new Document();
		dbquery.put("accountNumber", accountNumber);
		dbquery.put("SKU", new Document("$in", SKUListInOrder));
		return dbquery;
	}

}
