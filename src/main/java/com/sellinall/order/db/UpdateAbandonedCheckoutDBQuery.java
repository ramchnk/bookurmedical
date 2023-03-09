/**
 * 
 */
package com.sellinall.order.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.CurrencyUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.HttpsURLConnectionUtil;

/**
 * @author ManI
 * 
 */
public class UpdateAbandonedCheckoutDBQuery implements Processor {

	static Logger log = Logger.getLogger(UpdateAbandonedCheckoutDBQuery.class.getName());
	public static final int MC_MAX_EXPIRE_TIME = 15 * 60;

	public void process(Exchange exchange) throws Exception {
		JSONObject inBody = OrderUtil.parseToJsonObject((DBObject) JSON.parse(exchange.getIn().getBody(String.class)));
		Boolean hasCheckoutInDB = (Boolean) exchange.getProperty("hasCheckoutInDB");
		JSONObject checkoutMessageJSON = exchange.getProperty("message", JSONObject.class);
		BasicDBObject checkoutMessage = (BasicDBObject) JSON.parse(checkoutMessageJSON.toString());
		exchange.setProperty("isNewOrder", false);
		if (!hasCheckoutInDB) {
			insertOrderRecord(exchange, checkoutMessage, inBody);
			exchange.setProperty("isNewOrder", true);
			return;
		}
		updateOrderRecord(exchange, checkoutMessage);
	}

	private void insertOrderRecord(Exchange exchange, BasicDBObject checkoutMessage, JSONObject inBody)
			throws Exception {
		BasicDBObject site = new BasicDBObject();
		String nickNameID = checkoutMessage.getString("nickNameID");
		String accountNumber = checkoutMessage.getString("accountNumber");
		String checkoutID = checkoutMessage.getString("checkoutID");
		String siteName = checkoutMessage.getString("site");
		site.put("name", siteName);
		site.put("nickNameID", nickNameID);
		BasicDBObject checkoutRecord = new BasicDBObject();
		checkoutRecord.put("site", site);
		checkoutRecord.put("checkoutID", checkoutID);
		checkoutRecord.put("accountNumber", accountNumber);
		fillCheckoutRecord(exchange, checkoutRecord, checkoutMessage);
		fillOrderSoldAmountInUSD(checkoutRecord);
		checkoutRecord.put("timeCreated", DateUtil.getSIADateFormat());
		checkoutRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		if (checkoutMessage.containsField("timeAbandonedCartCreated")) {
			checkoutRecord.put("timeAbandonedCartCreated", checkoutMessage.getLong("timeAbandonedCartCreated"));
		} else {
			checkoutRecord.put("timeAbandonedCartCreated", System.currentTimeMillis() / 1000);
		}
		if (checkoutMessage.containsField("timeAbandonedCartUpdated")) {
			checkoutRecord.put("timeAbandonedCartUpdated", checkoutMessage.getLong("timeAbandonedCartUpdated"));
		}
		if (checkoutMessage.containsField("timeAbandonedCartCompleted")) {
			checkoutRecord.put("timeAbandonedCartCompleted", checkoutMessage.getLong("timeAbandonedCartCompleted"));
		}
		if (checkoutMessage.containsField("timeAbandonedCartClosed")) {
			checkoutRecord.put("timeAbandonedCartClosed", checkoutMessage.getLong("timeAbandonedCartClosed"));
		}
		if (checkoutRecord.containsField("orderItems")) {
			List<BasicDBObject> orderItems = (List<BasicDBObject>) checkoutRecord.get("orderItems");
			if (orderItems.size() == 0) {
				log.error("Insert - orderItems List is Empty for this orderId: "
						+ checkoutMessage.getString("checkoutID"));
			}
		}
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("abandonedCheckouts");
		UpdateOptions options = new UpdateOptions();
		options.upsert(true);
		try {
			Document checkoutDocument = getDocument(checkoutRecord);
			table.insertOne(checkoutDocument);
			checkoutRecord.put("_id", checkoutDocument.getObjectId("_id"));
		} catch (MongoWriteException e) {
			log.info("Order Insert - Duplicate message received for checkoutID: " + checkoutID);
			exchange.setProperty("stopProcess", true);
			return;
		}
		exchange.setProperty("checkoutRecord", checkoutRecord);
		exchange.getOut().setBody(checkoutMessage);
	}

