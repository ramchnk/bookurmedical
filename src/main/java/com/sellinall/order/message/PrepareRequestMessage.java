package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sellinall.order.util.OrderUtil;

/**
 * 
 * @author Raguvaran
 *
 */

public class PrepareRequestMessage implements Processor {
	static Logger log = Logger.getLogger(PrepareRequestMessage.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderRecord = OrderUtil
				.parseToJsonObject((DBObject) exchange.getProperty("orderRecord", BasicDBObject.class));
		orderRecord.put("merchantID", exchange.getProperty("merchantID", String.class));
		if(exchange.getProperties().containsKey("countryCode")){
			orderRecord.put("countryCode", exchange.getProperty("countryCode"));
		}
		JSONObject getIdObject = orderRecord.getJSONObject("_id");
		String orderObjectId = getIdObject.getString("$oid");
		orderRecord.put("objectId", orderObjectId);
		orderRecord.remove("_id");
		// prepare publish message to fee management server
		exchange.setProperty("publishMessage", orderRecord);		
		// prepare publish message for create in quickbooks server
		exchange.setProperty("publishToQuickBooks", false);
		if (exchange.getProperty("isNewOrder", boolean.class)) {
			Boolean isAccountingChannel = exchange.getProperty("isAccountingChannel", Boolean.class);
			if (isAccountingChannel) {
				exchange.setProperty("publishToQuickBooks", true);
			}
		}
		// prepare publish message for create & update in ninjaVan server
		Boolean isNinjaVanShippingCarrier = exchange.getProperty("isNinjaVanShippingCarrier", Boolean.class);
		exchange.setProperty("publishToNinjaVan", false);
		Boolean isSingPostShippingCarrier = exchange.getProperty("isSingPostShippingCarrier", Boolean.class);
		exchange.setProperty("publishToSingPost", false);
		Boolean isJTExpressShippingCarrier = exchange.getProperty("isJTExpressShippingCarrier", Boolean.class);
		exchange.setProperty("publishToJTExpress", false);
		// To skip the unnecessary order Update, receive from Shipping Carrier
		// channels
		boolean isOrderUpdatedByShippingCarrier = exchange.getProperty("isOrderUpdatedByShippingCarrier",
				Boolean.class);
		if (isNinjaVanShippingCarrier && !isOrderUpdatedByShippingCarrier) {
			exchange.setProperty("publishToNinjaVan", true);
		} else if (isSingPostShippingCarrier && !isOrderUpdatedByShippingCarrier) {
			exchange.setProperty("publishToSingPost", true);
		}  else if (isJTExpressShippingCarrier && !isOrderUpdatedByShippingCarrier) {
			exchange.setProperty("publishToJTExpress", true);
		}

		// prepare publish message for create & update in infor server
		boolean isInforWMS = exchange.getProperty("isInforWMS", Boolean.class);
		exchange.setProperty("publishToInfor", false);
		if (isInforWMS) {
			exchange.setProperty("publishToInfor", true);
		}
		// prepare publish message for create & update in satsaco server
		boolean isSatsacoWMS = exchange.getProperty("isSatsacoWMS", Boolean.class);
		exchange.setProperty("publishToSatsaco", false);
		if (isSatsacoWMS) {
			exchange.setProperty("publishToSatsaco", true);
		}
		// prepare publish message for create & update in netSuite server
		boolean isNetSuite = exchange.getProperty("isNetSuite", Boolean.class);
		exchange.setProperty("publishToNetSuite", false);
		if (isNetSuite) {
			exchange.setProperty("publishToNetSuite", true);
		}
		// prepare publish message for create & update in odoo server
		boolean isOdoo = exchange.getProperty("isOdoo", Boolean.class);
		exchange.setProperty("publishToOdoo", false);
		if (isOdoo) {
			exchange.setProperty("publishToOdoo", true);
		}

		// prepare publish message for create & update in aramex server
		boolean isAramex = exchange.getProperty("isAramexWMS", Boolean.class);
		exchange.setProperty("publishToAramex", false);
		if (isAramex) {
			exchange.setProperty("publishToAramex", true);
		}
		
		// prepare publish message for create & update in vend server
		boolean isVend = exchange.getProperty("isVend", Boolean.class);
		exchange.setProperty("publishToVend", false);
		if (isVend) {
			exchange.setProperty("publishToVend", true);
		}
	}
}