package org.cryptotrade.question;

import java.util.Map;

import org.cryptotrade.session.Session;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class Question {
	public static enum InputType {Button,TypeIn};
	protected static enum ConfirmAnswer{Yes,No,Cancel};
	protected static enum ProgressAnswer{Back,Next,Cancel};
	private String questionText;
	private String answer = "";
	protected String name;
	private String answePrefix = "";
	public final static String QueryDataSeparator = ":";
	protected boolean showOptionButton = false;
	private InputType type;
	
	protected Question(String _name,String _questionText,InputType _type) {
		this.questionText = _questionText;
		this.type = _type;
		this.name = _name;
	}
	
	public Question setAnswerPrefix(String _prefix) {
		answePrefix = _prefix;
		return this;
	}	

	/**
	 * 是否要要顯示額外選單紐,交由繼承的子類別決定實作內容
	 * @param _isShow
	 */
	public abstract void isShowExtraOpionButton(boolean _isShow);
	
	public String getName() {
		return this.name;
	}
	
	public InputType getType() {
		return this.type;
	}
	
	public boolean isQualifiedInputAnswer(String _inputAnswer) {
		return true;
	}
	
	public abstract JSONArray getInlineKeyboards(Session _session);
	
	public abstract Runnable getTask(JSONObject _event);
	
	public String getQuestionText() {
		return this.questionText;
	}
	
	public void setQuestionText(String _questionText) {
		this.questionText = _questionText;
	}
	
	public void setAnswer(String _answer) {
		this.answer = _answer;
	}
	
	public String getAnswer() {
		return this.answePrefix+this.answer;
	}
	
	public String getPureAnswer() {
		return this.answer;
	}
	
	
	protected static JSONArray ButtonMaker(Map<String,String>[] _buttonDatas) {
		JSONArray inlineKeyboard = null;
		JSONArray row1 = new JSONArray();
		for(Map<String,String> map : _buttonDatas) {
			JSONObject button = new JSONObject()
					.put("text", map.get("buttonText"))
					.put("callback_data",map.get("callbackData"));
			row1.put(button);
		}
		if(!row1.isEmpty()) {
			inlineKeyboard = new JSONArray();
			inlineKeyboard.put(row1);
		}
		return inlineKeyboard;
	}

}
