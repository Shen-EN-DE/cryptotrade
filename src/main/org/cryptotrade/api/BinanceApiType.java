package org.cryptotrade.api;

public enum BinanceApiType implements ApiType {
	Spot,
	Margin,
	uContract,
	Market;
	
	@Override
	public String toString() {
		return this.name();
	}
}
