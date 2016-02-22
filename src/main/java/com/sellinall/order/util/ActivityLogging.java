package com.sellinall.order.util;

import java.util.Random;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.util.DateUtil;
import com.sellinall.enums.SIAActivityLogStatus;
import com.sellinall.order.bl.InitMessageListenerRoute;

public class ActivityLogging implements Processor {
	static Logger log = Logger.getLogger(InitMessageListenerRoute.class.getName());
	private static CamelContext context;

	public static void setCamelContext(CamelContext context1) {
		context = context1;
	}

	public void process(Exchange exchange) throws Exception {
		exchange.getOut().setBody(exchange.getIn().getBody());
		String activityLogMethod = exchange.getIn().getHeader("activityLogMethod", String.class);
		if (activityLogMethod != null && activityLogMethod.equals("start")) {
			start(exchange);
			return;
		}
		if (activityLogMethod != null && activityLogMethod.equals("end")) {
			end(exchange);
			return;
		}
	}

	/**
	 * @desc  publish message for start of user's activity
	 * @param exchange : Exchange
	 * @return void
	 */
	public static void start(Exchange exchange) throws Exception {
		JSONObject inBody = exchange.getProperty("message", JSONObject.class);
		// In initMessageListenerRoute userId doesn't set to exchange so here its
		//get from inBody.
		if (!inBody.has("userId")) {
			return;
		}
		JSONObject activityLog = new JSONObject();
		ProducerTemplate template = context.createProducerTemplate();
		activityLog.put("accountNumber", inBody.getString("userId"));
		activityLog.put("serverName", "partnernotifserv");
		activityLog.put("operationName", exchange.getProperty("messageType"));
		activityLog.put("httpMethod", "Message");
		activityLog.put("status", SIAActivityLogStatus.STARTED.toString());
		String messageId = getMessageId();
		activityLog.put("messageId", messageId);
		exchange.setProperty("messageId", messageId);
		String description = (exchange.getProperty("message")).toString();
		if (description.length() > 100) {
			description = description.substring(0, 100);
		}
		activityLog.put("description", description);
		activityLog.put("timeStamp", DateUtil.getSIADateFormat().toString());
		log.debug(activityLog);
		template.asyncSendBody("direct:publishMessageToLogging", activityLog);

	}

	/**
	 * @desc  publish message for end of user's activity
	 * @param exchange : Exchange
	 * @return void
	 */
	public static void end(Exchange exchange) throws Exception {
		JSONObject message = exchange.getProperty("message", JSONObject.class);
		if (!message.has("userId")) {
			return;
		}
		ProducerTemplate template = context.createProducerTemplate();
		JSONObject activityLog = new JSONObject();
		activityLog.put("accountNumber", message.getString("userId"));
		activityLog.put("serverName", "partnernotifserv");
		activityLog.put("operationName", exchange.getProperty("messageType"));
		activityLog.put("httpMethod", "Message");
		activityLog.put("status", SIAActivityLogStatus.COMPLETED.toString());
		activityLog.put("timeStamp", DateUtil.getSIADateFormat().toString());
		activityLog.put("messageId", exchange.getProperty("messageId"));
		activityLog.put("description", "");
		log.debug(activityLog);
		template.asyncSendBody("direct:publishMessageToLogging", activityLog);

	}

	/**@desc method will be used in future
	 * @return void
	 */
	public static void add() {
		// This method will be used in future
	}

	/**@desc method will generate the random number
	 * @return String
	 */
	public static String getMessageId() {
		Random randomNumber = new Random();
		return "" + randomNumber.nextInt(100000000);// having 100000000 will give 8-digit Random number.
	}

}
