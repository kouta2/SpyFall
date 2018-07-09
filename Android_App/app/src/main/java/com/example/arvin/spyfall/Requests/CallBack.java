package com.example.arvin.spyfall.Requests;

import java.util.Enumeration;

/**
 * Created by arvin on 11/20/2017.
 */

public interface CallBack {
    public void callBack(String s);
    public void runOnUiThread(Runnable r);
}
