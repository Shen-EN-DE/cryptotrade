package org.cryptotrade.command;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONObject;

public class DataReportCommand extends Command{
	private static Logger Log = LogManager.getLogger();
	public DataReportCommand(JSONObject _messageObject) {
		super(_messageObject);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getDesctiption() {
		return "取得目前最新報整合資訊";
	}

	@Override
	public void run() {
		try {
			Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
			JSONObject tgData = new JSONObject()
					.put("chat_id", receiver)
					.put("reply_to_message_id", replyTo)
					.put("text", "已收到請求,資料處理中,請稍等");
			Log.debug("tgData: "+tgData);
			JSONObject tgResult = telegramApi.sendMessage(tgData);
			Log.debug("tgResult: "+tgResult);
			HttpURLConnection connection = null;

	    //Create connection
	  	String targetURL = "http://194.233.64.158:8002/coinStrategy?Secret=66fb1e0553ed813fd3f683f3df46fe2dd4e8d97461120e286b22989b8959977a";
	    URL url = new URL(targetURL);
	    String urlParameters = "";
	    connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestMethod("GET");
	    connection.setUseCaches(false);
	    connection.setDoOutput(true);
	    int responseCode = connection.getResponseCode();
	    Log.debug("responseCode: "+responseCode);
	    //Get Response  
	    InputStream is = connection.getInputStream();
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
	    String line;
	    while ((line = rd.readLine()) != null) {
	      response.append(line);
	      response.append('\r');
	    }
	    rd.close();
		 
			//HttpResponse<String> response = HttpHelper.Get("http://194.233.64.158:8002/coinStrategy?Secret=66fb1e0553ed813fd3f683f3df46fe2dd4e8d97461120e286b22989b8959977a", null);
			Log.debug("Python API response: "+response.toString());
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}

}
