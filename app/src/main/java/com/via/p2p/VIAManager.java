package com.via.p2p;

import android.app.Activity;
import android.view.Surface;

import java.net.URISyntaxException;

/**
 * Created by NedHuang on 2016/7/29.
 */
public class VIAManager {
    private P2PClientHelper mP2PClientHelper = null;
    private boolean bP2PEnable = false;
    private String mUsername;
    private String mPassword;
    private Activity mAct = null;

    public boolean isP2PEnable() {
        return bP2PEnable;
    }

    public void setP2PEnable(boolean b) {
        bP2PEnable = b;
        if (!bP2PEnable && mP2PClientHelper != null) {
            mP2PClientHelper.release();
            mP2PClientHelper = null;
        }
    }

    public void setSurface(Surface[] surface) throws Exception {
        if (bP2PEnable && mP2PClientHelper != null) {
            mP2PClientHelper.setSurfaces(surface);
        } else {
            throw new Exception("P2P is not enable");
        }
    }

    public void setP2PAccount(String username, String password) throws Exception {
        if (bP2PEnable) {
            if (username != null && password != null) {
                mUsername = username;
                mPassword = password;
            }
        } else {
            throw new Exception("Username or Password is not set.");
        }
    }

    public void startP2PService(Activity a, Surface[] surfaces) throws Exception {
        if (bP2PEnable) {
            mAct = a;
            try {
                if (mP2PClientHelper != null) {
                    mP2PClientHelper.release();
                    mP2PClientHelper = null;
                }
                mP2PClientHelper = P2PClientHelper.createP2PClientWithSurfaces(mAct, surfaces);
                mP2PClientHelper.setUsername(mUsername);
                mP2PClientHelper.setPassword(mPassword);
                mP2PClientHelper.prepare();
                mP2PClientHelper.start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            if (mP2PClientHelper.isReadyToPairing()) {
                                try {
                                    mP2PClientHelper.pairing();
                                    break;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            throw new Exception("P2P is not enable");
        }
    }

    public void startP2PService() throws Exception {
        if (bP2PEnable) {
            try {
                if (mP2PClientHelper != null) {
                    mP2PClientHelper.release();
                    mP2PClientHelper = null;
                }
                mP2PClientHelper = P2PClientHelper.createP2PClient(null);
                mP2PClientHelper.setUsername(mUsername);
                mP2PClientHelper.setPassword(mPassword);
                mP2PClientHelper.prepare();
                mP2PClientHelper.start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mP2PClientHelper.isReadyToPairing()) {
                            try {
                                mP2PClientHelper.pairing();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            throw new Exception("P2P enable is false");
        }
    }

    public void stopP2PService() {
        if (bP2PEnable && mP2PClientHelper != null) {
            mP2PClientHelper.release();
            mP2PClientHelper = null;
        }
    }

    public void requestStartP2PStreaming(String nickname, int onChannel) throws Exception {
        if (bP2PEnable && nickname != null) {
                mP2PClientHelper.sendMessage("LiveView:RUN:" + nickname + ":" + onChannel);
        } else if (nickname == null) {
            throw new Exception("Nickname is not set.");
        }
    }

    public void requestStopP2PStreaming(int onChannel) {
        mP2PClientHelper.sendMessage("LiveView:STOP:" + onChannel);
        mP2PClientHelper.setStop(onChannel);
    }

    public boolean isLogin() {
        return mP2PClientHelper.bLogin;
    }

    public String[] getNickname() {
        return mP2PClientHelper.getNickname();
    }
}
