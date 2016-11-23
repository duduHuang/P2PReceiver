package com.via.p2p;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by HankWu_Office on 2015/11/26.
 */
public class P2PClientHelper extends Thread {
    private libnice mNice;
    private String TAG = "P2PClientHelper";
    private String remoteSdp = "";
    private String localSdp = "";
    private Context application_ctx = null;
    private Activity mAct = null;
    private Socket mSocket;
    private libnice.ComponentListener[] componentListeners = new libnice.ComponentListener[5];
    private String username = null;
    private String password = null;
    private boolean bReadyToPair = false;
    private final static int MessageChannelNumber = 0;
    private final static int MAX_RECEIVE_CHANNEL_NUMBER = 4;
    private Surface[] mSurfaces = new Surface[MAX_RECEIVE_CHANNEL_NUMBER];
    protected boolean bLogin = false;

    private P2PClientHelper(Context ctx)  throws URISyntaxException {
        try {
            application_ctx = ctx;
            setSocketIOAndConnect();
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    private P2PClientHelper(Activity a, Surface[] surfaces) throws URISyntaxException {
        try {
            mAct = a;
//            application_ctx = ctx;
            mSurfaces = surfaces;
            setSocketIOAndConnect();
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    protected static P2PClientHelper createP2PClientWithSurfaces(Activity a, Surface[] surfaces) throws URISyntaxException {
        return new P2PClientHelper(a, surfaces);
    }

    protected static P2PClientHelper createP2PClient(Context c) throws URISyntaxException {
        return new P2PClientHelper(c);
    }

    protected void setSurfaces(Surface[] surfaces) {
        mSurfaces = surfaces;
    }

    protected void release() {
        Log.d("NED", "release: P2PClientHelper!!!!!!");
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.close();
            mSocket = null;
        }
        bReadyToPair = false;
        if (mNice != null) {
            mNice.release();
            mNice = null;
        }

        for (int i = 1; i < 5; i++) {
            if (componentListeners[i] != null) {
                Log.d("NED", "release: componentListeners[" + i + "] send stop transmit command.");
                ((CommunicationPartReceive)componentListeners[0]).sendMessage("VIDEO:" + "STOP" + ":" + i + ":");
                ((VideoRecvCallback)componentListeners[i]).setStop();
                componentListeners[i] = null;
            }
        }
        if (componentListeners[0] != null) {
            componentListeners[0] = null;
        }
        isAllReadyTable = null;
    }

    private boolean[] isAllReadyTable;

    protected void prepare() {
        bReadyToPair = false;
        isAllReadyTable = new boolean[5];
        disableReady();

        mNice = new libnice();
        if (initNice(mNice, mSurfaces)) {
            Log.d("HANK", "Init libnice success!!");
        }
    }

    protected void setUsername(String username) {
        this.username = username;
    }

    protected String getUsername() {
        return username;
    }

    protected void setPassword(String password) {
        this.password = password;
    }

    protected String getPassword() {
        return password;
    }

    protected boolean isReadyToPairing() {
        return bReadyToPair;
    }

    protected void pairing() throws Exception {
        if (bReadyToPair) {
            Log.d("P2PClientHelper", "Pairing!!");
            synchronized (waitForResult) {
                mSocket.emit("connect server", username + ":" + password);
                try {
                    waitForResult.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (loginSuccess) {
                    mSocket.emit("get remote sdp", username + ":" + localSdp);
                }
            }
        } else {
            throw new Exception("not ready to pair");
        }
    }

    private boolean initNice(libnice nice, Surface[] surfaces) {
        /*
			0 => Fail
			1 => Success
		 */
        if (nice.init() == 0) {
            return false;
        }
        /*
            If useReliable = 1, the libnice will send the few small packages which is separated by user giving package
						   = 0, the libnice will send the original package.
		 */
        int useReliable = 0;
        nice.createAgent(useReliable);
        nice.setStunAddress(DefaultSetting.stunServerIp[1], DefaultSetting.stunServerPort[1]);
		/*
			0 => controlling
			1 => controlled
		 */
        int controllMode = 0;
        nice.setControllingMode(controllMode);
        String streamName = "P2PStream";
		/*
            ret = 0 => Fail.
				= 1 => Success.
				= 2 => It has been added.
		 */
        if (nice.addStream(streamName) != 1) {
            return false;
        }
//         register a receive Observer to get byte array from jni side to ja+va side.
        int i = 0;
        componentListeners[i] = new CommunicationPartReceive(nice, i + 1);
        nice.setComponentHandler(libnice.ComponentIndex.Component1, componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaces[i - 1]);
        nice.setComponentHandler(libnice.ComponentIndex.Component2, componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaces[i - 1]);
        nice.setComponentHandler(libnice.ComponentIndex.Component3, componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaces[i - 1]);
        nice.setComponentHandler(libnice.ComponentIndex.Component4, componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaces[i - 1]);
        nice.setComponentHandler(libnice.ComponentIndex.Component5, componentListeners[i]);


        // register a state Observer to catch stream/component state change
        nice.setOnStateChangeListener(new NiceStateObserver(nice));
        nice.gatheringCandidate();
//        localSdp = mNice.getLocalSdp();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (nice.gatheringCandidate() == 1) {
            showToast("D", "gathering Candidate Success, please wait gathering done then getLocalSDP");
        } else {
            showToast("D", "gathering Candidate fail");
            return false;
        }

        return true;
    }

    protected String getSDP() {
        return localSdp;
    }

    protected void setSDP(String sdp) {
        mNice.setRemoteSdp(sdp);
    }

    @Override
    public void run() {
        super.run();
    }

    private void setSocketIOAndConnect() throws URISyntaxException {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            mSocket = IO.socket(DefaultSetting.serverUrl, opts);
            mSocket.on("response", onResponse);
            mSocket.on("get sdp", onGetSdp);
            mSocket.on("restart stream", onRestartStream);
            mSocket.on("login result", loginResult);
            mSocket.connect();
        } catch (URISyntaxException e) {
            showToast("I","Server is offline, please contact your application provider!");
            throw new RuntimeException(e);
        }
    }

    private Emitter.Listener onResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String message;
            try {
                message = data.getString("message");
                showToast("D","onRespone:" + message);
            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onGetSdp = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("P2PClientHelper","onGetSdp!!");
            JSONObject data = (JSONObject) args[0];
            String SDP;
            try {
                remoteSdp = data.getString("SDP");
                mNice.setRemoteSdp(remoteSdp);
                showToast("D","GetSDP:" + remoteSdp);
            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onRestartStream = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            showToast("I","on Restart Stream");
            release();
            mNice.restartStream();
            mNice.gatheringCandidate();
            localSdp = mNice.getLocalSdp();
            mSocket.emit("set local sdp", localSdp);
        }
    };

    private static Object waitForResult = new Object();
    private boolean loginSuccess = false;

    private Emitter.Listener loginResult = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String msg = String.valueOf(args[0]);
            if (msg.equalsIgnoreCase("login success")) {
                Log.d("NED", "success: " + msg);
                loginSuccess = true;
            } else {
                Log.d("NED", "fail: " + msg);
            }
            synchronized (waitForResult) {
                waitForResult.notify();
            }
        }
    };

