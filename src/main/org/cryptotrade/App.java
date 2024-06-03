package org.cryptotrade;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.api.ApiManager;
import org.cryptotrade.api.BinanceApiType;
import org.cryptotrade.api.FtxApiType;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.api.binance.BinanceMargin;
import org.cryptotrade.api.binance.BinanceMarket;
import org.cryptotrade.api.binance.BinanceSpot;
import org.cryptotrade.api.binance.BinanceUBaseContract;
import org.cryptotrade.api.ftx.FtxAccount;
import org.cryptotrade.api.ftx.FtxMarket;
import org.cryptotrade.api.ftx.FtxOrders;
import org.cryptotrade.command.AccountCommand;
import org.cryptotrade.command.CancelOrderCommand;
import org.cryptotrade.command.CommandManager;
import org.cryptotrade.command.DataReportCommand;
import org.cryptotrade.command.HelpCommand;
import org.cryptotrade.command.SendOrderCommand;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.http.HttpServer;
import org.cryptotrade.session.SessionManager;
import org.cryptotrade.task.OrderStatusTraceScheduledTask;
import org.cryptotrade.task.OrderStatusTraceScheduledTask.BundleField;
import org.json.JSONArray;
import org.json.JSONObject;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
	private static Logger Log = LogManager.getLogger();
	public static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(20);
	public static final Dotenv env = Dotenv.load();
	public static ApiManager apiManager = new ApiManager();
	private HttpServer httpServer;
	
	public static void main(String... args) {
		try {
			var app = new App();
			app.init();
			app.start();
		} catch(Exception e) {
			Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
		}
	}
	
	public App() {
		
	}
	
	public void init() {
		Log.info("系統初始化...");
		boolean isInitSuccess = true;
		apiManager.addApi("telegram",TelegramApiType.Bot,new Telegram(env.get("telegram.api.token")));
		
		// 設定TelegramBot的指令
		CommandManager.registerCommand("help", HelpCommand.class);
		CommandManager.registerCommand("order", SendOrderCommand.class);
		CommandManager.registerCommand("cancelorder", CancelOrderCommand.class);
		CommandManager.registerCommand("account", AccountCommand.class);
		CommandManager.registerCommand("data", DataReportCommand.class);
//		CommandManager.registerCommand("testinlinekeyboard", TestInlineKeyboardCommand.class);
//		CommandManager.registerCommand("splock", SpLockCommand.class);
		
		isInitSuccess &= initFtx();
		isInitSuccess &= initBinance();
		SessionManager.startBackgroudWork();
		if(isInitSuccess)
			Log.info("系統初始化...完成");
		else{
			Log.error("系統初始化...失敗!!!");
			System.exit(0);
		}
	}
	
	public void start() {
		httpServer = new HttpServer();
		httpServer.start();
	}
	
	/**
	 * 初始化FTX交易所資料(還未完成的訂單)
	 */
	private boolean initFtx() {
		boolean flag = true;
		FtxOrders ftxOrders = new FtxOrders(
				App.env.get("exchange.ftx.api.key"),
				App.env.get("exchange.ftx.api.secret"),
				App.env.get("exchange.ftx.api.subaccount")
		);
		
		// 抓取交易所內尚未成交完成的掛單,讓系統繼續追蹤
		JSONObject result = ftxOrders.getOpenOrders(null);
		if(result.getBoolean("success")) {
			if(!result.getJSONArray("result").isEmpty()) {
				Log.info("追蹤FTX未成交完成的掛單");
				JSONArray orders = result.getJSONArray("result");
				for(int i=0; i<orders.length(); i++) {
					JSONObject order = orders.getJSONObject(i);
					var task = new OrderStatusTraceScheduledTask(ftxOrders,	new HashMap<BundleField,Object>(){{
						put(BundleField.OrderId, order.get("id").toString());
					}});
					task.start(0, Long.valueOf(env.get("system.task.orderstatus.period")), TimeUnit.SECONDS);
					StringBuilder sb = new StringBuilder("FTX未成交完成的掛單: ");
					sb.append(order);
					Log.info(sb);
					scheduledThreadPool.schedule(()->{
						if(task.getStatus().getDouble("remainSize")!=0) {
							task.changeReceiver(App.env.get("telegram.api.default.receiver"));
						}
					},10,TimeUnit.SECONDS);
				}
			}
			else 
				Log.info("FTX無未成交的掛單");
			apiManager.addApi(Exchange.FTX.name(),FtxApiType.Order,ftxOrders);
		}
		else {
			Log.error("FTX order API異常: "+result.toString());
			flag = false;
		}

		FtxAccount ftxAccount = new FtxAccount(
				App.env.get("exchange.ftx.api.key"),
				App.env.get("exchange.ftx.api.secret"),
				App.env.get("exchange.ftx.api.subaccount")
		);		
		result = ftxAccount.getPosition();
		if(result.getBoolean("success")) {
			apiManager.addApi(Exchange.FTX.name(),FtxApiType.Account,ftxAccount);
		}
		else {
			Log.error("FTX account API異常: "+result.toString());
			flag = false;
		}
		
		FtxMarket ftxMarket = new FtxMarket(
				App.env.get("exchange.ftx.api.key"),
				App.env.get("exchange.ftx.api.secret")
		);
		result = ftxMarket.getMarkets("btc/usdt");
		if(result.getBoolean("success")) {
			apiManager.addApi(Exchange.FTX.name(),FtxApiType.Market,ftxMarket);
		}
		else {
			Log.error("FTX market API異常: "+result.toString());
			flag = false;
		}
		
		return flag;
	}
	
	private boolean initBinance() {
		boolean flag = true;
		String apiKey = App.env.get("exchange.binance.api.key");
		String apiSecret = App.env.get("exchange.binance.api.secret");
		BinanceSpot spot= new BinanceSpot(apiKey, apiSecret);
		JSONObject result = spot.testOrder();
		if(result==null || !result.getJSONObject("result").isEmpty())
			flag = false;
		if(!flag)
			Log.error("Binance API異常,apiKey或apiSecret錯誤: "+result);
		else {
			apiManager.addApi(Exchange.Binance.name(),BinanceApiType.Spot,spot);
			apiManager.addApi(Exchange.Binance.name(),BinanceApiType.uContract,new BinanceUBaseContract(apiKey, apiSecret));
			apiManager.addApi(Exchange.Binance.name(),BinanceApiType.Margin,new BinanceMargin(apiKey, apiSecret));
			apiManager.addApi(Exchange.Binance.name(),BinanceApiType.Market,new BinanceMarket(apiKey, apiSecret));

			// 抓取交易所內尚未成交完成的掛單,讓系統繼續追蹤
			
		}
		return flag;
	}
}
