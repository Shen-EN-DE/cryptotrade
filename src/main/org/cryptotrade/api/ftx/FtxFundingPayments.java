package org.cryptotrade.api.ftx;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

public class FtxFundingPayments extends FtxApi {

	public FtxFundingPayments(String _apiKey, String _apiSecret) {
		super(_apiKey, _apiSecret, "/funding_payments");
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 取得資金費率取付款紀錄
	 * @param _option - Map 參數內容如下:<br>
	 * <ul>
	 * start_time - Integer : (選填)搜尋區間的開始時間(秒)<br>
	 * end_time - Integer : (選填)搜尋區間的結束時間, buy, sell<br>
	 * future - String : (選填)指定期貨商品<br>
	 * </ul>
	 * @return JSONObject
	 * @throws Exception 
	 */
	public JSONObject fundingPayments(Map<String,Object> _option) throws Exception {
		JSONObject json = null;
		String endPoint = "";
		JSONObject params = new JSONObject();
		String query = "";
		if(_option!=null) {
			for(Entry<String,Object> entry : _option.entrySet()) {
				String key = entry.getKey();
				switch(key) {
				case "start_time":
				case "end_time":
					if(entry.getValue() instanceof Integer) throw new Exception(key+"型別必需是Integer");
					break;
				case "future":
					if(entry.getValue() instanceof String) throw new Exception(key+"型別必需是String");
					break;
				}
				params.put(key, entry.getValue());
			}
			for(Entry<String,Object> entry : _option.entrySet()) {
				if(!query.isEmpty()) query += "&";
				query += entry.getKey()+"="+entry.getValue().toString();
			}
			endPoint = query.isEmpty() ? endPoint : endPoint+"?"+query;
		}	
		json = get(endPoint);
		return json;
	}
	
	public JSONObject fundingPayments() {
		JSONObject json = null;
		try {
			json =  fundingPayments(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json;
	}

}
