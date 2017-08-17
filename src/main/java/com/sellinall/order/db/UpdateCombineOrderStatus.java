/**
 * 
 */
package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
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
		BasicDBList combineOrderIdsList = new BasicDBList();
		for (int index = 0; index < combineOrderIds.length(); index++) {
			combineOrderIdsList.add(combineOrderIds.getString(index));
		}
		BasicDBObject query = new BasicDBObject("accountNumber", exchange.getProperty("accountNumber", String.class));
		query.put("orderID", new BasicDBObject("$in", combineOrderIdsList));
		query.put("site.nickNameID", orderMessage.getString("nickNameID"));
		BasicDBObject updateSet = new BasicDBObject();
		updateSet.put("orderStatus", SIAOrderStatus.COMBINED.toString());
		updateSet.put("paymentStatus", SIAPaymentStatus.UNSUPPORTED.toString());
		updateSet.put("shippingStatus", SIAShippingStatus.UNSUPPORTED.toString());
		updateSet.put("combinedOrderId", orderMessage.getString("orderID"));
		DBCollection table = DbUtilities.getInventoryDBCollection("order");
		BasicDBObject update = new BasicDBObject("$set", updateSet);
		table.update(query, update, false, true);
	}
}
