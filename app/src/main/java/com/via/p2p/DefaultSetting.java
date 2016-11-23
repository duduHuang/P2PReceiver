package com.via.p2p;

/**
 * Created by HankWu_Office on 2015/12/3.
 */
public class DefaultSetting {
    final static int reliableMode = 0;
    final static public String WTAG = "CloudWatch/W";
    // register server / signaling server
    public final static String serverUrl = "http://xo.ns2go.com:3000/";
//    public final static String serverUrl = "http://118.163.85.223:15300/";
//    public final static String serverUrl = "http://192.168.12.76:3000/";
    // stun server
    public final static String[] stunServerIp = {"203.183.172.196","74.125.204.127"};
    public final static int[] stunServerPort = {3478,19302};

    public final static String sourcePeerUsername = "qqqq";
    public final static String sourcePassword     = "qqqq";
    public final static String findPeerUsername   = sourcePeerUsername;
    public final static String findPeerPassword   = sourcePassword;

    public final static String streamName = "DefaultStream";


    public final static boolean printLevelD = true;
    public final static boolean printLevelI = true;

    public final static boolean bShowConnectMenu = false;

    public static int isReliableMode() {
        return reliableMode;
    }
}
