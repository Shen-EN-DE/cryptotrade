package org.cryptotrade.question;

import org.cryptotrade.session.Session;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConfirmQuestion extends Question {
	protected static final String Name = "Confirm"; 
	private Runnable task;

	public ConfirmQuestion(String _name,String _questionText,InputType _type) {
		super(_name+Name, _questionText, _type);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * 是否顯示"取消"按鈕
	 * @param _isShow
	 */
	@Override
	public void isShowExtraOpionButton(boolean _isShow) {
		this.showOptionButton = _isShow;
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
		if(this.showOptionButton) {
			row.put(new JSONObject()
					.put("text", "取消")
					.put("callback_data", getName()+QueryDataSeparator+ConfirmAnswer.Cancel)
			);
		}
		JSONArray inlineKeyboards = new JSONArray().put(row);
		return inlineKeyboards;
	}
	
	public void setTask(Runnable _task) {
		this.task = _task;
	}

	@Override
	public Runnable getTask(JSONObject _event) {
		return task;
	}
}
