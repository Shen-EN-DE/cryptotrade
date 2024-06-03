package org.cryptotrade.api.ftx;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class FtxOrders extends FtxApi {	
	private static Logger Log = LogManager.getLogger();

	public FtxOrders(String _apiKey, String _apiSecret) {
		super(_apiKey, _apiSecret,"");
		// TODO Auto-generated constructor stub
	}
	
	public FtxOrders(String _apiKey, String _apiSecret,String _subAccountName) {
		super(_apiKey,_apiSecret,_subAccountName,"");
		Log.debug("apiKey: "+apiKey+", apiSecret: "+_apiSecret);
	}

	/**
	 * 下單
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * market - String : 交易標的,Ex 現貨BTC/USD、期貨BTC-PERP<br>
	 * side - String : 交易方向, buy, sell<br>
	 * price - Double : 交易價格,設定null表示使用市場價格<br>
	 * type - String : 類型, market或limit<br>
	 * size - Double : 交易量<br>
	 * reduceOnly - Boolean : (選填)只減少部位, API預設false<br>
	 * ioc - Boolean : (選填)API預設false<br>
	 * postOnly - Boolean : (選填)API預設false<br>
	 * clientId - String : (選填)客製訂單ID, API預設NULL<br>
	 * rejectOnPriceBand - Boolean : (選填) API預設false<br>
	 * rejectAfterTs - Long : (選填)API預設false<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject placeOrder(Map<String,Object> _option) throws Exception {
		JSONObject json = null;
		String endPoint = "/orders";
		Object obj = null;
		
		// 確認是否有必填參數
		String[] mustOption = new String[] {"market","side","price","type","size"};
		String errorString = "";
		for(String optionName : mustOption) {
			obj = _option.get(optionName);
			if(obj==null) errorString += (errorString.isEmpty() ? "" : ",")+optionName;
		}
		if(errorString.length()>0) throw new Exception("缺少右列必要參數: "+errorString);
		
		JSONObject params = new JSONObject();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "market":
			case "side":
			case "type":
			case "clientId":
				if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
				break;
			case "size":
			case "price":
				if(!(value instanceof Double) && !key.equalsIgnoreCase("price")) throw new Exception(key+"的型別必須是Double");
				break;
			case "reduceOnly":
			case "ioc":
			case "postOnly":
			case "rejectOnPriceBand":
				if(!(value instanceof Boolean) ) throw new Exception(key+"的型別必須是Boolean");
				break;
			case "rejectAfterTs":
				if(!(value instanceof Long) ) throw new Exception(key+"的型別必須是Long");
				break;				
			}
			params.put(key, value);
		}
		Log.debug("params: "+params);
		return post(endPoint,params.toString());
	}

	/**
	 * 取得特定單號的狀態
	 * @param _orderId 單號,下單時可取得
	 * @return JSONObject
	 */
	public JSONObject getOrderStatus(long _orderId) {
		JSONObject json = null;
		String endPoint = "/orders/"+_orderId;
		json = get(endPoint);
		return json;
	}
	
	/**
	 * 取得掛單
	 * @param market (選填)指定交易兌;設null取得所有掛單
	 * @return
	 */
	public JSONObject getOpenOrders(String market) {
		JSONObject json = null;
		String endPoint = "/orders";
		if(market!=null)
			endPoint += "?market="+market;
		json = get(endPoint);
		return json;
	}
	
	public JSONObject cancelOrder(long _orderId) {
		JSONObject json = null;
		String endPoint = "/orders/"+_orderId;
		json = delete(endPoint);
		return json;
	}
	
	public JSONObject cancelAllOrders(String _market,String _side,Boolean _conditionalOrdersOnly,Boolean _limitOrdersOnly) {
		JSONObject json = null;
		String endPoint = "/orders";
		JSONObject data = new JSONObject() {{
			put("market", _market);
			put("side", _side);
			put("conditionalOrdersOnly", _conditionalOrdersOnly);
			put("limitOrdersOnly", _limitOrdersOnly);
		}};
		json = delete(endPoint,data.toString());
		
		return json;
	}
	
	/**
	 * 
	 * @param _option
	 * <ul>
	 * market - String : 交易標的,Ex 現貨BTC/USD、期貨BTC-PERP<br>
	 * start_time - Long : 開始時間timestamp<br>
	 * end_time - Long : 結束時間timestamp<br>
	 * order - String : 排序方式,預設是desc,可使用'asc'<br>
	 * orderId - Long : 針對特定訂單編號<br>
	 * </ul>
	 * @return
	 */
	public JSONObject getFills(Map<String,Object> _option) {
		JSONObject json = null;
		String endPoint = "/fills";
		StringBuilder query = new StringBuilder();
		_option.forEach((name,value)->{
			if(query.length()>0) query.append("&");
			query.append(name).append("=").append(URLEncoder.encode(value.toString(),Charset.forName("UTF-8")));
		});
		if(query.length()>0) query.insert(0, "?");
		json = get(endPoint+query.toString());
		return json;
	}
}
