package org.cryptotrade.question;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.Storage;
import org.cryptotrade.Storage.StorageName;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class SameExchRateDirectionChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static enum Option{PositiveRate,NagetiveRate};
	public static final String Name = "RateDirectionChoose";

	public SameExchRateDirectionChooseQuestion() {
		this("","選擇利率方向");
	}
	
	public SameExchRateDirectionChooseQuestion(String _name, String _questionText) {
		super(_name+Name, _questionText, InputType.Button);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		// TODO Auto-generated method stub

	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		JSONArray inlineKeyboards = new JSONArray();
		JSONArray row = new JSONArray();
		row.put(new JSONObject()
						.put("text", "正利率")
						.put("callback_data", getName()+QueryDataSeparator+Option.PositiveRate))
				.put(new JSONObject()
						.put("text", "負利率")
						.put("callback_data", getName()+QueryDataSeparator+Option.NagetiveRate));
		inlineKeyboards.put(row);
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
				JSONObject callbackQuery = _event.getJSONObject("callback_query");
				JSONObject message = callbackQuery.getJSONObject("message");
				JSONObject user = callbackQuery.getJSONObject("from");
				JSONObject chat = message.getJSONObject("chat");
				Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
				JSONObject tgData = new JSONObject().put("callback_query_id", callbackQuery.get("id"));
				JSONObject tgResult = telegramApi.answerCallbackQuery(tgData);
				String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
				Session session = SessionManager.getSession(sessionName);
				if(session==null) return;
				
				String[] dataArray = callbackQuery.getString("data").split(QueryDataSeparator);
				String queryData = dataArray[1];
				JSONObject compareRateData = Storage.getData(StorageName.CompareRateData);
				List<String> coinOption = new ArrayList();
				String orderModule = session.getQuestion(OrderModuleChooseQuestion.Name).getPureAnswer();
				String spotPriceQestionName = PerpSpotOrderActionQuestion.ArgsName.SpotPrice.name()+PriceTextInputQuestion.Name;
				Question spotPrice = session.getQuestion(spotPriceQestionName);
				String spot_PerpPriceQestionName = PerpSpotOrderActionQuestion.ArgsName.PerpPrice.name()+PriceTextInputQuestion.Name;
				Question spot_PerpPrice = session.getQuestion(spot_PerpPriceQestionName);
				String marginPriceQestionName = PerpMarginOrderActionQuestion.ArgsName.MarginPrice.name()+PriceTextInputQuestion.Name;
				Question marginPrice = session.getQuestion(marginPriceQestionName);
				String margin_PerpPriceQestionName = PerpMarginOrderActionQuestion.ArgsName.PerpPrice.name()+PriceTextInputQuestion.Name;
				Question margin_PerpPrice = session.getQuestion(margin_PerpPriceQestionName);
				session.currQuestion().setAnswer(queryData);
				if(queryData.equalsIgnoreCase(Option.PositiveRate.name())) {
					String exchanger = session.getQuestion(ExchangerChooseQuestion.Name).getPureAnswer();
					if(exchanger.equalsIgnoreCase(Exchange.FTX.name())) {
						if(orderModule.equalsIgnoreCase(OrderModuleChooseQuestion.ButtonOption.SameExchSpotPerpLock.name())) {
							String searchFieldName = "FTX合約對現貨(正)";
							coinOption = SameExchRateDirectionChooseQuestion.this.getCoinOptions(Exchange.FTX,searchFieldName);
							spotPrice.setQuestionText("輸入現貨BuyLimit價格");
							spot_PerpPrice.setQuestionText("輸入合約SellLimit價格");
						}
						else if(orderModule.equalsIgnoreCase(OrderModuleChooseQuestion.ButtonOption.SameExchMargPerpLock.name())) {
							// FTX無槓桿帳戶,未實作
						}
					}
					else if(exchanger.equalsIgnoreCase(Exchange.Binance.name())) {
						if(orderModule.equalsIgnoreCase(OrderModuleChooseQuestion.ButtonOption.SameExchSpotPerpLock.name())) {
							String searchFieldName = "幣安合約空現貨多(正)";
							coinOption = SameExchRateDirectionChooseQuestion.this.getCoinOptions(Exchange.Binance,searchFieldName);
							spotPrice.setQuestionText("輸入現貨BuyLimit價格");
							spot_PerpPrice.setQuestionText("輸入合約SellLimit價格");
						}
						else if(orderModule.equalsIgnoreCase(OrderModuleChooseQuestion.ButtonOption.SameExchMargPerpLock.name())) {
							String searchFieldName = "幣安合約空槓桿多(正)";
							coinOption = SameExchRateDirectionChooseQuestion.this.getCoinOptions(Exchange.Binance,searchFieldName);
							marginPrice.setQuestionText("輸入槓桿BuyLimit價格");
							margin_PerpPrice.setQuestionText("輸入合約SellLimit價格");
						}
					}
				}
				else if(queryData.equalsIgnoreCase(Option.NagetiveRate.name())) {
					String exchanger = session.getQuestion(ExchangerChooseQuestion.Name).getPureAnswer();
					if(exchanger.equalsIgnoreCase(Exchange.FTX.name())) {
						if(orderModule.equalsIgnoreCase(OrderModuleChooseQuestion.ButtonOption.SameExchSpotPerpLock.name())) {
							String searchFieldName = "FTX合約對現貨(負)";
							coinOption = SameExchRateDirectionChooseQuestion.this.getCoinOptions(Exchange.FTX,searchFieldName);
							spotPrice.setQuestionText("輸入現貨SellBuyLimit價格");
							spot_PerpPrice.setQuestionText("輸入合約BuyLimit價格");
						}
					}
					else if(exchanger.equalsIgnoreCase(Exchange.Binance.name())) {
						if(orderModule.equalsIgnoreCase(OrderModuleChooseQuestion.ButtonOption.SameExchMargPerpLock.name())) {
							String searchFieldName = "幣安槓桿空合約多";
							coinOption = SameExchRateDirectionChooseQuestion.this.getCoinOptions(Exchange.Binance,searchFieldName);
							marginPrice.setQuestionText("輸入槓桿SellLimit價格");
							margin_PerpPrice.setQuestionText("輸入合約BuyLimit價格");
						}
					}
				}
				
				if(session.getQuestion(CoinChooseQuestion.Name)==null) {
					session.addQuestion(new CoinChooseQuestion(coinOption));
				}
				Session.nextStep(session,chat.get("id").toString(), message.getLong("message_id"));
				
			} catch(Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		};
	}
	
	private List<String> getCoinOptions(Exchange _exchanger,String _searchFieldName){
		List<String> coinOption = new ArrayList();
		JSONObject compareRateData = Storage.getData(StorageName.CompareRateData);
		JSONArray jsonRateDataArray = compareRateData.getJSONArray("data");
		for(int j=0; j<jsonRateDataArray.length(); j++) {
			JSONObject jsonRateDataObj = jsonRateDataArray.getJSONObject(j);
			String fieldName = jsonRateDataObj.keySet().iterator().next();
			if(!fieldName.equals(_searchFieldName)) continue;
			JSONArray jsonCoinDataArray = jsonRateDataObj.getJSONArray(_searchFieldName);
			for(int i=1; i<jsonCoinDataArray.length(); i++) {
				JSONObject jsonDataObj = jsonCoinDataArray.getJSONObject(i);
				String searchKeyword = "";
				if(_exchanger==Exchange.FTX) searchKeyword = "-PERP";
				else if(_exchanger==Exchange.Binance) searchKeyword = "USDT";
				String key = jsonDataObj.get("col1").toString();
				String coin = key.replace(searchKeyword.toUpperCase(), "").replace(searchKeyword.toLowerCase(), "");
				coinOption.add(coin);
			}
		}
		return coinOption;
	}
}
