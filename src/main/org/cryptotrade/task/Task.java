package org.cryptotrade.task;

public abstract class Task implements Runnable {
	public abstract void run();
	public abstract void start();
	public abstract String getName();
}
