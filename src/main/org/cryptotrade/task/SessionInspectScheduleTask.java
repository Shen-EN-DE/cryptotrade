package org.cryptotrade.task;

import org.cryptotrade.session.SessionManager;

public class SessionInspectScheduleTask extends ScheduledTask {

	@Override
	public void run() {
		SessionManager.inpectSessionList();
	}

	@Override
	public String getName() {
		return "SessionInspectScheduleTask";
	}

}
