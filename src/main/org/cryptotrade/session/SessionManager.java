package org.cryptotrade.session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.task.ScheduledTask;
import org.cryptotrade.task.SessionInspectScheduleTask;

public class SessionManager {
	private static Logger Log = LogManager.getLogger();
	private static List<Session> sessionList = new ArrayList<>();
	private static ScheduledTask scheduledTask;
	
	public static void startBackgroudWork() {
		if(scheduledTask==null)
			scheduledTask = new SessionInspectScheduleTask();
		scheduledTask.start(1,1,TimeUnit.SECONDS);
	}
	
	public static void stopBackgroudWork() {
		scheduledTask.finish();
	}
	
	public static void inpectSessionList() {
		Long currentTimestamp = System.currentTimeMillis();
		Long sessionIdleLimit = Long.valueOf(App.env.get("session.idle.limit","300"))*1000;
		for(int i=sessionList.size()-1; i>=0; i--) {
			Session session = sessionList.get(i);
			Long idle = currentTimestamp - session.getLastUpdateTimestamp();
			if(idle>=sessionIdleLimit) {
				session.notifySessionTimeout();
				session.finish();
				Log.debug("移除Session["+session.getName()+"]");
			}
		}
	}
	
	protected static void removeSession(Session _session) {
		sessionList.remove(_session);
	}
	
	public static Session getSession(String _name) {
		Session session = null;
		for(Session s : sessionList) {
			if(s.getName().equalsIgnoreCase(_name)) {
				session = s;
				break;
			}
		}
		return session;
	}
	
	public static void addSesstion(Session _session) {
		sessionList.add(_session);
	}	
}
