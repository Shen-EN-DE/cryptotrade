package org.cryptotrade.question;

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
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.entity.MarketType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.question.PageContralQuestion.PageOption;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class PendingOrderChooseQuestion extends PageContralQuestion {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "PendingOrderChoose";

	public PendingOrderChooseQuestion() {
		this("","選擇訂單");
	}
	
	public PendingOrderChooseQuestion(String _name, String _questionText) {
		super(_name+Name, _questionText);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 無作用
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		
	}

	@Override
	public String getName() {
		return Name;
	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		JSONArray inlineKeyboards = new JSONArray();
		JSONArray pageControlRow = new JSONArray();
		Question exchange = _session.getQuestion(ExchangerChooseQuestion.Name);
		Question marketType = _session.getQuestion(MarketTypeChooseQuestion.Name);
		Question symbol = _session.getQuestion(SymbolTextInputQuestion.Name);
		int totalOrders = 0;
		JSONArray orders = null;
		if(exchange.getPureAnswer().equalsIgnoreCase(Exchange.FTX.name())) {
			//只接受Spot跟Contract
			if(!marketType.getPureAnswer().equalsIgnoreCase(MarketType.Margin.name())){
				String marketSymbol = null;
				if(!symbol.getPureAnswer().equalsIgnoreCase(SymbolTextInputQuestion.Option.All.name()))
					marketSymbol = symbol.getPureAnswer();
				FtxOrders order = (FtxOrders)App.apiManager.getApi(Exchange.FTX.name(), FtxApiType.Order);
				JSONObject result = order.getOpenOrders(marketSymbol);
				if(result.getBoolean("success")) {
					orders = result.getJSONArray("result");
					for(int i=this.currPageOffset*ShowRowCountInPage; i<orders.length(); i++) {
						JSONObject orderObject = orders.getJSONObject(i);
						StringBuilder text = new StringBuilder();
						text.append("FTX");
						if(orderObject.has("future") && !orderObject.isNull("future") && !orderObject.getString("future").isEmpty())
							text.append("合約");
						else
							text.append("現貨");
						text.append("(").append(orderObject.get("id")).append(")");
						text.append("-").append(orderObject.get("market")).append("\n");
						text.append(orderObject.getString("side").toUpperCase()).append("-").append(orderObject.getString("type").toUpperCase());
						text.append("@").append(String.format("%f",orderObject.getFloat("price")));
						inlineKeyboards.put(new JSONArray().put(new JSONObject()
								.put("text", text)
								.put("callback_data", Name+QueryDataSeparator+DataOption.Data+orderObject.get("id"))
							)
						);						
					}
					inlineKeyboards.put(new JSONArray().put(new JSONObject()
							.put("text", "選擇全部共"+orders.length()+"單")
							.put("callback_data", Name+QueryDataSeparator+DataOption.All)
						)
					);
				}
			}
		}
		if(exchange.getAnswer().equalsIgnoreCase(Exchange.Binance.name())) {
			if(marketType.getAnswer().equalsIgnoreCase(MarketType.Spot.name())){
				BinanceSpot spot = (BinanceSpot)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Spot);
			}
			if(marketType.getAnswer().equalsIgnoreCase(MarketType.Margin.name())){
				BinanceMargin margin = (BinanceMargin)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Margin);
			}
			if(marketType.getAnswer().equalsIgnoreCase(MarketType.Contract.name())){
				BinanceUBaseContract uContract = (BinanceUBaseContract)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.uContract);
			}
		}
		if(orders!=null && orders.length()>ShowRowCountInPage) {
			int offset = this.currPageOffset;			
			JSONArray pageContralRow = new JSONArray();
			JSONObject back = new JSONObject();
			if(offset>0)
				back.put("text", "«上頁").put("callback_data", Name+QueryDataSeparator+PageOption.Page+(offset-1));
			else
				back.put("text", "").put("callback_data", Name+QueryDataSeparator+PageOption.NoAction);
			pageContralRow.put(back);
			
			int totalPage = (int)Math.ceil(orders.length()/(float)ShowRowCountInPage);
			JSONObject page = new JSONObject()
					.put("text", offset+"/"+totalPage)
					.put("callback_data", Name+QueryDataSeparator+PageOption.CurrentPage);
			pageContralRow.put(page);
			
			JSONObject next = new JSONObject();
			if(offset<totalPage-1)
				next.put("text", "下頁»").put("callback_data", Name+QueryDataSeparator+PageOption.Page+(offset+1));
			else
				next.put("text", "").put("callback_data", Name+QueryDataSeparator+PageOption.NoAction);
			pageContralRow.put(next);
			inlineKeyboards.put(pageContralRow);
		}
		return inlineKeyboards;
	}

	@Override
	public Runnable getTask(JSONObject _event) {
		return ()->{
			try {
				if(_event.isNull("callback_query")) {
					Log.warn("只接受callback_query");
					return;
				}
				Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
				JSONObject _callbackQuery = _event.getJSONObject("callback_query");
				JSONObject message = _callbackQuery.getJSONObject("message");
				JSONObject user = _callbackQuery.getJSONObject("from");
				JSONObject chat = message.getJSONObject("chat");
				JSONObject tgData = new JSONObject().put("callback_query_id", _callbackQuery.get("id"));
				JSONObject tgResult = telegramApi.answerCallbackQuery(tgData);
				String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
				Session session = SessionManager.getSession(sessionName);
				if(session==null) return;
				
				String[] dataArray = _callbackQuery.getString("data").split(QueryDataSeparator);
				String queryData = dataArray[1];
				
				int searchPageIndex = queryData.indexOf(PageOption.Page.name());
				int searchDataIndex = queryData.indexOf(DataOption.Data.name());
				if(searchPageIndex==0) {
					String page = queryData.replace(PageOption.Page.name(), "");
					this.currPageOffset = Integer.valueOf(page);
					tgData = new JSONObject()
							.put("message_id", message.get("message_id"))
							.put("chat_id", chat.get("id"))
							.put("text", message.get("text"))
							.put("reply_markup", session.getInlineKeyboards());
					tgResult = telegramApi.editMessageText(tgData);
				}
				else if(searchDataIndex==0) {
					String data = queryData.replace(DataOption.Data.name(), "");
					session.currQuestion().setAnswer(data);
					Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"));
				}
				else if(queryData.equalsIgnoreCase(DataOption.All.name())) {
					session.currQuestion().setAnswer(DataOption.All.name());
					Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"));
				}
			} catch(Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		};
	}

}
