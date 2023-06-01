package com.sellinall.order.request;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.mudra.sellinall.config.Config;
import com.sellinall.util.AuthConstant;

public class BuildInventoryAPIHeader implements Processor {

	public void process(Exchange exchange) throws Exception {
		String inBody = exchange.getIn().getBody(String.class);
		String inventoryUrl = Config.getConfig().getSIAInventoryManagementServerURL() + "/inventory/"
				+ exchange.getProperty("apiName", String.class);
		exchange.setProperty("inventoryUrl", inventoryUrl);
		exchange.getOut().setHeader("accountNumber", exchange.getProperty("accountNumber", String.class));
		exchange.getOut().setHeader(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		exchange.getOut().setHeader("SIAServer", "true");
		exchange.getOut().setBody(inBody);

	}

}
