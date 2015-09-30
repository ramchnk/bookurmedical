/**
 * This code is created by Vikraman
 * All copy rights should go to rightful owner of Mudra 
 */
package com.mudra.sellinall.authmgmt.security;

/**
 * @author vpanrutti
 *
 */
public enum AuthorizationResponseEnum {
	VALID("V"),INVALID("I"),NEWUSER("N"),UNKNOWN("U");
	
	private String authCode;
	private AuthorizationResponseEnum(String s){
		authCode = s;
	}
	/**
	 * @return the authCode
	 */
	public String getAuthCode() {
		return authCode;
	}
}
