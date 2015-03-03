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
		System.out.println(ANSI_YELLOW+"D/"+tag+" "+message+ANSI_RESET);
	}
	
	/**
	 * Logs error messages
	 * <p>
	 * @param tag		The tag you want to give the message 
	 * @param message	The message to be logged
	 */
	public static void e(String tag, String message){
		System.out.println(ANSI_RED+"D/"+tag+" "+message+ANSI_RESET);
	}
}
