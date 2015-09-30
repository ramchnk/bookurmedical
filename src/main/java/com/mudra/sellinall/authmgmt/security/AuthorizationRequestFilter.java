package com.mudra.sellinall.authmgmt.security;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.mongodb.DBObject;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 *  * Allow the system to serve xhr level 2 from all cross domain site  *  * @author
 * Vikraman (LGPLv3)  * @version 0.1  
 */
public class AuthorizationRequestFilter implements ContainerRequestFilter {
	static Logger log = Logger.getLogger(AuthorizationRequestFilter.class
			.getName());

	// private AuthorizationLifeCycle authLifeCycle;

	public ContainerRequest filter(ContainerRequest arg0)
			throws WebApplicationException {
		if (arg0.getMethod().equals("OPTIONS")) {
			throw new WebApplicationException(Status.ACCEPTED);
		}
		if (arg0.getPath().equals("auth")) {
			log.debug("no need to authorize this url becauseAuth");
			return arg0;
		}
		if (arg0.getPath().equals("linkedAccount")
				&& (arg0.getMethod().equals("POST"))) {
			System.out
					.println("no need to authorize this url because linkedaccount");
			return arg0;
		}
		if (arg0.getPath().equals("linkedAccount")
				&& (arg0.getMethod().equals("OPTIONS"))) {
			System.out
					.println("no need to authorize this url because linkedaccount");
			throw new WebApplicationException(Status.OK);
		}
		if ((arg0.getPath().equals("linkAccount/complete/payPal") || arg0
				.getPath().equals("linkAccount/complete/eBay"))
				&& arg0.getMethod().equals("GET")) {
			System.out
					.println("NO Need To authorize this url because ebay or Paypal");
			return arg0;
		}
		if (arg0.getPath().contains("receivenotification")
				&& arg0.getMethod().equals("POST")) {
			System.out
					.println("NO Need To authorize this url because ebay or Paypal");
			log.debug("Method=" + arg0.getMethod());
			return arg0;
		}

		/*
		 * if (arg0.getAbsolutePath().getHost().equals("localhost")) { return
		 * arg0; }
		 */

		log.debug("method is " + arg0.getMethod());
		try {
			AuthorizationLifeCycle authLifeCycle = new AuthorizationLifeCycle();
			String mudraToken = arg0.getHeaderValue("Mudra");
			String userID = null;
			AuthorizationResponseEnum authResponse = AuthorizationResponseEnum.UNKNOWN;
			String[] splitMudraToken = authLifeCycle
					.splitMudraToken(mudraToken);
			userID = splitMudraToken[0];
			log.debug("Test User id\t" + userID);
			authResponse = (AuthorizationResponseEnum) AuthorizationLifeCycle
					.isValidOAuthToken(userID, splitMudraToken[1]);
			if (authResponse == AuthorizationResponseEnum.VALID) {
				InBoundHeaders headers = new InBoundHeaders();
				headers.add("InternalFaceBookID", userID);
				DBObject authDetails = AuthorizationLifeCycle.getAuth(userID);
				headers.add("userID", authDetails.get("userID").toString());
				headers.add("mudraToken", mudraToken);
				headers.add("Content-Type", "application/json");
				log.debug("Add header to Account Number");
				arg0.setHeaders(headers);
				return arg0;
			}
		} catch (Exception e) {
			log.error(arg0.getAbsolutePath().getHost());
			log.error("some Exception or some one hacking\n" + e);
		}
		// break the code since it reached here
		log.debug("UNAUTHORIZED");
		throw new WebApplicationException(Status.UNAUTHORIZED);
	}
}
