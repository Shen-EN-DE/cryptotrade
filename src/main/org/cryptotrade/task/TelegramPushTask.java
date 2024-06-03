package org.cryptotrade.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONObject;

public class TelegramPushTask extends Task {
	private static Logger Log = LogManager.getLogger();
	private JSONObject data;
	
	/**
	 * 
	 * @param _data
	 */
	public TelegramPushTask(JSONObject _data) {
		data = _data;
	}
	
	@Override
	public void run() {
		try {
			Telegram tgApi = new Telegram(App.env.get("telegram.api.token"));
			Log.debug("send data: "+data);
			JSONObject result = tgApi.sendMessage(data);
			Log.debug("result data: "+result);
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}
	
	@Override
	public void start() {
		App.scheduledThreadPool.execute(this);
	}

	@Override
	public String getName() {
		return "TelegramPushTask-"+System.currentTimeMillis();
	}

}
