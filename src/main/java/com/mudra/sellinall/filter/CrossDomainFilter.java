package com.mudra.sellinall.filter;

import org.apache.log4j.Logger;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 *  * Allow the system to serve xhr level 2 from all cross domain site  *  * @author
 * Deisss (LGPLv3)  * @version 0.1  
 */
public class CrossDomainFilter implements ContainerResponseFilter {
	static Logger log = Logger.getLogger(CrossDomainFilter.class.getName());

	/**
	 *      * Add the cross domain data to the output if needed      *      * @param
	 * creq The container request (input)      * @param cres The container
	 * request (output)      * @return The output request with cross domain if
	 * needed      
	 */
	public ContainerResponse filter(ContainerRequest creq,
			ContainerResponse cres) {
		log.debug("Inside Cross Domain same origin Response FIlter ");
		cres.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
		cres.getHttpHeaders().add("Access-Control-Allow-Headers",
				"origin, content-type, accept, authorization, Mudra");
		cres.getHttpHeaders().add("Access-Control-Allow-Credentials", "true");
		cres.getHttpHeaders().add("Access-Control-Allow-Methods",
				"GET, POST, PUT, DELETE");
		cres.getHttpHeaders().add("Access-Control-Max-Age", "1209600");
		cres.getHttpHeaders().add("Content-type", "text/html");
		return cres;
	}
}