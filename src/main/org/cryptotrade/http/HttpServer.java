package org.cryptotrade.http;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.http.handler.PythonDataReceiveHandler;
import org.cryptotrade.http.handler.TelegramWebhookHandler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class HttpServer {
	private static Logger Log = LogManager.getLogger();

	public HttpServer() {
		
	}
	
	public void start() {
		try {
	    // Set up the socket address
			InetAddress inetAddress = InetAddress.getByName("0.0.0.0");
			
	    InetSocketAddress address = new InetSocketAddress(inetAddress, 
	    		Integer.valueOf(App.env.get("http.port", "443")));
			Log.info("HttpServer listen at: "+address);

	    // Initialise the HTTPS server
	    HttpsServer httpsServer = HttpsServer.create(address, 0);
	    SSLContext sslContext = SSLContext.getInstance("TLS");

	    // Initialise the keystore
	    char[] password = App.env.get("http.ssl.password").toCharArray();
	    KeyStore ks = KeyStore.getInstance("JKS");
	    FileInputStream fis = new FileInputStream(App.env.get("http.ssl.file"));
	    ks.load(fis, password);

	    // Set up the key manager factory
	    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
	    kmf.init(ks, password);

	    // Set up the trust manager factory
	    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
	    tmf.init(ks);

	    // Set up the HTTPS context and parameters
	    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
	    httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
        public void configure(HttpsParameters params) {
          try {
            // Initialise the SSL context
            SSLContext c = SSLContext.getDefault();
            SSLEngine engine = c.createSSLEngine();
            params.setNeedClientAuth(false);
            params.setCipherSuites(engine.getEnabledCipherSuites());
            params.setProtocols(engine.getEnabledProtocols());

            // Get the default parameters
            SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
            params.setSSLParameters(defaultSSLParameters);
          } catch (Exception e) {
          	Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
          }
        }
	    });
	    
	    // 註冊路徑
      httpsServer.createContext("/telegram-webhook", new TelegramWebhookHandler(App.env.get("telegram.webhook.authorization")));
      httpsServer.createContext("/python-data-receiver",new PythonDataReceiveHandler(
      		App.env.get("http.api.python.key"),
      		App.env.get("http.api.python.secret")
      ));
      
      httpsServer.setExecutor(null); // creates a default executor
      httpsServer.start();    
	    
		} catch (Exception exception) {
      System.err.println(exception.getMessage());
		}
	}
	
	public static String headersToString(Headers _headers) {
		Map<String,List<String>> headers = _headers;
		Map<String,String> map = new HashMap();
		Iterator<Entry<String,List<String>>> it = headers.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,List<String>> entry = it.next();
			String str = "";
			for(String tmp : entry.getValue())
				str += (str.length()>0 ? "," : "") + tmp ;
			map.put(entry.getKey(), "\""+str+"\"");
		}
		return map.toString();
	}
	
	public static String getHeaderData(Headers _headers,String _name) {
		String data = null;
		Map<String,List<String>> headers = _headers;
		Iterator<Entry<String,List<String>>> it = headers.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,List<String>> entry = it.next();
			if(entry.getKey().equals(_name)) {
				data = "";
				for(String tmp : entry.getValue())
					data += (data.length()>0 ? "," : "") + tmp ;
			}
		}		
		return data;
	}
	
	public static Map<String,String> queryToMap(String _query){
    Map<String, String> result = new HashMap<>();
    if(_query == null || _query.isEmpty()) 
    	return result;

    for (String param : _query.split("&")) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			}else{
				result.put(entry[0], "");
			}
    }
    return result;
	}
}
