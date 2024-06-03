package org.cryptotrade.question;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.entity.OrderType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrderTypeChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "OrderTypeChoose";

	public OrderTypeChooseQuestion() {
		this("");
	}

	public OrderTypeChooseQuestion(String _name) {
		super(_name+Name,"選擇下單類型", InputType.Button);
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
		Map<String,String>[] buttonDatas = new Map[2];
		buttonDatas[0] = new HashMap() {{
			put("buttonText",OrderType.Market.name());
			put("callbackData",getName()+QueryDataSeparator+OrderType.Market.name());
		}};
		buttonDatas[1] = new HashMap() {{
			put("buttonText",OrderType.Limit.name());
			put("callbackData",getName()+QueryDataSeparator+OrderType.Limit.name());
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
						Log.debug(session.getName()+"->"+OrderTypeChooseQuestion.this.getQuestionText()+":"+queryData.toLowerCase());
						Question currQuesion = session.currQuestion();
						currQuesion.setAnswer(queryData);
						Question price = session.getQuestion(PriceTextInputQuestion.Name);
						if(queryData.equalsIgnoreCase(OrderType.Market.name())) {
							if(price!=null) {
								session.removeQuestion(price);
							}
						}
						else if(queryData.equalsIgnoreCase(OrderType.Limit.name())) {
							if(price==null) {
								price = new PriceTextInputQuestion();
								if(currQuesion.getAnswer().length()!=currQuesion.getPureAnswer().length())
									price.setAnswerPrefix("-"+CommandOptionEnum.Price.getSymbol()+" ");
								session.addQuestion(price);
							}
						}
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
