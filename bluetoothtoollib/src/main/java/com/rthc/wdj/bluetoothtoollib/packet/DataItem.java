package com.rthc.wdj.bluetoothtoollib.packet;


/**
 * Created by Administrator on 2015/12/1 0001.
 */
public class DataItem {
    public byte[] data;
    public int len;

    public DataItem(byte[] data, int len) {
        this.data = data;
        this.len = len;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getLen() {
        return this.len;
    }
}
