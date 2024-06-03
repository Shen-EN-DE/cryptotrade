package org.cryptotrade.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApiManager {
	private static Logger Log = LogManager.getLogger();
	private Map<String,Map<String,Object>> apiMap = new HashMap();
	
	public void addApi(String _name,ApiType _type,Object _apiObj) {
		Map<String,Object> typeMap = apiMap.get(_name);
		if(apiMap.get(_name)==null) 
			apiMap.put(_name, new HashMap());
		apiMap.get(_name).put(_type.toString(), _apiObj);
	}
	
	public Object getApi(String _name,ApiType _type) {
		Object apiObj = null;
		if(apiMap.get(_name)!=null)
			apiObj = apiMap.get(_name).get(_type.toString());
		return apiObj;
	}
}