    private void stopAllProcess() {
        /*
            TODO: Add process killer.
         */
    }

    public void triggerSocketio() {
        mSocket.emit("trigger","abcd");
    }

    private void showToast(String level, final String tmp) {

        if(level.equalsIgnoreCase("D") && DefaultSetting.printLevelD) {
            Log.d(DefaultSetting.WTAG+"/"+TAG, tmp);
            if (mAct != null)
                Toast.makeText(mAct, tmp, Toast.LENGTH_SHORT).show();
        }

        if (level.equalsIgnoreCase("I") && DefaultSetting.printLevelI) {
            Log.d(DefaultSetting.WTAG + "/" + TAG, tmp);
            if (mAct != null)
                Toast.makeText(mAct, tmp, Toast.LENGTH_SHORT).show();
        }
    }

    protected String getLocalSdp() {
        return localSdp;
    }

    protected void setRemoteSDP(String sdp) {
        mNice.setRemoteSdp(sdp);
    }

    private class NiceStateObserver implements libnice.OnStateChangeListener {
        private libnice mNice;
        public NiceStateObserver(libnice nice) {
            mNice = nice;
        }

        @Override
        public void candiateGatheringDone() {
            localSdp = mNice.getLocalSdp();
            //mSocket.emit("get remote sdp", username + ":" + localSdp);
            bReadyToPair = true;
        }

        @Override
        public void componentStateChanged(int componentId, String stateName) {
            Log.d("componentStateChanged_c", "Component[" + componentId + "]:" + stateName);
            if (stateName.equalsIgnoreCase("ready")) {
                isAllReadyTable[componentId - 1] = true;
                checkAllReady();
            }
        }
    }

    private boolean checkAllReady() {
        boolean isAllReady = (isAllReadyTable[0] && isAllReadyTable[1] && isAllReadyTable[2] && isAllReadyTable[3] && isAllReadyTable[4]);
        if (isAllReady) {
            Log.d("I", "CONNECT!");
            sendMessage(CommunicationPartReceive.REQUEST_LIVE_VIEW_INFO);
            showToast("I", "Connect!!");
            bLogin = true;
        }
        return isAllReady;
    }

    private void disableReady() {
        for (int i = 0; i < isAllReadyTable.length; i++) {
            isAllReadyTable[i] = false;
        }
        bLogin = false;
    }

    protected void sendMessage(String msg) {
        Log.d("P2PClientHelper", "SendMesg:" + msg);
        ((CommunicationPartReceive) componentListeners[MessageChannelNumber]).sendMessage(msg);
//        String[] strings = msg.split(":");
//        if (strings[1].equals("STOP")) {
//            final int channel = Integer.parseInt(strings[2]);
//            ((VideoRecvCallback)componentListeners[channel]).setStop();
//        }
    }

    protected void setStop(int channel) {
        ((VideoRecvCallback) componentListeners[channel]).setStop();
    }

    protected String[] getNickname() {
        return ((CommunicationPartReceive) componentListeners[MessageChannelNumber]).mNickname;
    }

}
