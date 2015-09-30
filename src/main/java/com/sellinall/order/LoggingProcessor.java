package com.sellinall.order;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class LoggingProcessor implements Processor {
	static int si = 0;
	public void process(Exchange exchange) throws Exception {

		System.out.println("LoggingProcessor " + ++si + " {");
		System.out.println("Received Bodyr: " +

		exchange.getIn().getBody(String.class));

		System.out.println("Received header: " +

		exchange.getIn().getHeaders().toString());
try{
	System.out.println("Received Exception: " + exchange.getException()!=null?exchange.getException().getMessage():"no exception" );
	
}catch(Exception e){
	System.out.println("Exception"+e);
}
		System.out.println(si + "} done.");
	}

}