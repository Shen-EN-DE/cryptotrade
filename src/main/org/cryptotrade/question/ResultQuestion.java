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

public class ResultQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "Result";

	public ResultQuestion() {
		this("");
	}
	
	public ResultQuestion(String _name) {
		super(_name+Name,"請確認以下內容", InputType.Button);
	}

	/**
	 * 無作用
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		
	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		JSONObject nextButton = new JSONObject()
				.put("text", "繼續")
				.put("callback_data", getName()+QueryDataSeparator+ProgressAnswer.Next);
		return new JSONArray().put(new JSONArray().put(nextButton));
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
					if(session==null) return;
	
					String[] dataArray = _callbackQuery.getString("data").split(QueryDataSeparator);
					String queryData = dataArray[1];
					Log.debug(session.getName()+"->"+ResultQuestion.this.getQuestionText()+":"+queryData);
					if(queryData.equalsIgnoreCase(ProgressAnswer.Next.name())) {
						String text = message.getString("text");
						
						Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"));
					}
				} catch(Exception e) {
					Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
				}
			}
		};
	}

}
