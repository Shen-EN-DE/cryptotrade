package org.cryptotrade.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskManager extends Task {
	private static Map<String,ScheduledTask> taskMap = new HashMap<>();
	
	@Override
	public void start() {
		
	}
	
	/**
	 * 清除已經完成的ScheduledTask
	 */
	@Override
	public void run() {
		synchronized(taskMap) {
			Map<String,ScheduledTask> tmp = new HashMap<>();
			taskMap.forEach((_name,_task)->{
				if(!_task.isDone()) tmp.put(_name, _task);
			});
			taskMap = tmp;
		}
	}
	
	public static void addTask(ScheduledTask _task) {
		synchronized(taskMap) {
			taskMap.put(_task.getName(), _task);
		}
	}
	
	/**
	 * 
	 * @param _taskName
	 * @param _newPeriod
	 * @param _unit
	 * @return 如果變動成功回傳true,否則回傳false(找不到該task,或該task已經結束)
	 */
	public static boolean resetTaskPeriod(String _taskName, long _newPeriod, TimeUnit _unit) {
		boolean flag = false;
		synchronized(taskMap) {
			if(taskMap.containsKey(_taskName)) {
				ScheduledTask task = taskMap.get(_taskName);
				if(task!=null && task instanceof OrderStatusTraceScheduledTask && !task.isDone()) {
					task.finish();
					var tmpTask = new OrderStatusTraceScheduledTask((OrderStatusTraceScheduledTask)task);
					tmpTask.start(0L, _newPeriod, _unit);
					flag = true;
				}
			}
		}
		return flag;
	}

	@Override
	public String getName() {
		return "TaskManager";
	}
}
