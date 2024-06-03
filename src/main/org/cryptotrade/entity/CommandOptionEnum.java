package org.cryptotrade.entity;

public enum CommandOptionEnum {
	Exchanger("e","exchanger"),
	Symbol("m","symbol"),
	OrderSide("os","order_side"),
	OrderType("ot","order_type"),
	OrderModule("om","order_module"),
	Price("pc","price"),
	Comment("cmt","comment"),
	Volume("vol","volume"),
	OrderId("oid","order_id"),
	TimeInForce("tif", "time_in_force"),
	MarketType("mkt","market_type"),
	OpenOrder("od","openorder"),
	Position("pst","position"),
	Asset("ast","asset"),
	Target("tg","target"),
	MarginType("mgt","margin_type"),
	Help("h","help"),
	HelpMark("?","help");
	
	private String symbol;
	private String name;
	
	private CommandOptionEnum(String _symbol, String _name) {
		this.name = _name;
		this.symbol = _symbol;
	}
	
	public String getSymbol() {
		return this.symbol;
	}
	
	public String getName() {
		return this.name;
	}
}
