package org.cryptotrade.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.helper.HttpHelper;
import org.cryptotrade.helper.UtilsHelper;
import static org.cryptotrade.helper.UtilsHelper.Console;
import org.json.JSONObject;

public class Telegram {
	private static final String ApiBaseUrl = App.env.get("telegram.api.url", "https://api.telegram.org")+"/bot";
	private static Logger Log = LogManager.getLogger();
	private String token;
	
	public Telegram(String _token) {
		this.token = _token;
	}
	
	private JSONObject send(String _endPoint, JSONObject _data) {
		String url = ApiBaseUrl+token+"/"+_endPoint;
		Log.info("url: "+url);
		Log.info("data: "+_data);
		Map<String,String> header = new HashMap<>() {{
			put("Content-Type", HttpHelper.ContentType_JSON);
		}};
		Map<String,String> data = new HashMap<>() {{
			put("__json_data__", _data.toString());
		}};
		Map<String,Map<String,String>> option = new HashMap<>(){{
			put("header", header);
			put("data", data);
		}};
		try {
			String result = HttpHelper.Post(url, option).body();
			Log.info("result: "+UtilsHelper.DecodeUnicode(result));
			return new JSONObject(result);
		} catch (IOException|InterruptedException e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
		return null;
	}
	
	/**
	 * 送出Message事件
	 * @param _option JSONObject<br>
	 * <ul>
	 * 	<li>chat_id - Integer|String : Unique identifier for the target chat or username 
	 * 			of the target channel (in the format @channelusername)</li>
	 * 	<li>text - String : Text of the message to be sent, 1-4096 characters after entities parsing</li>
	 * 	<li>parse_mode - String : (Optional) Mode for parsing entities in the message text. 
	 * 			See "formatting options" for more details.</li>
	 * 	<li>entities - JSONArray : (Optional) list of special entities(MessageEntity) that appear in message text,
	 * 			which can be specified instead of parse_mode</li>
	 * 	<li>disable_web_page_preview - Boolean : (Optional) Disables link previews for links in this message</li>
	 * 	<li>disable_notification - Boolean : (Optional) Sends the message silently. Users will receive a notification with no sound.</li>
	 * 	<li>protect_content - Boolean : (Optional) Protects the contents of the sent message from forwarding and saving</li>
	 * 	<li>reply_to_message_id - Integer : (Optional) If the message is a reply, ID of the original message</li>
	 * 	<li>allow_sending_without_reply - Boolean : (Optional) Pass True, if the message should be sent 
	 * 			even if the specified replied-to message is not found</li>
	 * 	<li>reply_markup - JSONObject : (Optional) Object(InlineKeyboardMarkup or ReplyKeyboardMarkup or 
	 * 			ReplyKeyboardRemove or ForceReply) for an inline keyboard, custom reply keyboard, instructions 
	 * 			to remove reply keyboard or to force a reply from the user.	</li>
	 * </ul>
	 * @return JSONObject
	 */
	public JSONObject sendMessage(JSONObject _data) {
		return send(UtilsHelper.getMethodName(), _data);	
	}

	/**
	 * 編輯Message事件
	 * @param _option JSONObject<br>
	 * <ul>
	 * 	<li>chat_id - Integer|String : (Opt) Required if inline_message_id is not specified. 
	 * 			Unique identifier for the target chat or username of the target channel (in the 
	 * 			format @channelusername)</li>
	 * 	<li>message_id - Integer : (Opt) Required if inline_message_id is not specified. 
	 * 			Identifier of the message to edit</li>
	 * 	<li>inline_message_id - String : (Opt) Required if chat_id and message_id are not 
	 * 			specified. Identifier of the inline message</li>
	 * 	<li>text - String : Text of the message to be sent, 1-4096 characters after entities parsing</li>
	 * 	<li>parse_mode - String : (Opt) Mode for parsing entities in the message text. See formatting options for more details.</li>
	 * 	<li>entities - JSONArray : (Opt) list of special entities(MessageEntity) that appear in message text,
	 * 			which can be specified instead of parse_mode</li>
	 * 	<li>disable_web_page_preview - Boolean : (Opt) Disables link previews for links in this message</li>
	 * 	<li>reply_markup - JSONObject : (Opt) Object(InlineKeyboardMarkup or ReplyKeyboardMarkup or 
	 * 			ReplyKeyboardRemove or ForceReply) for an inline keyboard, custom reply keyboard, instructions 
	 * 			to remove reply keyboard or to force a reply from the user.	</li>
	 * </ul>
	 * @return JSONObject
	 */
	public JSONObject editMessageText(JSONObject _data) {
		return send(UtilsHelper.getMethodName(), _data);	
	}
	
	/**
	 * 
	 * @param _data <br>
	 * <ul>
	 * <li>callback_query_id - String : Unique identifier for the query to be answered</li>
	 * <li>text - String :(Optional) Text of the notification. If not specified, nothing 
	 * 		will be shown to the user, 0-200 characters</li>
	 * <li>show_alert - Boolean : (Optional) If True, an alert will be shown by the client 
	 * 		instead of a notification at the top of the chat screen. Defaults to false.</li>
	 * <li>url - String : (Optional) URL that will be opened by the user's client. If you 
	 * 		have created a Game and accepted the conditions via @BotFather, specify the URL 
	 * 		that opens your game - note that this will only work if the query comes from a 
	 * 		callback_game button. Otherwise, you may use links like t.me/your_bot?start=XXXX 
	 * 		that open your bot with a parameter.</li>
	 * <li>cache_time - Integer : (Optional) The maximum amount of time in seconds that the 
	 * 		result of the callback query may be cached client-side. Telegram apps will support 
	 * 		caching starting in version 3.14. Defaults to 0.</li>
	 * </ul>
	 * @return JSONObject
	 */
	public JSONObject answerCallbackQuery(JSONObject _data) {
		return send(UtilsHelper.getMethodName(), _data);	
	}
	
	/**
	 * 
	 * @param _data<br>
	 * <ul>
	 * 	<li>chat_id - Integer|String : Unique identifier for the target chat or username of
	 * 			the target channel (in the format @channelusername)</li>
	 * 	<li>message_id - Integer : Identifier of the message to delete</li>
	 * </ul>
	 * @return
	 */
	public JSONObject deleteMessage(JSONObject _data) {
		return send(UtilsHelper.getMethodName(), _data);	
	}
}
