package org.cryptotrade.question;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptotrade.App;
import org.cryptotrade.api.Telegram;
import org.cryptotrade.api.TelegramApiType;
import org.cryptotrade.command.Command;
import org.cryptotrade.command.SendOrderCommand;
import org.cryptotrade.entity.CommandOptionEnum;
import org.cryptotrade.entity.Exchange;
import org.cryptotrade.entity.MarketType;
import org.cryptotrade.entity.OrderSide;
import org.cryptotrade.entity.OrderType;
import org.cryptotrade.helper.UtilsHelper;
import org.cryptotrade.session.Session;
import org.cryptotrade.session.SessionManager;
import org.json.JSONObject;

public class PerpSpotOrderActionQuestion extends ActionQuestion {
	private static Logger Log = LogManager.getLogger();
	private static final String SubName = "PerpSpotOrder";
	public static final String Name = SubName+ActionQuestion.Name;
	public static enum ArgsName{SpotPrice,PerpPrice};
	
	public PerpSpotOrderActionQuestion() {
		this("","確認是否要下期現對鎖單");
	}

	public PerpSpotOrderActionQuestion(String _name, String _questionText) {
		super(_name+SubName, _questionText, InputType.Button);
	}
	
	@Override
	public Runnable getTask(JSONObject _event) {
		return ()->{
			try {
				if(_event.isNull("callback_query")) {
					Log.warn("只接受callback_query");
					return;
				}
				JSONObject callbackQuery = _event.getJSONObject("callback_query");
				JSONObject message = callbackQuery.getJSONObject("message");
				JSONObject user = callbackQuery.getJSONObject("from");
				JSONObject chat = message.getJSONObject("chat");
				Telegram telegramApi = (Telegram)App.apiManager.getApi("telegram", TelegramApiType.Bot);
				JSONObject tgData = new JSONObject().put("callback_query_id", callbackQuery.get("id"));
				JSONObject tgResult = telegramApi.answerCallbackQuery(tgData);
				String sessionName = Session.getDefaultSesstionName(user.get("id").toString(), chat.get("id").toString());
				Session session = SessionManager.getSession(sessionName);
				if(session==null) return;
				
				String[] dataArray = callbackQuery.getString("data").split(QueryDataSeparator);
				String queryData = dataArray[1];
				if(queryData.equalsIgnoreCase(ConfirmAnswer.Yes.name())) {

					String exchanger = session.getQuestion(ExchangerChooseQuestion.Name).getPureAnswer();
					String coin = session.getQuestion(CoinChooseQuestion.Name).getAnswer().toUpperCase();
					String spotPrice = session.getQuestion(ArgsName.SpotPrice.name()+PriceTextInputQuestion.Name).getPureAnswer();
					String perpPrice = session.getQuestion(ArgsName.PerpPrice.name()+PriceTextInputQuestion.Name).getPureAnswer();
					String orderSize = session.getQuestion(VolumeTextInputQuestion.Name).getPureAnswer();
					String rateDirection = session.getQuestion(SameExchRateDirectionChooseQuestion.Name).getPureAnswer();
					JSONObject messageEvent = new JSONObject()
							.put("message_id", message.get("message_id"))
							.put("chat",chat)
							.put("from", user);
					StringBuilder cmd = new StringBuilder("/order");
					cmd.append(" -").append(CommandOptionEnum.Volume.getSymbol()).append(" ").append(orderSize);
					if(exchanger.equalsIgnoreCase(Exchange.FTX.name())) {
						cmd.append(" -").append(CommandOptionEnum.Exchanger.getSymbol()).append(" ").append(Exchange.FTX);
						
						StringBuilder spotCmd = new StringBuilder();
						spotCmd.append(" -").append(CommandOptionEnum.Symbol.getSymbol()).append(" ").append(coin).append("/USD");
						spotCmd.append(" -").append(CommandOptionEnum.MarketType.getSymbol()).append(" ").append(MarketType.Spot);
						spotCmd.append(" -").append(CommandOptionEnum.Price.getSymbol()).append(" ").append(spotPrice);
						spotCmd.append(" -").append(CommandOptionEnum.OrderType.getSymbol()).append(" ").append(OrderType.Limit);
						if(rateDirection.equalsIgnoreCase(SameExchRateDirectionChooseQuestion.Option.PositiveRate.name()))
							spotCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Buy);
						else
							spotCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Sell);
						spotCmd.insert(0, cmd);
						messageEvent.put("text", spotCmd.toString());
						App.scheduledThreadPool.execute(new SendOrderCommand(messageEvent));					
						
						StringBuilder perpCmd = new StringBuilder();
						perpCmd.append(" -").append(CommandOptionEnum.Symbol.getSymbol()).append(" ").append(coin).append("-PERP");
						perpCmd.append(" -").append(CommandOptionEnum.MarketType.getSymbol()).append(" ").append(MarketType.Contract);
						perpCmd.append(" -").append(CommandOptionEnum.Price.getSymbol()).append(" ").append(perpPrice);		
						perpCmd.append(" -").append(CommandOptionEnum.OrderType.getSymbol()).append(" ").append(OrderType.Limit);
						if(rateDirection.equalsIgnoreCase(SameExchRateDirectionChooseQuestion.Option.PositiveRate.name()))
							perpCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Sell);
						else
							perpCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Buy);
						perpCmd.insert(0, cmd);
						messageEvent.put("text", perpCmd.toString());
						App.scheduledThreadPool.execute(new SendOrderCommand(messageEvent));					
					}
					else if(exchanger.equalsIgnoreCase(Exchange.Binance.name())) {
						cmd.append(" -").append(CommandOptionEnum.Exchanger.getSymbol()).append(" ").append(Exchange.Binance);
						cmd.append(" -").append(CommandOptionEnum.Symbol.getSymbol()).append(" ").append(coin).append("USDT");
						
						StringBuilder spotCmd = new StringBuilder();
						spotCmd.append(" -").append(CommandOptionEnum.MarketType.getSymbol()).append(" ").append(MarketType.Spot);
						spotCmd.append(" -").append(CommandOptionEnum.Price.getSymbol()).append(" ").append(spotPrice);
						spotCmd.append(" -").append(CommandOptionEnum.OrderType.getSymbol()).append(" ").append(OrderType.Limit);
						if(rateDirection.equalsIgnoreCase(SameExchRateDirectionChooseQuestion.Option.PositiveRate.name()))
							spotCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Buy);
						else
							spotCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Sell);
						spotCmd.insert(0, cmd);
						messageEvent.put("text", spotCmd.toString());
						App.scheduledThreadPool.execute(new SendOrderCommand(messageEvent));					
						
						StringBuilder perpCmd = new StringBuilder();
						perpCmd.append(" -").append(CommandOptionEnum.MarketType.getSymbol()).append(" ").append(MarketType.Contract);
						perpCmd.append(" -").append(CommandOptionEnum.Price.getSymbol()).append(" ").append(perpPrice);		
						perpCmd.append(" -").append(CommandOptionEnum.OrderType.getSymbol()).append(" ").append(OrderType.Limit);
						if(rateDirection.equalsIgnoreCase(SameExchRateDirectionChooseQuestion.Option.PositiveRate.name()))
							perpCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Sell);
						else
							perpCmd.append(" -").append(CommandOptionEnum.OrderSide.getSymbol()).append(" ").append(OrderSide.Buy);
						perpCmd.insert(0, cmd);
						messageEvent.put("text", perpCmd.toString());
						App.scheduledThreadPool.execute(new SendOrderCommand(messageEvent));	
					}
				}
				
				session.finish();
			} catch(Exception e) {
				Log.error(UtilsHelper.getExceptionStackTraceMessage(e));
			}
		};
	}

}