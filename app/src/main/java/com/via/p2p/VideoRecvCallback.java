package com.via.p2p;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Random;

import javax.crypto.SecretKey;

public class VideoRecvCallback implements libnice.ComponentListener {
    private boolean bVideo = false;
    private int w = 0;
	private int h = 0;
	private String sps = null;
	private String pps = null;
	private String mime= null;
    private String server_ip = null;
	
    private LocalServerSocket mLss = null;
    private LocalSocket mReceiver = null;
    private LocalSocket mSender   = null;
    private int         mSocketId;
    private final String LOCAL_ADDR = "DataChannelToVideoDecodeThread-";
    private OutputStream os = null;
    private WritableByteChannel writableByteChannel;
    private InputStream is = null;
    VideoThread vt = null;
    private SurfaceView  videosv  = null;
    private Surface surface = null;
    private final static String TAG = "VideoRecvCallback";
    protected VideoRecvCallback(Surface s) {
        surface = s;
//        try {
//            dos = new DataOutputStream(new FileOutputStream("/mnt/usbdisk/usbdisk2/hank.264"));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    protected void release() {
//        Log.d("NED", "release: VideoRecvCallback!!!!!!");
        if (mReceiver != null) {
            try {
                is.close();
                mReceiver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mReceiver = null;
        }
        if (mLss != null) {
            try {
                os.flush();
                os.close();
                mLss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mLss = null;
        }
        if (mSender != null) {
            try {
                mSender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSender = null;
        }
    }

    public void setStop() {
        bVideo = false;
        if (vt != null) {
            vt.setStop();
            vt.interrupt();
            try {
                vt.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            vt = null;
        }
        release();
    }

    protected void setSurfaceView(SurfaceView sv) {
        videosv = sv;
    }
    
    private void LOGD(String msg) {
    	Log.d(TAG,msg);
    }

    File f = null;
    FileOutputStream fos = null;
    DataOutputStream dos = null;

    public void onMessage(byte[] msg) {
		if(!bVideo) {
			String tmp = new String(msg);
//            LOGD(tmp);
			if(tmp.startsWith("Video")) {
				bVideo = true;
				String[] tmps = tmp.split(":");
				mime = tmps[1];
				w = Integer.valueOf(tmps[2]);
				h = Integer.valueOf(tmps[3]);
				sps = tmps[4];
				pps = tmps[5];
                server_ip = tmps[6];
                for (int jj = 0; jj < 10; jj++) {
                    try {
                        mSocketId = new Random().nextInt();
                        mLss = new LocalServerSocket(LOCAL_ADDR + mSocketId);
                        break;
                    } catch (IOException e) {
                        LOGD("fail to create localserversocket :" + e);
                    }
                }
                //    DECODE FLOW
                //
                //    Intermediary:                             Localsocket       MediaCodec inputBuffer     MediaCodec outputBuffer
                //        Flow    : Data Channel =======> Sender ========> Receiver ==================> Decoder =================> Display to surface/ Play by Audio Track
                //       Thread   : |<---Data Channel thread--->|          |<--------- Decode Thread --------->|                 |<--------- Display/play Thread -------->|
                //
                mReceiver = new LocalSocket();
                try {
                    mReceiver.connect(new LocalSocketAddress(LOCAL_ADDR + mSocketId));
                    mReceiver.setReceiveBufferSize(1024 * 1024);
                    mReceiver.setSoTimeout(2000);
                    mSender = mLss.accept();
                    mSender.setSendBufferSize(1024 * 1024);
                } catch (IOException e) {
                    LOGD("fail to create mSender mReceiver :" + e);
                    e.printStackTrace();
                    release();
                }
                try {
                    os = mSender.getOutputStream();
                    writableByteChannel = Channels.newChannel(os);
                    is = mReceiver.getInputStream();
                } catch (IOException e) {
                    LOGD("fail to get mSender mReceiver :" + e);
                    e.printStackTrace();
                    release();
                }
                vt = new VideoThread(surface, mime, w, h, sps, pps, is);
                vt.setPriority(Thread.MAX_PRIORITY);
                vt.start();
			}
//			LOGD(tmp);
		} else {
			try {
//                if(bFirst) {
//                    bFirst = false;
//                    dos.write(hexStringToByteArray(sps));
//                    dos.write(hexStringToByteArray(pps));
//                }
//                dos.write(msg);
//                os.write(msg);
                writableByteChannel.write(ByteBuffer.wrap(msg));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGD("os write fail" + e);
                release();
            }
        }
	}
    boolean bFirst = true;

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}