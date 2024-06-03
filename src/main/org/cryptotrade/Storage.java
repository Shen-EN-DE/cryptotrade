package org.cryptotrade;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class Storage {
	private static Logger Log = LogManager.getLogger();
	public static enum StorageName{
		CompareRateData
	};
	private static Map<StorageName,JSONObject> storage = new HashMap();
	
	public static JSONObject getData(StorageName _name) {
		JSONObject returnData = null;
		synchronized(storage) {
			returnData = storage.get(_name);
		}
		return returnData;
	}
	
	public static void setData(StorageName _name,JSONObject _data) {
		synchronized(storage) {
			storage.put(_name, _data);
		}
	}
}
