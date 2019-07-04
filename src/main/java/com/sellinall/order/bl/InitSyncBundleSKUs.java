package com.sellinall.order.bl;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.codehaus.jettison.json.JSONObject;

import com.sellinall.order.enums.NotificationOrderActionStatus;
import com.sellinall.util.enums.SIAOrderCancelReasons;

public class InitSyncBundleSKUs implements Processor {

	public void process(Exchange exchange) throws Exception {
		String bundledCustomSKU = exchange.getProperty("customSKU", String.class);
		String bundleDelimiter = exchange.getProperty("bundleDelimiter", String.class);
		String customSKUList[] = bundledCustomSKU.split("[" + bundleDelimiter + "]");
		JSONObject orderMessage = exchange.getProperty("message", JSONObject.class);
		int quantity = exchange.getProperty("quantity", Integer.class);
		exchange.setProperty("isEligibleToProcess", false);
		NotificationOrderActionStatus notificationOrderActionStatus = (NotificationOrderActionStatus) exchange
				.getProperty("notificationOrderActionStatus");
		boolean isNewOrder = notificationOrderActionStatus.equals(NotificationOrderActionStatus.INITIATED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERY_FAILED);
		boolean isCancelledOrder = notificationOrderActionStatus
				.equals(NotificationOrderActionStatus.INITIATED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DISPATCHED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_RETURNED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.DELIVERED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_PENDING_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.CANCEL_REQUESTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.ACCEPTED_TO_CANCELLED)
				|| notificationOrderActionStatus.equals(NotificationOrderActionStatus.PROCESSING_TO_RETURNED);

		if (!isNewOrder && !isCancelledOrder) {
			return;
		}
		boolean isOutOfStock = false;
		if (isCancelledOrder && orderMessage.has("cancelDetails")) {
			JSONObject cancelDetails = orderMessage.getJSONObject("cancelDetails");
			if (cancelDetails.has("cancelReason") && !cancelDetails.getString("cancelReason").isEmpty()
					&& cancelDetails.getString("cancelReason").equals(SIAOrderCancelReasons.OUT_OF_STOCK.toString())) {
				isOutOfStock = true;
			}
		}
		exchange.setProperty("isOutOfStock", isOutOfStock);
		exchange.setProperty("baseSKUWithSoldQuantityDetailsObject", constructSKUWithSoldQuantityDetailsObject(
				customSKUList, quantity, isOutOfStock, isCancelledOrder, isNewOrder));
		exchange.setProperty("isEligibleToProcess", true);
	}

	private Map<String, Integer> constructSKUWithSoldQuantityDetailsObject(String[] customSKUList, int quantity,
			boolean isOutOfStock, boolean isCancelledOrder, boolean isNewOrder) {
		Map<String, Integer> baseSKUWithSoldQuantityDetailsObject = new HashMap<String, Integer>();
		for (String customSKU : customSKUList) {
			String baseCustomSKU = customSKU;
			int quantityToUpdate = quantity;
			if (customSKU.matches(".+(x|X)[1-9]+[0-9]*$")) {
				String splitCustomSKU[] = customSKU.split("[x|X]");
				quantityToUpdate = quantity * Integer.parseInt(splitCustomSKU[splitCustomSKU.length - 1]);
				baseCustomSKU = customSKU.substring(0, customSKU.toUpperCase().lastIndexOf("X"));
			}
			if (isCancelledOrder) {
				if (isOutOfStock) {
					quantityToUpdate = 0;
				}
				baseSKUWithSoldQuantityDetailsObject.put(baseCustomSKU, quantityToUpdate);
			} else if (isNewOrder) {
				baseSKUWithSoldQuantityDetailsObject.put(baseCustomSKU, -quantityToUpdate);
			}
		}
		return baseSKUWithSoldQuantityDetailsObject;
	}
}
