package com.mudra.sellinall.config;

import org.springframework.context.ApplicationContext;

public class PostingSites {
	public static ApplicationContext context;
	private String sites;
	private String[] sitesList;

	public String getSites() {
		return sites;
	}

	public void setSites(String sites) {
		this.sites = sites;
	}

	public String[] getSitesList() {	
		sitesList = sites.split("-");
		return sitesList;
	}
	
	public static PostingSites getConfig() {
		return (PostingSites) context.getBean("PostingSites");
	}
}
