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

public class SessionCancelQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	private static final String Name = "SessionCancel";

	public SessionCancelQuestion() {
		this("","是否取消對話");
	}
	
	public SessionCancelQuestion(String _name,String _questionText) {
		super(_name+Name, _questionText, InputType.Button);
	}

	public static JSONObject getButton() {
		return new JSONObject()
				.put("text", "取消")
				.put("callback_data", Name+QueryDataSeparator+ProgressAnswer.Cancel);
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
					if(session==null) return;
					
					String[] dataArray = _callbackQuery.getString("data").split(QueryDataSeparator);
					String queryData = dataArray[1];
					Log.debug(session.getName()+"->"+SessionCancelQuestion.this.getQuestionText()+":"+queryData);
					if(queryData.equalsIgnoreCase(ProgressAnswer.Cancel.name())) {
						tgData = new JSONObject()
								.put("chat_id",chat.get("id"))
								.put("message_id", message.get("message_id"))
								.put("text", "對話已取消");
						tgResult = telegramApi.editMessageText(tgData);	
						session.finish();
						
						String text = user.getString("first_name")+" 已取消對話";
						JSONArray entities = new JSONArray();
						JSONObject messageEntity = new JSONObject()
								.put("type", "text_mention")
								.put("user", user)
								.put("offset", text.indexOf(user.getString("first_name")))
								.put("length", user.getString("first_name").length());
						entities.put(messageEntity);
						tgData = new JSONObject()
								.put("chat_id",chat.get("id"))
								.put("reply_to_message_id", message.get("message_id"))
								.put("entities", entities)
								.put("text", text);
						tgResult = telegramApi.sendMessage(tgData);
					}
				} catch(Exception e) {
					Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
				}
			}
		};
	}

}
