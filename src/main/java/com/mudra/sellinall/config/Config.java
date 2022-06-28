package com.mudra.sellinall.config;

import org.springframework.context.ApplicationContext;

public class Config {
	public static ApplicationContext context;

	private String FbTokenExchangeUri;

	private String orderConfigDBURI;
	private String orderConfigDBName;
	private String userConfigDBURI;
	private String userConfigDBName;
	private String inventoryConfigDBURI;
	private String inventoryConfigDBName;

	private String uploadImageUri;
	private String SIAOrderPageURL;
	private String Ragasiyam;
	private String SIAAccountingChannels;
	private String SIAfeeManagementServerURL;
	private String SIAInventoryManagementServerURL;
	private String warehouses;
	private boolean whatsAppEnabled;
	private String MemcachedCloudUsername;
	private String MemcachedCloudPassword;
	private String MemcachedCloudServers;
	private String docUploadPath;
	private String SIAFinopsServerURL;
	private String maatramIntegratedShippingCarrier;
	private String maatramIntegratedWms;
	private String maatramIntegratedErp;
	private String maatramIntegratedOms;
	private String maatramBridgeIntegratedServers;
	
	public String getSIAInventoryManagementServerURL() {
		return SIAInventoryManagementServerURL;
	}

	public void setSIAInventoryManagementServerURL(String sIAInventoryManagementServerURL) {
		SIAInventoryManagementServerURL = sIAInventoryManagementServerURL;
	}

	public String getRagasiyam() {
		return Ragasiyam;
	}

	public void setRagasiyam(String ragasiyam) {
		Ragasiyam = ragasiyam;
	}

	public String getFbTokenExchangeUri() {
		return FbTokenExchangeUri;
	}

	public void setFbTokenExchangeUri(String fbTokenExchangeUri) {
		FbTokenExchangeUri = fbTokenExchangeUri;
	}
	
	public String getOrderConfigDBURI() {
		return orderConfigDBURI;
	}

	public void setOrderConfigDBURI(String orderConfigDBURI) {
		this.orderConfigDBURI = orderConfigDBURI;
	}

	public String getOrderConfigDBName() {
		return orderConfigDBName;
	}

	public void setOrderConfigDBName(String orderConfigDBName) {
		this.orderConfigDBName = orderConfigDBName;
	}
	
	public String getUserConfigDBURI() {
		return userConfigDBURI;
	}

	public void setUserConfigDBURI(String userConfigDBURI) {
		this.userConfigDBURI = userConfigDBURI;
	}

	public String getUserConfigDBName() {
		return userConfigDBName;
	}

	public void setUserConfigDBName(String userConfigDBName) {
		this.userConfigDBName = userConfigDBName;
	}

	public String getInventoryConfigDBURI() {
		return inventoryConfigDBURI;
	}

	public void setInventoryConfigDBURI(String inventoryConfigDBURI) {
		this.inventoryConfigDBURI = inventoryConfigDBURI;
	}

	public String getInventoryConfigDBName() {
		return inventoryConfigDBName;
	}

	public void setInventoryConfigDBName(String inventoryConfigDBName) {
		this.inventoryConfigDBName = inventoryConfigDBName;
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

	public String getSIAOrderPageURL() {
		return SIAOrderPageURL;
	}

	public void setSIAOrderPageURL(String sIAOrderPageURL) {
		SIAOrderPageURL = sIAOrderPageURL;
	}

	public String getSIAAccountingChannels() {
		return SIAAccountingChannels;
	}

	public void setSIAAccountingChannels(String sIAAccountingChannels) {
		SIAAccountingChannels = sIAAccountingChannels;
	}

	public String getSIAfeeManagementServerURL() {
		return SIAfeeManagementServerURL;
	}

	public void setSIAfeeManagementServerURL(String sIAfeeManagementServerURL) {
		SIAfeeManagementServerURL = sIAfeeManagementServerURL;
	}

	public String getWarehouses() {
		return warehouses;
	}

	public void setWarehouses(String warehouses) {
		this.warehouses = warehouses;
	}

	public boolean getWhatsAppEnabled() {
		return whatsAppEnabled;
	}

	public void setWhatsAppEnabled(boolean whatsAppEnabled) {
		this.whatsAppEnabled = whatsAppEnabled;
	}

	public String getMemcachedCloudUsername() {
		return MemcachedCloudUsername;
	}

	public void setMemcachedCloudUsername(String memcachedCloudUsername) {
		MemcachedCloudUsername = memcachedCloudUsername;
	}

	public String getMemcachedCloudPassword() {
		return MemcachedCloudPassword;
	}

	public void setMemcachedCloudPassword(String memcachedCloudPassword) {
		MemcachedCloudPassword = memcachedCloudPassword;
	}

	public String getMemcachedCloudServers() {
		return MemcachedCloudServers;
	}

	public void setMemcachedCloudServers(String memcachedCloudServers) {
		MemcachedCloudServers = memcachedCloudServers;
	}

	public String getDocUploadPath() {
		return docUploadPath;
	}

	public void setDocUploadPath(String docUploadPath) {
		this.docUploadPath = docUploadPath;
	}

	public String getSIAFinopsServerURL() {
		return SIAFinopsServerURL;
	}

	public void setSIAFinopsServerURL(String sIAFinopsServerURL) {
		SIAFinopsServerURL = sIAFinopsServerURL;
	}

	public String getMaatramIntegratedWms() {
		return maatramIntegratedWms;
	}

	public void setMaatramIntegratedWms(String maatramIntegratedWms) {
		this.maatramIntegratedWms = maatramIntegratedWms;
	}

	public String getMaatramIntegratedErp() {
		return maatramIntegratedErp;
	}

	public void setMaatramIntegratedErp(String maatramIntegratedErp) {
		this.maatramIntegratedErp = maatramIntegratedErp;
	}

	public String getMaatramIntegratedShippingCarrier() {
		return maatramIntegratedShippingCarrier;
	}

	public void setMaatramIntegratedShippingCarrier(String maatramIntegratedShippingCarrier) {
		this.maatramIntegratedShippingCarrier = maatramIntegratedShippingCarrier;
	}

	public String getMaatramBridgeIntegratedServers() {
		return maatramBridgeIntegratedServers;
	}

	public void setMaatramBridgeIntegratedServers(String maatramBridgeIntegratedServers) {
		this.maatramBridgeIntegratedServers = maatramBridgeIntegratedServers;
	}

	public String getMaatramIntegratedOms() {
		return maatramIntegratedOms;
	}

	public void setMaatramIntegratedOms(String maatramIntegratedOms) {
		this.maatramIntegratedOms = maatramIntegratedOms;
	}

}
