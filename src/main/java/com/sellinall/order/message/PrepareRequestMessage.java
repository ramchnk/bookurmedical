package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;

/**
 * 
 * @author Raguvaran
 *
 */

public class PrepareRequestMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareRequestMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = new JSONObject(exchange.getIn().getBody(String.class));
		// To construct site object for order message in order to publish for
		// other channel servers (e.g)"site": {"name":
		// "magento","nickNameID": "magento-1"}
		constructSiteObjectForOrder(orderMessage);

		// prepare publish message to fee management server
		exchange.setProperty("publishToFeeManagement", true);
		exchange.setProperty("publishMessage", orderMessage);

		// prepare publish message for create in quickbooks server
		exchange.setProperty("publishToQuickBooks", false);
		if (exchange.getProperty("isNewOrder", boolean.class)) {
			Boolean isAccountingChannel = exchange.getProperty("isAccountingChannel", Boolean.class);
			if (isAccountingChannel) {
				JSONObject orderRecord = new JSONObject(
						exchange.getProperty("orderRecord", BasicDBObject.class).toString());
				exchange.setProperty("publishMessage", orderRecord);
				exchange.setProperty("publishToQuickBooks", true);
			}
		}
		// prepare publish message for create & update in ninjaVan server
		Boolean isNinjaVanShippingCarrier = exchange.getProperty("isNinjaVanShippingCarrier", Boolean.class);
		exchange.setProperty("publishToNinjaVan", false);
		if (isNinjaVanShippingCarrier) {
			exchange.setProperty("publishToNinjaVan", true);
		}
	}

	private void constructSiteObjectForOrder(JSONObject publishMessage) throws JSONException {
		JSONObject site = new JSONObject();
		String siteName = publishMessage.getString("site");
		site.put("name", siteName);
		site.put("nickNameID", publishMessage.getString("nickNameID"));
		publishMessage.put("site", site);
		publishMessage.remove("nickNameID");
	}

}