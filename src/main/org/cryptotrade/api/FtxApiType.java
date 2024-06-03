package org.cryptotrade.api;

public enum FtxApiType implements ApiType {
	Order,
	Market,
	Account;
	
	@Override
	public String toString() {
		return this.name();
	}
}
