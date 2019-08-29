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
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;

/**
 * @author vikraman
 * 
 */
public class LoadUserDataByNicknameId implements Processor {
	static Logger log = Logger.getLogger(LoadUserDataByNicknameId.class.getName());

	@SuppressWarnings("unchecked")
	public void process(Exchange exchange) throws Exception {
		String nickNameID = exchange.getProperty("nickNameID", String.class);
		String siteName = exchange.getProperty("siteName", String.class);
		String accountNumber = exchange.getProperty("accountNumber", String.class);
		String accountingChannel = Config.getConfig().getSIAAccountingChannels();
		BasicDBObject queryResult = runQuery(accountNumber, nickNameID, siteName, accountingChannel);
		exchange.setProperty("syncInventory", (Boolean) queryResult.get("syncInventory"));
		List<BasicDBObject> userSiteSpecificObjectList = (List<BasicDBObject>) queryResult.get(siteName);
		// always userSiteSpecificObject contains only one siteName(eBay-1 only)
		BasicDBObject userSiteSpecificObject = userSiteSpecificObjectList.get(0);
		String profileID = "";
		if (userSiteSpecificObject.containsField("invoiceProfile")
				&& userSiteSpecificObject.get("invoiceProfile") != null
				&& !userSiteSpecificObject.get("invoiceProfile").equals("null")) {
			profileID = userSiteSpecificObject.getString("invoiceProfile");
		} else if (userSiteSpecificObject.containsField("profile") && userSiteSpecificObject.get("profile") != null
				&& !userSiteSpecificObject.get("profile").equals("null")) {
			profileID = userSiteSpecificObject.getString("profile");
		}
		if (!profileID.isEmpty()) {
			List<BasicDBObject> userProfileList = (List<BasicDBObject>) queryResult.get("profile");
			String invoiceNumberPrefix = getinvoiceNumberPrefix(userProfileList, profileID);
			exchange.setProperty("invoiceNumberPrefix", invoiceNumberPrefix);
			exchange.setProperty("profileID", profileID);
		}

		exchange.setProperty("isAccountingChannel", false);
		String[] channels = accountingChannel.split("-");
		for (String channel : channels) {
			if (queryResult.containsKey(channel)) {
				exchange.setProperty("isAccountingChannel", true);
			}
		}
		exchange.setProperty("isNinjaVanShippingCarrier", false);
		if (userSiteSpecificObject.containsKey("shippingCarrier")
				&& userSiteSpecificObject.get("shippingCarrier") != null) {
			BasicDBList shippingCarrier = (BasicDBList) userSiteSpecificObject.get("shippingCarrier");
			if (shippingCarrier.contains("ninjaVan")) {
				exchange.setProperty("isNinjaVanShippingCarrier", true);
			}
		}
		exchange.setProperty("isInforWMS", false);
		exchange.setProperty("isSatsacoWMS", false);
		if (userSiteSpecificObject.containsKey("wms") && userSiteSpecificObject.get("wms") != null) {
			BasicDBList wmsList = (BasicDBList) userSiteSpecificObject.get("wms");
			for (int i = 0; i < wmsList.size(); i++) {
				String wms = wmsList.get(i).toString();
				if (wms.startsWith("satsaco")) {
					exchange.setProperty("isSatsacoWMS", true);
					break;
				}
				if (wms.startsWith("infor")) {
					exchange.setProperty("isInforWMS", true);
					break;
				}
			}
		}
		if (exchange.getProperty("isInforWMS", boolean.class)
				|| exchange.getProperty("isNinjaVanShippingCarrier", boolean.class)) {
			exchange.setProperty("isPartnerLogistics", true);
		}
		exchange.setProperty("merchantID", queryResult.get("merchantID"));
		if(userSiteSpecificObject.containsField("countryCode")){
			exchange.setProperty("countryCode", userSiteSpecificObject.getString("countryCode"));
		}
		exchange.setProperty("userSiteSpecificObject", userSiteSpecificObject);
		Boolean ignoreSoldEvent = false;
		if (userSiteSpecificObject.containsField("ignoreSoldEvent")) {
			ignoreSoldEvent = userSiteSpecificObject.getBoolean("ignoreSoldEvent");
		}
		exchange.setProperty("ignoreSoldEvent", ignoreSoldEvent);
		Boolean isManaged = false;
		if (userSiteSpecificObject.containsField("isManaged")) {
			isManaged = userSiteSpecificObject.getBoolean("isManaged");
		}
		exchange.setProperty("isManaged", isManaged);

		Boolean processRule = false;
		if (userSiteSpecificObject.containsField("processRule")) {
			processRule = userSiteSpecificObject.getBoolean("processRule");
		}
		exchange.setProperty("processRule", processRule);

		boolean isTransactionFee = false;
		if (userSiteSpecificObject.containsField("isTransactionFee")) {
			isTransactionFee = userSiteSpecificObject.getBoolean("isTransactionFee");
		}
		exchange.setProperty("isTransactionFee", isTransactionFee);

		boolean syncDuplicateSKUs = false;
		if (queryResult.containsField("syncDuplicateSKUs")) {
			syncDuplicateSKUs = (Boolean) queryResult.get("syncDuplicateSKUs");
		} else if (queryResult.containsField("individualSKUPerChannel")) {
			syncDuplicateSKUs = (Boolean) queryResult.get("individualSKUPerChannel");
		}
		exchange.setProperty("syncDuplicateSKUs", syncDuplicateSKUs);
		boolean syncMultipleUnitSKUs = false;
		if (queryResult.containsField("syncMultipleUnitSKUs")) {
			syncMultipleUnitSKUs = (Boolean) queryResult.get("syncMultipleUnitSKUs");
		}
		boolean syncBundleSKUs = false;
		if (queryResult.containsField("syncBundleSKUs")) {
			syncBundleSKUs = queryResult.getBoolean("syncBundleSKUs");
			// Default delimiter
			String bundleDelimiter = "+";
			if (queryResult.containsField("bundleDelimiter")) {
				bundleDelimiter = queryResult.getString("bundleDelimiter");
			}
			exchange.setProperty("bundleDelimiter", bundleDelimiter);

		}
		exchange.setProperty("syncBundleSKUs", syncBundleSKUs);
		exchange.setProperty("syncMultipleUnitSKUs", syncMultipleUnitSKUs);
		if(queryResult.containsField("showOnlyManagedOrders")) {
			exchange.setProperty("showOnlyManagedOrders", queryResult.getBoolean("showOnlyManagedOrders"));
		}
	}

