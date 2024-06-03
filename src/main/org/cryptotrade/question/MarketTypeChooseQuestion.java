package org.cryptotrade.question;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.MarketType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class MarketTypeChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "MarketTypeChoose";

	public MarketTypeChooseQuestion() {
		this("");
	}
	
	public MarketTypeChooseQuestion(String _name) {
		super(_name+Name,"選擇交易市場類型", InputType.Button);
		// TODO Auto-generated constructor stub
	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		Map<String,String>[] buttonDatas = new Map[3];
		buttonDatas[0] = new HashMap() {{
			put("buttonText",MarketType.Spot.name());
			put("callbackData",getName()+QueryDataSeparator+MarketType.Spot.name());
		}};
		buttonDatas[1] = new HashMap() {{
			put("buttonText",MarketType.Margin.name());
			put("callbackData",getName()+QueryDataSeparator+MarketType.Margin.name());
		}};
		buttonDatas[2] = new HashMap() {{
			put("buttonText",MarketType.Contract.name());
			put("callbackData",getName()+QueryDataSeparator+MarketType.Contract.name());
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
						Log.debug(session.getName()+"->"+MarketTypeChooseQuestion.this.getQuestionText()+":"+queryData);
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
	}

	/**
	 * 無作用
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		
	}
}
