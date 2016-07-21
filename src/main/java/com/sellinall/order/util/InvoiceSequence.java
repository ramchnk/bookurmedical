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


	public static String getInvoiceSequence(String merchantId, String invoiceProfile) {
		return getInvoice(merchantId, "invoiceSeq", invoiceProfile);
	}

	public static String getInvoice(String merchantId, String seqName, String invoiceProfile) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", merchantId);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("profileid", invoiceProfile);
		BasicDBObject invoiceSeqQuery = new BasicDBObject("$elemMatch", elemMatch);
		searchQuery.put(seqName, invoiceSeqQuery);
		BasicDBObject increase = new BasicDBObject("invoiceSeq.$.inv", 1);
		BasicDBObject updateQuery = new BasicDBObject("$inc", increase);
		DBObject result = sequence.findAndModify(searchQuery, null, null, false, updateQuery, true, true);
		List<BasicDBObject> resultProcess = (List<BasicDBObject>) result.get(seqName);
		int seq = 0;
		for (BasicDBObject searchInv : resultProcess) {
			if (searchInv.getString("profileid").equals(invoiceProfile)) {
				seq = (Integer) searchInv.get("inv");
			}
		}

		NumberFormat numberFormat = new DecimalFormat("00000000");
		String seqString = numberFormat.format(seq).toString();
		System.out.println(seqString);
		return seqString;
	}

}
