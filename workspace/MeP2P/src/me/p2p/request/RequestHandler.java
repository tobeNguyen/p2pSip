package me.p2p.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

import me.p2p.log.Log;
import me.p2p.spec.MessageCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestHandler extends Thread {
	static final String TAG = "RequestHandler";
	Socket socket;
	boolean inMessage = false;
	StringBuilder msgData = null;
	MessageCallback msgListener;
	String clientRequest = null;
	BufferedReader bufferedReader;
	boolean stopHandle = false;

	ArrayList<String> requestQueue;

	public RequestHandler(Socket socket, MessageCallback msgListener) {
		// TODO Auto-generated constructor stub
		// client;
		this.socket = socket;
		this.msgListener = msgListener;

		this.requestQueue = new ArrayList<String>();
		this.clientRequest = null;
	}

	public void run() {
		// TODO Auto-generated method stub
		while (!stopHandle) {

			try {
				this.bufferedReader = new BufferedReader(new InputStreamReader(
						this.socket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				clientRequest = bufferedReader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (clientRequest != null) {
				if (!clientRequest.equals(RequestType.END_MSG)) {
					requestQueue.add(clientRequest);
				} else {
					// handle;
					for (String s : requestQueue) {
						if (s.equals(RequestType.START_MSG)) {
							// create msg data;
							msgData = new StringBuilder();
							// call listener;
							if (msgListener != null) {
								msgListener.onMessageStart();
							}
						} else {
							if (s.equals(RequestType.END_MSG)) {
								// end msg;
								if (msgListener != null) {
									JSONObject data = null;
									try {
										data = new JSONObject(msgData.toString());
									} catch (JSONException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

									msgListener.onMessage(data);
								}
								// stop handle msg
								stopHandle();
							} else {
								msgData.append(s);
							}
						}
					}
				}
			} else {
				stopHandle();
			}
		}

		Log.logToConsole(TAG, "Stop handle request");
	}

	public void handleRequest() {
		Log.logToConsole(TAG, "Handle message...");
		start();
	}

	public void stopHandle() {
		stopHandle = true;
	}
}