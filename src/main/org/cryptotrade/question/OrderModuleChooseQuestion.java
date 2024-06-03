package org.cryptotrade.question;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrderModuleChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static enum ButtonOption{
		SameExchSpotPerpLock,
		SameExchMargPerpLock,
		DiffExchMargPerpLock,
		DiffExchPerpPairLock;
	};
	public static final String Name = "OrderMudoleChoose";
	
	public OrderModuleChooseQuestion() {
		this("","選擇下單模組");
	}
	
	public OrderModuleChooseQuestion(String _name, String _questionText) {
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
		inlineKeyboards.put(new JSONArray().put(new JSONObject() {{
			put("text", "同所期現對鎖");
			put("callback_data", getName()+QueryDataSeparator+ButtonOption.SameExchSpotPerpLock);			
		}}));
		inlineKeyboards.put(new JSONArray().put(new JSONObject() {{
			put("text", "同所期槓對鎖");
			put("callback_data", getName()+QueryDataSeparator+ButtonOption.SameExchMargPerpLock);			
		}}));
		inlineKeyboards.put(new JSONArray().put(new JSONObject() {{
			put("text", "異所期槓對鎖");
			put("callback_data", getName()+QueryDataSeparator+ButtonOption.DiffExchMargPerpLock);			
		}}));
		inlineKeyboards.put(new JSONArray().put(new JSONObject() {{
			put("text", "異所期期對鎖");
			put("callback_data", getName()+QueryDataSeparator+ButtonOption.DiffExchPerpPairLock);			
		}}));
		
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
				
				String[] dataArray = callbackQuery.getString("data").split(Question.QueryDataSeparator);
				String queryData = dataArray[1];
				session.currQuestion().setAnswer(queryData);
				if(queryData.equalsIgnoreCase(ButtonOption.SameExchSpotPerpLock.name())) {
					session.setTitle(session.getTitle()+"-同所期現對鎖");
					if(session.getQuestion(ExchangerChooseQuestion.Name)==null) 
						session.addQuestion(new ExchangerChooseQuestion(),true);
					if(session.getQuestion(SameExchRateDirectionChooseQuestion.Name)==null)
						session.addQuestion(new SameExchRateDirectionChooseQuestion(),true);
					if(session.getQuestion(PerpSpotOrderActionQuestion.ArgsName.PerpPrice.name()+PriceTextInputQuestion.Name)==null)
						session.addQuestion(new PriceTextInputQuestion(PerpSpotOrderActionQuestion.ArgsName.PerpPrice.name(),"請輸入合約價格"),true);
					if(session.getQuestion(PerpSpotOrderActionQuestion.ArgsName.SpotPrice.name()+PriceTextInputQuestion.Name)==null)
						session.addQuestion(new PriceTextInputQuestion(PerpSpotOrderActionQuestion.ArgsName.SpotPrice.name(),"請輸入現貨價格"),true);
					if(session.getQuestion(VolumeTextInputQuestion.Name)==null)
						session.addQuestion(new VolumeTextInputQuestion(),true);
					if(session.getQuestion(PerpSpotOrderActionQuestion.Name)==null)
						session.addQuestion(new PerpSpotOrderActionQuestion(),true);
				}
				else if(queryData.equalsIgnoreCase(ButtonOption.SameExchMargPerpLock.name())) {
					session.setTitle(session.getTitle()+"-同所期槓對鎖");
					if(session.getQuestion(ExchangerChooseQuestion.Name)==null) {
						ExchangerChooseQuestion echanger = new ExchangerChooseQuestion();
						echanger.setButtonOption(new ArrayList() {{add(Exchange.Binance);}});
						session.addQuestion(echanger,true);
					}
					if(session.getQuestion(SameExchRateDirectionChooseQuestion.Name)==null)
						session.addQuestion(new SameExchRateDirectionChooseQuestion(),true);
					if(session.getQuestion(PerpMarginOrderActionQuestion.ArgsName.PerpPrice.name()+PriceTextInputQuestion.Name)==null)
						session.addQuestion(new PriceTextInputQuestion(PerpMarginOrderActionQuestion.ArgsName.PerpPrice.name(),"請輸入合約價格"),true);
					if(session.getQuestion(PerpMarginOrderActionQuestion.ArgsName.MarginPrice.name()+PriceTextInputQuestion.Name)==null)
						session.addQuestion(new PriceTextInputQuestion(PerpMarginOrderActionQuestion.ArgsName.MarginPrice.name(),"請輸入槓桿價格"),true);
					if(session.getQuestion(VolumeTextInputQuestion.Name)==null)
						session.addQuestion(new VolumeTextInputQuestion(),true);
					if(session.getQuestion(PerpMarginOrderActionQuestion.Name)==null)
						session.addQuestion(new PerpMarginOrderActionQuestion(),true);
					
				}
				else if(queryData.equalsIgnoreCase(ButtonOption.DiffExchMargPerpLock.name())) {
//					session = new Session(user,chat,"異所期槓對鎖");
					
				}
				else if(queryData.equalsIgnoreCase(ButtonOption.DiffExchPerpPairLock.name())) {
//					session = new Session(user,chat,"異所期期對鎖");					
				}
				Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"));
				
			} catch(Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		};
	}

}
