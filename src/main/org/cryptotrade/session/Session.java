package org.cryptotrade.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.question.Question;
import org.cryptotrade.question.ResultQuestion;
import org.cryptotrade.question.SessionBackQuestion;
import org.cryptotrade.question.SessionCancelQuestion;
import org.cryptotrade.question.SessionNextQuestion;
import org.json.JSONArray;
import org.json.JSONObject;

public class Session {
	private static Logger Log = LogManager.getLogger();
	public static enum StepMessageType{Edit,New}
	private static enum StepType{Next,Current,Back}
	private Long lastUpdateTimestamp = 0L;
	private String name;
	private Long series = System.currentTimeMillis();
	private String title;
	private JSONObject chat;
	private JSONObject user;
	private List<Question> questionList = new ArrayList();
	private List<Question> questionRegisterList = new ArrayList();
	private int progressIndex = -1;
	private int reachIndex = -1;
	
	public Session(JSONObject _user,JSONObject _chat, String _title) {
		this(_user,_chat,_title,null);
	}
	
	private Session(JSONObject _user,JSONObject _chat,String _title,String _name) {
		this.user = _user;
		this.chat = _chat;
		this.title = _title;
		this.name = _name==null ? "uid"+_user.get("id").toString()+":cid"+_chat.get("id").toString() : _name;
		Log.debug("create new session named "+name);
		this.updateTimestamp();
		questionRegisterList.add(new SessionBackQuestion());
		questionRegisterList.add(new SessionCancelQuestion());
		questionRegisterList.add(new SessionNextQuestion());
		SessionManager.addSesstion(this);
	}
	
	public Long getSeries() {
		return this.series;
	}
	
