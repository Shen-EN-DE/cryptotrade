package org.cryptotrade.test.api;

import static org.cryptotrade.helper.UtilsHelper.Console;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class TelegramTest {
	private static Telegram api;
	private static String receiver;
	private static String message;
	
	@BeforeClass
	public static void setBeforeClass() {
		api = new Telegram(App.env.get("telegram.api.token"));
		receiver = App.env.get("telegram.api.test.receiver");
		message = "This is test message from unit test";
	}
	
	@Test
	public void sendMessage() {
		JSONObject option = new JSONObject() {{
			put("chat_id", receiver);
			put("text", message);
		}};
		JSONObject data = api.sendMessage(option);
		assertNotNull("sendMessage() not working",data);
		JSONObject result = data.getJSONObject("result");
		assertNotNull("sendMessage() send fail", result);
		String text = result.getString("text");
		assertTrue("get wrong message back: "+text, text.equalsIgnoreCase(message));
	}
}
