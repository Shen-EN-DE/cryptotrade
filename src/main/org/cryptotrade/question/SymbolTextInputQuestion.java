package org.cryptotrade.question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.BinanceApiType;
import org.cryptotrade.api.FtxApiType;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.api.binance.BinanceMarket;
import org.cryptotrade.api.ftx.FtxMarket;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.entity.MarketType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class SymbolTextInputQuestion extends NormalTextInputQuestion {
	private static Logger Log = LogManager.getLogger();
	private static final String SubName = "Symbol";
	public static enum Option{All};
	public static final String Name = SubName+NormalTextInputQuestion.Name;
	private boolean showOptionButton = false;
	
	public SymbolTextInputQuestion() {
		super(SubName, "請輸入交易標的", Type.Text);
	}
	
	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		if(!showOptionButton) return null;
		Question exchager = _session.getQuestion(ExchangerChooseQuestion.Name);
		Map<String,String>[] buttonDatas = new Map[0];
		if(exchager!=null) {
			//有些交易所可以接受全交易對參數
			if(exchager.getPureAnswer().equalsIgnoreCase(Exchange.FTX.name())) {
				buttonDatas = new Map[1];
				buttonDatas[0] = new HashMap() {{
					put("buttonText","所有交易標的");
					put("callbackData",getName()+QueryDataSeparator+Option.All.name());
				}};
			}
		}
		return ButtonMaker(buttonDatas);
	}
	
	/**
	 * 是否要顯示"所有標的"按鈕
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		this.showOptionButton = _isShow;
	}
	
	@Override
	public Runnable getTask(JSONObject _event) {
		return new Runnable() {
			public void run() {
				try {
					Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
					JSONObject message = null;
					Long messageId = null;
					JSONObject user = null;
					JSONObject chat = null;
					JSONObject tgData;
					JSONObject tgResult;
					Session.StepMessageType stepMessageType = Session.StepMessageType.Edit;
					String answer = null;
					if(_event.has("message")) {
						message = _event.getJSONObject("message");
						user = message.getJSONObject("from");
						chat = message.getJSONObject("chat");
						answer = message.getString("text");
						String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
						Session session = SessionManager.getSession(sessionName);
						if(session==null) return;
						
						//以下內容都在作輸入驗證
						Question exchanger = session.getQuestion(ExchangerChooseQuestion.Name);
						String echangeName = exchanger.getPureAnswer();
						boolean isLegalSymbol = false;
						if(exchanger!=null) {
							if(echangeName.equalsIgnoreCase(Exchange.FTX.name())) {
								FtxMarket ftxMarket = (FtxMarket)App.apiManager.getApi(Exchange.FTX.name(), FtxApiType.Market);
								JSONObject result = ftxMarket.getMarkets(answer);
								isLegalSymbol = result.getBoolean("success");
							}
							if(echangeName.equalsIgnoreCase(Exchange.Binance.name())) {
								Question marketType = session.getQuestion(MarketTypeChooseQuestion.Name);
								String marketTypeName = marketType.getPureAnswer();
								BinanceMarket binanceMarket = (BinanceMarket)App.apiManager.getApi(Exchange.Binance.name(), BinanceApiType.Market);
								List<String> symbolList = new ArrayList();
								symbolList.add(answer.replace("/","").toUpperCase());
								JSONObject result;
								if(marketTypeName.equalsIgnoreCase(MarketType.Contract.name()))
									result = binanceMarket.exchangeUsdContractInfo(symbolList);
								else 
									result = binanceMarket.exchangeSpotMarginInfo(symbolList);
								if(result.getBoolean("success")) {
									JSONArray symbols = result.getJSONObject("result").getJSONArray("symbols");									
									isLegalSymbol = symbols!=null && symbols.length()>0;
								}
							}
						}
						
						if(!isLegalSymbol) {
							String warnText = echangeName+"裡沒有該交易標的!";
							Session.currentStep(session, chat.get("id").toString(), message.getLong("message_id"), warnText,Session.StepMessageType.New);
							return;
						}					
						stepMessageType = Session.StepMessageType.New;
					}
					else if(_event.has("callback_query")) {
						JSONObject callbackQuery = _event.getJSONObject("callback_query");
						tgData = new JSONObject().put("callback_query_id", callbackQuery.get("id"));
						Log.debug("tgData: "+tgData);
						tgResult = telegramApi.answerCallbackQuery(tgData);
						Log.debug("tgResult: "+tgResult);
						message = callbackQuery.getJSONObject("message");
						user = callbackQuery.getJSONObject("from");
						chat = message.getJSONObject("chat");
						answer = callbackQuery.getString("data").split(Question.QueryDataSeparator)[1];			
					}
					String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
					Session session = SessionManager.getSession(sessionName);
					if(session==null) return;
					
					Log.debug(session.getName()+"->"+SymbolTextInputQuestion.this.getQuestionText()+":"+answer);
					session.currQuestion().setAnswer(answer);
					Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"),stepMessageType);
				} catch(Exception e) {
					Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
				}
			}
		};
	}

}
