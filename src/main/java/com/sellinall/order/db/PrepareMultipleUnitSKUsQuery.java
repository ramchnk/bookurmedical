/**
 * 
 */
package com.sellinall.order.db;

import java.util.regex.Pattern;

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
public class PrepareMultipleUnitSKUsQuery implements Processor {

	static Logger log = Logger.getLogger(PrepareMultipleUnitSKUsQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String customSKU = exchange.getProperty("customSKU", String.class);
		Document searchQuery = new Document();
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		searchQuery.put("accountNumber", orderMessage.get("accountNumber").toString());
		searchQuery.put("customSKU", Pattern.compile(customSKU + "(x|X)[1-9]+[0-9]*$"));
		searchQuery.put("variants", new Document("$exists", false));
		Document fieldsFilter = new Document("SKU", 1);
		fieldsFilter.put("sync", 1);
		fieldsFilter.put("customSKU", 1);
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
		log.debug("searchQuery " + searchQuery + " fieldsFilter " + fieldsFilter);
	}
}