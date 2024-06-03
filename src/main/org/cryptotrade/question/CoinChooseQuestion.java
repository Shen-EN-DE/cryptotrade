package org.cryptotrade.question;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class CoinChooseQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static final String Name = "CoinChoose";
	private List<String> option;
	private int buttonColumns = 3;
	
	public CoinChooseQuestion(List<String> _option) {
		this("","請選擇幣種",_option);
	}

	public CoinChooseQuestion(String _name, String _questionText,List<String> _option) {
		super(_name+Name, _questionText, InputType.Button);
		this.option = _option;
	}
	
	public void setButtonColumns(int _column) {
		this.buttonColumns = _column;
	}

	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		// TODO Auto-generated method stub

	}

	@Override
	public JSONArray getInlineKeyboards(Session _session) {
		JSONArray inlineKeyboards = new JSONArray();
		for(int i=0;i<option.size();) {
			JSONArray row = new JSONArray();
			for(int j=0; j<buttonColumns; j++) {
				if(i>=option.size()) break;
				String coin = option.get(i++);
				JSONObject button = new JSONObject()
						.put("text", coin)
						.put("callback_data", getName()+QueryDataSeparator+coin);
				row.put(button);
			}
			inlineKeyboards.put(row);
		}
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
				String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
				Session session = SessionManager.getSession(sessionName);
				if(session==null) return;
				
				String[] dataArray = callbackQuery.getString("data").split(QueryDataSeparator);
				String queryData = dataArray[1];				
				
				String answer = null;
				for(String coin : option) {
					if(queryData.equalsIgnoreCase(coin)) {
						answer = coin;
						break;
					}
				}
				if(answer==null) return;
				
				this.setAnswer(answer);				
				Session.nextStep(session, chat.get("id").toString(), message.getLong("message_id"));
				
			} catch(Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		};
	}

}
