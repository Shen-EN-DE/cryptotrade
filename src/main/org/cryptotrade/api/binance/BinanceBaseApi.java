package org.cryptotrade.api.binance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.helper.HttpHelper;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BinanceBaseApi {
	private static Logger Log = LogManager.getLogger();

	/**
	 * 在繼承的子類別中賦值
	 */
	protected String baseUrl;

	/**
	 * 在繼承的子類別中賦值
	 */
	protected String baseEndPointUrl;
	

	/**
	 * 在繼承的子類別中賦值
	 */
	protected String apiKey;

	/**
	 * 在繼承的子類別中賦值
	 */
	protected String apiSecret;
		
	/**
	 * 使用GET方法取得資料
	 * @param _path API的EndPoint(url最尾端的EndPoint)
	 * @param _data 給定Query資料
	 * @param _isSignature 是否要使用Binance的API加密方式
	 * @return JSONTokener|String 如果HttpRequest正常回傳,回傳JSONTokener;反之如果失敗回傳String(內有錯誤訊息)
	 */
	protected Object get(String _path, Map<String, String> _data, boolean _isSignature) {
		Map<String,String> data = _data;
		if(_isSignature) {
			String signaturePayload = HttpHelper.GetHttpQueryString(data);
			String signature = UtilsHelper.SHA256(signaturePayload, this.apiSecret);
			data.put("signature", signature);
		}
		
		Map<String,String> header = new HashMap<>() {{
			put("X-MBX-APIKEY", apiKey);
		}};
		
		Map<String,Map<String,String>> option = new HashMap<>() {{
			put("header", header);
			put("query",data);
		}};		
		
		String url = baseUrl+baseEndPointUrl+_path;
		String errorMsg = "";
		try {
			HttpResponse<String> response = HttpHelper.Get(url, option);
			Log.debug("response.body(): "+response.body());
			return new JSONTokener(response.body());
		} catch (IOException | InterruptedException e) {
			errorMsg = e.getMessage();
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return errorMsg;
	}

	/**
	 * 使用POST方法取得或傳送資料
	 * @param _path API的EndPoint(url最尾端的EndPoint)
	 * @param _data 給定資料
	 * @return JSONTokener|String 如果HttpRequest正常回傳,回傳JSONTokener;反之如果失敗回傳String(內有錯誤訊息)
	 */
	protected Object post(String _url, Map<String, String> _data) {
		Map<String,String> data = _data;
		String signaturePayload = HttpHelper.GetHttpQueryString(data);
		String signature = UtilsHelper.SHA256(signaturePayload, this.apiSecret);
		data.put("signature", signature);
		
		Map<String,String> header = new HashMap<>() {{
			put("Content-Type", HttpHelper.ContentType_FromData);
			put("X-MBX-APIKEY", apiKey);
		}};
		
		Map<String,Map<String,String>> option = new HashMap<>() {{
			put("header", header);
			put("data",data);
		}};		
		
		String url = baseUrl+baseEndPointUrl+_url;
		String errorMsg = "";
		try {
			HttpResponse<String> response = HttpHelper.Post(url, option);
			Log.debug("response.body(): "+response.body());
			return new JSONTokener(response.body());
		} catch (IOException | InterruptedException e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return errorMsg;
	}

	/**
	 * 使用DELETE方法取得或傳送資料
	 * @param _path API的EndPoint(url最尾端的EndPoint)
	 * @param _data 給定資料
	 * @return JSONTokener|String 如果HttpRequest正常回傳,回傳JSONTokener;反之如果失敗回傳String(內有錯誤訊息)
	 */
	protected Object delete(String _url,Map<String,String> _data) {
		Map<String,String> data = _data;
		String signaturePayload = HttpHelper.GetHttpQueryString(data);
		String signature = UtilsHelper.SHA256(signaturePayload, this.apiSecret);
		data.put("signature", signature);
		
		Map<String,String> header = new HashMap<>() {{
			put("Content-Type", HttpHelper.ContentType_FromData);
			put("X-MBX-APIKEY", apiKey);
		}};
		
		Map<String,Map<String,String>> option = new HashMap<>() {{
			put("header", header);
			put("data",data);
		}};		
		
		String url = baseUrl+baseEndPointUrl+_url;
		String errorMsg = "";
		try {
			HttpResponse<String> response = HttpHelper.Delete(url, option);
			Log.debug("response.body(): "+response.body());
			return new JSONTokener(response.body());
		} catch (IOException | InterruptedException e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return errorMsg;
	}
	
	protected Object get(String _url, Map<String, String> _data) {
		return this.get(_url, _data, true);
	}
	
	private String getSignature(String message) {		
    var signature = "";

		try {
			var sha256_HMAC = Mac.getInstance("HmacSHA256");
	    SecretKeySpec secret_key = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
	    sha256_HMAC.init(secret_key);
	    signature = byteArrayToHexString(sha256_HMAC.doFinal(message.getBytes()));// 重点
	    System.out.println(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return signature;
	}
	
	private String byteArrayToHexString(byte[] b) {
		StringBuilder hs = new StringBuilder();
		String stmp;
		for (int n = 0; b!=null && n < b.length; n++) {
			stmp = Integer.toHexString(b[n] & 0XFF);
			if (stmp.length() == 1)
				hs.append('0');
			hs.append(stmp);
		}
		return hs.toString().toLowerCase();
	}
	
}
