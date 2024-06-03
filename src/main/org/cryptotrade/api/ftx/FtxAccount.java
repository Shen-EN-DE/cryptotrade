package org.cryptotrade.api.ftx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class FtxAccount extends FtxApi {
	private static Logger Log = LogManager.getLogger();
	public FtxAccount(String _apiKey, String _apiSecret) {
		super(_apiKey, _apiSecret,"");
		// TODO Auto-generated constructor stub
	}
	
	public FtxAccount(String _apiKey, String _apiSecret,String _subAccountName) {
		super(_apiKey,_apiSecret,_subAccountName,"");
		Log.debug("apiKey: "+apiKey+", apiSecret: "+_apiSecret);
	}
	
	public JSONObject getPosition() {
		JSONObject json = null;
		String endPoint = "/positions?showAvgPrice=true";
		json = get(endPoint);
		return json;
	}
}
