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
public class PrepareDuplicateSKUsDBQuery implements Processor {

	static Logger log = Logger.getLogger(PrepareDuplicateSKUsDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String customSKU = exchange.getProperty("customSKU", String.class);
		String SKU = exchange.getProperty("SKU", String.class);
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		Document searchQuery = new Document("accountNumber", orderMessage.get("accountNumber").toString());
		searchQuery.put("SKU", new Document("$ne", SKU));
		searchQuery.put("customSKU", customSKU);
		searchQuery.put("variants", new Document("$exists", false));

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