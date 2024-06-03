package org.cryptotrade.api.binance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BinanceUBaseContract extends BinanceBaseApi {
	private static Logger Log = LogManager.getLogger();
	
	public BinanceUBaseContract(String _apiKey,String _apiSecret) {
		this.apiKey = _apiKey;
		this.apiSecret = _apiSecret;
		this.baseUrl = "https://fapi.binance.com";
		this.baseEndPointUrl = "/fapi";
	}

	/**
	 * 下單
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * side - String : 交易方向, buy, sell<br>
	 * positionSide - String : (選填)持仓方向，单向持仓模式下非必填，默认且仅可填BOTH;在双向持仓模式下必填,且仅可选择 LONG 或 SHORT<br>
	 * type - String : 订单类型 LIMIT, MARKET, STOP, TAKE_PROFIT, STOP_MARKET, TAKE_PROFIT_MARKET, TRAILING_STOP_MARKET<br>
	 * reduceOnly - Boolean : (選填)true, false; 非双开模式下默认false；双开模式下不接受此参数； 使用closePosition不支持此参数。<br>
	 * quantity - Double : 基礎貨幣交易量<br>
	 * price - Double : 交易價格,設定null表示使用市場價格<br>
	 * newClientOrderId - String : (選填)客户自定义的唯一订单ID。 如果未发送，则自动生成<br>
	 * stopPrice - Double : (選填)仅 STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, 和TAKE_PROFIT_LIMIT 需要此参数<br>
	 * closePosition - Boolean : (選填)	true, false；触发后全部平仓，仅支持STOP_MARKET和TAKE_PROFIT_MARKET；不与quantity合用；自带只平仓效果，不与reduceOnly 合用<br>
	 * activationPrice - Double : (選填)追踪止损激活价格，仅TRAILING_STOP_MARKET 需要此参数, 默认为下单当前市场价格(支持不同workingType)<br>
	 * callbackRate - Double : (選填)追踪止损回调比例，可取值范围[0.1, 5],其中 1代表1% ,仅TRAILING_STOP_MARKET 需要此参数<br>
	 * timeInForce - String : (選填)交易模式: GTC,IOC,FOK,<br>
	 * workingType - String : (選填)stopPrice 触发类型: MARK_PRICE(标记价格), CONTRACT_PRICE(合约最新价). 默认 CONTRACT_PRICE<br>
	 * priceProtect - Boolean : (選填)条件单触发保护："TRUE","FALSE", 默认"FALSE". 仅 STOP, STOP_MARKET, TAKE_PROFIT, TAKE_PROFIT_MARKET 需要此参数<br>
	 * newOrderRespType - String : (選填) 设置响应JSON。 ACK，RESULT或FULL； "MARKET"和" LIMIT"订单类型默认为"FULL"，所有其他订单默认为"ACK"<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject setOrder(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/v1/order";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
				case "symbol":
				case "side":
				case "type":
				case "positionSide":
				case "timeInForce":
				case "closePosition":
				case "newClientOrderId":
				case "newOrderRespType":
				case "workingType":
				case "priceProtect":
					if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
					params.put(key, value.toString());
					break;
				case "price":
				case "quantity":
				case "stopPrice":
				case "activationPrice":
				case "callbackRate":
					if(!(value instanceof Double) && !key.equalsIgnoreCase("price")) throw new Exception(key+"的型別必須是Double");
					params.put(key, String.format("%f",value));
					break;
				case "reduceOnly":
					if(!(value instanceof Boolean) ) throw new Exception(key+"的型別必須是Boolean");
					params.put(key, value.toString());
					break;
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
		Object obj = post(endPoint, params);
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
	 * origClientOrderId - String : (選填)用户自定义的订单号<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject deleteOrder(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/v1/order";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "symbol":
			case "origClientOrderId":
				if(!(value instanceof String) ) throw new Exception(key+"的型別必須是String");
				break;
			case "orderId":
				if(!(value instanceof Long) ) throw new Exception(key+"的型別必須是Long");
				break;				
			}
			params.put(key, value.toString());
		}
		params.put("recvWindow", "10000");
		params.put("timestamp", System.currentTimeMillis()+"");
		Log.debug("params: "+params);
		Object obj = delete(endPoint, params);
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
	 * 查詢執行中的掛單
	 * 请注意，如果订单满足如下条件，不会被查询到：
	 * <ul>
	 * <li>订单的最终状态为 CANCELED 或者 EXPIRED, 并且</li>
	 * <li>订单没有任何的成交记录, 并且</li>
	 * <li>订单生成时间 + 7天 < 当前时间</li>
	 * </ul>
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * orderId - Long: (選填)訂單ID<br>
	 * origClientOrderId - String : (選填)用户自定义的订单号<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject getOpenOrder(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/v1/openOrder";
		Map<String,String> params = new HashMap<>();
		for(Entry<String, Object> entry : _option.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch(key) {
			case "symbol":
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
		Object obj = get(endPoint, params);
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
	 * 查詢所有訂單
	 * 请注意，如果订单满足如下条件，不会被查询到：
	 * <ul>
	 * <li>订单的最终状态为 CANCELED 或者 EXPIRED, 并且</li>
	 * <li>订单没有任何的成交记录, 并且</li>
	 * <li>订单生成时间 + 7天 < 当前时间</li>
	 * </ul>
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * symbol - String : 交易標的,Ex BTCUSDT<br>
	 * orderId - Long: (選填)訂單ID<br>
	 * startTime - Long : (選填)起始时间<br>
	 * endTime - Long : (選填)结束时间<br>
	 * limit - Long : (選填)返回的结果集数量 默认值:500 最大值:1000<br>
	 * recvWindow - Long : (選填)限制交易所伺服器等待執行時間,赋值不能大于 60000<br>
	 * timestamp - Long : (選填)<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 如果必填選項未填,或是參數型別錯誤
	 */
	public JSONObject getAllOrders(Map<String,Object> _option) throws Exception {
		JSONObject json = new JSONObject();
		String endPoint = "/v1/allOrders";
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
		Object obj = get(endPoint, params);
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
