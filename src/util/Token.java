package util;

import static java.lang.Long.parseLong;
import static util.Hash.of;

public class Token {

	static final int TKN_TIMEOUT = 10000;
	static final String DELIMITER = "/";

	private static String token;
	
	public static void set(String t) {
		token = t;
	}
	
	public static String get() {
		return token == null ? "" : token ;
	}
	
	public boolean matches(String t) {
		return token != null && token.equals( t );
	}

	public static String createToken(String id, long currTime) {
		String msg = id + currTime + Token.get();
		return of(msg) + DELIMITER + currTime;
	}

	public static boolean validateToken(String id, String token) {
		if(token == null) return false;
		String[] tokens = token.split(DELIMITER);
		long time = parseLong(tokens[1]);
		String newToken = createToken(id, time);
		System.out.println(id + " " + token + " " + newToken + " " + time);
		if(System.currentTimeMillis() - time > TKN_TIMEOUT) return false;
		System.out.println(token + " " + newToken + " " + time);
		if(!token.equals(newToken)) return false;
		System.out.println(token + " " + newToken + " " + time);
		return true;
	}
}
