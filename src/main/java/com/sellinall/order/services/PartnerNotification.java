package com.sellinall.order.services;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.codehaus.jettison.json.JSONObject;

@Path("/notification")
@Produces(MediaType.APPLICATION_JSON)
public class PartnerNotification {
	// This is being set by main function
	private static CamelContext context;

	public static void setCamelContext(CamelContext context1) {
		context = context1;
	}

	@POST
	public JSONObject post(@HeaderParam("accountNumber") String accountNumber, JSONObject payload) throws Exception {
		JSONObject json = new JSONObject();
		json.put("accountNumber", accountNumber);
		json.put("payload", payload);
		ProducerTemplate template = context.createProducerTemplate();
		template.sendBody("direct:publishMessage", json);
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("response", "success");
		return jsonResponse;
	}
}
