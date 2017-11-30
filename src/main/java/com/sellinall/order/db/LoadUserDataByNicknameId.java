/**
 * 
 */
package com.sellinall.order.db;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
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
		DBObject queryResult = runQuery(accountNumber, nickNameID, siteName);
		exchange.setProperty("syncInventory",(Boolean)queryResult.get("syncInventory"));
		List<BasicDBObject> userSiteSpecificObjectList = (List<BasicDBObject>) queryResult.get(siteName);
		// always userSiteSpecificObject contains only one siteName(eBay-1 only)
		BasicDBObject userSiteSpecificObject = userSiteSpecificObjectList.get(0);

		if (userSiteSpecificObject.containsField("invoiceProfile")
				&& userSiteSpecificObject.get("invoiceProfile") != null
				&& !userSiteSpecificObject.get("invoiceProfile").equals("null")) {
			List<BasicDBObject> userProfileList = (List<BasicDBObject>) queryResult.get("profile");
			String invoiceNumberPrefix = getinvoiceNumberPrefix(userProfileList,
					userSiteSpecificObject.getString("invoiceProfile"));
			exchange.setProperty("invoiceNumberPrefix", invoiceNumberPrefix);
			exchange.setProperty("profileID", userSiteSpecificObject.getString("invoiceProfile"));
		}

		exchange.setProperty("merchantID", queryResult.get("merchantID"));
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

		boolean syncDuplicateSKUs = false;
		if (queryResult.containsField("syncDuplicateSKUs")) {
			syncDuplicateSKUs = (Boolean) queryResult.get("syncDuplicateSKUs");
		}
		exchange.setProperty("syncDuplicateSKUs", syncDuplicateSKUs);
		boolean syncMultipleUnitSKUs = false;
		if (queryResult.containsField("syncMultipleUnitSKUs")) {
			syncMultipleUnitSKUs = (Boolean) queryResult.get("syncMultipleUnitSKUs");
		}
		exchange.setProperty("syncMultipleUnitSKUs", syncMultipleUnitSKUs);
	}

	private DBObject runQuery(String accountNumber, String nickNameID, String siteName) {
		BasicDBObject elemMatch = new BasicDBObject("nickName.id", nickNameID);
		BasicDBObject searchQuery = new BasicDBObject(siteName, new BasicDBObject("$elemMatch", elemMatch));
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);

		BasicDBObject fields = new BasicDBObject(siteName + ".$", 1);
		fields.put("merchantID", 1);
		fields.put("profile", 1);
		fields.put("syncDuplicateSKUs", 1);
		fields.put("syncMultipleUnitSKUs", 1);
		fields.put("syncInventory",1);
		DBCollection table = DbUtilities.getDBCollection("accounts");
		DBObject object = table.findOne(searchQuery, fields);
		return object;
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