package org.cryptotrade.api;

public enum TelegramApiType implements ApiType {
	Bot;
	
	@Override
	public String toString() {
		return this.name();
	}
}
