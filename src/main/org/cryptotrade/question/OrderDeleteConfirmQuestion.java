package org.cryptotrade.question;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.command.CancelOrderCommand;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.question.PageContralQuestion.DataOption;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrderDeleteConfirmQuestion extends ConfirmQuestion {
	private static Logger Log = LogManager.getLogger();
	private static final String SubName = "OrderDelete";
	public static final String Name = SubName+ConfirmQuestion.Name;

	public OrderDeleteConfirmQuestion() {
		this("","確認是否刪除以上所選定單");
	}
	
	public OrderDeleteConfirmQuestion(String _name, String _questionText) {
		super(_name+SubName, _questionText, InputType.Button);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		// TODO Auto-generated method stub

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
				if(queryData.equalsIgnoreCase(ConfirmAnswer.Yes.name())){
					Question selectOrder = session.getQuestion(PendingOrderChooseQuestion.Name);
					if(selectOrder.getPureAnswer().equalsIgnoreCase(DataOption.All.name())) {
						selectOrder.setAnswerPrefix("");
					}
					Question symbol = session.getQuestion(SymbolTextInputQuestion.Name);
					if(symbol.getPureAnswer().equalsIgnoreCase(SymbolTextInputQuestion.Option.All.name())) {
						symbol.setAnswerPrefix("").setAnswer("");
					}
					String cmdString = "/cancelorder "+session.finish();
					JSONObject messageData = new JSONObject()
							.put("message_id", message.get("message_id"))
							.put("chat", chat)
							.put("from", user)
							.put("text", cmdString);
					App.scheduledThreadPool.execute(new CancelOrderCommand(messageData));

					String userName = user.getString("first_name");
					boolean hasUsername = !user.isNull("username");
					userName = hasUsername ? user.getString("username") : userName;
					String questionWithAnswer = session.getQuestionWithAnswer();
					String text = "由"+userName+"發起的"+session.getTitle()+"-"+session.getSeries()+"\n"+questionWithAnswer+"\n\n已送出下單請求";
					JSONObject entity = new JSONObject()
							.put("type", hasUsername ? "mention" : "text_mention")
							.put("user", user)
							.put("offset", text.indexOf(userName))
							.put("length", userName.length());
					JSONArray entities = new JSONArray().put(entity);
					tgData = new JSONObject()
							.put("chat_id",chat.get("id"))
							.put("message_id", message.get("message_id"))
							.put("text", text)
							.put("entites", entities);
					tgResult = telegramApi.editMessageText(tgData);	
					session.finish();
				}
				
			} catch(Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		};
	}

}
