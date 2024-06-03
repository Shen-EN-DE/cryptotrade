package org.cryptotrade.question;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

public abstract class PageContralQuestion extends Question {
	private static Logger Log = LogManager.getLogger();
	public static enum PageOption{Page,CurrentPage,NoAction}
	public static enum DataOption{Data,All};
	protected static final int ShowRowCountInPage = 6;
	public static final String Name = "PageContral";
	protected int currPageOffset = 0;
	protected JSONArray datas;

	public PageContralQuestion(String _name, String _questionText) {
		super(_name+Name, _questionText, InputType.Button);
	}

}
