package org.ma3map.api.handlers;

public class Log {
	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_BLACK = "\u001B[30m";
	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_YELLOW = "\u001B[33m";
	private static final String ANSI_BLUE = "\u001B[34m";
	private static final String ANSI_PURPLE = "\u001B[35m";
	private static final String ANSI_CYAN = "\u001B[36m";
	private static final String ANSI_WHITE = "\u001B[37m";
	/**
	 * Logs debug messages
	 * <p>
	 * @param tag		The tag you want to give the message 
	 * @param message	The message to be logged
	 */
	public static void d(String tag, String message){
		System.out.println("D/"+tag+" "+message);
	}
	
	/**
	 * Logs information messages
	 * <p>
	 * @param tag		The tag you want to give the message 
	 * @param message	The message to be logged
	 */
	public static void i(String tag, String message){
		System.out.println("I/"+tag+" "+message);
	}
	
	/**
	 * Logs warning messages
	 * <p>
	 * @param tag		The tag you want to give the message 
	 * @param message	The message to be logged
	 */
	public static void w(String tag, String message){
		System.out.println(ANSI_YELLOW+"W/"+tag+" "+message+ANSI_RESET);
	}
	
	/**
	 * Logs error messages
	 * <p>
	 * @param tag		The tag you want to give the message 
	 * @param message	The message to be logged
	 */
	public static void e(String tag, String message){
		System.out.println(ANSI_RED+"E/"+tag+" "+message+ANSI_RESET);
	}

	/**
	 * Logs progress messages. Don't print anything between p prints.
	 * <p>
	 * @param tag		The tag you want to give the message
	 * @param message	The message to be logged
	 */
	public static void i(String tag, String message, int currIndex, int maxIndex) {
		double noHashes = (double)(currIndex)/(double)maxIndex;
		noHashes = noHashes*50;
		String hashes = "";
		for(int i = 0; i <= 50; i++) {
			if(i <= noHashes) hashes += "#";
			else hashes += " ";
		}
		noHashes = noHashes * 2;
		double userFriendly = (double)Math.round(noHashes * 100)/100;
		System.out.print("I/" + tag + " " + message + " [" + hashes + "] " + String.valueOf(userFriendly) + "%\r");
		if(currIndex == maxIndex) System.out.println("");
	}

	/**
	 * Logs progress info messages. Don't print anything progress prints
	 * <p>
	 * @param tag		The tag you want to give the message
	 * @param message	The message to be logged
	 */
	public static void d(String tag, String message, int currIndex, int maxIndex) {
		double noHashes = (double)(currIndex)/(double)maxIndex;
		noHashes = noHashes*50;
		String hashes = "";
		for(int i = 0; i <= 50; i++) {
			if(i <= noHashes) hashes += "#";
			else hashes += " ";
		}
		noHashes = noHashes * 2;
		double userFriendly = (double)Math.round(noHashes * 100)/100;
		System.out.print("D/" + tag + " " + message + " [" + hashes + "] " + String.valueOf(userFriendly) + "%\r");
		if(currIndex == maxIndex) System.out.println("");
	}

	/**
	 * Logs progress debug messages. Don't print anything between progress prints.
	 * <p>
	 * @param tag		The tag you want to give the message
	 * @param message	The message to be logged
	 */
	public static void w(String tag, String message, int currIndex, int maxIndex) {
		double noHashes = (double)(currIndex)/(double)maxIndex;
		noHashes = noHashes*50;
		String hashes = "";
		for(int i = 0; i <= 50; i++) {
			if(i <= noHashes) hashes += "#";
			else hashes += " ";
		}
		noHashes = noHashes * 2;
		double userFriendly = (double)Math.round(noHashes * 100)/100;
		System.out.print("W/" + tag + " " + message + " [" + hashes + "] " + String.valueOf(userFriendly) + "%\r");
		if(currIndex == maxIndex) System.out.println("");
	}
}
