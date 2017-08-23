/**
 * 
 */
package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.PostingSites;

/**
 * @author Senthil
 *
 */
public class PrepareBasicUnitSKUQuery implements Processor {

	static Logger log = Logger.getLogger(PrepareBasicUnitSKUQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String customSKU = exchange.getProperty("customSKU", String.class);
		String[] splitCustomSKU = customSKU.split("[xX]");
		String basicUnitCustomSKU = splitCustomSKU[0];
		for (int i = 1; i < splitCustomSKU.length - 1; i++) {
			basicUnitCustomSKU = basicUnitCustomSKU + splitCustomSKU[i];
		}
		exchange.setProperty("customSKU", basicUnitCustomSKU);
		int lotSize = Integer.parseInt(splitCustomSKU[splitCustomSKU.length - 1]);
		JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
		int quantity = orderItemMessage.getInt("quantity") * lotSize;
		exchange.setProperty("quantity", quantity);
		exchange.setProperty("processBasicUnitSKU", true);
		DBObject searchQuery = new BasicDBObject("customSKU", basicUnitCustomSKU);
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		searchQuery.put("variants", new BasicDBObject("$exists", false));
		searchQuery.put("variantDetails", new BasicDBObject("$exists", false));

		BasicDBObject fieldsFilter = new BasicDBObject("SKU", 1);
		fieldsFilter.put("sync", 1);
		fieldsFilter.put("noOfItem", 1);
		fieldsFilter.put("accountNumber", 1);
		String[] sites = PostingSites.getConfig().getSitesList();
		for (int i = 0; i < sites.length; i++) {
			fieldsFilter.put(sites[i] + ".nickNameID", 1);
			fieldsFilter.put(sites[i] + ".noOfItem", 1);
			fieldsFilter.put(sites[i] + ".status", 1);
		}
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);

		exchange.getOut().setBody(searchQuery);
	}
}