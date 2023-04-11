package com.sellinall.order.message;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
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
			// for finops createOrder & updateOrder
			if (publishTo.equals("finops")) {
				publishMessage.put("requestType", "order");
			}
			// for quickBooks createInvoice
			if (publishTo.equals("quickbooks")) {
				publishMessage.put("requestType", "createInvoice");
				publishMessage.remove("feeType");
			}
			if (publishTo.equals("SiAWMS")) {
				publishMessage.put("processTo", "wms");
			}
			if (publishTo.equals("odoo")) {
				publishMessage.put("processTo", "erp");
			}
			// for ninjaVan createOrder & updateOrder
			// for infor createOrder & updateOrder
			if (publishTo.equals("ninjaVan") || publishTo.equals("janio") || publishTo.equals("infor")
					|| publishTo.equals("satsaco") || publishTo.equals("netSuite") || publishTo.equals("odoo")
					|| publishTo.equals("singPost") || publishTo.equals("aramex") || publishTo.equals("vend")
					|| publishTo.equals("jtExpress") || publishTo.equals("aramexShipping")
					|| publishTo.equals("maatramBridgeIntegratedServer") || publishTo.equals("SiAWMS")) {
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
				if (exchange.getProperties().containsKey("isItemsReAllocated")) {
					publishMessage.put("isItemsReAllocated", exchange.getProperty("isItemsReAllocated", boolean.class));
				}
				if (publishTo.equals("maatramBridgeIntegratedServer")) {
					publishMessage.put("isMaatramBridgeIntegratedWMS",
							exchange.getProperty("publishToMaatramBridgeIntegratedWms", boolean.class));
					publishMessage.put("isMaatramBridgeIntegratedERP",
							exchange.getProperty("publishToMaatramBridgeIntegratedErp", boolean.class));
					publishMessage.put("isMaatramBridgeIntegratedShippingCarrier",
							exchange.getProperty("publishToMaatramBridgeIntegratedShippingCarrier", boolean.class));
					publishMessage.put("isMaatramBridgeIntegratedOms",
							exchange.getProperty("publishToMaatramBridgeIntegratedOms", boolean.class));
				}
				publishMessage.remove("feeType");
			}
		}
		log.debug("publishMessage " + publishMessage);
		exchange.getOut().setBody(publishMessage);

	}
}
