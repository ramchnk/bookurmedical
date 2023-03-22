/**
 * 
 */
package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

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

		JSONObject orderItemMessage = new JSONObject(exchange.getProperty("orderItemMessage", String.class));
		int quantity = orderItemMessage.getInt("quantity");
		if (customSKU.matches(".+(x|X)[1-9]+[0-9]*$")) {
			int lotSize = Integer.parseInt(splitCustomSKU[splitCustomSKU.length - 1]);
			quantity = quantity * lotSize;
		}
		exchange.setProperty("quantity", quantity);
		exchange.setProperty("processBasicUnitSKU", true);
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		Document searchQuery = new Document("accountNumber", orderMessage.getString("accountNumber"));
		searchQuery.put("customSKU", basicUnitCustomSKU);
		searchQuery.put("variants", new Document("$exists", false));
		searchQuery.put("variantDetails", new Document("$exists", false));

		Document fieldsFilter = new Document("SKU", 1);
		fieldsFilter.put("sync", 1);
		fieldsFilter.put("noOfItem", 1);
		fieldsFilter.put("accountNumber", 1);
		String[] sites = PostingSites.getConfig().getSitesList();
		for (int i = 0; i < sites.length; i++) {
			fieldsFilter.put(sites[i] + ".nickNameID", 1);
			fieldsFilter.put(sites[i] + ".noOfItem", 1);
			fieldsFilter.put(sites[i] + ".status", 1);
		}
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_PROJECTION, fieldsFilter);

		exchange.getOut().setBody(searchQuery);
	}
}