package org.cryptotrade.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.api.binance.BinanceMargin;
import org.cryptotrade.api.binance.BinanceSpot;
import org.cryptotrade.api.binance.BinanceUBaseContract;
import org.cryptotrade.api.ftx.FtxOrders;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrderStatusTraceScheduledTask extends ScheduledTask {
	private static Logger Log = LogManager.getLogger();
	public static enum BundleField{Receiver,ReplyTo,OrderId,Symbol,User};
	private Object apiObject;
	private Map<BundleField,Object> bundleData;
	private JSONObject defaultTgData;
	private JSONObject user;
	private JSONObject syncData = new JSONObject() {{
		put("id", -1);
		put("size", 0);
		put("side", "");
		put("price", 0d);
		put("filledSize", 0d);
		put("remainSize", 0d);
		put("avgFillPrice", JSONObject.NULL);
	}};
	
	/**
	 * @param _apiObject
	 * @param _bundleData <br>
	 * <ul>
	 * <li>receiver : String - Telegram的chat_id</li>
	 * <li>replyTo : Long - (選) Telegram的message_id</li>
	 * <li>orderId : Long</li>
	 * <li>symbol : String : 交易標的,Binance交易所必須要指定</li>
	 * <li>user : JSONObject - Telegram的User物件</li>
	 * </ul>
	 */
	public OrderStatusTraceScheduledTask(Object _apiObject,Map<BundleField,Object> _bundleData) {
		this.apiObject = _apiObject;
		this.bundleData = _bundleData;
		this.user = (JSONObject)_bundleData.get(BundleField.User);
		this.defaultTgData = new JSONObject() {{
			put("chat_id", _bundleData.get(BundleField.Receiver));
			put("reply_to_message_id", _bundleData.get(BundleField.ReplyTo));
		}};
//		Log.debug("defaultTgData: "+defaultTgData);
	}
	
	public OrderStatusTraceScheduledTask(OrderStatusTraceScheduledTask _origin) {
		this.bundleData = _origin.bundleData;
		this.apiObject = _origin.apiObject;
		this.syncData = _origin.syncData;
		this.defaultTgData = _origin.defaultTgData;
	}
	
	public void setReplyTo(String _messageId) {
		bundleData.put(BundleField.ReplyTo, _messageId);
		defaultTgData.put("reply_to_message_id", _messageId);
	}
	
	public String changeReceiver(Object _newReceiver) {
		return bundleData.put(BundleField.Receiver,_newReceiver).toString();
	}
	
	@Override
	public String getName() {
		String name = bundleData.get(BundleField.OrderId).toString();
		if(apiObject instanceof FtxOrders) name = "Ftx-"+name;
		if(	apiObject instanceof BinanceSpot ||
				apiObject instanceof BinanceMargin ||
				apiObject instanceof BinanceUBaseContract) 
			name = "Binance-"+name;
		return name;
	}
	
	@Override
	public void start(long _delay, long _period, TimeUnit _unit) {
		super.start(_delay, _period, _unit);
		if(apiObject instanceof FtxOrders) Log.info("訂單追蹤("+getName()+")開始");		
	}

	@Override
	public void run() {
		try {
			if(apiObject instanceof FtxOrders) ftx();
			if(	apiObject instanceof BinanceSpot ||
					apiObject instanceof BinanceUBaseContract ||
					apiObject instanceof BinanceMargin) 
				binance();
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}
	
	private void ftx() {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		FtxOrders ftxOrders = (FtxOrders)apiObject;
		JSONObject result = ftxOrders.getOrderStatus(Long.valueOf(bundleData.get(BundleField.OrderId).toString()));
		Log.debug("ftx result: "+result);
		JSONObject tgData = new JSONObject(this.defaultTgData.toString()); 
		StringBuilder sb = new StringBuilder();
		if(!result.getBoolean("success")) {
			tgData.put("text", result.toString());
			Log.error("FTX API回傳錯誤: "+result.toString());
		}
		else{
			result = result.getJSONObject("result");
			Double remainingSize = result.getDouble("remainingSize");
			Double preRemainingSize = remainingSize;
			synchronized(syncData) {
				preRemainingSize = syncData.getDouble("remainSize");
				syncData.put("id", result.get("id"))
					.put("size", result.get("size"))
					.put("price", result.get("price"))
					.put("avgFillPrice", result.get("avgFillPrice"))
					.put("remainSize", result.get("remainingSize"))
					.put("filledSide", result.get("filledSize"));
			}
			if(remainingSize>0) {
				if(result.getString("status").equalsIgnoreCase("closed")) {
					sb.append("訂單(").append(result.get("id")).append(")未交易完成,已取消");
					sb.append("\n掛單價格: ").append(result.get("price"));
					sb.append("\n做單方向: ").append(result.get("side"));
					sb.append("\n交易標的: ").append(result.get("market"));
					sb.append("\n訂單總量: ").append(String.format("%f",result.getDouble("size")));
					sb.append("\n成交總量: ").append(String.format("%f",result.getDouble("filledSize")));
					if(!result.isNull("avgFillPrice"))
						sb.append("\n成交均價: ").append(String.format("%f",result.getDouble("avgFillPrice")));
					sb.append("\n未成交量: ").append(String.format("%f",remainingSize));
					Log.info("訂單追蹤("+getName()+")結束");
					finish();
				}
				else {
					if(preRemainingSize!=remainingSize.doubleValue()) {
						sb.append("訂單(").append(result.get("id")).append(")進行中");
						sb.append("\n掛單價格: ").append(result.get("price"));
						sb.append("\n做單方向: ").append(result.get("side"));
						sb.append("\n交易標的: ").append(result.get("market"));
						sb.append("\n訂單總量: ").append(String.format("%f",result.getDouble("size")));
						sb.append("\n成交總量: ").append(String.format("%f",result.getDouble("filledSize")));
						if(!result.isNull("avgFillPrice"))
							sb.append("\n成交均價: ").append(String.format("%f",result.getDouble("avgFillPrice")));
						sb.append("\n未成交量: ").append(String.format("%f",remainingSize));
					}
				}
			}
			else {
				sb.append("訂單(").append(result.get("id")).append(")");
				if(result.getDouble("size")==result.getDouble("filledSize"))
					sb.append("已全數交易完成");
				else {
					sb.append("已被取消,尚有未完成交易");
					sb.append("\n掛單總量: ").append(result.get("size"));
				}
				sb.append("\n掛單價格: ").append(result.get("price"));
				sb.append("\n做單方向: ").append(result.get("side"));
				sb.append("\n交易標的: ").append(result.get("market"));
				sb.append("\n成交總量: ").append(String.format("%f",result.getDouble("filledSize")));
				if(!result.isNull("avgFillPrice"))
					sb.append("\n成交均價: ").append(String.format("%f",result.getDouble("avgFillPrice")));
				Log.info("訂單追蹤("+getName()+")結束");
				finish();
			}
		}
		
		Object dataObj = bundleData.get(BundleField.Receiver);
		String receiver = dataObj!=null ? dataObj.toString() : null;
		if(receiver!=null && !receiver.isEmpty() && sb.length()>0) {
			String text = getName()+"追蹤回報:\n"+sb.toString();
			if(this.user!=null) {
				String userName = this.user.getString("first_name");
				boolean hasUsername = !this.user.isNull("username");
				userName = hasUsername ? this.user.getString("username") : userName;
				text = userName+" "+text;
				JSONArray entities = new JSONArray().put(new JSONObject()
						.put("type", hasUsername ? "mention" : "text_mention")
						.put("user", this.user)
						.put("offset", text.indexOf(userName))
						.put("length", userName.length()));
				tgData.put("entities", entities);
			}
			tgData.put("text", text);
			Log.debug("tgData: "+tgData);
			tgData = telegramApi.sendMessage(tgData);
			Log.debug("tgResult: "+tgData);	
		}
	}
	
	public JSONObject getStatus() {
		JSONObject status = null;
		synchronized(syncData) {
			status = syncData;
		}
		return status;
	}
	
	private void binance() throws Exception {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		Map<String,Object> args = new HashMap<>() {{
			put("symbol", bundleData.get(BundleField.Symbol).toString().toUpperCase());
			put("orderId", Long.valueOf(bundleData.get(BundleField.OrderId).toString()));
		}};
		JSONObject result = null;
		Double avgFillPrice = 0d;
		String marketType = "";
		if(apiObject instanceof BinanceSpot) {
			BinanceSpot spot = (BinanceSpot)apiObject;
			result = spot.getMyTrades(args);
			marketType = "現貨";
		}
		if(apiObject instanceof BinanceMargin) {
			BinanceMargin margin = (BinanceMargin)apiObject;
			result = margin.getMyTrades(args);
			marketType = "槓桿";
		}
		if(apiObject instanceof BinanceUBaseContract) {
			BinanceUBaseContract uContract = (BinanceUBaseContract)apiObject;
			result = uContract.getAllOrders(args);
			marketType = "合約";
		}
		
		JSONObject tgData = new JSONObject(this.defaultTgData.toString());
		StringBuilder sb = new StringBuilder();
		if(!result.getBoolean("success")) {
			tgData.put("text", result.toString());
			Log.error("Binance API回傳錯誤: "+result.toString()+", "+apiObject.toString());
		}
		else{
			// 細部計算現貨訂單的平均成交價
			if(apiObject instanceof BinanceSpot) {
				BinanceSpot spot = (BinanceSpot)apiObject;
				JSONArray jsonArray = result.getJSONArray("result");
				double totalSize=0, totalVolume=0;
				for(int i=0; i<jsonArray.length(); i++) {
					JSONObject trade = jsonArray.getJSONObject(i);
					totalSize += Double.valueOf(trade.get("qty").toString());
					totalVolume += Double.valueOf(trade.get("quoteQty").toString());
				}
				avgFillPrice = totalSize==0 ? 0 : totalVolume/totalSize;
				result = spot.getAllOrders(args);
			}
			// 細部計算槓桿訂單的平均成交價
			if(apiObject instanceof BinanceMargin) {
				BinanceMargin margin = (BinanceMargin)apiObject;
				Log.debug(result);
				JSONArray jsonArray = result.getJSONArray("result");
				double totalSize=0, totalVolume=0;
				for(int i=0; i<jsonArray.length(); i++) {
					JSONObject trade = jsonArray.getJSONObject(i);
					totalSize += Double.valueOf(trade.get("qty").toString());
					totalVolume += Double.valueOf(trade.get("qty").toString())*Double.valueOf(trade.get("price").toString());
				}
				avgFillPrice = totalSize==0 ? 0 : totalVolume/totalSize;
				result = margin.getAllOrders(args);
			}
			
			JSONArray jsonArray = result.getJSONArray("result");
			for(int i=0; i<jsonArray.length(); i++) {
				result = jsonArray.getJSONObject(i);
				if(result.getLong("orderId")!=Long.valueOf(bundleData.get(BundleField.OrderId).toString())) continue;
				
				Double size = Double.valueOf(result.get("origQty").toString());
				Double price = Double.valueOf(result.get("price").toString());
				Double filledSize = Double.valueOf(result.get("executedQty").toString());
				avgFillPrice = result.has("avgPrice") ? Double.valueOf(result.get("avgPrice").toString()) : avgFillPrice;
				Double remainingSize = size - filledSize;
				Double preRemainingSize = remainingSize;
				synchronized(syncData) {
					preRemainingSize = syncData.getDouble("remainSize");
					syncData.put("id", Long.valueOf(result.get("orderId").toString()))
						.put("size", size)
						.put("price", price)
						.put("avgFillPrice", avgFillPrice)
						.put("remainSize", remainingSize)
						.put("filledSize", filledSize);
				};
				if(size-filledSize>0) {
					if(result.getString("status").equalsIgnoreCase("CANCELED")) {
						sb.append(marketType).append("訂單(").append(result.get("orderId")).append(")未交易完成,已取消");
						sb.append("\n交易標的: ").append(result.get("symbol"));
						sb.append("\n做單方向: ").append(result.get("side"));
						sb.append("\n掛單價格: ").append(price);
						sb.append("\n訂單總量: ").append(String.format("%f",size));
						sb.append("\n成交總量: ").append(filledSize);
						sb.append("\n成交均價: ").append(avgFillPrice);
						Log.info("訂單追蹤("+getName()+")結束");
						finish();
					}
					else {
						if(preRemainingSize!=remainingSize.doubleValue()){
							sb.append(marketType).append("訂單(").append(result.get("orderId")).append(")進行中");
							sb.append("\n交易標的: ").append(result.get("symbol"));
							sb.append("\n做單方向: ").append(result.get("side"));
							sb.append("\n掛單價格: ").append(price);
							sb.append("\n訂單總量: ").append(String.format("%f",size));
							sb.append("\n成交總量: ").append(filledSize);
							sb.append("\n成交均價: ").append(avgFillPrice);
							sb.append("\n目前未成交量: ").append(String.format("%f",remainingSize));
						}
					}
				}
				else {
					sb.append(marketType).append("訂單(").append(result.get("orderId")).append(")");
					if(remainingSize==0)
						sb.append("已全數交易完成");
					else {
						sb.append("已被取消,尚有未完成交易");
						sb.append("\n掛單總量: ").append(size);
					}
					sb.append("\n交易標的: ").append(result.get("symbol"));
					sb.append("\n做單方向: ").append(result.get("side"));
					sb.append("\n掛單價格: ").append(String.format("%f",price));
					sb.append("\n訂單總量: ").append(String.format("%f",size));
					sb.append("\n成交總量: ").append(filledSize);
					sb.append("\n成交均價: ").append(avgFillPrice);
					Log.info("訂單追蹤("+getName()+"-"+marketType+")結束");
					finish();
				}
			}
		}
		Object dataObj = bundleData.get(BundleField.Receiver);
		String receiver = dataObj!=null ? dataObj.toString() : null;
		if(receiver!=null && !receiver.isEmpty() && sb.length()>0) {
			String userName = this.user.getString("first_name");
			boolean hasUsername = !this.user.isNull("username");
			userName = hasUsername ? this.user.getString("username") : userName;
			String text = userName+" "+getName()+"追蹤回報:\n"+sb.toString();
			JSONArray entities = new JSONArray().put(new JSONObject()
					.put("type", hasUsername ? "mention" : "text_mention")
					.put("user", this.user)
					.put("offset", text.indexOf(userName))
					.put("length", userName.length()));
			tgData.put("text", text).put("entities", entities);
			Log.debug("tgData: "+tgData);
			tgData = telegramApi.sendMessage(tgData);
			Log.debug("tgResult: "+tgData);	
		}
	}

}
