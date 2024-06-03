package org.cryptotrade.http.handler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.Storage;
import org.cryptotrade.Storage.StorageName;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.http.HttpServer;
import org.cryptotrade.task.TelegramPushTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PythonDataReceiveHandler implements HttpHandler {
	private static Logger Log = LogManager.getLogger();
	private static String apiKey;
	private static String apiSecret;
	private static JSONObject recordData;
	
	public PythonDataReceiveHandler(String _apiKey,String _apiSecret) {
		apiKey = _apiKey;
		apiSecret = _apiSecret;
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// 預先使用try-catch包裹,避免發生錯誤時抓不到錯誤資訊
		try {
			JSONObject returnData = new JSONObject();
			Headers headers = exchange.getRequestHeaders();
			Log.debug("headers: "+HttpServer.headersToString(headers));
			BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			StringBuilder strBuilder = new StringBuilder().append("\n");
			String queryString = exchange.getRequestURI().getQuery();
			queryString = queryString==null ? "" : queryString;
			Log.debug("querys: "+queryString);
			Map<String,String> queryMap = HttpServer.queryToMap(URLDecoder.decode(queryString, Charset.forName("UTF-8")));
			boolean isLegalApiKey = false;
			boolean isLegalSignature = false;
			String contentType = "";
			Iterator<Entry<String,List<String>>> it = headers.entrySet().iterator();
			while(it.hasNext()){
				Entry<String,List<String>> entry = it.next();
				strBuilder.append(entry.getKey()).append(" : ");
				String _headData = "";
				for(String context : entry.getValue()) 
					_headData += (context+"\n");
				strBuilder.append(_headData);
				if( apiKey!=null && entry.getKey().compareToIgnoreCase("X-PYTHON-DATA-TOKEN")==0 ){
					isLegalApiKey ^= _headData.trim().compareToIgnoreCase(apiKey)==0;
				}
				if( entry.getKey().equalsIgnoreCase("Content-type") ) {
					contentType = _headData;
				}
			}
			// 設定檔位設定apiKey,表示不用驗證
			if(!isLegalApiKey && apiKey==null) isLegalApiKey = true;
			if(isLegalApiKey) {	
				String inData = "";
				String tmp = "";
				while((tmp=in.readLine())!=null)
					inData += tmp;
				
				JSONObject receiveData = null;
				String inputSignature = null;
				String dataString = null;
				boolean isJsonParseSuccess = true;
				if(contentType.toLowerCase().contains("json")) {
					receiveData = new JSONObject(inData).getJSONObject("data");
					Matcher matcher = Pattern.compile(",\"signature\":\"[a-fA-F0-9]{10,}\"").matcher(inData);
					String match = matcher.find() ? matcher.group() : null;
					String tmpData = match!=null ? matcher.group().replace(",", "") : null;			
					List<String> matchList = UtilsHelper.getRegepMatchString("[a-fA-F0-9]{64}", tmpData);
					inputSignature = matchList.size()>0 ? matchList.get(0) : null;
					dataString = inData.replace("{\"data\":","").replace(match+"}", "")+apiSecret;
				}
				if(contentType.toLowerCase().contains("x-www-form-urlencoded")) {
					inData = URLDecoder.decode(inData,Charset.forName("UTF-8"));
					Map<String,String> dataMap = HttpServer.queryToMap(inData);
					dataString = dataMap.get("data")+apiSecret;
					inputSignature = dataMap.get("signature");
					try {
						receiveData = new JSONObject(dataString);
					} catch(JSONException jsonException) {
						isJsonParseSuccess = false;
					}
				}
				Log.debug("inData: "+inData);
				
				String signature = UtilsHelper.SHA256(dataString);
				Log.debug("inputSignature: "+inputSignature);
				Log.debug("dataString: "+dataString);
				Log.debug("signature: "+signature);
				if(receiveData!=null) {
					if(apiKey==null && apiSecret==null || signature.equalsIgnoreCase(inputSignature)) 
						isLegalSignature = true;
					if(isLegalSignature) {
						StringBuilder sb = new StringBuilder();
						try {
							JSONArray dataBlocks = receiveData.getJSONArray("data");
							for(int j=0; j<dataBlocks.length(); j++) {
								JSONObject block = dataBlocks.getJSONObject(j); 
								Iterator<String> keyIterator1 = block.keys();
								if(sb.length()>0) sb.append("\n==========\n");
								while(keyIterator1.hasNext()) {
									String fieldKey = keyIterator1.next();
									JSONArray array = block.getJSONArray(fieldKey);
									sb.append(j+1).append(". ").append(fieldKey);
									for(int i=0; i<array.length(); i++) {
										JSONObject jObj = array.getJSONObject(i);		
										StringBuilder tmpStringBuilder = new StringBuilder();
										for(int k=0; k<jObj.length(); k++) {
											String dataKey = "col"+(k+1);
											if(tmpStringBuilder.length()>0) tmpStringBuilder.append(", ");
											tmpStringBuilder.append(jObj.get(dataKey));
										}
										sb.append("\n").append(tmpStringBuilder);						
									}
								};
							}
							returnData.put("code", 0).put("message", "success");
						} catch(JSONException e) {
							Log.warn(UtilsHelper.getExceptionStackTraceMessage(e));
							returnData.put("code", 4).put("message", "JSON內部格式不完整");
						}
						Storage.setData(StorageName.CompareRateData, receiveData);
						JSONArray receivers = new JSONArray(App.env.get("telegram.api.default.receiver","[]"));
						if(sb.length()>0) {
							receivers.forEach(obj->{
								JSONObject tgData = new JSONObject() {{
									put("chat_id", Long.valueOf(obj.toString()));
									put("text", sb.toString());
								}};
								new TelegramPushTask(tgData).start();							
							});
						}
					}
					else
						returnData.put("code", -2).put("message", "signature invaild");
				} //endif(receiveData!=null)
				else 
					returnData.put("code", -3).put("message", "\"data\" only accept json formatted");
			}
			else
				returnData.put("code", -1).put("message", "no matched X-PYTHON-DATA-TOKEN");
			Log.debug("returnData: "+returnData);
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, returnData.toString().length());
			PrintWriter out = new PrintWriter(new BufferedOutputStream(exchange.getResponseBody()));
			out.print(returnData.toString());
			out.flush();
			exchange.close();
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			exchange.sendResponseHeaders(500, 0);
			exchange.close();
		}

	}
	
	public static synchronized void setData(JSONObject _data) {
		recordData = _data;
	}
	
	public static synchronized JSONObject getData() {
		return recordData;
	}

}
