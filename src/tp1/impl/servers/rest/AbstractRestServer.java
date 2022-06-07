package tp1.impl.servers.rest;

import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.impl.discovery.Discovery;
import tp1.impl.servers.common.AbstractServer;
import tp1.tls.InsecureHostnameVerifier;
import util.IP;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public abstract class AbstractRestServer extends AbstractServer {
	
	protected static String SERVER_BASE_URI = "https://%s:%s/rest";
	
	protected AbstractRestServer(Logger log, String service, int port) {
		super(log, service, port);
	}


	protected void start() {
		String ip = IP.hostAddress();
		String serverURI = String.format(SERVER_BASE_URI, ip, port);

		ResourceConfig config = new ResourceConfig();

		registerResources(config);

		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

		System.err.println(">>>>>" + port );

		try {
			JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config, SSLContext.getDefault());
		} catch(Exception e) {
			e.printStackTrace();
		}

		Log.info(String.format("%s Server ready @ %s\n", service, serverURI));

		Discovery.getInstance().announce(service, serverURI);
	}
	
	abstract void registerResources( ResourceConfig config );

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
}
