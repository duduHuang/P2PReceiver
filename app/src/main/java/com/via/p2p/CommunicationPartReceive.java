package com.via.p2p;


import android.util.Log;

public class CommunicationPartReceive implements libnice.ComponentListener {
	int COMMUNICATION_COMPONENT_ID = -1;
	libnice mNice = null;
	String loggingMessage = "";
	protected final static String REQUEST_LIVE_VIEW_INFO = "REQUEST_LIVE_VIEW_INFO";
	private final static String FEEDBACK_LIVE_VIEW_INFO = "FEEDBACK_LIVE_VIEW_INFO";
	protected String [] mNickname = null;


	protected CommunicationPartReceive(libnice nice, int compId) {
		mNice = nice;
		COMMUNICATION_COMPONENT_ID = compId;
	}

	public void onMessage(byte[] buf) {
		String tmp = new String(buf);
		Log.d("p2pClientHelper", "onMessage:" + tmp);

		loggingMessage += tmp + "\n";

		if (tmp.startsWith(FEEDBACK_LIVE_VIEW_INFO)) {
			//TODO: Store the feedback information of live view
			String[] inputMsgSplits = tmp.split(":");
			mNickname = new String[inputMsgSplits.length];
			for (int i = 1; i < inputMsgSplits.length; i++) {
				LiveViewInfo liveView = new LiveViewInfo(inputMsgSplits[i], "IDLE");
				Log.d("p2pClientHelper", "onMessage: FEEDBACK_LIVE_VIEW_INFO " + liveView.getName());
				mNickname[i - 1] = liveView.getName();
			}
		}
	}

	protected void sendMessage(String msg) {
		mNice.sendMsg(msg, COMMUNICATION_COMPONENT_ID);
	}
}
