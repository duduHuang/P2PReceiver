package com.via.p2p;

/**
 * Created by HankWu_Office on 2015/11/25.
 */
public class LiveViewInfo {
    String name;
    String status;
    String path;
    LiveViewInfo(String n,String s) {
        name = n;
        status = s;
    }
    LiveViewInfo(String n,String s,String p) {
        name = n;
        status = s;
        path = p;
    }
    protected void setName(String n) {
        name = n;
    }
    protected void setStatus(String s) {
        status = s;
    }
    protected String getName() {
        return name;
    }
    protected String getStatus() {
        return status;
    }
}

