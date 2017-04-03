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
		if (customSKU.contains("x")) {
			String[] splitCustomSKU = customSKU.split("x");
			String basicUnitCustomSKU = splitCustomSKU[0];
			exchange.setProperty("customSKU", basicUnitCustomSKU);
			// TODO: handle exception
			int lotSize = Integer.parseInt(splitCustomSKU[1]);
			JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
			int quantity = orderItemMessage.getInt("quantity") * lotSize;
			exchange.setProperty("quantity", quantity);
			exchange.setProperty("processBasicUnitSKU", true);
			DBObject searchQuery = new BasicDBObject("customSKU", basicUnitCustomSKU);
			JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
			if (orderMessage.has("userId")) {
				searchQuery.put("userId", orderMessage.getString("userId"));
			}
			searchQuery.put("variants", new BasicDBObject("$exists", false));
			searchQuery.put("variantDetails", new BasicDBObject("$exists", false));

			BasicDBObject fieldsFilter = new BasicDBObject("SKU", 1);
			fieldsFilter.put("sync", 1);
			fieldsFilter.put("noOfItem", 1);
			fieldsFilter.put("userId", 1);
			String[] sites = PostingSites.getConfig().getSitesList();
			for (int i = 0; i < sites.length; i++) {
				fieldsFilter.put(sites[i] + ".nickNameID", 1);
				fieldsFilter.put(sites[i] + ".noOfItem", 1);
				fieldsFilter.put(sites[i] + ".status", 1);
			}
			exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);

			exchange.getOut().setBody(searchQuery);
		} else {
			exchange.getOut().setBody(null);
		}
	}
}