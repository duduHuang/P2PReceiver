package com.via.p2p;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;

import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by HankWu_Office on 2015/8/20.
 */
public class VideoThread extends Thread {
    private final static String TAG = "libnice-vt";

//    private byte[] inputStreamTmp = new byte[1024 * 1024 * 2];
    private byte[] inputStreamTmp = new byte[65536];
    private ByteBuffer rawDataCollectBuffer = ByteBuffer.allocate(1024 * 1024 * 5);
    private byte[] dst = new byte[1024 * 1024 * 2];
    private MediaCodec decoder;
    private Surface surface;
    private InputStream is;
    private boolean bStart = true;
    VideoDisplayThread vdt = null;
    public long startTime = -1;
    public int currentFPS = 0;
    public boolean bDropMode = false;
    int lostFPS = 0;
    int inIndex = -1;

    protected void setStop() {
        bStart = false;
    }

    private String mMime;
    private int    mWidth;
    private int    mHeight;
    private String mSPS;
    private String mPPS;
    private int collectLen = 0;
    private SecretKey mSecretKey = null;
    private boolean isKey = true;
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    protected VideoThread(Surface surf, String mime, int width, int height, String sps, String pps, InputStream inputStream) {
        this.surface  = surf;
        this.mMime    = mime;
        this.mWidth   = width;
        this.mHeight  = height;
        this.mSPS     = sps;
        this.mPPS     = pps;
        this.is       = inputStream;
        Log.d("VideoThread", mime + "," + width + "," + height + "," + sps + "," + pps + ",");
    }

    final static String MediaFormat_SPS = "csd-0";
    final static String MediaFormat_PPS = "csd-1";
    private final Object playerThreadLock = new Object();

