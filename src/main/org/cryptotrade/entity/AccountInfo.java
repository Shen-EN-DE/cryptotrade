package org.cryptotrade.entity;

import java.util.Map;

public class AccountInfo {
	public String apiKey;
	public String apiSecret;
	public Map<String,Map<String,Object>> balance;
	public Map<String,Map<String,Object>> position;
}
