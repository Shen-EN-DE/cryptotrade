package org.cryptotrade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cryptotrade.entity.PerpContract;

public class PerpContractDataFetchTask implements Runnable{
	
	public void run() {
		Map<String,Map<String,Object>> symbols = getPerpFuture();
		PerpContract.SetSymbolDataMap(symbols);
		
		// 將所有永續合約的資金費率列表
		Iterator<Entry<String, Map<String, Object>>> itSymbolList = symbols.entrySet().iterator();
		
		// 鎖定FoundingRate以確保不會有其他執行續在更新資料時讀取
		List<Entry<String,Double>> foundingRateList = new ArrayList<>();
		Map<String,Double> rateMap = new HashMap<>();
		while(itSymbolList.hasNext()) {
			Entry<String, Map<String, Object>> symbolElement = itSymbolList.next();
			String symbol = symbolElement.getKey();
			// 跳過非永續合約期貨
			if(!symbol.contains("-PERP")) continue; 

			Iterator<Entry<String,Object>> itSymbol = ((Map<String, Object>) symbolElement.getValue()).entrySet().iterator();
			while(itSymbol.hasNext()) {
				Entry<String, Object> symbolProp = itSymbol.next();
				String key = symbolProp.getKey();
				if(key!="nextFundingRate") continue;
				Double fundingRate = Double.valueOf(symbolProp.getValue().toString());
				rateMap.put(symbolElement.getKey(), fundingRate);
				break;
			}
		}
		foundingRateList = new ArrayList<>(rateMap.entrySet());
		foundingRateList.sort(Entry.comparingByKey());
		PerpContract.SetFoundingRateList(foundingRateList);
	}
	

	private Map<String,Map<String,Object>> getPerpFuture(){
		// TODO waiting for ftx-api part finish
		
		return null;
	}
}