    @TargetApi(21)
    public void run() {
        /// Create Decoder -START- ///
        try {
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, mMime);
            format.setInteger(MediaFormat.KEY_WIDTH, mWidth);
            format.setInteger(MediaFormat.KEY_HEIGHT, mHeight);
            format.setByteBuffer(MediaFormat_SPS, ByteBuffer.wrap(hexStringToByteArray(mSPS)));
            format.setByteBuffer(MediaFormat_PPS, ByteBuffer.wrap(hexStringToByteArray(mPPS)));

            decoder = MediaCodec.createDecoderByType(mMime);
            if (decoder == null) {
                Log.d(TAG, "This device cannot support codec :" + mMime);
            }
            decoder.configure(format, surface, null, 0);
        } catch (Exception e) {
            Log.d(TAG,"Create Decoder Fail, because "+e);
            //Log.d(TAG, "This device cannot support codec :" + mMime);
        }
        if (decoder == null) {
            Log.e("DecodeActivity", "Can't find video info!");
            return;
        }
        decoder.start();
        /// Create Decoder -END- ///
        ByteBuffer[] inputBuffers = null;
        ByteBuffer[] outputBuffers = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outputBuffers = decoder.getOutputBuffers();
            inputBuffers = decoder.getInputBuffers();
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        /// Decode -START- ///
        int readSize = 0;
        int firstNalu = 0;
        int secondNalu = 0;
        boolean controlFPS = false;
        int count = 1;
        int divide = 7;
        if (vdt == null) {
            vdt = new VideoDisplayThread(decoder, outputBuffers, info);
            vdt.start();
        }
        while (!Thread.interrupted() && bStart && decoder != null) {
            inIndex = decoder.dequeueInputBuffer(10000);
            if (inIndex > 0) {
                while (!Thread.interrupted() && bStart && decoder != null && is != null) {
                    try {
                        readSize = is.read(inputStreamTmp);
                        if (count > 30) {
                            count = 1;
                        } else {
                            count++;
                        }
//                        if (isKey) {
//                            byte[] bytes = new byte[readSize];
//                            System.arraycopy(inputStreamTmp, 0, bytes, 0, bytes.length);
//                            mSecretKey = new SecretKeySpec(bytes, "AES");
//                            Log.d(TAG, "run: AES KEY: " + Base64.encodeToString(bytes, 0));
//                            isKey = false;
//                            continue;
//                        }
                        if (controlFPS) {
                            if (count % divide == 0) {
                                inputStreamTmp = null;
                                inputStreamTmp = new byte[65536];
                                continue;
                            }
                        }
                        if (readSize > 0) {
                            rawDataCollectBuffer.put(inputStreamTmp, 0, readSize);
//                            rawDataCollectBuffer.put(decryption(inputStreamTmp), 0, readSize);
                        }
                    } catch (BufferOverflowException e1) {
                        Log.d(TAG, "BufferOverflowException: inputstream cannot read : " + e1);
                        inputStreamTmp = null;
                        inputStreamTmp = new byte[65536];
                        rawDataCollectBuffer.clear();
                        controlFPS = true;
                        divide--;
                        Log.d(TAG, "FPS: " + (30 - (30 / divide)));
                    } catch (Exception e) {
                        Log.d(TAG, "Exception: inputstream cannot read : " + e);
                        if (!bStart)
                            break;
                    }
                    firstNalu = findNalu(0, rawDataCollectBuffer);
//                    Log.d(TAG, "firstNalue: " + firstNalu + " rawDataCollectBuffer: " + rawDataCollectBuffer.get(0) + rawDataCollectBuffer.get(1) + rawDataCollectBuffer.get(2) + rawDataCollectBuffer.get(3) + rawDataCollectBuffer.get(4) + rawDataCollectBuffer.get(5));
                    if (firstNalu != -1) {
                        secondNalu = findNalu(firstNalu + 3, rawDataCollectBuffer);
                        if (secondNalu != -1 && secondNalu > firstNalu) {
                            int naluLength = secondNalu - firstNalu;
                            rawDataCollectBuffer.flip();
                            rawDataCollectBuffer.position(firstNalu);
                            //Log.d(TAG,"FirstNALU :"+firstNalu+" ,SecondNALU :"+secondNalu+"size :"+ (secondNalu-firstNalu)+", rawDataCollectBuffer remaining:"+rawDataCollectBuffer.remaining());
                            rawDataCollectBuffer.get(dst, 0, naluLength);
                            rawDataCollectBuffer.compact();
//                            int nalu_unit_type = (dst[4] & 0x1F);
                            //if(nalu_unit_type!=8 && nalu_unit_type!=7 && nalu_unit_type!=6)
                            synchronized (playerThreadLock) {
                                {
                                    ByteBuffer buffer = null;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        buffer = decoder.getInputBuffer(inIndex);
                                    } else {
                                        buffer = decoder.getInputBuffers()[inIndex];
                                    }
                                    buffer.clear();
                                    buffer.put(dst, 0, naluLength);
                                    decoder.queueInputBuffer(inIndex, 0, naluLength, 0, 0);
                                    playerThreadLock.notifyAll();
                                    break;
                                    //Log.d(TAG,"Time Of Decode One frame : "+(System.currentTimeMillis()-currentTime));
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Something wrong");
                        decoder.flush();
                        break;
                    }
                }
            }
        }
        vdt.interrupt();
        try {
            vdt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vdt = null;
        decoder.stop();
        decoder.release();
    }
    /// Decode -END- ///

    public class VideoDisplayThread extends Thread {
        MediaCodec decoder = null;
        ByteBuffer[] outputBuffers = null;
        MediaCodec.BufferInfo info = null;
        public VideoDisplayThread(MediaCodec codec,ByteBuffer[] bbs, MediaCodec.BufferInfo bi) {
            this.decoder = codec;
            this.outputBuffers = bbs;
            this.info = bi;
        }

        public void run() {
            while (bStart && decoder!=null ) {
                //Log.d("libnice", "coming");
                if (Thread.interrupted()) {
                    return;
                }
                synchronized (playerThreadLock) {
                    try {
                        playerThreadLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int outIndex = this.decoder.dequeueOutputBuffer(this.info, 10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                        Log.d("libnice", "INFO_OUTPUT_BUFFERS_CHANGED");
                        //this.outputBuffers = this.decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                        Log.d("libnice", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        Log.d("libnice", "INFO_TRY_AGAIN_LATER");
//                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
//                         ByteBuffer buffer = outputBuffers[outIndex];
//                      Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " );
                        //Log.d("libnice", "coming2");
                        this.decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }
            }
        }
    }

    int findNalu(int offset,ByteBuffer bb) {
    	int limit = bb.limit();
    	int ret = -1;
    	int currentPos = bb.position();

    	if(offset > currentPos) {
    		return ret;
    	}

    	for(int i=offset;i<(currentPos-4);i++) {
	    	if ((bb.get(i)==0 && bb.get(i+1) == 0 && bb.get(i+2) == 0 && bb.get(i+3) == 1 )) {
	    		return i;
	    	}
    	}
    	return ret;
    }

    private byte[] decryption(byte[] bytes) {
        try {
//            SecretKey secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, mSecretKey);
            return cipher.doFinal(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }
}

