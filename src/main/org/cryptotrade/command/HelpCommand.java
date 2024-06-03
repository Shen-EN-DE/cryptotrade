package org.cryptotrade.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONObject;

public class HelpCommand extends Command {
	private static Logger Log = LogManager.getLogger();
	public HelpCommand(JSONObject _messageObject) {
		super(_messageObject);
	}

	@Override
	public String getDesctiption() {
		StringBuilder des = new StringBuilder();
		des.append("顯示所有指令資訊");
		return des.toString();
	}
	
	@Override
	public String getOptionDescription() {
		return getDesctiption();
	}

	@Override
	public void run() {
		try {
			Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
			JSONObject option = new JSONObject() {{
				put("chat_id", receiver);
				put("text", CommandManager.getAllCommandDescription());
			}};
			Log.info("option: "+option);
			JSONObject result = telegramApi.sendMessage(option);
			Log.info("result: "+result);
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}

}
