/**
 * 
 */
package com.sellinall.order.db;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author vikraman
 *
 */
public class UpdateSKUDBQuery implements Processor {
	static Logger log = Logger.getLogger(UpdateSKUDBQuery.class.getName());

	// https://mws.amazonservices.com?AWSAccessKeyId=AKIAIOGR2TDHPBPDRGRA&Action=SubmitFeed&Merchant=A3P79OXU5J6LWN&MWSAuthToken=amzn.mws.eca6a8c6-37d9-f50b-21cc-69cc9fbe473e&SignatureVersion=2&Timestamp=2015-05-27T06%3A13%3A28Z&Version=2009-01-01&Signature=CVublKnQ3LG2RnXNvCvjQe8CqQafaJ1I2pBlm4Q8t6w%3D&SignatureMethod=HmacSHA256&FeedType=_POST_PRODUCT_DATA_&PurgeAndReplace=false
	public void process(Exchange exchange) throws Exception {

		String inBody = exchange.getIn().getBody(String.class);
		System.out.println("UpdateSKUDBQuery Received sku: " + inBody);
		Object[] outBody = createBody(exchange);
		exchange.getOut().setBody(outBody);
	}

	private Object[] createBody(Exchange exchange) {
		BasicDBObject inventory = (BasicDBObject) exchange.getProperty("inventory");
		String SKU = inventory.getString("SKU");
		BasicDBObject amazon = (BasicDBObject) inventory.get("amazon");
		String nickNameID = amazon.getString("nickNameID");
		DBObject filterField1 = new BasicDBObject("SKU", SKU);
		DBObject filterField2 = new BasicDBObject("amazon.nickNameID", nickNameID);
		BasicDBList and = new BasicDBList();
		and.add(filterField1);
		and.add(filterField2);
		DBObject filterField = new BasicDBObject("$and", and);

		// It looks like ASIN is not unique to a seller. So using SKU as the
		// reference id for Amazon.
		DBObject updateObject = new BasicDBObject("$set", new BasicDBObject("amazon.$.refrenceID", SKU).append(
				"amazon.$.itemUrl", nickNameID));

		return new Object[] { filterField, updateObject };
	}
}