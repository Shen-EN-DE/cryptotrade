package org.cryptotrade.entity;

public enum OrderSide {
	Buy("buy"),
	Sell("sell"),
	BuyLimit("buylimit"),
	SellLimit("selllimit");

	OrderSide(String string) {
		// TODO Auto-generated constructor stub
	}
	
	public static OrderSide forName(String _name) {
		for(var e : OrderSide.values()) {
			if(e.name().compareToIgnoreCase(_name)==0)
				return e;
		}
		return null;
	}
}
