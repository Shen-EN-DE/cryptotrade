package org.cryptotrade.question;

public class VolumeTextInputQuestion extends NormalTextInputQuestion {
	private static final String SubName = "Volume";
	public static final String Name = SubName+NormalTextInputQuestion.Name;

	public VolumeTextInputQuestion() {
		this("", "請輸入交易量");
	}
	
	public VolumeTextInputQuestion(String _name,String _questionText) {
		super(_name+SubName, _questionText, Type.Number);
	}

}
