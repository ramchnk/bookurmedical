package com.mudra.sellinall.config;

import org.springframework.context.ApplicationContext;

public class Config {
	public static ApplicationContext context;

	private String FbTokenExchangeUri;

	private String DbUserName;
	private String DbPassword;
	private String UserCollectionHostName;
	private String UserCollectionPort;
	private String UserCollectionDBName;
	private String InventoryCollectionHostName;
	private String InventoryCollectionPort;
	private String InventoryCollectionDBName;
	private String uploadImageUri;

	public String getFbTokenExchangeUri() {
		return FbTokenExchangeUri;
	}

	public void setFbTokenExchangeUri(String fbTokenExchangeUri) {
		FbTokenExchangeUri = fbTokenExchangeUri;
	}

	public String getDbUserName() {
		return DbUserName;
	}

	public void setDbUserName(String dbUserName) {
		DbUserName = dbUserName;
	}

	public String getDbPassword() {
		return DbPassword;
	}

	public void setDbPassword(String dbPassword) {
		DbPassword = dbPassword;
	}

	public String getUserCollectionHostName() {
		return UserCollectionHostName;
	}

	public void setUserCollectionHostName(String userCollectionHostName) {
		UserCollectionHostName = userCollectionHostName;
	}

	public String getUserCollectionPort() {
		return UserCollectionPort;
	}

	public void setUserCollectionPort(String userCollectionPort) {
		UserCollectionPort = userCollectionPort;
	}

	public String getUserCollectionDBName() {
		return UserCollectionDBName;
	}

	public void setUserCollectionDBName(String userCollectionDBName) {
		UserCollectionDBName = userCollectionDBName;
	}

	public String getInventoryCollectionHostName() {
		return InventoryCollectionHostName;
	}

	public void setInventoryCollectionHostName(
			String inventoryCollectionHostName) {
		InventoryCollectionHostName = inventoryCollectionHostName;
	}

	public String getInventoryCollectionPort() {
		return InventoryCollectionPort;
	}

	public void setInventoryCollectionPort(String inventoryCollectionPort) {
		InventoryCollectionPort = inventoryCollectionPort;
	}

	public String getInventoryCollectionDBName() {
		return InventoryCollectionDBName;
	}

	public void setInventoryCollectionDBName(String inventoryCollectionDBName) {
		InventoryCollectionDBName = inventoryCollectionDBName;
	}

	public String getUploadImageUri() {
		return uploadImageUri;
	}

	public void setUploadImageUri(String uploadImageUri) {
		this.uploadImageUri = uploadImageUri;
	}

	public static Config getConfig() {
		return (Config) context.getBean("Config");
	}
}
