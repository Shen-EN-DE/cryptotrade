package org.cryptotrade.http.handler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.command.Command;
import org.cryptotrade.command.CommandManager;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.question.Question;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TelegramWebhookHandler implements HttpHandler {
	
	private static Logger Log = LogManager.getLogger();
	private static String authorization;
	
	public TelegramWebhookHandler(String _authorization) {
		authorization = _authorization;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String data = "True";
			exchange.getResponseHeaders().add("Content-Type","application/json");
			Map<String,List<String>> headers = exchange.getRequestHeaders();
			StringBuilder strBuilder = new StringBuilder().append("\n");
			boolean isAuthorized = false;
			Iterator<Entry<String,List<String>>> it = headers.entrySet().iterator();
			while(it.hasNext()){
				Entry<String,List<String>> entry = it.next();
				strBuilder.append(entry.getKey()).append(" : ");
				String _headData = "";
				for(String context : entry.getValue()) 
					_headData += (context+"\n");
				strBuilder.append(_headData);
				if(authorization!=null && entry.getKey().compareToIgnoreCase("X-telegram-bot-api-secret-token")==0){
					isAuthorized ^= _headData.trim().compareToIgnoreCase(authorization)==0;
				}
			}
			// 設定檔位設定authorization,表示不用驗證
			if(!isAuthorized && authorization==null) isAuthorized = true;
	
			BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			String requestData = null;
			strBuilder.append("Request data: \n");
			String body = "";
			while((requestData=in.readLine())!=null) {
				body += requestData+"\n";
			}
			body = body.replace("\n", "");
			
//			strBuilder.append(UtilsHelper.jsonBeautify(UtilsHelper.DecodeUnicode(body)));
			strBuilder.append(UtilsHelper.DecodeUnicode(body));
			if(!isAuthorized) {			
				strBuilder = new StringBuilder()
						.append("\n!!!!! Authorize fail !!!!!")
						.append(strBuilder.toString().replace("\n", "\n!! "))
						.append("\n!!!!! Authorize fail !!!!!");
			}
			Log.info(strBuilder.toString());
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, data.length());
			PrintWriter out = new PrintWriter(new BufferedOutputStream(exchange.getResponseBody()));
			out.print(data.toString());
			out.flush();
			exchange.close();
			
			if(!isAuthorized) return;
			
			// 安全應證通過,開始處理指令
			/**
			 * 接收文件格式
			 * {
				  "update_id" : 553823071,
				  "message" : {
				    "message_id" : 29718,
				    "from" : {
				      "id" : 5236664583,
				      "is_bot" : false,
				      "first_name" : "瑞克🧙",
				      "language_code" : "zh-hant"
				    },
				    "chat" : {
				      "id" : -790302296,
				      "title" : "🤖推撥測試",
				      "type" : "group",
				      "all_members_are_administrators" : true
				    },
				    "date" : 1659247532,
				    "text" : "/test 測試",
				    "entities" : [
				      {
				        "offset" : 0,
				        "length" : 5,
				        "type" : "bot_command"
				      }
				    ]
				  }
				}
			*/
			JSONObject event = new JSONObject(body);
			
			// 只處理messagne,不處理edit_massage
			if(event.has("message")) handleMassege(event);	
			if(event.has("callback_query")) handleCallbackQeruy(event);
			
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			exchange.sendResponseHeaders(500, 0);
			exchange.close();
		}
	}
	
	private void commandHandler(JSONObject _message,String _text) throws InstantiationException,
				IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<String> commands = UtilsHelper.getRegepMatchString("(^/[a-zA-Z0-9]+)|(\\s+/[a-zA-Z0-9]+)", _text);
		boolean hasCorresponseCommand = false;
		for(int i=0; i<commands.size(); i++) {
			String currentCmd = commands.get(i);
			String nextCmd = (i+1<commands.size()) ? commands.get(i+1) : null;
			Constructor<Command> cnst = CommandManager.getCommandInstance(currentCmd.replace("/",""));
			if(cnst==null) continue;
			
			Command cmd = cnst.newInstance(_message);
			CommandManager.execute(cmd);
			hasCorresponseCommand = true;
		}
		if(!hasCorresponseCommand)
			Log.info("沒有對應的可處理得Command => "+commands);
	}
	
	private void handleMassege(JSONObject _even) throws InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			JSONException {		
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		JSONObject message = _even.getJSONObject("message");
		JSONObject chat = message.getJSONObject("chat");
		JSONObject user = message.getJSONObject("from");
		String text = message.isNull("text") ? "" : message.getString("text").replace("\\n", "\n");
		
		String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
		Session session = SessionManager.getSession(sessionName);
		if(session!=null) {
			session.updateTimestamp();
			Question question = session.currQuestion();
			Question.InputType type = question.getType();
			if(type==Question.InputType.Button) {
				JSONObject tgData = new JSONObject()
						.put("chat_id", chat.get("id"))
						.put("reply_to_message_id", message.get("message_id"))
						.put("text", "目前還有對話尚未完成,請先完成對話內容:\n"+question.getQuestionText())
						.put("reply_markup", session.getInlineKeyboards());
				JSONObject tgResult = telegramApi.sendMessage(tgData);
			}
			if(type==Question.InputType.TypeIn) {
				if(!question.isQualifiedInputAnswer(text)) {
					JSONObject tgData = new JSONObject()
							.put("chat_id", chat.get("id"))
							.put("reply_to_message_id", message.get("message_id"))
							.put("text", "輸入的格式不符,請重新輸入;\n"+question.getQuestionText());
					JSONObject tgResult = telegramApi.sendMessage(tgData);
				}
				else {
					Runnable task = question.getTask(_even);
					App.scheduledThreadPool.execute(task);
				}
			}
		}

		if(!message.isNull("entities")) {
			JSONArray entities = message.getJSONArray("entities");
			for(int i=0; i<entities.length(); i++) {
				JSONObject entity = entities.getJSONObject(i);
				if(entity.getString("type").equalsIgnoreCase("bot_command")) {
					// TODO
				} else
				if(entity.getString("type").equalsIgnoreCase("url")) {
					// TODO
				} else
				if(entity.getString("type").equalsIgnoreCase("text_mention")) {
					// TODO
				} else
				if(entity.getString("type").equalsIgnoreCase("mention")) {
					// TODO
				} else
				if(entity.getString("type").equalsIgnoreCase("hashtag")) {
					// TODO					
				} else
				if(entity.getString("type").equalsIgnoreCase("cashtag")) {
					// TODO					
				} else
				if(entity.getString("type").equalsIgnoreCase("email")) {
					// TODO					
				} else
				if(entity.getString("type").equalsIgnoreCase("phone_number")) {
					// TODO					
				} else
				if(entity.getString("type").equalsIgnoreCase("bold")) {
					// TODO					
				} else
				if(entity.getString("type").equalsIgnoreCase("italic")) {
					// TODO
				}else
				if(entity.getString("type").equalsIgnoreCase("strikethrough")) {
					// 
				}else
				if(entity.getString("type").equalsIgnoreCase("spoiler")) {
					// TODO
				}else
				if(entity.getString("type").equalsIgnoreCase("code")) {
					// TODO
				}else
				if(entity.getString("type").equalsIgnoreCase("pre")) {
					// TODO
				}else
				if(entity.getString("type").equalsIgnoreCase("text_link")) {
					// TODO
				}
			} // end for
		}	// end if
		if(text.indexOf("/")==0) {
			String replyTo = message.get("message_id").toString();
			commandHandler(message,text);
		}
	}
	
	private void handleCallbackQeruy(JSONObject _event) {
		Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
		JSONObject callbackQuery = _event.getJSONObject("callback_query");
		JSONObject message = callbackQuery.getJSONObject("message");
		JSONObject user = callbackQuery.getJSONObject("from");
		JSONObject chat = message.getJSONObject("chat");
		String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
		Session session = SessionManager.getSession(sessionName);
		if(session==null) {
			Log.warn("沒有對應的Session => "+sessionName);
			return;
		}
		String data = callbackQuery.getString("data");
		String questionName = data.split(Question.QueryDataSeparator)[0];
		Log.debug("Reply question name: "+questionName);
		Question question = session.getQuestion(questionName);
		if(question!=null) {
			Runnable task = question.getTask(_event);
			App.scheduledThreadPool.execute(task);
		}
		else {
			Log.warn("沒有對應的Question => "+questionName);
			JSONObject tgData = new JSONObject()
					.put("callback_query_id", callbackQuery.get("id"));
			JSONObject tgResult = telegramApi.answerCallbackQuery(tgData);
		}
	}
}
