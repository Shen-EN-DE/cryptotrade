package org.cryptotrade.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PerpContract {
	private static Map<String, Map<String, Object>> SymbolDataMap = new HashMap<>();
	private static List<Entry<String,Double>> FoundingRateList = new ArrayList<>();
	
	public static void SetSymbolDataMap(Map<String, Map<String, Object>> map) {
		synchronized(SymbolDataMap) {
			SymbolDataMap = map;
		}
	}
	
	public static Map<String, Map<String, Object>> GetSymbolDataMap(){
		Map<String,Map<String,Object>> map = new HashMap<String,Map<String,Object>>();
		synchronized(SymbolDataMap) {
			map = SymbolDataMap;
		}
		return map;
	}
	
	public static void SetFoundingRateList(List<Entry<String,Double>> list) {
		synchronized(FoundingRateList) {
			FoundingRateList = list;
		}
	}
	
	public static List<Entry<String,Double>> GetFoundingRateList(){
		List<Entry<String,Double>> list = new ArrayList<>();
		synchronized(FoundingRateList) {
			list = FoundingRateList;
		}
		return list;
	}
}
