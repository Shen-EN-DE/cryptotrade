package org.cryptotrade.question;

public class PriceTextInputQuestion extends NormalTextInputQuestion {
	private static final String SubName = "Price";
	public static final String Name = SubName+NormalTextInputQuestion.Name;
	
	public PriceTextInputQuestion() {
		this("","請輸入交易價格");
	}
	
	public PriceTextInputQuestion(String _name,String _questionText) {
		super(_name+SubName, _questionText, Type.Number);
	}

}
