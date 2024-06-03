package org.cryptotrade.command;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public abstract class Command implements Runnable{	
	protected String receiver;
	protected String replyTo;
	protected String fullCommand;
	protected Map<String,String> optionDescription;
	protected Map<String,String> commandOption = new HashMap<>();
	protected JSONObject message;
	public Command(JSONObject _messageObject) {
		message = _messageObject;
		if(_messageObject!=null && !_messageObject.isEmpty()) {
			JSONObject chat = message.getJSONObject("chat");
			this.receiver = chat.get("id").toString();
			this.replyTo = message.get("message_id").toString();
			this.fullCommand = message.getString("text");
			commandReveal(fullCommand);
		}
	}
	
	protected void commandReveal(String _fullCommand) {
		String[] array1 = _fullCommand.split("\\s+\\-");
		
		// 從1開始,跳過0,0是command主體
		for(int i=1; i<array1.length; i++) {
			String text = array1[i];
			String optionCmd = text.split("\\s+")[0].replace("-","");
			String optionData = text.substring(optionCmd.length());
			commandOption.put(optionCmd, optionData.trim());
		}
	}

	public String getOptionDescription() {
		StringBuilder sb = new StringBuilder();
		if(optionDescription!=null) {
			optionDescription.forEach((option,description)->{
				if(sb.length()>0) sb.append("\n");
				sb.append("\t-").append(option).append("\t").append(description);
			});
		}
		return sb.toString();
	}
	
	public abstract String getDesctiption();
	public abstract void run();
}
