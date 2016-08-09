package com.mudra.sellinall.authmgmt.security;

import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.social.MissingAuthorizationException;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.FacebookProfile;
import org.springframework.social.facebook.api.impl.FacebookTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mudra.sellinall.config.Config;
import com.sellinall.database.DbUtilities;
import com.sellinall.util.HttpURLConnectionUtil;

public class AuthorizationLifeCycle {
	static Logger log = Logger
			.getLogger(AuthorizationLifeCycle.class.getName());
	private Facebook facebook;

	public AuthorizationLifeCycle() {
	}
	

	public static String getIDWithAuthtype(String id, String authType) {
		// Method returns corresponding userid based on faceBook id
		log.debug("Inside Get Data authtype id=" + id);
		DBCollection table = DbUtilities.getDBCollection("user");
		DBObject object = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put(authType + ".id", id);
		DBObject fields = new BasicDBObject();
		fields.put("_id", 1);
		log.debug("Inside Get Data authtype id=" + searchQuery);
		object = table.findOne(searchQuery, fields);
		return object.get("_id").toString();
	}

	public boolean isValidFaceBookAuthToken(String faceBookID,
			String accessToken) {
		log.debug("Inside isValidFaceBookAuthToken");
		try {
			if (accessToken == null || accessToken.equals("")
					|| faceBookID == null || faceBookID.equals("")) {
				return false;
			}

			facebook = new FacebookTemplate(accessToken);
			FacebookProfile profile = facebook.userOperations()
					.getUserProfile();

			// facebook.feedOperations().post(profile.getId(),
			log.debug("Some Thing" + profile.getId());
			if (faceBookID.equals(profile.getId())) {
				return true;
			}
			log.debug("FaceBook ID mismatch fb = " + profile.getId()
					+ "; request fb =" + faceBookID);
		} catch (MissingAuthorizationException missExcep) {
			log.error("AuthException" + missExcep);
		} catch (Exception ex) {
			log.error("AuthException" + ex);
		}
		return false;
	}

	// Method update/create Auth Token to auth table
	public static Boolean updateToken(String faceBookID, String accessToken) {
		log.debug("update token");
		DBCollection table = DbUtilities.getDBCollection("mongoAuthDetails");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("faceBookAuth.faceBookId", faceBookID);
		log.debug("Search Query" + searchQuery);
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.append("timeUpdated", new Date());
		dbObject.append("authType", "faceBook");
		BasicDBObject faceBookAuth = new BasicDBObject();
		faceBookAuth.append("faceBookId", faceBookID);
		faceBookAuth.append("authToken", accessToken);
		dbObject.append("faceBookAuth", faceBookAuth);
		String userId = getIDWithAuthtype(faceBookID, new String("faceBook"));
		dbObject.append("userID", userId);
		log.debug("success reacehd udatetoken" + userId);
		table.update(searchQuery, dbObject, true, false);
		log.debug("success reacehd udatetoken");
		try {
			String url = Config.getConfig().getFbTokenExchangeUri()
					+ accessToken;
			String responseString = HttpURLConnectionUtil.doGet(url);
			String[] value = responseString.split("&");
			String[] parsedToken = value[0].split("=");
			String longToken = parsedToken[1];
			table = DbUtilities.getDBCollection("user");
			searchQuery = new BasicDBObject();
			searchQuery.put("faceBook.id", faceBookID);
			DBObject updateData = new BasicDBObject();
			updateData.put("faceBook.$.postHelper.authToken", longToken);
			log.debug("searchQueary for update Post Helper\n" + searchQuery);
			log.debug("Update Data\n" + updateData);
			table.update(searchQuery, new BasicDBObject("$set", updateData));
		} catch (Exception exception) {
			log.error("Exception for get Long Token");
		}
		return true;
	}

	// isValidOAuthToken Method Check Only mudraToken and Time of Last
	// Transaction
	// When Last Transaction time exceed to 30 min mudra token will update in
	// auth table
	public static Object isValidOAuthToken(String userId, String mudraToken) {
		log.debug("inside 2");
		DBCollection table = DbUtilities.getDBCollection("mongoAuthDetails");
		DBObject authDetails = new BasicDBObject();
		DBObject searchUserQuery = new BasicDBObject();
		searchUserQuery.put("userID", userId);
		log.debug("test2" + searchUserQuery);
		authDetails = table.findOne(searchUserQuery);
		log.debug("sample test" + authDetails);
		try {
			if (authDetails.get("userID").toString().equals(userId)) {
				log.debug("inside duplicate 1");
				Date currentTimeStamp = new Date();
				Date authDate = (Date) authDetails.get("timeUpdated");
				long diff = currentTimeStamp.getTime() - authDate.getTime();
				log.debug("test2");
				long diffMinutes = diff / (60 * 1000) % 60;
				DBObject faceBookAuth = (DBObject) authDetails
						.get("faceBookAuth");
				log.debug("inside duplicate 2" + faceBookAuth.get("faceBookId"));
				log.debug("Minites=" + diffMinutes);
				if (faceBookAuth.get("authToken").toString().equals(mudraToken)
						&& diffMinutes < 300) {
					log.debug("FaceBook id is valid authenticated");
					return AuthorizationResponseEnum.VALID;
				} else {
					log.debug("session invalid");
					return AuthorizationResponseEnum.INVALID;
				}
			}
		} catch (Exception e) {
			log.error("Exception in is Validoathtoken " + e);
		}

		return AuthorizationResponseEnum.UNKNOWN;
	}

	public static DBObject getAuth(String userID) {
		log.debug("Get Auth Details");
		log.debug("Inside Get Data auth Userid=" + userID);
		DBCollection table = DbUtilities.getDBCollection("mongoAuthDetails");
		DBObject searchQuary = new BasicDBObject();
		DBObject auth = new BasicDBObject();
		searchQuary.put("userID", userID);
		try {
			auth = table.findOne(searchQuary);
			log.debug(auth.toString());
		} catch (Exception exception) {
			log.error(exception);
		}
		return auth;
	}

	public Facebook getFacebook() {
		return facebook;
	}

	/**
	 * @param facebook
	 *            the facebook to set
	 */
	public void setFacebook(Facebook facebook) {
		this.facebook = facebook;
	}

	public String[] splitMudraToken(String mudraToken) {
		if (mudraToken == null || mudraToken == "") {
			log.debug("mudratoken empty");
			return null;
		}
		return mudraToken.split("--");
	}

	public String createMudra(String faceBookID, String accessToken) {
		return faceBookID + "--" + accessToken;
	}

}
