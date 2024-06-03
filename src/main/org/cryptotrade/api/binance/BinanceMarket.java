package org.cryptotrade.api.binance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BinanceMarket extends BinanceBaseApi {
	private static Logger Log = LogManager.getLogger();
	public BinanceMarket(String _apiKey,String _apiSecret) {
		this.apiKey = _apiKey;
		this.apiSecret = _apiSecret;
		this.baseEndPointUrl = "";
	}
	
	public JSONObject exchangeSpotMarginInfo(List<String> _symbolList) {
		this.baseUrl = "https://api.binance.com";
		JSONObject json = new JSONObject();
		String endPoint = "/api/v3/exchangeInfo";
		Map<String,String> data = new HashMap();
		if(_symbolList!=null) {
			if(_symbolList.size()==1) {
				data.put("symbol", _symbolList.get(0));
			}
			else {
				String symbols = "";
				for(String symbol : _symbolList) {
					if(symbols.length()>0) symbols += "\",\"";
					symbols += symbol;
				}
				if(symbols.length()>0) {
					symbols = "[\""+symbols+"\"]";
					data.put("symbols", symbols);
				}
			}
		}
		Object obj = get(endPoint,data,false);
		if(obj instanceof JSONTokener) {
			JSONTokener tokener = (JSONTokener)obj;
			Object valueObject = tokener.nextValue();
			if(valueObject instanceof JSONObject) {
				JSONObject value = (JSONObject) valueObject;
				if(!value.isNull("code") && value.getInt("code")!=0)
					json.put("success", false);
				if(!json.has("success")) json.put("success", true);
				json.put("result", value);
			}
			else {
				json.put("success", true);
				json.put("result", obj);				
			}
		}
		else {
			json.put("success", false);
			json.put("result", obj);
		}
		return json;		
	}
	
	public JSONObject exchangeUsdContractInfo(List<String> _symbolList) {
		this.baseUrl = "https://fapi.binance.com";
		JSONObject json = new JSONObject();
		String endPoint = "/fapi/v1/exchangeInfo";
		Map<String,String> data = new HashMap();
		Object obj = get(endPoint,data,false);
		if(obj instanceof JSONTokener) {
			JSONTokener tokener = (JSONTokener)obj;
			Object valueObject = tokener.nextValue();
			if(valueObject instanceof JSONObject) {
				JSONObject value = (JSONObject) valueObject;
				if(!value.isNull("code") && value.getInt("code")!=0)
					json.put("success", false);
				if(!json.has("success")) {
					json.put("success", true);
					if(_symbolList!=null) {
						JSONArray jsonArray = new JSONArray();
						JSONArray orginArray = value.getJSONArray("symbols");
						for(String symbol : _symbolList) {
							for(int i=0; i<orginArray.length(); i++) {
								JSONObject symbolObject = orginArray.getJSONObject(i);
								if(symbolObject.getString("symbol").equalsIgnoreCase(symbol))
									jsonArray.put(symbolObject);
							}
						}
						value.put("symbols", jsonArray);
					}
				}
				json.put("result", value);
			}
			else {
				json.put("success", true);
				json.put("result", obj);
			}
		}
		else {
			json.put("success", false);
			json.put("result", obj);
		}
		return json;		
	}

}
