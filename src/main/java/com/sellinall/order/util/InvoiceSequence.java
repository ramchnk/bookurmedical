package com.sellinall.order.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class InvoiceSequence {

	private final static String SEQUENCE = "sequence";
	private final static String INVOICE_KEY_SUFFIX = "InvoiceNumber";
	private static DBCollection sequence;

	public static void init(String dbName, String hostName, String port, String userName, String password)
			throws Exception {
		List<ServerAddress> seeds = new ArrayList<ServerAddress>();
		String[] hostNames = hostName.split(",");
		String[] ports = port.split(",");
		for (int i = 0; i < hostNames.length; i++) {
			seeds.add(new ServerAddress(hostNames[i], Integer.parseInt(ports[i])));
		}

		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(MongoCredential.createScramSha1Credential(userName, dbName, password.toCharArray()));

		MongoClient mongoClient = new MongoClient(seeds, credentials);
		DB db = mongoClient.getDB(dbName);

		sequence = db.getCollection(SEQUENCE);
	}

	public static String getInvoiceSequence(String merchantId, String invoiceProfileName) {
		return getInvoice(merchantId, invoiceProfileName);
	}

	public static String getInvoice(String merchantId, String profileID) {
		BasicDBObject searchQuery = new BasicDBObject("_id", merchantId);
		BasicDBObject increase = new BasicDBObject(profileID + INVOICE_KEY_SUFFIX, 1);
		BasicDBObject updateQuery = new BasicDBObject("$inc", increase);
		DBObject result = sequence.findAndModify(searchQuery, null, null, false, updateQuery, true, true);
		int seq = (Integer) result.get(profileID + INVOICE_KEY_SUFFIX);
		NumberFormat numberFormat = new DecimalFormat("00000000");
		return numberFormat.format(seq).toString();
	}

}
