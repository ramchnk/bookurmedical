package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Raguvaran
 *
 */

public class ConstructRequestType implements Processor {
	static Logger log = Logger.getLogger(ConstructRequestType.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject publishMessage = exchange.getProperty("publishMessage", JSONObject.class);
		exchange.setProperty("isEligibleToProceed", true);
		if (exchange.getProperties().containsKey("publishTo")) {
			String publishTo = exchange.getProperty("publishTo", String.class);
			// for feemanagement createOrder & updateOrder
			if (publishTo.equals("feeManagement")) {
				publishMessage.put("feeType", "order");
				if (exchange.getProperty("isReconciliation", boolean.class)) {
					parseOrderItems(publishMessage, exchange.getProperty("orderItemList", JSONArray.class));
					publishMessage.put("isReconciliation", true);
				}
			}
			// for finops createOrder & updateOrder
			if (publishTo.equals("finops")) {
				publishMessage.put("requestType", "order");
			}
			// for quickBooks createInvoice
			if (publishTo.equals("quickbooks")) {
				publishMessage.put("requestType", "createInvoice");
				publishMessage.remove("feeType");
			}
			// for ninjaVan createOrder & updateOrder
			// for infor createOrder & updateOrder
			if (publishTo.equals("ninjaVan") || publishTo.equals("infor") || publishTo.equals("satsaco")
					|| publishTo.equals("netSuite") || publishTo.equals("odoo") || publishTo.equals("singPost")
					|| publishTo.equals("aramex") || publishTo.equals("vend")) {
				String requestType = "";
				if (exchange.getProperty("isNewOrder", boolean.class)) {
					requestType = "createOrder";
					publishMessage.put("requestType", requestType);
					exchange.setProperty("requestType", requestType);
				}
				if (!exchange.getProperty("isNewOrder", boolean.class)) {
					requestType = "updateOrder";
					publishMessage.put("requestType", requestType);
					exchange.setProperty("requestType", requestType);
				}
				publishMessage.remove("feeType");
			}
		}
		log.debug("publishMessage " + publishMessage);
		exchange.getOut().setBody(publishMessage);

	}

	private void parseOrderItems(JSONObject publishMessage, JSONArray jsonArray) throws JSONException {
		JSONArray orderItems = publishMessage.getJSONArray("orderItems");
		for (int index = 0; index < orderItems.length(); index++) {
			JSONObject orderItem = orderItems.getJSONObject(index);
			JSONObject object = jsonArray.getJSONObject(index);
			if (object.has("settlementDetails")) {
				JSONObject settlementDetails = object.getJSONObject("settlementDetails");
				if (settlementDetails.has("refunded")) {
					JSONObject refunded = settlementDetails.getJSONObject("refunded");
					if (orderItem.has("settlementDetails")) {
						JSONObject settlementDetailsObject = orderItem.getJSONObject("settlementDetails");
						if (settlementDetailsObject.has("refunded")) {
							JSONObject refundedObject = settlementDetails.getJSONObject("refunded");
							fillUpFeesDetails(refunded, refundedObject);
							settlementDetailsObject.put("refunded", refundedObject);
							orderItem.put("settlementDetails", settlementDetailsObject);
						}
					}
				}
			}
			fillUpFeesDetails(object,orderItem);
			orderItems.put(index, orderItem);
		}
	}

	private void fillUpFeesDetails(JSONObject object, JSONObject orderItem) throws JSONException {
		if (object.has("expectedMarketPlaceCommission")) {
			orderItem.put("expectedMarketPlaceCommission", object.get("expectedMarketPlaceCommission"));
		}
		if (object.has("feesFieldsToUpdate")) {
			orderItem.put("feesFieldsToUpdate", object.get("feesFieldsToUpdate"));
		}
	}
}
