package org.cryptotrade.question;

public abstract class ActionQuestion extends ConfirmQuestion {
	private static final String SubName = "Action";
	public static final String Name = SubName+ConfirmQuestion.Name;
	

	protected ActionQuestion(String _name, String _questionText, InputType _type) {
		super(_name+SubName, _questionText, _type);
	}
}
