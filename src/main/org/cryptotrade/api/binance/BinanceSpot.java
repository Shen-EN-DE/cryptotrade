package org.cryptotrade.api.binance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BinanceSpot extends BinanceBaseApi {
	private static Logger Log = LogManager.getLogger();
	
	public BinanceSpot(String _apiKey,String _apiSecret) {
		this.apiKey = _apiKey;
		this.apiSecret = _apiSecret;
		this.baseUrl = "https://api.binance.com";
		this.baseEndPointUrl = "/api/v3";
	}
	
	public JSONObject testOrder() {
		Map<String,String> params = new HashMap<>();
		params.put("symbol", "BTCUSDT");
		params.put("type", "market");
		params.put("quantity", "0.001");
		params.put("side", "buy");
		params.put("recvWindow", "10000");
		params.put("timestamp", System.currentTimeMillis()+"");
		JSONObject json = new JSONObject();
		Object obj = post("/order/test", params);
		if(obj instanceof JSONTokener) {
			JSONTokener tokener = (JSONTokener)obj;
			json.put("success", true);
			json.put("result", tokener.nextValue());
		}
		else {
			json.put("success", false);
			json.put("result", obj);
		}
		return json;
	}

	/**
	 * 下單
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * side - String : 交易方向, buy, sell<br>
	 * type - String : 類型, market或limit<br>
	 * timeInForce - String : (選填)交易模式: GTC,IOC,FOK,<br>
	 * quantity - Double : 基礎貨幣交易量<br>
	 * quoteOrderQty - Double : 計價貨幣交易量<br>
	 * price - Double : 交易價格,設定null表示使用市場價格<br>
	 * newClientOrderId - String : (選填)客户自定义的唯一订单ID。 如果未发送，则自动生成<br>
	 * stopPrice - Double : (選填)仅 STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, 和TAKE_PROFIT_LIMIT 需要此参数<br>
	 * trailingDelta - Long : (選填)用于 STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, 和 TAKE_PROFIT_LIMIT 类型的订单. 更多追踪止盈止损订单细节<br>
	 * icebergQty - Double : (選填)仅使用 LIMIT, STOP_LOSS_LIMIT, 和 TAKE_PROFIT_LIMIT 创建新的 iceberg 订单时需要此参数<br>
	 * newOrderRespType - String : (選填) 设置响应JSON。 ACK，RESULT或FULL； "MARKET"和" LIMIT"订单类型默认为"FULL"，所有其他订单默认为"ACK"<br>
	 * strategyId - Long : (選填)<br>
	 * strategyType - Long : (選填)不能低于 1,000,000<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject setOrder(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/order";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "symbol":
				case "side":
				case "type":
				case "timeInForce":
				case "newClientOrderId":
				case "newOrderRespType":
					if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
					params.put(key, value.toString());
					break;
				case "price":
				case "quantity":
				case "quoteOrderQty":
				case "stopPrice":
				case "icebergQty":
					if(!(value instanceof Double) && !key.equalsIgnoreCase("price")) throw new Exception(key+"的型別必須是Double");
					params.put(key, String.format("%f", value));
					break;
				case "rejectOnPriceBand":
					if(!(value instanceof Boolean) ) throw new Exception(key+"的型別必須是Boolean");
					params.put(key, value.toString());
					break;
				case "trailingDelta":
				case "strategyId":
				case "strategyType":
				case "recvWindow":
				case "timestamp":
					if(!(value instanceof Long) ) throw new Exception(key+"的型別必須是Long");
					params.put(key, value.toString());
					break;				
			}
		}
		if(!params.containsKey("recvWindow")) params.put("recvWindow", "10000");
		if(!params.containsKey("timestamp")) params.put("timestamp", System.currentTimeMillis()+"");
		Log.debug("params: "+params);
		Object obj =  post(endPoint, params);
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

	/**
	 * 刪除掛單
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * orderId - Long: (選填)訂單ID<br>
	 * origClientOrderId - String : (選填)<br>
	 * newClientOrderId - String : (選填)用户自定义的本次撤销操作的ID(注意不是被撤销的订单的自定义ID)。如无指定会自动赋值。<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject deleteOrder(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/order";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "symbol":
			case "newClientOrderId":
			case "origClientOrderId":
				if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
				break;
			case "orderId":
			case "recvWindow":
			case "timestamp":
				if(!(value instanceof Long) ) throw new Exception(key+"的型別必須是Long");
				break;				
			}
			params.put(key, value.toString());
		}
		if(!params.containsKey("recvWindow")) params.put("recvWindow", "10000");
		if(!params.containsKey("timestamp")) params.put("timestamp", System.currentTimeMillis()+"");
		Log.debug("params: "+params);
		Object obj =  delete(endPoint, params);
		if(obj instanceof JSONTokener) {
			JSONTokener tokener = (JSONTokener)obj;
			json.put("success", true);
			json.put("result", tokener.nextValue());
		}
		else {
			json.put("success", false);
			json.put("result", obj);
		}
		return json;
	}
	

	/**
	 * 查詢訂單
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * orderId - Long: (選填)訂單ID<br>
	 * startTime - Long : (選填)<br>
	 * endTime - Long : (選填)用户自定义的本次撤销操作的ID(注意不是被撤销的订单的自定义ID)。如无指定会自动赋值。<br>
	 * limit - Long : (選填)用户自定义的本次撤销操作的ID(注意不是被撤销的订单的自定义ID)。如无指定会自动赋值。<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject getAllOrders(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/allOrders";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "symbol":
				if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
				break;
			case "orderId":
			case "startTime":
			case "endTime":
			case "limit":
			case "recvWindow":
			case "timestamp":
				if(!(value instanceof Long) ) throw new Exception(key+"的型別必須是Long");
				break;				
			}
			params.put(key, value.toString());
		}
		if(!params.containsKey("recvWindow")) params.put("recvWindow", "10000");
		if(!params.containsKey("timestamp")) params.put("timestamp", System.currentTimeMillis()+"");
		Log.debug("params: "+params);
		Object obj =  get(endPoint, params);
		if(obj instanceof JSONTokener) {
			JSONTokener tokener = (JSONTokener)obj;
			json.put("success", true);
			json.put("result", tokener.nextValue());
		}
		else {
			json.put("success", false);
			json.put("result", obj);
		}
		return json;
	}

	/**
	 * 查詢交易單
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * orderId - Long: (選填)訂單ID<br>
	 * startTime - Long : (選填)<br>
	 * endTime - Long : (選填)用户自定义的本次撤销操作的ID(注意不是被撤销的订单的自定义ID)。如无指定会自动赋值。<br>
	 * fromId - Long : (選填)获取TradeId，默认获取近期交易历史<br>
	 * limit - Long : (選填)用户自定义的本次撤销操作的ID(注意不是被撤销的订单的自定义ID)。如无指定会自动赋值。<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject getMyTrades(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/myTrades";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "symbol":
				if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
				break;
			case "orderId":
			case "startTime":
			case "endTime":
			case "fromId":
			case "limit":
			case "recvWindow":
			case "timestamp":
				if(!(value instanceof Long) ) throw new Exception(key+"的型別必須是Long");
				break;				
			}
			params.put(key, value.toString());
		}
		if(!params.containsKey("recvWindow")) params.put("recvWindow", "10000");
		if(!params.containsKey("timestamp")) params.put("timestamp", System.currentTimeMillis()+"");
		Log.debug("params: "+params);
		Object obj =  get(endPoint, params);
		if(obj instanceof JSONTokener) {
			JSONTokener tokener = (JSONTokener)obj;
			json.put("success", true);
			json.put("result", tokener.nextValue());
		}
		else {
			json.put("success", false);
			json.put("result", obj);
		}
		return json;
	}
}
