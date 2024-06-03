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

public class NormalTextInputQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static enum Type{Number,Text};
	public static final String Name = "NormalTextInput";
	private Type type = Type.Text;

	public NormalTextInputQuestion(String _name,String _questionText,Type _type) {
		super(_name+Name,_questionText, InputType.TypeIn);
		this.type = _type;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Runnable getTask(JSONObject _event) {
		return new Runnable() {
			public void run() {
				try {
					if(_event.isNull("message")) {
						Log.warn("只接受message");
						return;
					}
					Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
					JSONObject message = _event.getJSONObject("message");
					JSONObject user = message.getJSONObject("from");
					JSONObject chat = message.getJSONObject("chat");
					JSONObject tgData;
					JSONObject tgResult;
					String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
					Session session = SessionManager.getSession(sessionName);
					if(session==null) return;
					
					String queryData = message.getString("text");
					Log.debug(session.getName()+"->"+NormalTextInputQuestion.this.getQuestionText()+":"+queryData);
					if(type!=null && type==Type.Number) {
						try {
							Double doubleValue = Double.valueOf(queryData);
							String[] digitArray = queryData.split("\\.");
							int digitLength = digitArray.length>1 ? digitArray[1].length() : 0 ;
							session.currQuestion().setAnswer(String.format("%."+digitLength+"f",doubleValue));
						} catch (Exception e){
							tgData = new JSONObject()
									.put("chat_id", chat.get("id"))
									.put("reply_to_message_id", message.get("message_id"))
									.put("text", "輸入格式錯誤,請輸入數字");
							tgResult = telegramApi.sendMessage(tgData);
							return;
						}
					}
					else {
						session.currQuestion().setAnswer(queryData);
					}
					Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"),Session.StepMessageType.New);
				} catch(Exception e) {
					Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
				}
			}
		};
	}
}
