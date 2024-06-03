package org.cryptotrade.question;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class SessionBackQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	private static final String Name = "SessionBack";
	
	public SessionBackQuestion() {
		this("","");
	}
	
	public SessionBackQuestion(String _name,String _questionText) {
		super(_name+Name, _questionText, InputType.Button);
	}
	
	public static JSONObject getButton() {
		return new JSONObject()
				.put("text", "«Back")
				.put("callback_data", Name+QueryDataSeparator+ProgressAnswer.Back);
	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		JSONObject yesButton = new JSONObject()
				.put("text", "是")
				.put("callback_data", getName()+QueryDataSeparator+ConfirmAnswer.Yes);
		JSONObject noButton = new JSONObject()
				.put("text", "否")
				.put("callback_data", getName()+QueryDataSeparator+ConfirmAnswer.No);
		JSONArray row = new JSONArray().put(yesButton).put(noButton);
		JSONArray inlineKeyboards = new JSONArray().put(row);
		return inlineKeyboards;
	}

	/**
	 * 無作用
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		
	}

	@Override
	public Runnable getTask(JSONObject _event) {
		return new Runnable() {
			public void run() {
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
					if(session==null) {
						Log.debug("找不到Session: "+sessionName);
						return;
					}
	
					String[] dataArray = _callbackQuery.getString("data").split(QueryDataSeparator);
					String queryData = dataArray[1];
					Log.debug(session.getName()+"->"+SessionBackQuestion.this.getQuestionText()+":"+queryData);
					if(queryData.equalsIgnoreCase(ProgressAnswer.Back.name())) {
						Session.backStop(session, chat.get("id").toString(), message.getLong("message_id"));
					}
				} catch(Exception e) {
					Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
				}
			} 
		};
	}

}