	public void setTitle(String _title) {
		this.title = _title;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public static String getDefaultSesstionName(String _userId,String _chatId) {
		return "uid"+_userId+":cid"+_chatId;
	}
	
	public Question getQuestion(String _name) {
		for(Question question : questionRegisterList) {
			if(question.getName().equalsIgnoreCase(_name)) {
				 return question;
			}
		}
		return null;
	}
	
	public Session addQuestion(Question _question) {
		return addQuestion(_question,false);
	}
	
	/**
	 * 
	 * @param _question 
	 * @param _addToEnd 是否將問題加到最尾端
	 * @return
	 */
	public Session addQuestion(Question _question,boolean _addToEnd) {
		if(progressIndex>-1 && progressIndex+1<questionList.size() && !_addToEnd) {
			questionList.add(progressIndex+1, _question);
		}
		else {
			questionList.add(_question);
		}
		if(questionRegisterList.indexOf(_question)<0)
			questionRegisterList.add(_question);
		return this;
	}
	
	public Session removeQuestion(Question _question) {
		int quesionIndex = questionList.indexOf(_question);
		if(questionList.remove(_question)) {
			reachIndex += quesionIndex<=reachIndex ? -1 : 0;
			progressIndex += quesionIndex<=progressIndex ? -1 : 0;
		}
		questionRegisterList.remove(_question);
		return this;
	}
	
	public Question nextQuestion() {
		if(progressIndex+1<questionList.size()) {
			progressIndex++;
			if(reachIndex<progressIndex) reachIndex = progressIndex;
			return questionList.get(progressIndex);
		}
		else
			return null;
	}
	
	public Question currQuestion() {
		return questionList.get(progressIndex);
	}
	
	public Question preQuestion() {
		if(progressIndex-1>=0) {
			progressIndex--;
			return questionList.get(progressIndex);
		}
		return null;
	}
	
	public JSONObject getInlineKeyboards() {
		JSONArray inlineKeyboards = questionList.get(progressIndex).getInlineKeyboards(this);
		inlineKeyboards = inlineKeyboards==null ? new JSONArray().put(new JSONArray()) : inlineKeyboards;
		JSONArray controlRow = new JSONArray();
		if(progressIndex>0) {
			JSONObject preButton = SessionBackQuestion.getButton();
			controlRow.put(preButton);
		}
		JSONObject cancelButton = SessionCancelQuestion.getButton();
		controlRow.put(cancelButton);
		if(progressIndex<reachIndex) {
			JSONObject preButton = SessionNextQuestion.getButton();
			controlRow.put(preButton);
		}
		inlineKeyboards.put(controlRow);
		return new JSONObject().put("inline_keyboard",inlineKeyboards);
	}
	
	public void updateTimestamp() {
		synchronized(lastUpdateTimestamp) {
			this.lastUpdateTimestamp = System.currentTimeMillis();
		}
	}
	
	protected Long getLastUpdateTimestamp() {
		Long timestamp = 0L;
		synchronized(lastUpdateTimestamp) {
			timestamp = this.lastUpdateTimestamp;
		}
		return timestamp;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getQuestionWithAnswer() {
		String data = "";
		for(int i=0; i<=reachIndex; i++) {
			Question question = questionList.get(i);
			String answer = question.getAnswer();
			if(data.length()>0) data+="\n";
			data += question.getQuestionText()+": "+answer;
		}
		return data;
	}
	
	public String previewAnswer() {
		String data = "";
		for(Question question : questionList) {
			data += " "+question.getAnswer();
		}
		return data.trim();
	}
	
	public String finish() {
		SessionManager.removeSession(this);
		return previewAnswer();
	};

	public static void nextStep(Session _session,String _chatId,Long _messageId) {
		nextStep(_session,_chatId,_messageId,StepMessageType.Edit);
	}
	
	public static void nextStep(Session _session,String _chatId,Long _messageId,StepMessageType _messageType) {
		telegramDataSender(_session,_chatId,_messageId,StepType.Next,_messageType);
	}
	
	public static void currentStep(Session _session,String _chatId,Long _messageId,String _text,StepMessageType _messageType) {
		telegramDataSender(_session,_chatId,_messageId,_text,StepType.Current,_messageType);
	}
	
	public static void backStop(Session _session,String _chatId,Long _messageId) {
		backStop(_session,_chatId,_messageId,StepMessageType.Edit);
	}
	
	public static void backStop(Session _session,String _chatId,Long _messageId,StepMessageType _messageType) {
		telegramDataSender(_session,_chatId,_messageId,StepType.Back,_messageType);
	}
	
	private static void telegramDataSender(Session _session,String _chatId,Long _messageId,StepType _stepType,StepMessageType _messageType) {
		telegramDataSender(_session,_chatId,_messageId,null,_stepType,_messageType);
	}
	
	private static void telegramDataSender(Session _session,String _chatId,Long _messageId,String _text,StepType _stepType,StepMessageType _msessageType) {	
		String userName = _session.user.getString("first_name");
		boolean hasUsername = !_session.user.isNull("username");
		userName = hasUsername ? _session.user.getString("username") : userName;
		String questionWithAnswer = _session.getQuestionWithAnswer();
		String text = "由"+userName+"發起的"+_session.title+"-"+_session.series+"\n";
		JSONObject entity = new JSONObject()
				.put("type", hasUsername ? "mention" : "text_mention")
				.put("user", _session.user)
				.put("offset", text.indexOf(userName))
				.put("length", userName.length());
		JSONArray entities = new JSONArray().put(entity);
		text += questionWithAnswer.isEmpty() ? "" : questionWithAnswer+"\n\n";
		Question question = null;
		if(_stepType==StepType.Next) {
			question = _session.nextQuestion();
			if(question==null) {
				Log.warn("沒有更多的Question");
				return;
			}
		}
		else if(_stepType==StepType.Current) {
			question = _session.currQuestion();
		}
		else if(_stepType==StepType.Back) {
			question = _session.preQuestion();
			if(question==null) {
				Log.warn("沒有前一個Question");
				return;
			}
		}
		
		text += (_text==null?"":_text+"\n")+question.getQuestionText();
		if(question instanceof ResultQuestion) text += "\n>>["+_session.previewAnswer()+"]<<";
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		JSONObject tgData = new JSONObject()
				.put("chat_id", _chatId)
				.put("text", text)
				.put("entities", entities)
				.put("reply_markup", _session.getInlineKeyboards())
				.put("message_id", _messageId);
		if(_msessageType==StepMessageType.Edit) {
			JSONObject tgResult = telegramApi.editMessageText(tgData);	
		}
		else if(_msessageType==StepMessageType.New) {
			JSONObject tgResult = telegramApi.sendMessage(tgData);	
		}
		
	}

	public void begin() {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		Question question = this.nextQuestion();
		String userName = this.user.getString("first_name");
		boolean hasUsername = !this.user.isNull("username");
		userName = hasUsername ? this.user.getString("username") : userName;
		String text = "由"+userName+"發起的"+title+"-"+series+"\n"+question.getQuestionText();
		JSONObject entity = new JSONObject()
				.put("type", hasUsername ? "mention" : "text_mention")
				.put("user", this.user)
				.put("offset", text.indexOf(userName))
				.put("length", userName.length());
		JSONArray entities = new JSONArray().put(entity);
		JSONObject tgData = new JSONObject()
				.put("chat_id", chat.get("id"))
				.put("text", text)
				.put("entities", entities)
				.put("reply_markup", this.getInlineKeyboards());
		JSONObject tgResult = telegramApi.sendMessage(tgData);		
	}
	
	public void notifySessionTimeout() {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		String userName = this.user.getString("first_name");
		boolean hasUsername = !this.user.isNull("username");
		userName = hasUsername ? this.user.getString("username") : userName;
		String text = userName+" ["+this.title+"-"+this.series+"]對話閒置已超過"+App.env.get("session.idle.limit","300")+"秒,對話已結束";
		JSONObject entity = new JSONObject()
				.put("type", hasUsername ? "mention" : "text_mention")
				.put("user", this.user)
				.put("offset", text.indexOf(userName))
				.put("length", userName.length());
		JSONArray entities = new JSONArray().put(entity);
		JSONObject tgData = new JSONObject()
				.put("chat_id", this.chat.get("id"))
				.put("text", text)
				.put("entities", entities);
		JSONObject tgResult = telegramApi.sendMessage(tgData);		
	}
}
