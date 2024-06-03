package org.cryptotrade.api.ftx;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.helper.HttpHelper;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONObject;

public class FtxApi {
	private static Logger Log = LogManager.getLogger();
	public static final String BaseUrl = "https://ftx.com";
	public static final String BaseApiPath = "/api";
	protected String subApiPath;
	protected String apiKey;
	protected String apiSecret;
	protected String subAccountName;
	
	public FtxApi(String _apiKey, String _apiSecret, String _subApiPath) {
		this(_apiKey,_apiSecret,null,_subApiPath);
	}
	
	public FtxApi(String _apiKey, String _apiSecret,String _subAccountName, String _subApiPath) {
		this.apiKey = _apiKey;
		this.apiSecret = _apiSecret;
		this.subApiPath = _subApiPath;
		this.subAccountName = _subAccountName;
	}
	
	/**
	 * 指定使用子帳戶(該子帳戶必須存在)
	 * @param _subAccountName 如果要使用主帳戶,將該參數給null
	 */
	public void setSubAccount(String _subAccountName) {
		this.subAccountName = _subAccountName;
	}
	
	protected JSONObject get(String _endPoint) {
		String url = BaseUrl + BaseApiPath + this.subApiPath + _endPoint;
		Map<String,String> header = getSignatureHeader(_endPoint, "", "get");
		Map<String,Map<String,String>> option = new HashMap<>() {{
			put("header", header);
		}};
		
		try {
			HttpResponse<String> response = HttpHelper.Get(url, option);
			return new JSONObject(response.body());
		} catch (Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return null;
	}
	
	protected JSONObject post(String _endPoint) {
		return post(_endPoint,"");
	}
	
	protected JSONObject post(String _endPoint, String _data) {
		String url = BaseUrl + BaseApiPath + this.subApiPath + _endPoint;
		Map<String,String> header = getSignatureHeader(_endPoint, _data, "post");
		header.put("Content-Type", HttpHelper.ContentType_JSON);
		Map<String,String> data = new HashMap<>() {{
			put("__json_data__", _data);
		}};
		Map<String,Map<String,String>> option = new HashMap<>() {{
			put("header", header);
			put("data", data);
		}};
		
		try {
			Log.debug("POST option: "+option);
			HttpResponse<String> response = HttpHelper.Post(url, option);
			return new JSONObject(response.body());
		} catch (Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return null;
	}
	
	protected JSONObject delete(String _endPoint) {
		return delete(_endPoint,"");
	}
	
	protected JSONObject delete(String _endPoint, String _data) {
		String url = BaseUrl + BaseApiPath + this.subApiPath + _endPoint;
		Map<String,String> header = getSignatureHeader(_endPoint, _data, "delete");
		header.put("Content-Type", HttpHelper.ContentType_JSON);
		Map<String,String> data = new HashMap<>() {{
			put("__json_data__", _data);
		}};
		Map<String,Map<String,String>> option = new HashMap<>() {{
			put("header", header);
			put("data", data);
		}};
		try {
			HttpResponse<String> response = HttpHelper.Delete(url, option);
			return new JSONObject(response.body());
		} catch (Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return null;
	}
	
	private Map<String,String> getSignatureHeader(String _endPoint, String _data, String _method){
		Long timestamp = System.currentTimeMillis();
		String method = _method.toUpperCase();
		String apiPath = BaseApiPath + this.subApiPath + _endPoint + _data;
		Log.debug(apiPath);
		StringBuilder signaturePayload = new StringBuilder();
		signaturePayload.append(timestamp).append(method).append(apiPath);
		Log.debug("signaturePayload: "+signaturePayload);
		String signature = UtilsHelper.SHA256(signaturePayload.toString(), apiSecret);
		Log.debug("signature: "+signature);
		
		Map<String,String> header = new HashMap<>() {{
			put("FTX-KEY", apiKey);
			put("FTX-TS", String.valueOf(timestamp));
			put("FTX-SIGN", signature);
		}};
		if(subAccountName!=null && !subAccountName.equalsIgnoreCase("main"))
			header.put("FTX-SUBACCOUNT", URLEncoder.encode(subAccountName, Charset.forName("UTF-8")));
		Log.debug("header: "+header.toString());
		return header;
	}
}
