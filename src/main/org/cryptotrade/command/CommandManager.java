package org.cryptotrade.command;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONObject;

public class CommandManager {
	private static Map<String,Class<Command>> CommandMap = new HashMap<>();
	private static Logger Log = LogManager.getLogger();
	private static ExecutorService taskThreadPool = Executors.newCachedThreadPool(); 
	
	public static void registerCommand(String _commandName, Class _commandClass) {
		CommandMap.put(_commandName, _commandClass);
	}
	
	public static Constructor<Command> getCommandInstance(String _commandNmae) {
		Class<Command> clazz = CommandMap.get(_commandNmae);
		Constructor<Command> ctor = null;
		if(clazz!=null) {
			try {
				ctor = clazz.getConstructor(JSONObject.class);
			} catch (NoSuchMethodException | SecurityException e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		}
		return ctor;
	}
	
	public static String getAllCommandDescription() {
		StringBuilder msg = new StringBuilder();
		if(CommandMap.isEmpty()) return msg.toString();
		msg.append("以下為可用指令:");
		CommandMap.forEach((_commandName, _command)->{
			try {
				Constructor<Command> cnst = _command.getConstructor(JSONObject.class);
				Command cmd = cnst.newInstance(new JSONObject());
				if(msg.length()>0) msg.append("\n");
				msg.append("\t/").append(_commandName).append(" - ").append(cmd.getDesctiption());
			} catch (Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		});
		
		return msg.toString();
	}
	
	public static void execute(Command _command) {
		taskThreadPool.execute(_command);
	}
	
	
}
