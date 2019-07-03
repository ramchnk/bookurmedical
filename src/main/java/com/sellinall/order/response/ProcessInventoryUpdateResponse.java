package com.sellinall.order.response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

public class ProcessInventoryUpdateResponse implements Processor {
	static Logger log = Logger.getLogger(ProcessInventoryUpdateResponse.class.getName());

	public void process(Exchange exchange) throws Exception {
		Integer httpStatusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		String responseString = exchange.getIn().getBody(String.class);
		if (httpStatusCode != HttpStatus.MULTI_STATUS_207) {
			log.error("UpdateInventory failed for orderID: " + exchange.getProperty("orderID", String.class)
					+ " ,Response: " + responseString);
			return;
		}
		JSONObject response = new JSONObject(responseString);
		JSONArray results = response.getJSONArray("result");
		boolean isAnyUpdateFailed = false;
		for (int index = 0; index < results.length(); index++) {
			JSONObject result = results.getJSONObject(index);
			if (result.getInt("httpCode") != HttpStatus.OK_200) {
				isAnyUpdateFailed = true;
				break;
			}
		}
		if (isAnyUpdateFailed) {
			log.error("UpdateInventory failed for some items for orderID: "
					+ exchange.getProperty("orderID", String.class) + " ,Response: " + responseString);
		}
	}

}
