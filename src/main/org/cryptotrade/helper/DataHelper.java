package org.cryptotrade.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.cryptotrade.entity.PerpContract;

public class DataHelper {
	
	/**
	 * 取得前N個由大到小排名的資金費率合約名稱
	 * @param n - 名單數
	 * @return
	 */
	public static List<Entry<String,Double>> GetPerpContractPositiveNthRate(int n){
		List<Entry<String,Double>> list = PerpContract.GetFoundingRateList();
		int size = list.size();
		list = list.subList(size-n, size);
		list.sort(Entry.comparingByValue(Comparator.reverseOrder()));
		return list;
	}
	
	/**
	 * 取得前N個由小到大排名的資金費率合約名稱
	 * @param n - 名單數
	 * @return
	 */
	public static List<Entry<String,Double>> GetPerpContractNagetiveNthRate(int n){
		List<Entry<String,Double>> list = PerpContract.GetFoundingRateList();
		return list.subList(0, n);
	}
	
	
}
