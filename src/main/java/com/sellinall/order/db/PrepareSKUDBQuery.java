/**
 * 
 */
package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Raju
 *
 */
public class PrepareSKUDBQuery implements Processor {

	static Logger log = Logger.getLogger(PrepareSKUDBQuery.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderItem = exchange.getIn().getBody(JSONObject.class);
		log.debug("Test property : " + exchange.getProperty("itemQuantity", String.class));
		DBObject outBody = new BasicDBObject("SKU", orderItem.getString("SKU"));
		exchange.getOut().setBody(outBody);
	}
}