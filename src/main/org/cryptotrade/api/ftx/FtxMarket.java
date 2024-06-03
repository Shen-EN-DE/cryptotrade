package org.cryptotrade.api.ftx;

import org.json.JSONObject;

public class FtxMarket extends FtxApi {

	public FtxMarket(String _apiKey, String _apiSecret) {
		super(_apiKey, _apiSecret, "/markets");
		// TODO Auto-generated constructor stub
	}
	
	public JSONObject getMarkets() {
		return this.getMarkets(null);
	}
	
	public JSONObject getMarkets(String _market) {
		JSONObject json = null;
		String endPoint = "";
		if(_market!=null) endPoint = "/"+_market.toUpperCase();
		json = get(endPoint);
		return json;
	}

}
