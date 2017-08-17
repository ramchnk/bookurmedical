/**
 * 
 */
package com.sellinall.order.splitter;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class AggregatorStrategy implements AggregationStrategy {
	
	static Logger log = Logger.getLogger(AggregatorStrategy.class.getName());

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		if (oldExchange == null) {
			try {
				JSONObject outBody = new JSONObject();
				outBody.put("accountNumber", newExchange.getProperty("accountNumber", String.class));
				newExchange.getIn().setBody(outBody);
				return newExchange;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		// Since all orderItems belongs to one Merchant, just ignoring and return oldExchange value 
		return oldExchange;
	}
}