package org.cryptotrade.helper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpHelper {
	private static final Logger Log = LogManager.getLogger();
	public static final String ContentType_FromData = "application/x-www-form-urlencoded";
	public static final String ContentType_JSON = "application/json";
	
	private static HttpClient Client = HttpClient.newHttpClient();
	
	public static HttpResponse<String> Get(String _url, Map<String,Map<String,String>> _option)
			throws IOException, InterruptedException {
		Builder builder = SetHttpRequest(_url, _option);	
		HttpRequest request = builder.GET().build();
		Log.debug(request.toString());
		return Client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	/**
	 * 
	 * @param _url
	 * @param _option <br>
	 * <ul>
	 * 	欲傳ContentType為JSON, _option中的data項需要將資料放進"__json_data__"<br>
	 * 	即 _option: { data: { __json_data__: "{}" } }
	 * </ul>
	 * @return
	 * @throws IOException	
	 * @throws InterruptedException
	 */
	public static HttpResponse<String>  Post(String _url, Map<String,Map<String,String>> _option) 
			throws IOException, InterruptedException {
		Builder builder = SetHttpRequest(_url, _option);
		if(_option!=null) {
			Map<String,String> header = _option.get("header");
			Map<String,String> data = _option.get("data");
			if(header!=null) {
				String contentType = header.get("Content-Type");
				if(contentType==ContentType_FromData)
					builder.POST(BodyPublishers.ofString(GetHttpURLEncodeQueryString(data)));
				if(contentType==ContentType_JSON && data!=null)
					builder.POST(BodyPublishers.ofString(data.get("__json_data__")));
			}
		}
		else
			builder.POST(BodyPublishers.noBody());
		HttpRequest request = builder.build();
		Log.debug(request.toString());
		return Client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	public static HttpResponse<String> Delete(String _url, Map<String,Map<String,String>> _option)
			throws IOException, InterruptedException {
		Builder builder = SetHttpRequest(_url, _option);
		if(_option!=null) {
			Map<String,String> header = _option.get("header");
			Map<String,String> data = _option.get("data");
			if(header!=null) {
				String contentType = header.get("Content-Type");
				if(contentType==ContentType_FromData)
					builder.method("DELETE",BodyPublishers.ofString(GetHttpURLEncodeQueryString(data)));
				if(contentType==ContentType_JSON && data!=null)
					builder.method("DELETE",BodyPublishers.ofString(data.get("__json_data__")));
			}
		}
		else
			builder.method("DELETE",BodyPublishers.noBody());
			
		HttpRequest request = builder.build();
		return Client.send(request, HttpResponse.BodyHandlers.ofString());		
	}
	
	private static Builder SetHttpRequest(String _url, Map<String,Map<String,String>> _option) {
		Builder builder = HttpRequest.newBuilder();
		if(_option!=null) {
			Map<String,String> header = _option.get("header");
			if(header!=null)
				header.entrySet().forEach(entry -> {
					builder.header(entry.getKey(), entry.getValue());
				});
			Map<String,String> query = _option.get("query");
			String queryString = GetHttpURLEncodeQueryString(query);
			String url = _url + (queryString.isEmpty() ? "" : "?"+queryString);
			builder.uri(URI.create(url));
		}
		else
			builder.uri(URI.create(_url));
		return builder;	
	}
			
	/**
	 * 將參數打包成url query字串(未經過URLEncode)
	 * @param data - Map<String,String> 要打包的資料對
	 * @return 
	 */
	public static String GetHttpQueryString(Map<String,String> data) {
		if(data==null) return "";
		var iterator = data.entrySet().iterator();
		StringBuilder dataString = new StringBuilder();
		while(iterator.hasNext()) {
			var entry = iterator.next();
			if(dataString.length()>0) dataString.append("&");
			dataString.append(entry.getKey()+"="+entry.getValue());
		}
		return dataString.toString();
	}

	
	/**
	 * 將參數打包成url query字串並做URL encode處理
	 * @param data - Map<String,String> 要打包的資料對
	 * @return 
	 */
	public static String GetHttpURLEncodeQueryString(Map<String,String> data) {
		if(data==null) return "";
		var string = new StringBuilder();
		var iterator = data.entrySet().iterator();
		while(iterator.hasNext()) {
			var entry = iterator.next();
			if(string.length()>0) string.append("&");
			string.append(URLEncoder.encode(entry.getKey(), Charset.forName("UTF8")));
			string.append("=");
			string.append(URLEncoder.encode(entry.getValue(), Charset.forName("UTF8")));
		}
		return string.toString();
	}

}
