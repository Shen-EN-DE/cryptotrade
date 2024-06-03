package org.cryptotrade.command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.BinanceApiType;
import org.cryptotrade.api.FtxApiType;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.api.binance.BinanceMargin;
import org.cryptotrade.api.binance.BinanceSpot;
import org.cryptotrade.api.binance.BinanceUBaseContract;
import org.cryptotrade.api.ftx.FtxOrders;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.entity.MarginEffect;
import org.cryptotrade.entity.MarketType;
import org.cryptotrade.entity.OrderType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.question.ExchangerChooseQuestion;
import org.cryptotrade.question.MarketTypeChooseQuestion;
import org.cryptotrade.question.OrderModuleChooseQuestion;
import org.cryptotrade.question.OrderSideChooseQuestion;
import org.cryptotrade.question.OrderTypeChooseQuestion;
import org.cryptotrade.question.SameExchRateDirectionChooseQuestion;
import org.cryptotrade.question.ResultQuestion;
import org.cryptotrade.question.SendOrderConfiremQuestion;
import org.cryptotrade.question.SymbolTextInputQuestion;
import org.cryptotrade.question.VolumeTextInputQuestion;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.cryptotrade.task.OrderStatusTraceScheduledTask;
import org.cryptotrade.task.OrderStatusTraceScheduledTask.BundleField;
import org.json.JSONObject;

public class SendOrderCommand extends Command {
	private static Logger Log = LogManager.getLogger();
	private OrderStatusTraceScheduledTask task;
	
	public SendOrderCommand(JSONObject _messageObject) {
		super(_messageObject);
		Log.error(message);
		optionDescription = new HashMap<String,String>(){{
			put(CommandOptionEnum.Symbol.getSymbol(), "指定交易目標");
			put(CommandOptionEnum.OrderSide.getSymbol(), "指定交易方向(buy,sell)");
			put(CommandOptionEnum.Volume.getSymbol(), "指定交易量");
			put(CommandOptionEnum.MarketType.getSymbol(), "指定市場類型(Binance必須指定): stop(現貨),margin(槓桿),contract(合約)");
			put(CommandOptionEnum.Exchanger.getSymbol(), "(選填)指定交易所(預設ftx,選項:ftx,binacne)");
			put(CommandOptionEnum.OrderType.getSymbol(), "(選填)指定交易類型(market,limit;預設market)");
			put(CommandOptionEnum.MarginType.getSymbol(), "(選填)指定槓桿交易模式(no-effect, margin(使用槓桿), auto-reply(自動還款);默认为 no-effect)");
			put(CommandOptionEnum.Price.getSymbol(), "(選填)指定價格(未指定,以市場價)");
			put(CommandOptionEnum.TimeInForce.getSymbol(), "(選填)TimeInForce(GTC,IOC,FOK;預設GTC)");
			put(CommandOptionEnum.OrderModule.getSymbol(), "(選填)下單模組選擇(單獨使用");
			put(CommandOptionEnum.Help.getSymbol(), "(選填)列出所有選項(單獨使用)");
		}};
	}

	private boolean requireOptionCheck() {
		Boolean flag = false;
		Map<String,Boolean> requireOption = new HashMap<>() {{
				put(CommandOptionEnum.Symbol.getSymbol(),false);
				put(CommandOptionEnum.OrderSide.getSymbol(),false);
				put(CommandOptionEnum.Volume.getSymbol(),false);
				put(CommandOptionEnum.MarketType.getSymbol(),false);
		}};
		requireOption.forEach((opt,hasIt)->{
			commandOption.forEach((option,data)->{
				if(opt.equals(option)) requireOption.put(opt, true);
			});
		});
		flag = true;
		Iterator<Entry<String,Boolean>> it = requireOption.entrySet().iterator();
		while(it.hasNext()) 
			flag &= it.next().getValue();
		
		// 設定預設選項
		if(flag) {
			if(commandOption.get(CommandOptionEnum.Exchanger.getSymbol())==null) 
				commandOption.put(CommandOptionEnum.Exchanger.getSymbol(), "ftx");
			if(commandOption.get(CommandOptionEnum.MarginType.getSymbol())==null) 
				commandOption.put(CommandOptionEnum.MarginType.getSymbol(), "no-effect");
		}
		return flag;
	}
	