	private BasicDBObject runQuery(String accountNumber, String nickNameID, String siteName, String accountingChannel) {
		BasicDBObject elemMatch = new BasicDBObject("nickName.id", nickNameID);
		BasicDBObject searchQuery = new BasicDBObject(siteName, new BasicDBObject("$elemMatch", elemMatch));
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);

		BasicDBObject projection = new BasicDBObject(siteName + ".$", 1);
		projection.put("merchantID", 1);
		projection.put("profile", 1);
		projection.put("syncDuplicateSKUs", 1);
		projection.put("individualSKUPerChannel", 1);
		projection.put("syncMultipleUnitSKUs", 1);
		projection.put("syncInventory", 1);
		projection.put("showOnlyManagedOrders", 1);
		projection.put("syncBundleSKUs", 1);
		projection.put("bundleDelimiter", 1);

		String[] channels = accountingChannel.split("-");
		for (String channel : channels) {
			projection.put(channel, 1);
		}
		MongoCollection<Document> table = DbUtilities.getDBCollection("accounts");
		Document accountDocument = table.find(searchQuery).projection(projection).first();
		BasicDBObject accountDetails = (BasicDBObject) JSON.parse(accountDocument.toJson());
		return accountDetails;
	}

	private static String getinvoiceNumberPrefix(List<BasicDBObject> proflieList, String profileID) {
		for (BasicDBObject profile : proflieList) {
			BasicDBObject nickName = (BasicDBObject) profile.get("nickName");
			if (nickName.getString("id").equals(profileID) && profile.containsField("invoiceNumberPrefix")) {
				return profile.getString("invoiceNumberPrefix");
			}
		}
		return "";
	}
}