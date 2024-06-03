package org.cryptotrade.task;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.cryptotrade.App;

public abstract class ScheduledTask extends Task {
	private ScheduledFuture<?> future;
	
	public void start() {
		start(0, Long.valueOf(App.env.get("system.task.schedule.peroid")), TimeUnit.SECONDS);
	}
	
	public void start(long _delay, long _period, TimeUnit _unit) {
		future = App.scheduledThreadPool.scheduleAtFixedRate(this, _delay, _period, _unit);
		ScheduledTaskManager.addTask(this);
	}
	
	public void finish() {
		future.cancel(false);
	}
	
	public boolean isDone() {
		return future.isDone();
	}
}
