package com.rthc.wdj.bluetoothtoollib;

/**
 * Created by Administrator on 2015/12/4.
 */
public interface ValveHandler {

    int callback(boolean success);

    void timeOut();
}
