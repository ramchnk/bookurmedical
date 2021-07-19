package com.sellinall.order;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mudra.sellinall.config.Config;
import com.mudra.sellinall.config.PostingSites;
import com.sellinall.order.services.PartnerNotification;
import com.sellinall.order.util.OrderUtil;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.InvoiceSequence;

/**
 * 
 * This class launches the web application in an embedded Jetty container. This
 * is the entry point to your application. The Java command that is used for
 * launching should fire this main method.
 * 
 */
public class MainPrg {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String webappDirLocation = "src/main/webapp/";
		Config.context = new ClassPathXmlApplicationContext("Propertycfg.xml");
		PostingSites.context = new ClassPathXmlApplicationContext("PostingSitescfg.xml");
		// The port that we should run on can be set into an environment
		// variable
		// Look for that variable and default to 8081 if it isn't there.
		String webPort = System.getenv("PORT");
		if (webPort == null || webPort.isEmpty()) {
			webPort = "8081";
		}

		Server server = new Server(Integer.valueOf(webPort));
		WebAppContext root = new WebAppContext();

		root.setContextPath("/");
		root.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
		root.setResourceBase(webappDirLocation);
		BasicConfigurator.configure();
		// Parent loader priority is a class loader setting that Jetty accepts.
		// By default Jetty will behave like most web containers in that it will
		// allow your application to replace non-server libraries that are part
		// of the
		// container. Setting parent loader priority to true changes this
		// behavior.
		// Read more here:
		// http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
		root.setParentLoaderPriority(true);

		server.setHandler(root);
		// Init memory cache
		OrderUtil.initMemoryCached();
		ApplicationContext appContext = new ClassPathXmlApplicationContext("CamelContext.xml");
		CamelContext camelContext = SpringCamelContext.springCamelContext(appContext, false);
		camelContext.start();
		ProducerTemplate template = camelContext.createProducerTemplate();
		PartnerNotification.setProducerTemplate(template);

		Config config = Config.getConfig();
		InvoiceSequence.init(config.getInventoryConfigDBURI(), config.getInventoryConfigDBName());
		config.setRagasiyam(System.getenv(AuthConstant.RAGASIYAM_KEY));
		server.start();
		server.join();
	}

}
