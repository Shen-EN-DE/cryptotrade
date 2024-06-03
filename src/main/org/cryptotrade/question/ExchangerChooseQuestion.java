package org.cryptotrade.question;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExchangerChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "ExchagerChoose";
	private List<Exchange> buttonOption;
	
	public ExchangerChooseQuestion() {
		this("");
	}
	
	public ExchangerChooseQuestion(String _name) {
		super(_name+Name,"選擇交易所",InputType.Button);
	}
	
	public void setButtonOption(List<Exchange> _option) {
		this.buttonOption = _option;
	}
	
	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		Map<String,String>[] buttonDatas = null;
		Exchange[] exchanerArray = null;
		if(buttonOption==null) {
			exchanerArray = Exchange.values();
		}
		else {
			exchanerArray = new Exchange[buttonOption.size()];
			for(int i=0; i<buttonOption.size(); i++) {
				exchanerArray[i] = buttonOption.get(i);
			}			
		}
		buttonDatas = new Map[exchanerArray.length];
		for(int i=0; i<exchanerArray.length; i++) {
			Exchange exchanger = exchanerArray[i];
			buttonDatas[i] = new HashMap() {{
				put("buttonText", exchanger.name());
				put("callbackData", getName()+QueryDataSeparator+exchanger.name());
			}};				
		}
		return ButtonMaker(buttonDatas);
	}

	@Override
	public Runnable getTask(JSONObject _event) {
		Runnable task = new Runnable(){
			public void run() {
				try {
					Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
					if(_event.isNull("callback_query")) {
						Log.warn("只接受callback_query");
						return;
					}
					JSONObject _callbackQuery = _event.getJSONObject("callback_query");
					JSONObject tgData = new JSONObject().put("callback_query_id", _callbackQuery.get("id"));
					Log.debug("tgData: "+tgData);
					JSONObject tgResult = telegramApi.answerCallbackQuery(tgData);
					Log.debug("tgResult: "+tgResult);
					
					JSONObject message = _callbackQuery.getJSONObject("message");
					JSONObject user = _callbackQuery.getJSONObject("from");
					JSONObject chat = message.getJSONObject("chat");
					String[] dataArray = _callbackQuery.getString("data").split(Question.QueryDataSeparator);
					String queryData = dataArray[1];
					
					String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
					Session session = SessionManager.getSession(sessionName);
					if(session!=null) {
						Log.debug(session.getName()+"->"+ExchangerChooseQuestion.this.getQuestionText()+":"+queryData);
						session.currQuestion().setAnswer(queryData);
						Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"));
					}
					else {
						Log.debug("沒有對應的Session => "+sessionName);
					}
				} catch(Exception e) {
					Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
				}
			}
		};
		return task;
	}

	/**
	 * 無作用
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		
	}

}
