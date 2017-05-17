package com.rthc.wdj.bluetoothtoollib;

import java.util.HashMap;

/**
 * Created by Administrator on 2015/12/4.
 */
public interface MeterHandler {
    //result:抄表结果;
    //返回值:执行状态返回码
    int callback(float result, HashMap map);

    void timeOut();
}
