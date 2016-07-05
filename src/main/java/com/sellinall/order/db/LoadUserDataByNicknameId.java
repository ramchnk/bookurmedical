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
		List<BasicDBObject> userSiteSpecificObjectList = (List<BasicDBObject>) queryResult.get(siteName);
		// always userSiteSpecificObject contains only one siteName(eBay-1 only)
		BasicDBObject userSiteSpecificObject = userSiteSpecificObjectList.get(0);
		exchange.setProperty("userSiteSpecificObject", userSiteSpecificObject);
		Boolean ignoreSoldEvent = false;
		if (userSiteSpecificObject.containsField("ignoreSoldEvent")) {
			ignoreSoldEvent = userSiteSpecificObject.getBoolean("ignoreSoldEvent");
		}
		exchange.setProperty("ignoreSoldEvent", ignoreSoldEvent);
	}

	private DBObject runQuery(String accountNumber, String nickNameID, String siteName) {
		BasicDBObject elemMatch = new BasicDBObject("nickName.id", nickNameID);
		BasicDBObject searchQuery = new BasicDBObject(siteName, new BasicDBObject("$elemMatch", elemMatch));
		ObjectId objId = new ObjectId(accountNumber);
		searchQuery.put("_id", objId);

		BasicDBObject fields = new BasicDBObject(siteName + ".$", 1);
		fields.put("merchantID", 1);

		DBCollection table = DbUtilities.getDBCollection("user");
		DBObject object = table.findOne(searchQuery, fields);
		return object;
	}
}