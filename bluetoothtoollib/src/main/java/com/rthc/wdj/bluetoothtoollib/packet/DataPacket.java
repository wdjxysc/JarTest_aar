package com.rthc.wdj.bluetoothtoollib.packet;

import android.util.Log;


import com.rthc.wdj.bluetoothtoollib.cmd.BleCmd;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by Administrator on 2015/12/1 0001.
 * --------------------------------------------------------------------------
 * | 同步头 | Head |	Length | Control |    Timeout   | Data   | CS	 | Tail  |
 * ---------------------------------------------------------------------------
 * | 2Byte | 1Byte|	1Byte  | 1Byte	 |    2Byte     | N Byte | 2Byte | 1byte |
 * ---------------------------------------------------------------------------
 * | 0xFE  | 0xAA |	N+8    | 控制字	 | (10~65535)Ms | 数据   | CRC16 | 0x55   |
 * ----------------------------------------------------------------------------
 */
public class DataPacket {

    private Queue<DataItem> frames = new ConcurrentLinkedQueue<DataItem>();
    private int packetLen = 0;
    private int currentReceiveLen = 0;
    private byte[] packet;

    private int sync_length = 0;

    /**
     * int packetLen
     * 数据包的总长度
     */
    public DataPacket(/*DataItem data,*/ int packetLen) {
//        frames.add(data);
//        this.currentReceiveLen += data.getLen();
        this.packetLen = packetLen;
    }

    public void add(DataItem data) {
        frames.add(data);
        this.currentReceiveLen += data.getLen();
    }

    public boolean isGetAllData() {
        Log.i("wdj", "currentReceiveLen:" + currentReceiveLen + " packetLen:" + packetLen);
        if (this.currentReceiveLen < this.packetLen)
            return false;

        Log.i("wdj", "isGetAllData Test");

        return true;
    }

    //组装数据
    private void assembleFrames() {
        packet = new byte[packetLen];

        DataItem frame;
        int copyIndex = 0;
        while ((frame = frames.poll()) != null) {
            System.arraycopy(frame.getData(), 0, packet, copyIndex, frame.getLen());
            copyIndex += frame.getLen();
        }

        //去掉同步头
        int headIndex = BleCmd.isFirstFrame(packet);
        if (headIndex != -1) {
            packet = Arrays.copyOfRange(packet, headIndex, packet.length);
        }

        Log.i("wdj", Tools.Bytes2HexString(packet, packet.length));

    }

    public byte[] getAllData() {
        if (isGetAllData()) {
            assembleFrames();
            return packet;
        }
        return null;
    }

}
