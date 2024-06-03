package org.cryptotrade.helper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UtilsHelper {
	private static Logger Log = LogManager.getLogger();
	
	public static String SHA256(String _massage) {
		MessageDigest messageDigest;
		String encodeStr = "";
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] hash = messageDigest.digest(_massage.getBytes("UTF-8"));
			encodeStr = Hex.encodeHexString(hash);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			Log.error(getExceptionStackTraceMessage(e));
		}
		return encodeStr;		
	}

	public static String SHA256(String message, final String secret) {
		StringBuilder hs = new StringBuilder();
    Mac sha256_HMAC;
		try {
			sha256_HMAC = Mac.getInstance("HmacSHA256");
	    SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
	    sha256_HMAC.init(secret_key);
	    byte[] hashByte = sha256_HMAC.doFinal(message.getBytes());
			String stmp;
			for (int n = 0; hashByte!=null && n < hashByte.length; n++) {
				stmp = Integer.toHexString(hashByte[n] & 0XFF);
				if (stmp.length() == 1)
					hs.append('0');
				hs.append(stmp);
			}
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return hs.toString().toLowerCase();
	}
	
	public static void Console(Object obj) {
		StackTraceElement element = new Exception().getStackTrace()[1];
		String title = element.getClassName()+"."+element.getMethodName()+"()";
		StringBuilder sb = new StringBuilder();
		sb.append(title).append(":").append(obj);
		System.out.println(sb.toString());
	}
	
	public static String getExceptionStackTraceMessage(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	
	public static String getMethodName() {
		StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();
    return stackTraceElements[1].getMethodName();
	}
	
	public static List<String> getRegepMatchString(String _regex, String _target){
		List<String> data = new ArrayList<>();
		Matcher m = Pattern.compile(_regex).matcher(_target);
		while(m.find()) 
			data.add(m.group());		
		return data;
	}
	
	public static String jsonBeautify(String _jsonFormatData) {
		String originString = _jsonFormatData;
		StringBuilder output = new StringBuilder();
		int tabNum = 0;
		for(int i=0; i<originString.length(); i++) {
			char unit = originString.charAt(i);
			switch(unit) {
			case '{':
			case '[':
				tabNum++;
				output.append(unit).append("\n").append(String.format("%"+(tabNum*2)+"s", ""));
				break;
			case '"':
				output.append(unit);
				if(originString.charAt(i+1)==':') output.append(" ");
				break;
			case ':': output.append(unit).append(" "); break;
			case ',': output.append(unit).append("\n").append(String.format("%"+(tabNum*2)+"s", "")); break;
			case '}':
			case ']':
				tabNum--;
				String format = "%"+(tabNum<=0 ? "" : tabNum*2)+"s";
				output.append("\n").append(String.format(format, "")).append(unit);
				break;
			default:
				output.append(unit);
			}
		}
		return output.toString();
	}

	public static String DecodeUnicode(String _msg) {
		String originString = _msg;
		Matcher matcher = Pattern.compile("(\\\\u[0-9a-fA-F]{4})+").matcher(originString);
		String text = "";
		while(matcher.find()) {
			String matchSrting = matcher.group();
			String[] units = matchSrting.split("\\\\u");
			String targetText = "";
			for(int i=1;i<units.length;i++)
				targetText += (char)Integer.parseInt(units[i], 16);
			originString = originString.replace(matchSrting,targetText);
		}
		return originString;
	}
}
