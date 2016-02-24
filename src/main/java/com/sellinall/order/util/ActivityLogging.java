package com.sellinall.order.util;

import java.util.Random;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mudra.sellinall.util.DateUtil;
import com.sellinall.enums.SIAActivityLogStatus;

public class ActivityLogging implements Processor {
	static Logger log = Logger.getLogger(ActivityLogging.class.getName());
	static final String SERVER_NAME = "partnernotifserv";
	static final String REQUEST_TYPE_MESSAGE = "message";
	static final int EIGHT_DIGIT_RANDOM_NUMBER = 100000000;
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
	 * @desc publish message for start of user's activity
	 * @param exchange
	 *            Exchange
	 * @return void
	 */
	public static void start(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		if (accountNumber == null) {
			return;
		}
		JSONObject activityLog = new JSONObject();
		initMessage(exchange, accountNumber, activityLog);
		activityLog.put("status", SIAActivityLogStatus.STARTED.toString());
		String messageId = getMessageId();
		activityLog.put("messageId", messageId);
		exchange.setProperty("messageId", messageId);
		activityLog.put("timeStamp", DateUtil.getSIADateFormat().toString());
		String description = exchange.getProperty("message", JSONObject.class).toString();
		if (description.length() > 100) {
			description = description.substring(0, 100);
		}
		activityLog.put("description", description);
		log.debug(activityLog);
		sendMessage(activityLog);
	}

	private static void initMessage(Exchange exchange, String accountNumber, JSONObject activityLog)
			throws JSONException {
		activityLog.put("accountNumber", accountNumber);
		activityLog.put("serverName", SERVER_NAME);
		activityLog.put("operationName", exchange.getProperty("messageType"));
		activityLog.put("httpMethod", REQUEST_TYPE_MESSAGE);
	}

	/**
	 * @desc publish message for end of user's activity
	 * @param exchange
	 *            Exchange
	 * @return void
	 */
	public static void end(Exchange exchange) throws Exception {
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		if (accountNumber == null) {
			return;
		}
		JSONObject activityLog = new JSONObject();
		initMessage(exchange, accountNumber, activityLog);
		activityLog.put("messageId", exchange.getProperty("messageId"));
		activityLog.put("timeStamp", DateUtil.getSIADateFormat().toString());
		activityLog.put("description", "");
		activityLog.put("status", SIAActivityLogStatus.COMPLETED.toString());
		log.debug(activityLog);
		sendMessage(activityLog);
	}

	private static void sendMessage(JSONObject activityLog) {
		ProducerTemplate template = context.createProducerTemplate();
		template.asyncSendBody("direct:publishMessageToLogging", activityLog);
	}

	/**
	 * @desc method will be used in future
	 * @return void
	 */
	public static void add() {
		// This method will be used in future
	}

	/**
	 * @desc method will generate the random number
	 * @return String
	 */
	public static String getMessageId() {
		Random randomNumber = new Random();
		return "" + randomNumber.nextInt(EIGHT_DIGIT_RANDOM_NUMBER);
	}
}
