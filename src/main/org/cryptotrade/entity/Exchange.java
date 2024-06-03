package org.cryptotrade.entity;

public enum Exchange {
	FTX,
	Binance;
	
	public static Exchange forName(String _name) {
		if(_name==null) return FTX;
		for(var e : Exchange.values()) {
			if(e.name().compareToIgnoreCase(_name)==0)
				return e;
		}
		return FTX;
	}
}
