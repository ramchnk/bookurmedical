/**
 * 
 */
package com.sellinall.order.db;

import java.io.IOException;
import java.math.BigDecimal;
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

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
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
		JSONObject inBody = OrderUtil.parseToJsonObject(Document.parse(exchange.getIn().getBody(String.class)));
		Boolean hasCheckoutInDB = (Boolean) exchange.getProperty("hasCheckoutInDB");
		JSONObject checkoutMessageJSON = exchange.getProperty("message", JSONObject.class);
		Document checkoutMessage = Document.parse(checkoutMessageJSON.toString());
		exchange.setProperty("isNewOrder", false);
		if (!hasCheckoutInDB) {
			insertOrderRecord(exchange, checkoutMessage, inBody);
			exchange.setProperty("isNewOrder", true);
			return;
		}
		updateOrderRecord(exchange, checkoutMessage);
	}

	private void insertOrderRecord(Exchange exchange, Document checkoutMessage, JSONObject inBody) throws Exception {
		Document site = new Document();
		String nickNameID = checkoutMessage.getString("nickNameID");
		String accountNumber = checkoutMessage.get("accountNumber").toString();
		String checkoutID = checkoutMessage.getString("checkoutID");
		String siteName = checkoutMessage.getString("site");
		site.put("name", siteName);
		site.put("nickNameID", nickNameID);
		Document checkoutRecord = new Document();
		checkoutRecord.put("site", site);
		checkoutRecord.put("checkoutID", checkoutID);
		checkoutRecord.put("accountNumber", accountNumber);
		fillCheckoutRecord(exchange, checkoutRecord, checkoutMessage);
		fillOrderSoldAmountInUSD(checkoutRecord);
		checkoutRecord.put("timeCreated", DateUtil.getSIADateFormat());
		checkoutRecord.put("timeLastUpdated", DateUtil.getSIADateFormat());
		if (checkoutMessage.containsKey("timeAbandonedCartCreated")) {
			checkoutRecord.put("timeAbandonedCartCreated", new BigDecimal(checkoutMessage.get("timeAbandonedCartCreated").toString()).longValue());
		} else {
			checkoutRecord.put("timeAbandonedCartCreated", System.currentTimeMillis() / 1000);
		}
		if (checkoutMessage.containsKey("timeAbandonedCartUpdated")) {
			checkoutRecord.put("timeAbandonedCartUpdated", new BigDecimal(checkoutMessage.get("timeAbandonedCartUpdated").toString()).longValue());
		}
		if (checkoutMessage.containsKey("timeAbandonedCartCompleted")) {
			checkoutRecord.put("timeAbandonedCartCompleted", new BigDecimal(checkoutMessage.get("timeAbandonedCartCompleted").toString()).longValue());
		}
		if (checkoutMessage.containsKey("timeAbandonedCartClosed")) {
			checkoutRecord.put("timeAbandonedCartClosed", new BigDecimal(checkoutMessage.get("timeAbandonedCartClosed").toString()).longValue());
		}
		if (checkoutRecord.containsKey("orderItems")) {
			List<Document> orderItems = (List<Document>) checkoutRecord.get("orderItems");
			if (orderItems.size() == 0) {
				log.error("Insert - orderItems List is Empty for this orderId: "
						+ checkoutMessage.getString("checkoutID"));
			}
		}
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("abandonedCheckouts");
		UpdateOptions options = new UpdateOptions();
		options.upsert(true);
		try {
			Document checkoutDocument = checkoutRecord;
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

	private void updateOrderRecord(Exchange exchange, Document checkoutMessage) throws Exception {
		Document checkoutRecord = new Document();
		Document searchQuery = new Document();
		searchQuery.put("accountNumber", checkoutMessage.get("accountNumber").toString());
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
		UpdateResult result = table.updateOne(searchQuery, new Document("$set", checkoutRecord));
		if (result.getModifiedCount() == 0) {
			log.info("Order :" + checkoutMessage.getString("chekoutID")
					+ " is already updated. this is duplicate message.");
			exchange.setProperty("stopProcess", true);
			return;
		}
	}

	private void fillOrderSoldAmountInUSD(Document checkoutRecord) {
		if (checkoutRecord.containsKey("orderSoldAmount")) {
			Document orderSoldAmount = (Document) checkoutRecord.get("orderSoldAmount");
			try {
				double exchangeRate = getExchangeRateFromApi(orderSoldAmount.getString("currencyCode"), "USD");
				if (exchangeRate == 0) {
					log.error("orderSoldAmountInUSD field is not set for the checkoutID: "
							+ checkoutRecord.getString("checkoutID"));
					return;
				}
				long amount = (long) (Math.round(new BigDecimal(orderSoldAmount.get("amount").toString()).longValue()) * exchangeRate);
				Document orderSoldAmountInUSD = CurrencyUtil.getAmountObject(amount, "USD");
				checkoutRecord.put("orderSoldAmountInUSD", orderSoldAmountInUSD);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void fillCheckoutRecord(Exchange exchange, Document checkoutRecord, Document checkoutMessage) {
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

	private void fillTransactionKeyValuePair(Document checkoutRecord, String key, Document checkoutMessage) {
		if (checkoutMessage.containsKey(key)) {
			checkoutRecord.put(key, checkoutMessage.get(key));
		}
	}

}
