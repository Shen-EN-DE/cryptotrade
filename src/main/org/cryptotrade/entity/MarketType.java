package org.cryptotrade.entity;

public enum MarketType {
	Spot("spot"),
	Contract("contract"),
	Margin("margin");

	MarketType(String string) {
		// TODO Auto-generated constructor stub
	}
	
	public static MarketType forName(String _name) {
		for(var e : MarketType.values()) {
			if(e.name().compareToIgnoreCase(_name)==0)
				return e;
		}
		return null;
	}
}
