package com.via.p2p;

import android.util.Log;

import java.nio.ByteBuffer;

public class libnice {
	static {
//		System.loadLibrary("gstreamer_android");
		System.loadLibrary("nice4android");
	}
	final String TAG = "libnice";
	/*
		get agentCtx and gloop handle
	 */
	private long agentCtxHandle = 0;
	private long gloopLong = 0;
	/*
		it will get one stream id at one class
	 */
	private int  streamId = -1;
	private String localSdp = "";
	/*
		One NiceAgent will get one stream id currently
		because i don't know what scenario will use a lot stream
	 */
	private native long initNative();
	private native long /*agent handle*/ createAgentNative(long gloop,int useReliable);
	private native int setStunAddressNative(long agentHandle,String stun_ip,int stun_port);
	private native int setControllingModeNative(long agentHandle,int controllingMode);
	private native int /*stream id*/ addStreamNative(long agentHandle,String streamName, int numberOfComponent); // return stream id which is signed by libnice

	private native int /*0: fail 1:success*/ gatheringCandidateNative(long agentHandle,int stream_id);
	/*
		it need to wait for gathering complete and then call getLocalSdpNative
	 */
	private native String /*sdp which is encoded by base 64*/ getLocalSdpNative(long agentHandle,int stream_id);
	private native int setRemoteSdpNative(long agentHandle,String jremoteSdp,long Size);
	private native int sendMsgNative(long agentHandle,String data,int streamId,int compId);
	private native int sendDataNative(long agentHandle,byte[] data,int len ,int streamId,int compId);
	private native int sendDataDirectNative(long agentHandle,ByteBuffer data, int len ,int streamId,int compId);
	private native int setDirectBufferIndexNative(long agentHandle,ByteBuffer data, int index);
	private native int sendDataDirectByIndexNative(long agentHandle,ByteBuffer data, int len ,int index,int streamId,int compId);
	private native int restartStreamNative(long agentHandle,int streamId);

	public int restartStream() {
		localSdp = "";
		return restartStreamNative(agentCtxHandle, streamId);
	}

	public int sendDataDirectByIndex(ByteBuffer data,int len,int index,int streamId,int CompId) {
		return sendDataDirectByIndexNative(agentCtxHandle, data, len, index, streamId, CompId);
	}

	public int setDirectBufferIndex(ByteBuffer data,int index) {
		return setDirectBufferIndexNative(agentCtxHandle, data, index);
	}

	private native void mainLoopStart(long gloop);
	private native void mainLoopStop(long gloop);
	// register callback function for stream[streamId],component[compId]
	private native void registerReceiveCallbackNative(long agentHandle,libnice.ReceiveCallback recv_cb_obj,int streamId,int compId);
	private native void registerStateObserverNative(long agentHandle,libnice.StateObserver obs);
	private native void destroyAgentNative(long agentHandle);


	private boolean debug = false;
	public void enableDebugMessage(boolean b) {
		debug = b;
	}

	private Thread mainLoopThread = new Thread(new Runnable(){
		public void run() {
			// Just use it to run gloop
			mainLoopStart(gloopLong);
		}
	});

	public int init() {
		int ret = 0;
		debugMessage("init");
        for(int i=0;i<MAX_COMPONENT;i++) {
            componentListeners[i] = null;
        }

		gloopLong = initNative();
		if(gloopLong!=0) {
			mainLoopThread.start();
			ret = 1;
			debugMessage("init success");
		}
		return ret;
	}

	public void release() {
		debugMessage("release");
		//destroyAgentNative(agentCtxHandle);
		mainLoopStop(gloopLong);
	}


	public int createAgent(int useReliable) {
		debugMessage("createAgent: useReliable="+useReliable);
		agentCtxHandle = createAgentNative(gloopLong, useReliable);
		if(agentCtxHandle==0) {
			return 0;
		}
		return 1;
	}

	public int setStunAddress(String stun_ip, int stun_port) {
		debugMessage("setStunAddress: stun_ip="+stun_ip+",stun_port="+stun_port);
		return setStunAddressNative(agentCtxHandle, stun_ip, stun_port);
	}

	public int setControllingMode(int controllingMode) {
		debugMessage("setControllingMode: controllingMode="+controllingMode);
		return setControllingModeNative(agentCtxHandle, controllingMode);
	}

	private final int MAX_COMPONENT = 5;

	public int addStream(String streamName) {
        int numberOfComponent = MAX_COMPONENT;
		debugMessage("addStream: streamName="+streamName+",Component="+numberOfComponent);


		int ret = 0;
		if(streamId<0) {
			streamId = addStreamNative(agentCtxHandle, streamName, numberOfComponent);
			if(streamId>0) {
				debugMessage("addStream success");
				ret = 1;

                /*
                    Auto register callback from c to java.
                 */
                registerNativeCallbackToJava();
            } else {
				debugMessage("addStream fail");
				ret = 0;
			}
		} else {
			debugMessage("Stream has been added!");
			ret = 2;
		}
		return ret;
	}

	public String getLocalSdp() {
		debugMessage("getLocalSdp");
		localSdp = getLocalSdpNative(agentCtxHandle, streamId);
		return localSdp;
	}

	public int gatheringCandidate() {
		debugMessage("gatheringCandidate");
		return gatheringCandidateNative(agentCtxHandle, streamId);
	}

	public void setRemoteSdp(String remoteSdp) {
		debugMessage("setRemoteSdp:"+remoteSdp);
		setRemoteSdpNative(agentCtxHandle,remoteSdp,remoteSdp.length());
	}

