package org.cryptotrade.command;

import java.util.HashMap;
import java.util.Map;

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
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.question.ExchangerChooseQuestion;
import org.cryptotrade.question.MarketTypeChooseQuestion;
import org.cryptotrade.question.OrderDeleteConfirmQuestion;
import org.cryptotrade.question.PendingOrderChooseQuestion;
import org.cryptotrade.question.Question;
import org.cryptotrade.question.SymbolTextInputQuestion;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class CancelOrderCommand extends Command {
	private static Logger Log = LogManager.getLogger();
	private JSONObject defaultTgData;

	public CancelOrderCommand(JSONObject _messageObject) {
		super(_messageObject);
		// optionDescription是父類別member
		optionDescription = new HashMap<>() {{
			put(CommandOptionEnum.Exchanger.getSymbol(), "(選填)指定交易所(預設ftx,選項:ftx,binance)");
			put(CommandOptionEnum.Symbol.getSymbol(), "(選填)指定交易目標;若未指定細節,將取消該交易標的所有掛單(Binance必填)");
			put(CommandOptionEnum.MarketType.getSymbol(), "(選填)指定市場類型(Binance必填): stop(現貨),margin(槓桿),contract(合約)");
			put(CommandOptionEnum.OrderSide.getSymbol(), "(選填)指定交易方向(buy,sell)");
			put(CommandOptionEnum.OrderId.getSymbol(), "(選填)取消特定訂單(Binance必填)");
			put(CommandOptionEnum.Help.getSymbol(), "(選填)列出所有選項(單獨使用)");
		}};
		defaultTgData = new JSONObject() {{
			put("chat_id", receiver);
			put("reply_to_message_id", replyTo);
		}};
	}

	@Override
	public String getDesctiption() {
		StringBuilder dscp = new StringBuilder();
		dscp.append("取消掛單");
		return dscp.toString();
	}

	@Override
	public void run() {		
		// 加上try-catch避免Exception中傳ThreadPool的Thread
		try {
			if(commandOption.isEmpty()) {
				JSONObject user = message.getJSONObject("from");
				JSONObject chat = message.getJSONObject("chat");
				String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
				Session session = SessionManager.getSession(sessionName);
				if(session==null) {
					session = new Session(user, chat,"取消掛單");
					SymbolTextInputQuestion symbol = new SymbolTextInputQuestion();
					symbol.setAnswerPrefix("-"+CommandOptionEnum.Symbol.getSymbol()+" ");
					symbol.isShowExtraOpionButton(true);
					session.addQuestion(new ExchangerChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.Exchanger.getSymbol()+" "))
							.addQuestion(new MarketTypeChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.MarketType.getSymbol()+" "))
							.addQuestion(symbol)
							.addQuestion(new PendingOrderChooseQuestion().setAnswerPrefix("-"+CommandOptionEnum.OrderId.getSymbol()+" "))
							.addQuestion(new OrderDeleteConfirmQuestion());
					session.begin();
				}
			}
			else {
				Exchange exchanger = Exchange.FTX;
				String optSymbol = CommandOptionEnum.Exchanger.getSymbol();
				if(commandOption.containsKey(optSymbol)) exchanger = Exchange.forName(commandOption.get(optSymbol));
				switch(exchanger) {
					case FTX: ftx(); break;
					case Binance: binance(); break;
				}
			}
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}

	private void ftx() {
		// commandOption是父類別member
		JSONObject tgData = new JSONObject(this.defaultTgData.toString());
		JSONObject result = null;
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		Log.info("commandOption: "+commandOption);
		if(commandOption.containsKey(CommandOptionEnum.Help.getSymbol())) {
			tgData.put("text", getOptionDescription());
			Log.info("tgData: "+tgData);
			result = telegramApi.sendMessage(tgData);
			Log.info("tgResult: "+result);
			return;
		}
		
		FtxOrders ftxOrders = (FtxOrders)App.apiManager.getApi(Exchange.FTX.name(), FtxApiType.Order);
		String optionKey = CommandOptionEnum.Symbol.getSymbol();
		result = ftxOrders.getOpenOrders(commandOption.get(optionKey));
		JSONArray orders = result.getJSONArray("result");
		
		// 沒有指訂單編號,適用批量取消
		StringBuilder text = new StringBuilder("FTX");
		if(!commandOption.containsKey(CommandOptionEnum.OrderId.getSymbol())) {
			String market = commandOption.get(CommandOptionEnum.Symbol.getSymbol());
			String side = commandOption.get(CommandOptionEnum.OrderSide.getSymbol());
			result = ftxOrders.cancelAllOrders(market, side, null, true);
			if(result.getBoolean("success")) 
				text.append("指令已被交易所接受");
			else {
				text.append("※!指令未被交易所接受!※\n").append(result);
				StringBuilder sb = new StringBuilder()
						.append("ftxOrders.cancelAllOrders()失敗: market=").append(market)
						.append(", side=").append(side)
						.append("\nresult: ").append(result);
				Log.error(sb);
			}
		}
		else {
			optionKey = CommandOptionEnum.OrderId.getSymbol();
			if(commandOption.containsKey(optionKey)) {
				String id = commandOption.get(optionKey);
				for(int i=0; i<orders.length(); i++) {
					JSONObject order = orders.getJSONObject(i);
					if(order.get("id").toString().equalsIgnoreCase(id)) {
						result = ftxOrders.cancelOrder(Long.valueOf(id));
						if(result.getBoolean("success")) 
							text.append("指令已被交易所接受");
						else
							text.append("※!指令未被交易所接受!※\n").append(result);
					}
				} //endfor
			} //endif
		} //endif(!commandOption.containsKey(CommandOptionEnum.ID.getSymbol()))
		if(receiver!=null && !receiver.isEmpty()) {
			tgData.put("text", text);
			Log.info("tgData: "+tgData);
			result = telegramApi.sendMessage(tgData);
			Log.info("tgResult: "+result);
		}
	}
	
	private void binance() throws Exception {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		JSONObject tgData = new JSONObject(this.defaultTgData.toString());
		String marketType = commandOption.get(CommandOptionEnum.MarketType.getSymbol());
		String symbol = commandOption.get(CommandOptionEnum.Symbol.getSymbol()).replace("/", "");
		String orderId = commandOption.get(CommandOptionEnum.OrderId.getSymbol());
		JSONObject result = null;
		StringBuilder warnSB = new StringBuilder();
		// 檢查MarketType
		if(marketType==null) {
			warnSB.append("未指定市場類型(-")
					.append(CommandOptionEnum.MarketType.getSymbol())
					.append(")");
		}
		if(symbol==null) {
			if(warnSB.length()>0) warnSB.append("\n");
			warnSB.append("未指定交易標的(-")
					.append(CommandOptionEnum.Symbol.getSymbol())
					.append(")");
		}
		if(orderId==null) {
			if(warnSB.length()>0) warnSB.append("\n");
			warnSB.append("未指定訂單編號(-")
					.append(CommandOptionEnum.OrderId.getSymbol())
					.append(")");
		}
		
		if(warnSB.length()>0) {
			tgData.put("text", "Binance取消單失敗:\n"+warnSB);
			Log.info("tgData: "+tgData);
			JSONObject tgResult = telegramApi.sendMessage(tgData);
			Log.info("tgResult: "+tgResult);
			return;
		}
		
		Map<String,Object> args = new HashMap<>() {{
			put("symbol", symbol.toUpperCase());
			put("orderId", Long.valueOf(orderId));
		}};		

		StringBuilder text = new StringBuilder("Binance");
		result = null;
		if(marketType.equalsIgnoreCase("spot")) {
			BinanceSpot spot = (BinanceSpot)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Spot);
			result = spot.deleteOrder(args);
			
		}
		else if(marketType.equalsIgnoreCase("margin")) {
			BinanceMargin margin = (BinanceMargin)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Margin);
			result = margin.deleteOrder(args);
		}
		else if(marketType.equalsIgnoreCase("contract")) {
			BinanceUBaseContract uContract = (BinanceUBaseContract)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.uContract);
			result = uContract.deleteOrder(args);
		}
		
		if(result!=null && result.getBoolean("success") && result.getJSONObject("result").isNull("msg")) {
			text.append("指令已被交易所接受");
			Log.debug("指令已被交易所接受: "+result);
		}
		else {
			text.append("※!指令未被交易所接受!※\n").append(result);
			StringBuilder sb = new StringBuilder()
					.append("BinanceSpot.deleteOrder()失敗: symbol=").append(symbol)
					.append(", orderId=").append(orderId)
					.append("\nresult: ").append(result);
			Log.error(sb);
		}
		if(receiver!=null && !receiver.isEmpty()) {
			tgData.put("text", text);
			Log.info("tgData: "+tgData);
			result = telegramApi.sendMessage(tgData);
			Log.info("tgResult: "+result);
		}
		
	}
}
