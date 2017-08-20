/**
 * 
 */
package com.sellinall.order.db;

import java.util.regex.Pattern;

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
public class PrepareMultipleUnitSKUsQuery implements Processor {

	static Logger log = Logger.getLogger(PrepareMultipleUnitSKUsQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		String customSKU = exchange.getProperty("customSKU", String.class);
		DBObject searchQuery = new BasicDBObject("customSKU", Pattern.compile(customSKU + "(x|X)[1-9]+[0-9]*$"));
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		if (orderMessage.has("userId")) {
			searchQuery.put("accountNumber", orderMessage.getString("userId"));
		}
		if (orderMessage.has("accountNumber")) {
			searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		}
		searchQuery.put("variants", new BasicDBObject("$exists", false));
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String siteName = exchange.getProperty("siteName", String.class);
		BasicDBObject elemMatch = new BasicDBObject("nickNameID", nickNameID);
		BasicDBObject searchSite = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put(siteName, searchSite);

		BasicDBObject fieldsFilter = new BasicDBObject("SKU", 1);
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
		exchange.getOut().setHeader(MongoDbConstants.FIELDS_FILTER, fieldsFilter);

		exchange.getOut().setBody(searchQuery);
		log.debug("searchQuery " + searchQuery + " fieldsFilter " + fieldsFilter);
	}
}