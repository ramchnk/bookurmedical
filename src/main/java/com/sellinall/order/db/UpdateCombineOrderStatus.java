/**
 * 
 */
package com.sellinall.order.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.enums.SIAOrderStatus;
import com.sellinall.util.enums.SIAPaymentStatus;
import com.sellinall.util.enums.SIAShippingStatus;

/**
 * @author Mallikarjun
 * 
 */
public class UpdateCombineOrderStatus implements Processor {
	static Logger log = Logger.getLogger(UpdateCombineOrderStatus.class.getName());

	public void process(Exchange exchange) throws Exception {
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		updateOrderCollection(exchange, orderMessage);
	}

	private void updateOrderCollection(Exchange exchange, JSONObject orderMessage) throws JSONException {
		JSONArray combineOrderIds = orderMessage.getJSONArray("combinedOrderIds");
		List<String> combineOrderIdsList = new ArrayList<String>();
		for (int index = 0; index < combineOrderIds.length(); index++) {
			combineOrderIdsList.add(combineOrderIds.getString(index));
		}
		Document query = new Document("accountNumber", exchange.getProperty("accountNumber", String.class));
		query.put("orderID", new Document("$in", combineOrderIdsList));
		query.put("site.nickNameID", orderMessage.getString("nickNameID"));
		Document updateSet = new Document();
		updateSet.put("orderStatus", SIAOrderStatus.COMBINED.toString());
		updateSet.put("paymentStatus", SIAPaymentStatus.UNSUPPORTED.toString());
		updateSet.put("shippingStatus", SIAShippingStatus.UNSUPPORTED.toString());
		updateSet.put("combinedOrderId", orderMessage.get("orderID").toString());
		MongoCollection<Document> table = DbUtilities.getOrderDBCollection("order");
		Document update = new Document("$set", updateSet);
		table.updateMany(query, update);
	}
}
