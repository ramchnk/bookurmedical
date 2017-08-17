package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

/**
 * @author Mallikarjun
 *
 */
public class PrepareOrderIdDBQuery implements Processor {
	static Logger log = Logger.getLogger(PrepareOrderIdDBQuery.class.getName());
	
	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		BasicDBObject outBody = createBody(orderMessage);
		log.debug("outBody "+ outBody);
		exchange.getOut().setBody(outBody);
	}

	private BasicDBObject createBody(JSONObject orderMessage) throws JSONException {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", orderMessage.getString("accountNumber"));
		searchQuery.put("orderID", orderMessage.getString("orderID"));
		searchQuery.put("site.nickNameID", orderMessage.getString("nickNameID"));
		searchQuery.put("site.name", orderMessage.getString("site"));
		return searchQuery;
	}
}