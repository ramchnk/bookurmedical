package com.sellinall.order.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;

import com.mongodb.MongoConfigurationException;
import com.mongodb.client.MongoCollection;
import com.sellinall.database.DbUtilities;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthService {
	@GET
	public Response getResponseCode(@QueryParam("isMongoException") boolean isMongoException,
			@QueryParam("isNoClassDefError") boolean isNoClassDefError) throws JSONException {
		if (checkDBHealth(isMongoException, isNoClassDefError)) {
			return Response.status(Response.Status.OK).build();
		}
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
	}

	private boolean checkDBHealth(boolean isMongoException, boolean isNoClassDefError) {
		try {
//			MongoCollection<Document> table = DbUtilities.getDBCollection("accounts");
//			table.countDocuments();
			stageTestMethod(isMongoException, isNoClassDefError);
		} catch (Throwable t) {
			t.printStackTrace();
			String error = t.getMessage();
			Throwable cause = t.getCause();
			if ((error == null || error.isEmpty()) && cause != null) {
				error = cause.getMessage();
			}
			if (error != null && (error.contains("Could not initialize class com.sellinall.database.DbUtilities")
					|| error.contains("Unable to look up TXT record for host"))) {
				return false;
			}
		}
		return true;
	}

	private void stageTestMethod(boolean isMongoException, boolean isNoClassDefError) {
		if (isMongoException) {
			throw new MongoConfigurationException("Unable to look up TXT record for host stage.q5phi.gcp.mongodb.net");
		} else if (isNoClassDefError) {
			throw new NoClassDefFoundError("Could not initialize class com.sellinall.database.DbUtilities");
		}

	}
}