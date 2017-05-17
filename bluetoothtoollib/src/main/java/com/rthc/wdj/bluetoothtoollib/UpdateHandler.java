package com.rthc.wdj.bluetoothtoollib;


/**
 * 盒子升级回调
 */
public interface UpdateHandler {
    void success();
    void failed();
    void timeout();
}
