package org.cryptotrade.question;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.OrderSide;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrderSideChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "OrderSideChoose";
	
	public OrderSideChooseQuestion() {
		this("");
	}

	/**
	 * 無作用
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		
	}
	
	public OrderSideChooseQuestion(String _name) {
		super(_name+Name,"選擇做單方向", InputType.Button);
	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		Map<String,String>[] buttonDatas = new Map[2];
		buttonDatas[0] = new HashMap() {{
			put("buttonText",OrderSide.Buy.name());
			put("callbackData",getName()+QueryDataSeparator+OrderSide.Buy.name());
		}};
		buttonDatas[1] = new HashMap() {{
			put("buttonText",OrderSide.Sell.name());
			put("callbackData",getName()+QueryDataSeparator+OrderSide.Sell.name());
		}};
		return ButtonMaker(buttonDatas);
	}

	@Override
	public Runnable getTask(JSONObject _event) {
		return new Runnable(){
			public void run() {
				try {
					if(_event.isNull("callback_query")) {
						Log.warn("只接受callback_query");
						return;
					}
					Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
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
						Log.debug(session.getName()+"->"+OrderSideChooseQuestion.this.getQuestionText()+":"+queryData);
						session.currQuestion().setAnswer(queryData.toLowerCase());
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
	}

}
