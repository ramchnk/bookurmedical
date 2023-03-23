package com.sellinall.order.db;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.sellinall.database.DbUtilities;
import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.DateUtil;
import com.sellinall.util.enums.SIAOrderCancelReasons;

public class UpdateProductMaster implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		JSONObject inventoryDBRecordJSON = OrderUtil
				.parseToJsonObject((DBObject) JSON.parse(exchange.getProperty("inventory", String.class)));
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange
				.getProperty("notificationOrderActionStatus");
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		int quantitySold = exchange.getProperty("quantity", Integer.class);

		boolean isOutOfStock = false;
		boolean isNewOrder = OrderUtil.checkIsNewOrder(notificationOrderActionStatus);
		boolean isCancelledOrder = OrderUtil.checkIsCancelledOrder(notificationOrderActionStatus);
		if (isCancelledOrder && orderMessage.has("cancelDetails")) {
			JSONObject cancelDetails = orderMessage.getJSONObject("cancelDetails");
			//SELLER_UNABLE_TO_RESERVE_STOCK usually came from lazada for which no need to decrement the stock.
			if (cancelDetails.has("cancelReason") && !cancelDetails.getString("cancelReason").isEmpty()
					&& (cancelDetails.getString("cancelReason").equals(SIAOrderCancelReasons.OUT_OF_STOCK.toString())
							|| cancelDetails.getString("cancelReason")
							.equals(SIAOrderCancelReasons.SELLER_UNABLE_TO_RESERVE_STOCK.toString()))) {
				isOutOfStock = true;
			}
		}

		if (inventoryDBRecordJSON.has("customSKU")) {
			String wmsName = ((ArrayList<String>) exchange.getProperty("wmsList")).get(0);
			String accountNumber = exchange.getProperty("accountNumber", String.class);
			String sellerSKU = inventoryDBRecordJSON.getString("customSKU");
			updateProductMaster(accountNumber, sellerSKU, wmsName, quantitySold, isNewOrder, isCancelledOrder,
					isOutOfStock);
		}

	}

	private void updateProductMaster(String accountNumber, String sellerSKU, String wmsName, int quantitySold,
			boolean isNewOrder, boolean isCancelledOrder, boolean isOutOfStock) {
		BasicDBObject searchQueryProductMaster = new BasicDBObject();
		searchQueryProductMaster.put("accountNumber", accountNumber);
		searchQueryProductMaster.put("sellerSKU", sellerSKU);
		searchQueryProductMaster.put("quantities.warehouseID", wmsName);
		BasicDBObject updateProductMaster = new BasicDBObject();
		BasicDBObject incObject = new BasicDBObject();
		BasicDBObject setObject = new BasicDBObject();
		if (isNewOrder) {
			incObject.put("quantities.$.quantity", -quantitySold);
		} else if (isCancelledOrder) {
			if (isOutOfStock) {
				setObject.put("quantities.$.quantity", 0);
			} else {
				incObject.put("quantities.$.quantity", quantitySold);
			}
		}
		setObject.put("timeLastUpdated", DateUtil.getSIADateFormat());
		if (!incObject.isEmpty() || (isOutOfStock && !setObject.isEmpty())) {
			if (!incObject.isEmpty()) {
				updateProductMaster.put("$inc", incObject);
			}
			updateProductMaster.put("$set", setObject);
			MongoCollection<Document> productMasterTable = DbUtilities.getInventoryDBCollection("productMaster");
			productMasterTable.findOneAndUpdate(searchQueryProductMaster, updateProductMaster);
		}

	}

}