package org.cryptotrade.api.ftx;

import org.json.JSONArray;
import org.json.JSONObject;

public class FtxWellet extends FtxApi {
	
	public FtxWellet(String _apiKey, String _apiSecret) {
		super(_apiKey, _apiSecret, "/wallet");
		// TODO Auto-generated constructor stub
	}

	/**
	 * 取得帳戶內的資產詳細資訊(不包含持有量)
	 * @return JSONObject
	 */
	public JSONObject getCoin() {
		JSONObject json = null;
		String endPoint = "/coins";
		json = get(endPoint);
		return json;
	}
	
	/**
	 * 取得帳戶內的資產餘額狀態
	 * @return JSONObject
	 */
	public JSONObject getBalance() {
		JSONObject json = null;
		String endPoint = "/balances";
		json = get(endPoint);
		return json;
	}
	
	/**
	 * 取得所有帳戶(包含子帳戶)的資產餘額狀態
	 * @return JSONObject
	 */
	public JSONObject getAllBalance() {
		JSONObject json = null;
		String endPoint = "/all_balances";
		json = get(endPoint);
		return json;
	}
	
	/**
	 * 
	 * @param _coin 資產名稱
	 * @param _method 提幣鏈名<br/>
	 * <ul><li>ERC20=>erc20</li>
	 * <li>TRC20資產=>trx</li>
	 * <li>SPL=>sol</li>
	 * <li>Omni=>omni</li>
	 * <li>BEP2=>bep2</li>
	 * <li>Binance Smart Chain=>bsc</li>
	 * <li>Fantom=>ftm</li>
	 * <li>Avax=>avax</li>
	 * <li>Matic=>matic</li></ul>
	 * @return JSONObject
	 */
	public JSONObject getDepositAddress(String _coin,String _method) {
		JSONObject json = null;
		String endPoint = String.format("/deposit_address/%s?method=%s", _coin, _method);
		json = get(endPoint);
		return json;
	}
	
	/**
	 * 回傳該幣種的入金地址(可指定顯示鏈)
	 * @param _jsonArray 包含至少一個JSONObject,每個JSONObject包含{coin,method}元素(method非必要)
	 * @return JSONObject
	 */
	public JSONObject getDepositAddressList(JSONArray _jsonArray) {
		JSONObject json = null;
		String endPoint = "/deposit_address/list";
		if(_jsonArray==null) return json;
		// API有問題,未完成
		json = post(endPoint, _jsonArray.toString());
		return json;		
	}
}
