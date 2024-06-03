package org.cryptotrade.command;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.FtxApiType;
import org.cryptotrade.api.ftx.FtxAccount;
import org.cryptotrade.api.ftx.FtxOrders;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.helper.UtilsHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class AccountCommand extends Command {
	private static Logger Log = LogManager.getLogger();
	public AccountCommand(JSONObject _messageObject) {
		super(_messageObject);
		// optionDescription是父類別member
		optionDescription = new HashMap<>() {{
			put(CommandOptionEnum.Exchanger.getSymbol(), "指定交易所(預設ftx,選項:ftx)");
			put(CommandOptionEnum.OpenOrder.getSymbol(), "顯示目前未完成交易的掛單");
			put(CommandOptionEnum.Asset.getSymbol(), "顯示所有持有幣種");
			put(CommandOptionEnum.Position.getSymbol(), "顯示所有持有合約");
			put(CommandOptionEnum.Help.getSymbol(), "列出所有選項(單獨使用)");
		}};
	}

	@Override
	public String getDesctiption() {
		return "顯示帳戶資訊";
	}

	@Override
	public void run() {
		// 加上try-catch避免Exception中傳ThreadPool的Thread
		try {
			// commandOption是父類別member
			String optionKey = CommandOptionEnum.Exchanger.getSymbol();
			Exchange exchanger = Exchange.forName(commandOption.get(optionKey));
			switch(exchanger) {
			case FTX: ftx();break;
			case Binance: binance();break;
			}
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}
	
	private void ftx() {
		JSONObject tgData = new JSONObject() {{
			put("chat_id", receiver);
			put("reply_to_message_id", replyTo);
		}};
		StringBuilder sb = new StringBuilder();
		boolean isOnlyOption = commandOption.size()==1;
		
		String optionKey = CommandOptionEnum.OpenOrder.getSymbol();
		if(commandOption.containsKey(optionKey)) {
			StringBuilder tmp = new StringBuilder();
			FtxOrders ftxOrders = (FtxOrders)App.apiManager.getApi(Exchange.FTX.name(), FtxApiType.Order);
			JSONObject result = ftxOrders.getOpenOrders(null);
			if(result.getBoolean("success")) {
				JSONArray orders = result.getJSONArray("result");
				for(int i=0; i<orders.length(); i++) {
					JSONObject order = orders.getJSONObject(i);
					if(tmp.length()>0) tmp.append("\n");
					tmp.append("編號:").append(order.get("id")).append(", ")
							.append(order.get("market")).append(", ")
							.append(order.get("side")).append(" ").append(order.get("type")).append(" 價 ").append(order.get("price"));
					if(isOnlyOption) {
						tmp.append(", 總量:").append(order.get("size"));
						tmp.append(", 均價:").append(order.isNull("avgFillPrice")?0:order.get("avgFillPrice"));
					}
					tmp.append(", ").append("餘 ").append(order.get("remainingSize"));
				}
			}
			else
				tmp.append("API無法取得目前未完成交易的掛單");
			if(sb.length()>0) sb.append("\n");
			sb.append("所有未完成交易單:\n").append(tmp);
		}

		optionKey = CommandOptionEnum.Position.getSymbol();
		if(commandOption.containsKey(optionKey)) {
			StringBuilder tmp = new StringBuilder();
			FtxAccount ftxAccount = (FtxAccount)App.apiManager.getApi(Exchange.FTX.name(),FtxApiType.Account);
			JSONObject result = ftxAccount.getPosition();
			if(result.getBoolean("success")) {
				JSONArray positions = result.getJSONArray("result");
				for(int i=0; i<positions.length(); i++) {
					JSONObject pos = positions.getJSONObject(i);
					if(pos.getDouble("size")==0) continue;
					tmp.append(pos.get("future"))
							.append(", 總持量:").append(pos.get("size"))
							.append(", 方向:").append(pos.get("side"))
							.append(", 損益平價:").append(pos.get("recentBreakEvenPrice"));
				}
			}
			else
				tmp.append("API無法取得目前持倉資訊");
			if(sb.length()>0) sb.append("\n");
			sb.append("所有持倉合約:\n").append(tmp);
		}
	}
	
	private void binance() {
		
	}

}
