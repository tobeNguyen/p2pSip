package me.p2p.log;

import java.io.File;

import me.p2p.data.DataManager;

public class Log {
	File logFile;
	DataManager dataManager;
	
	public static void logToConsole(String tag, String message) {
		System.out.println(tag + ": " + message);
	}
}
