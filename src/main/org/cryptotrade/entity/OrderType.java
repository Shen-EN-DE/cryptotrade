package org.cryptotrade.entity;

public enum OrderType {
	Market,
	Limit;
	
	public static OrderType forName(String _name) {
		for(var e : OrderType.values()) {
			if(e.name().compareToIgnoreCase(_name)==0)
				return e;
		}
		return null;
	}
}