	public void sendData(byte[] buf, int len, int compId) {
		debugMessage("sendDataDirect:Length="+len+",Component:"+compId);
		sendDataNative(agentCtxHandle,buf,len,streamId,compId);
	}

	public int sendDataDirect(ByteBuffer buf, int len, int compId) {
		debugMessage("sendDataDirect:Length="+len+",Component:"+compId);
		return sendDataDirectNative(agentCtxHandle,buf,len,streamId,compId);
	}

	public int sendMsg(String msg, int compId) {
		debugMessage("sendMsg:msg="+msg+",Component:"+compId);
		return sendMsgNative(agentCtxHandle,msg,streamId,compId);
	}

    private void registerReceiveCallback(libnice.ReceiveCallback obs, int compId) {
		debugMessage("registerReceiveCallback forComponent:"+compId);
		this.registerReceiveCallbackNative(agentCtxHandle,obs,streamId,compId);
	}

    private void registerStateObserver(libnice.StateObserver stateObserver) {
		debugMessage("registerStateObserver");
		this.registerStateObserverNative(agentCtxHandle,stateObserver);
	}

	private interface StateObserver {
		String[] STATE_TABLE = {"disconnected", "gathering", "connecting",
				"connected", "ready", "failed"};
		void cbCandidateGatheringDone(int stream_id);
		void cbComponentStateChanged(int stream_id, int component_id, int state);
	}

    private interface ReceiveCallback {
		void onMessage(byte[] buf);
	}

	private void debugMessage(String s) {
		if(debug) {
			Log.d(TAG,s);
		}
	}

	public class Parameter {
		private boolean useReliable = false;
		private String stunIp = DefaultSetting.stunServerIp[0];
		private int stunPort = DefaultSetting.stunServerPort[0];
		private boolean controlling = false;
		private String streamName = "VIAP2PStream";
		private int numberOfComponents = 5;

        public boolean isUseReliable() {
            return useReliable;
        }

        public void setUseReliable(boolean useReliable) {
            this.useReliable = useReliable;
        }

        public String getStunIp() {
            return stunIp;
        }

        public void setStunIp(String stunIp) {
            this.stunIp = stunIp;
        }

        public int getStunPort() {
            return stunPort;
        }

        public void setStunPort(int stunPort) {
            this.stunPort = stunPort;
        }

        public boolean isControlling() {
            return controlling;
        }

        public void setControlling(boolean controlling) {
            this.controlling = controlling;
        }

        public String getStreamName() {
            return streamName;
        }

        public void setStreamName(String streamName) {
            this.streamName = streamName;
        }

        public int getNumberOfComponents() {
            return numberOfComponents;
        }

        public void setNumberOfComponents(int numberOfComponents) {
            this.numberOfComponents = numberOfComponents;
        }
    }

	private class ReceiveObserver implements ReceiveCallback {
		int mComponentIndex;
		ReceiveObserver(int componentIndex) {
			this.mComponentIndex = componentIndex;
		}

		@Override
		public void onMessage(byte[] buf) {
			debugMessage("ReceiveObserver,Component["+(mComponentIndex+1)+"]:onMessage => message length="+buf.length);
            if(componentListeners[mComponentIndex]!=null) {
                componentListeners[mComponentIndex].onMessage(buf);
            }
		}
	}

    private void registerNativeCallbackToJava() {
        for(int compIndex=0;compIndex<5;compIndex++) {
            this.registerReceiveCallback(new ReceiveObserver(compIndex), compIndex+1);
        }
        this.registerStateObserver(new StateObserver() {
			@Override
			public void cbCandidateGatheringDone(int stream_id) {
				debugMessage("cbCandidateGatheringDone");
				if(onStateChangeListener!=null) {
					onStateChangeListener.candiateGatheringDone();
				}
			}

			@Override
			public void cbComponentStateChanged(int stream_id, int component_id, int state) {
				debugMessage("cbComponentStateChanged,["+component_id+"]:"+StateObserver.STATE_TABLE[state]);
				if(onStateChangeListener!=null) {
					onStateChangeListener.componentStateChanged(component_id,StateObserver.STATE_TABLE[state]);
				}
			}
		});
    }

    public void createByDefault() throws Exception {
        Parameter parameter = new Parameter();
        Exception createFailException = new Exception("createByDefault Fail");

        if(this.init()==0) throw createFailException;
        this.createAgent(parameter.isUseReliable()?1:0);
        this.setStunAddress(parameter.getStunIp(),parameter.getStunPort());
        this.setControllingMode(parameter.isControlling()?1:0);
        if(this.addStream(parameter.getStreamName())!=1) throw createFailException;
    }

    public enum ComponentIndex {
        Component1(0),
        Component2(1),
        Component3(2),
        Component4(3),
        Component5(4);

        private int index = -1;
        ComponentIndex(int i) {
            index = i;
        }

        protected int getIndex() {
            return index;
        }
    }


	// register for user
	private ComponentListener[] componentListeners = new ComponentListener[MAX_COMPONENT];
	private OnStateChangeListener onStateChangeListener = null;

	public interface ComponentListener {
		void onMessage(byte[] bytes);
	}

	public interface OnStateChangeListener {
		void candiateGatheringDone();
		void componentStateChanged(int componentId,String stateName);
	}

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    public void setComponentHandler(ComponentIndex componentIndex,ComponentListener componentListener) {
        componentListeners[componentIndex.getIndex()] = componentListener;
    }

}