	private static double getExchangeRateFromApi(String fromCurrency, String toCurrency)
			throws JSONException, IOException {
		if (fromCurrency.equals(toCurrency)) {
			return 1;
		}
		String MCKey = OrderUtil.getMCkeyforGcStatusWaitingSKUS(fromCurrency, toCurrency);
		Object mcValue = OrderUtil.getValueFromMemcache(MCKey, true);
		if (mcValue != null) {
			return (double) mcValue;
		} else {
			String url = Config.getConfig().getSIAFinopsServerURL() + "/exchange?fromCurrency=" + fromCurrency
					+ "&toCurrency=" + toCurrency;
			Map<String, String> header = new HashMap<String, String>();
			header.put("Content-Type", "application/json");
			header.put(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
			JSONObject response = HttpsURLConnectionUtil.doGet(url, header);
			log.debug("exchange rate:" + response);
			int httpCode = response.getInt("httpCode");
			if (httpCode == HttpStatus.OK_200) {
				JSONObject payload = new JSONObject(response.getString("payload"));
				double exchangeRate = payload.getDouble("exchangeRate");
				OrderUtil.updateMemcache(MCKey, MC_MAX_EXPIRE_TIME, exchangeRate);
				return exchangeRate;
			} else {
				log.error("Get " + url + " failed with status code " + httpCode + " and the response is: " + response);
				return 0;
			}
		}
	}

	private void updateOrderRecord(Exchange exchange, BasicDBObject checkoutMessage) throws Exception {
		BasicDBObject checkoutRecord = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("accountNumber", checkoutMessage.getString("accountNumber"));
		searchQuery.put("checkoutID", checkoutMessage.getString("checkoutID"));
		String siteName = checkoutMessage.getString("site");
		searchQuery.put("site.name", siteName);
		searchQuery.put("site.nickNameID", checkoutMessage.getString("nickNameID"));

		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("abandonedCheckouts");
		fillTransactionKeyValuePair(checkoutRecord, "buyerDetails", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "paymentMethods", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "orderSoldAmount", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "orderItems", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "sellerVoucherCodes", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "shippingDetails", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "billingDetails", checkoutMessage);
		fillOrderSoldAmountInUSD(checkoutRecord);
		UpdateResult result = table.updateOne(searchQuery, new BasicDBObject("$set", checkoutRecord));
		if (result.getModifiedCount() == 0) {
			log.info("Order :" + checkoutMessage.getString("chekoutID")
					+ " is already updated. this is duplicate message.");
			exchange.setProperty("stopProcess", true);
			return;
		}
	}

	private void fillOrderSoldAmountInUSD(BasicDBObject checkoutRecord) {
		if (checkoutRecord.containsField("orderSoldAmount")) {
			BasicDBObject orderSoldAmount = (BasicDBObject) checkoutRecord.get("orderSoldAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderSoldAmount.getString("currencyCode"), "USD");
				if (exchangeRate == 0) {
					log.error("orderSoldAmountInUSD field is not set for the checkoutID: "
							+ checkoutRecord.getString("checkoutID"));
					return;
				}
				long amount = Math.round(orderSoldAmount.getLong("amount") * exchangeRate);
				DBObject orderSoldAmountInUSD = CurrencyUtil.getAmountObject(amount, "USD");
				checkoutRecord.put("orderSoldAmountInUSD", orderSoldAmountInUSD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Document getDocument(BasicDBObject doc) {
		if (doc == null)
			return null;
		return new Document(doc.toMap());
	}

	private void fillCheckoutRecord(Exchange exchange, BasicDBObject checkoutRecord, BasicDBObject checkoutMessage) {
		fillTransactionKeyValuePair(checkoutRecord, "merchantID", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "buyerDetails", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "cartToken", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "paymentMethods", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "orderSoldAmount", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "orderItems", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "sellerVoucherCodes", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "abandonedCheckoutURL", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "buyerAcceptsMarketing", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "shippingDetails", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "billingDetails", checkoutMessage);
		fillTransactionKeyValuePair(checkoutRecord, "landingURL", checkoutMessage);
	}

	private void fillTransactionKeyValuePair(BasicDBObject checkoutRecord, String key, BasicDBObject checkoutMessage) {
		if (checkoutMessage.containsField(key)) {
			checkoutRecord.put(key, checkoutMessage.get(key));
		}
	}

}