	public OrderStatusTraceScheduledTask getTraceTask() {
		return this.task;
	}


	@Override
	public String getDesctiption() {
		StringBuilder dscp = new StringBuilder();
		dscp.append("下單指令");
		return dscp.toString();
	}

	@Override
	public void run() {
		try { //預先包起try-catch避免thread pool的thread被中斷
			Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
			JSONObject user = message.getJSONObject("from");
			JSONObject chat = message.getJSONObject("chat");
			String userId = user.get("id").toString();
			String chatId = chat.get("id").toString();
			
			JSONObject tgData,tgResult;
			
			// 沒有輸入任何指令選項,進入Session對話
			if(commandOption.size()==0) {
				String sessionName = Session.getDefaultSesstionName(userId, chatId);
				Session session = SessionManager.getSession(sessionName);
				if(session==null) {
					session = new Session(user, chat,"下單對話");
					session.addQuestion(new ExchangerChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.Exchanger.getSymbol()+" "))
							.addQuestion(new MarketTypeChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.MarketType.getSymbol()+" "))
							.addQuestion(new SymbolTextInputQuestion().setAnswerPrefix("-"+CommandOptionEnum.Symbol.getSymbol()+" "))
							.addQuestion(new OrderSideChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.OrderSide.getSymbol()+" "))
							.addQuestion(new OrderTypeChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.OrderType.getSymbol()+" "))
							.addQuestion(new VolumeTextInputQuestion().setAnswerPrefix("-"+CommandOptionEnum.Volume.getSymbol()+" "))
							.addQuestion(new ResultQuestion())
							.addQuestion(new SendOrderConfiremQuestion());
					session.begin();
				}
			}
			else if(commandOption.containsKey(CommandOptionEnum.OrderModule.getSymbol())) {
				String sessionName = Session.getDefaultSesstionName(userId, chatId);
				Session session = SessionManager.getSession(sessionName);
				if(session==null) {
					session = new Session(user, chat,"模組下單");
					session.addQuestion(new OrderModuleChooseQuestion());
					session.begin();
				}
			}
			else {
				Log.debug("commandOption: "+commandOption);
				boolean reqOptChk = requireOptionCheck();
				if(!reqOptChk || commandOption.containsKey(CommandOptionEnum.Help.getSymbol())) {
					JSONObject data = new JSONObject() {{
						put("chat_id", receiver);
						put("text", (reqOptChk ? "" : !commandOption.containsKey(CommandOptionEnum.Help.getSymbol()) ? "缺少必要選項\n" : "")+getOptionDescription());
						put("reply_to_message_id", replyTo);
					}};
					Log.info("option: "+data);
					data = telegramApi.sendMessage(data);
					Log.info("result: "+data);
					return;
				}
				Exchange exchanger = Exchange.forName(commandOption.get(CommandOptionEnum.Exchanger.getSymbol()));
				switch(exchanger) {
					case FTX: ftx(); break;
					case Binance: binance(); break;
				}
			}
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		
	}
	
	private void ftx() throws Exception {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		FtxOrders ftxOrders = (FtxOrders)App.apiManager.getApi(Exchange.FTX.name(), FtxApiType.Order);
		Double price = null;
		String type = "market";
		String optSymbol = CommandOptionEnum.OrderType.getSymbol();
		if(commandOption.containsKey(optSymbol) && commandOption.get(optSymbol).equalsIgnoreCase("limit")) {
			type = "limit";
			price = Double.valueOf(commandOption.get(CommandOptionEnum.Price.getSymbol()));
		}
		Map<String,Object> option = new HashMap<>() {{
			put("market", commandOption.get(CommandOptionEnum.Symbol.getSymbol()).toUpperCase());
			put("side", commandOption.get(CommandOptionEnum.OrderSide.getSymbol()).toLowerCase());
			put("size", Double.valueOf(commandOption.get(CommandOptionEnum.Volume.getSymbol())));
		}};
		option.put("type", type);
		option.put("price", price==null?JSONObject.NULL:price);
		
		JSONObject result = ftxOrders.placeOrder(option);
		JSONObject tgData = new JSONObject() {{
			put("chat_id", receiver);
			put("reply_to_message_id", replyTo);
		}};
		StringBuilder sb = new StringBuilder("FTX");
		if(result.getBoolean("success")) {
			var tmp = result.getJSONObject("result");
			sb.append("訂單(").append(tmp.get("id")).append(")下單成功");
			if(commandOption.containsKey(CommandOptionEnum.OrderType.getSymbol())) {
				if(commandOption.get(CommandOptionEnum.OrderType.getSymbol()).equalsIgnoreCase("limit")) {
					sb.append("\n下單類型: Limit");
					sb.append("\n訂單價格: ").append(tmp.get("price"));
				}
				if(commandOption.get(CommandOptionEnum.OrderType.getSymbol()).equalsIgnoreCase("market")) sb.append("\n下單類型: 市價交易");
			}
			if(commandOption.get(CommandOptionEnum.OrderSide.getSymbol()).equalsIgnoreCase("buy")) sb.append("\n做單方向: Buy");
			if(commandOption.get(CommandOptionEnum.OrderSide.getSymbol()).equalsIgnoreCase("sell")) sb.append("\n做單方向: Sell");
			sb.append("\n下單總量: ").append(tmp.get("size"));
					
			Map<BundleField,Object> bundleData = new HashMap(){{
				put(BundleField.Receiver, receiver);
				put(BundleField.User,message.getJSONObject("from"));
			}};
			bundleData.put(BundleField.OrderId, tmp.get("id").toString());
			task = new OrderStatusTraceScheduledTask((Object)ftxOrders, bundleData);
			task.setReplyTo(replyTo);
			task.start(0, Long.valueOf(App.env.get("system.task.orderstatus.period")), TimeUnit.SECONDS);
		}
		else {
			result.put("cmd", message.get("text"));
			String marketType = commandOption.get(CommandOptionEnum.MarketType.getSymbol());
			if(marketType.equalsIgnoreCase(MarketType.Spot.name())) marketType = "現貨";
			else if(marketType.equalsIgnoreCase(MarketType.Contract.name())) marketType = "合約";
			sb.append(marketType).append("下單失敗!\n").append(result);
		}
		tgData.put("text", sb.toString());
		Log.info("tgData: "+tgData);
		JSONObject tgResult = telegramApi.sendMessage(tgData);
		Log.info("tgResult: "+tgResult);		
	}
	
	private void binance() throws Exception {
		Log.error(message);
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		OrderType type = OrderType.Market;
		Map<String,Object> option = new HashMap<>() {{
			put("symbol", commandOption.get(CommandOptionEnum.Symbol.getSymbol()).toUpperCase().replace("/", ""));
			put("side", commandOption.get(CommandOptionEnum.OrderSide.getSymbol()).toUpperCase());
			put("quantity", Double.valueOf(commandOption.get(CommandOptionEnum.Volume.getSymbol())));
		}};
		String optSymbol = CommandOptionEnum.OrderType.getSymbol();
		if(commandOption.containsKey(optSymbol) && commandOption.get(optSymbol).equalsIgnoreCase(OrderType.Limit.name())) {
			type = OrderType.Limit;
			option.put("price", Double.valueOf(commandOption.get(CommandOptionEnum.Price.getSymbol())));
			String tif = commandOption.get(CommandOptionEnum.TimeInForce.getSymbol());			
			option.put("timeInForce", tif==null?"GTC":tif.toUpperCase());
		}
		option.put("type", type.name().toUpperCase());

		JSONObject tgData = new JSONObject() {{
			put("chat_id", receiver);
			put("reply_to_message_id", replyTo);
		}};
		optSymbol = CommandOptionEnum.MarketType.getSymbol();
		JSONObject result = null;
		Object argObject = null;
		if(commandOption.get(optSymbol).equalsIgnoreCase(MarketType.Spot.name())) {
			BinanceSpot spot = (BinanceSpot)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Spot);
			Log.debug("spot option: "+option);
			result = spot.setOrder(option);
			Log.debug("spot result: "+result);
			argObject = spot;
		}
		else if(commandOption.get(optSymbol).equalsIgnoreCase(MarketType.Margin.name())){
			BinanceMargin margin = (BinanceMargin)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Margin);
			option.put("sideEffectType", getBinanceMarginType(commandOption.get(CommandOptionEnum.MarginType.getSymbol())));
			Log.debug("margin option: "+option);
			result = margin.setOrder(option);
			Log.debug("margin result: "+result);
			argObject = margin;
		}
		else if(commandOption.get(optSymbol).equalsIgnoreCase(MarketType.Contract.name())){
			BinanceUBaseContract uContract = (BinanceUBaseContract)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.uContract);
			Log.debug("U base contract option: "+option);
			result = uContract.setOrder(option);
			Log.debug("U base contract result: "+result);
			argObject = uContract;
		}
		else {
			tgData.put("text","找不到對應的市場類型(spot,margin,contract");
		}

		StringBuilder sb = new StringBuilder("Binance");
		String marketType = commandOption.get(CommandOptionEnum.MarketType.getSymbol());
		if(marketType.equalsIgnoreCase(MarketType.Spot.name())) marketType = "現貨";
		else if(marketType.equalsIgnoreCase(MarketType.Margin.name())) marketType = "槓桿";
		else if(marketType.equalsIgnoreCase(MarketType.Contract.name())) marketType = "合約";
		sb.append(marketType);
		if(result!=null && result.getBoolean("success")) {
			var tmp = result.getJSONObject("result");
			sb.append("訂單(").append(tmp.get("orderId")).append(")下單成功");
			if(commandOption.containsKey(CommandOptionEnum.OrderType.getSymbol())) {
				if(commandOption.get(CommandOptionEnum.OrderType.getSymbol()).equalsIgnoreCase("limit")) {
					sb.append("\n下單類型: Limit");
					sb.append("\n訂單價格: ").append(tmp.get("price"));
				}
				if(commandOption.get(CommandOptionEnum.OrderType.getSymbol()).equalsIgnoreCase("market")) sb.append("\n下單類型: 市價交易");
			}
			if(commandOption.get(CommandOptionEnum.OrderSide.getSymbol()).equalsIgnoreCase("buy")) sb.append("\n做單方向: Buy");
			if(commandOption.get(CommandOptionEnum.OrderSide.getSymbol()).equalsIgnoreCase("sell")) sb.append("\n做單方向: Sell");
			sb.append("\n下單總量: ").append(tmp.get("origQty"));
			task = new OrderStatusTraceScheduledTask(argObject, new HashMap<BundleField,Object>(){{
				put(BundleField.Receiver, receiver);
				put(BundleField.OrderId, tmp.get("orderId").toString());
				put(BundleField.Symbol, tmp.get("symbol").toString());
				put(BundleField.ReplyTo, replyTo);
				put(BundleField.User,message.getJSONObject("from"));
			}})  ;
			task.start(0, Long.valueOf(App.env.get("system.task.orderstatus.period")), TimeUnit.SECONDS);
		}
		else {
			result.put("cmd", message.get("text"));
			sb.append("下單失敗!\n").append(result);
		}
		tgData.put("text", sb);
		if(receiver!=null && !receiver.isEmpty()) {
			Log.info("tgData: "+tgData);
			JSONObject tgResult = telegramApi.sendMessage(tgData);
			Log.info("tgResult: "+tgResult);
		}
	}
	
	private String getBinanceMarginType(String _marginEffect) {
		String type = "MARGIN_BUY";
		if(_marginEffect.equalsIgnoreCase(MarginEffect.Normal.name())) type = "NO_SIDE_EFFECT";
		if(_marginEffect.equalsIgnoreCase(MarginEffect.AutoRepay.name())) type = "AUTO_REPAY";
		return type;
	}
	
}
