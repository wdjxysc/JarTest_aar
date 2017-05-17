package com.rthc.wdj.bluetoothtoollib.cmd;

import android.util.Log;


import com.rthc.wdj.bluetoothtoollib.util.CRC16;
import com.rthc.wdj.bluetoothtoollib.util.StringTool;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * 生成命令类
 */
public class Cmd {

    /**
     * 序列号  主站发送的序号 SER ，在每次通讯前，按模 256 加 1运算后产生
     */
    public static int SER = 0x00;

    /**
     * 同步头 表收到的前置FE一律过滤
     */
    public static final String SyncHead = "FEFEFEFEFEFE";

    /**
     * 模块模式
     * 需要兼容 2ID和4ID的模块 收发命令长度会因此发生改变
     */
    public enum RF_NODE_ID_TYPE {
        NODE_ID_2_BYTES,
        NODE_ID_4_BYTES
    }

    /**
     * 当前模块模式 2ID和4ID的模块   默认为4byteID
     */
    public static RF_NODE_ID_TYPE rf_node_id_type = RF_NODE_ID_TYPE.NODE_ID_4_BYTES;


    /**
     * 传输模式
     * 有中继与无中继的模式 数据格式不同
     */
    public enum RF_TRANSMISSION_TYPE {
        RELAY,
        NO_RELAY
    }


    /**
     * 当前模块传输模式 默认为不通过中继
     */
    public static RF_TRANSMISSION_TYPE rf_transmission_type = RF_TRANSMISSION_TYPE.NO_RELAY;

    /**
     * 当前中继ID
     */
    public static long rf_relay_id = 0;


    /**
     * 根据map组装要发送的命令
     *
     * @param map 对象封装命令
     * @return 返回命令byte数组
     */
    public static byte[] AssembleCmd(HashMap map) {
        byte[] data;
        int dataType = (Integer) map.get(KEY_DATA_TYPE);
        long nodeid = (Long) map.get(KEY_NODE_ID);

        String meterIdStr = (String) map.get(KEY_METER_ID);

        if(map.containsKey(key_rf_transmission_type)){
            rf_transmission_type = (RF_TRANSMISSION_TYPE)map.get(key_rf_transmission_type);
            if (rf_transmission_type == RF_TRANSMISSION_TYPE.RELAY){
                if(map.containsKey(key_rf_relay_id)){
                    rf_relay_id = (Integer)map.get(key_rf_relay_id);
                }
            }
        }

        if(map.containsKey(key_rf_node_id_type)){
            rf_node_id_type = (RF_NODE_ID_TYPE)map.get(key_rf_node_id_type);
        }

        //无线模块所加报文头+"FEFEFE"(自定义无意义码)  FE会被表程序过滤掉
//        String front = "FF078D9810" + String.format("%08d", nodeid) + "FEFEFE";
        String headbytestr = "AA";
        String rf_relay_id_str = "";

        switch (rf_transmission_type) {
            case RELAY:
                headbytestr = "55";
                rf_relay_id_str = String.format("%06X", rf_relay_id);
                break;
            case NO_RELAY:
                headbytestr = "AA";
                rf_relay_id_str = "";
                break;
        }

        String front = headbytestr + rf_relay_id_str + String.format("%08X", nodeid) + SyncHead;
        switch (rf_node_id_type) {
            case NODE_ID_2_BYTES:
                front = headbytestr + rf_relay_id_str + String.format("%04X", nodeid) + SyncHead;
                break;
            case NODE_ID_4_BYTES:
                front = headbytestr + rf_relay_id_str + String.format("%08X", nodeid) + SyncHead;
                break;
        }

        //表收到的报文
        String cmdstr;
        SER = (SER + 1) % 256;
        if (dataType == Const.DATAID_UPDATE_FIRMWARE) {
            cmdstr = String.format("%02X",Const.HEAD)
                    + String.format("%02X", Const.METERTYPE)
                    + ReversalHexStr(meterIdStr)
                    + String.format("%02X", Const.CTR_3_UPDATE_FIRMWARE)
                    + "03"
                    + ReversalHexStr(String.format("%04X", Const.DATAID_UPDATE_FIRMWARE))
                    + String.format("%02X", SER)
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_SCAN_ALL_METER_ID) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + "AAAAAAAAAAAAAA"
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_SCAN_ALL_METER_ID).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_SCAN_ALL_METER_ID).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_UPDATE_FIRMWARE_RESET) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_UPDATE_FIRMWARE).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_UPDATE_FIRMWARE_RESET).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_VALVE) {
            String valvestate = "55";//默认开阀
            if (map.get(KEY_VALUE) == "0") //关阀
            {
                valvestate = "99";//关阀
            }

            String length = StringTool.padLeft(Integer.toHexString(valvestate.length() / 2 + 3).toUpperCase(), 2, '0');

            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_VALVE).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + valvestate
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_START_USE) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + "04"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_START_USE).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_METER_STATE) {
            //写表状态 默认正常关阀
            String value = "0100";
            String length = StringTool.padLeft(Integer.toHexString(value.length() / 2 + 3).toUpperCase(), 2, '0');
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_METER_STATE).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + value
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_JIESUANRI) {
            //结算日 结算日 为  DD  bcd码
            int date = (Integer) map.get(KEY_VALUE);//整数
            String dataStr = String.format("%02d", date);
            String length = StringTool.padLeft(Integer.toHexString(dataStr.length() / 2 + 3).toUpperCase(), 2, '0');
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_JIESUANRI).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + dataStr
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_CHAOBIAORI) {
            //结算日 抄表日 为  DD   bcd码
            int date = (Integer) map.get(KEY_VALUE);//整数
            String dataStr = String.format("%02d", date);
            String length = StringTool.padLeft(Integer.toHexString(dataStr.length() / 2 + 3).toUpperCase(), 2, '0');
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_CHAOBIAORI).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + dataStr
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_JIESUANRI) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_JIESUANRI).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_CHAOBIAORI) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_CHAOBIAORI).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_DATA) {
            String meterdata = (String) map.get(KEY_VALUE);//十进制一位小数 字符串
            String formatdata = "";//转换格式后的格式
            try {
                double f = Double.parseDouble(meterdata) * 10;
                int datai = (int) f;
                String datastr1 = String.format("%010d", datai);

                for (int k = 4; k > -1; k--) {
                    formatdata += datastr1.substring(k * 2, (k + 1) * 2);
                }

            } catch (Exception ex) {
                Log.i("ex", ex.getMessage());
            }

            String length = StringTool.padLeft(Integer.toHexString(formatdata.length() / 2 + 3).toUpperCase(), 2, '0');

            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_SYS_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_DATA).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + formatdata
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_ADDRESS) {
            String address = (String) map.get(KEY_VALUE);//14位表ID  bcd码
            String formatdata = "";//转换格式后的格式

            for (int k = 6; k > -1; k--) {
                formatdata += address.substring(k * 2, (k + 1) * 2);
            }

            String length = StringTool.padLeft(Integer.toHexString(formatdata.length() / 2 + 3).toUpperCase(), 2, '0');

            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_ADRRESS).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_ADDRESS).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + formatdata
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_WRITE_TIME) {
            Date time = (Date) map.get(KEY_VALUE);//时间
            String formatdata = "";//转换格式后的格式
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                String datastr = df.format(time);

                for (int k = 6; k > -1; k--) {
                    formatdata += datastr.substring(k * 2, (k + 1) * 2);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            String length = StringTool.padLeft(Integer.toHexString(formatdata.length() / 2 + 3).toUpperCase(), 2, '0');

            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_WRITE_TIME).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + formatdata
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_DATA) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_DATA).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_TIME) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_TIME).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_METER_STATE) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_METER_STATE).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_M_DATA_1) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_M_DATA_1).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_M_DATA_2) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_M_DATA_2).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_D_DATA_1) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_D_DATA_1).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_D_DATA_2) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_D_DATA_2).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_D_DATA_3) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_D_DATA_3).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_D_DATA_4) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_D_DATA_4).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_D_DATA_5) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_D_DATA_5).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_READ_HISTORY_D_DATA_6) {
            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_0_READ_DATA).toUpperCase(), 2, '0')
                    + "03"
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_READ_HISTORY_D_DATA_6).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else if (dataType == Const.DATAID_SET_METER_RF_PARAM) {

            String formatDataStr = "";//转换格式后的数据

            String baudratestr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_baudrate)), 2, '0');
            String frequencystr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_frequency)), 6, '0');
            String factorstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_factor)), 2, '0');
            String bwstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_bw)), 2, '0');
            String nodeidstr = (String) map.get(key_meter_rf_new_nodeid);
            String netidstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_new_netid)), 2, '0');
            String powerstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_power)), 2, '0');
            String breathstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(key_meter_rf_breath)), 2, '0');

            formatDataStr = baudratestr.toUpperCase()  //波特率 9600
                    + "00"    //校验位
                    + frequencystr//频率 MHz
                    + factorstr.toUpperCase()     //扩频因子
                    + bwstr.toUpperCase()     //扩频带宽
                    + nodeidstr.toUpperCase()      //节点ID
                    + netidstr.toUpperCase()      //网络ID
                    + powerstr.toUpperCase()       //发射功率
                    + breathstr.toUpperCase()       //呼吸时间
                    + "00";      //CAD检测时间

            String length = StringTool.padLeft(Integer.toHexString(formatDataStr.length() / 2 + 3).toUpperCase(), 2, '0');

            cmdstr = StringTool.padLeft(Integer.toHexString(Const.HEAD).toUpperCase(), 2, '0')
                    + StringTool.padLeft(Integer.toHexString(Const.METERTYPE).toUpperCase(), 2, '0')
                    + ReversalHexStr(meterIdStr)
                    + StringTool.padLeft(Integer.toHexString(Const.CTR_3_WRITE_DATA).toUpperCase(), 2, '0')
                    + length
                    + ReversalHexStr(StringTool.padLeft(Integer.toHexString(Const.DATAID_SET_METER_RF_PARAM).toUpperCase(), 4, '0'))
                    + StringTool.padLeft(Integer.toHexString(SER).toUpperCase(), 2, '0')
                    + formatDataStr
                    + "00"
                    + StringTool.padLeft(Integer.toHexString(Const.TAIL).toUpperCase(), 2, '0');
        } else {
            return null;
        }


        //添加和校验
        byte checksum = Tools.CheckSum(Tools.HexString2Bytes(cmdstr.substring(0, cmdstr.length() - 4)));

        String checksumstr = StringTool.padLeft(Tools.Bytes2HexString(new byte[]{checksum}, 1).toUpperCase(), 2, '0');
        cmdstr = cmdstr.substring(0, cmdstr.length() - 4) + checksumstr + cmdstr.substring(cmdstr.length() - 2, cmdstr.length());

        data = Tools.HexString2Bytes(front + cmdstr);

        return data;
    }


    /**
     * 获取升级bin文件 转换为命令组
     *
     * @return 命令组
     */
    public static ArrayList<byte[]> GetUpdateCmdListFromBin() {
        ArrayList<byte[]> datalist = new ArrayList<byte[]>();

        try {
            String filename = Const.UPDATE_BIN_PATH;
            FileInputStream fin = new FileInputStream(filename);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);

            int offset = 0;
            while (offset < buffer.length) {
                byte[] data = new byte[32];
                if (buffer.length - offset >= 32) {
                    System.arraycopy(buffer, offset, data, 0, 32);
                } else {
                    System.arraycopy(buffer, offset, data, 0, buffer.length - offset);
                    for (int index = buffer.length - offset; index < 32; index++) {
                        data[index] = (byte) 0xFF;
                    }
                }
                offset += 32;
                datalist.add(data);
            }

            fin.close();
        } catch (IOException ex) {
            Log.i("ex", ex.getMessage());
        }

        return datalist;
    }


    /**
     * 每包数据最大字节数
     */
    private final static int PACKLENGTH = 64;

    /**
     * 获取升级txt文件 转换为命令组
     *
     * @return 命令组
     */
    public static ArrayList<byte[]> GetUpdateCmdListFromTxt(HashMap map) {

        String nodeidstr = (String) map.get("nodeid");
        int nodeid = Integer.parseInt(nodeidstr, 16);
        //无线模块所加报文头
//        String front = "FF078D9810" + String.format("%08d", nodeid);
        String front = String.format("%04X", nodeid);

        ArrayList<byte[]> datalist = new ArrayList<byte[]>();


        ArrayList<String> datastrlist = new ArrayList<String>();


        try {
            String filename = Const.UPDATE_TXT_PATH;
            FileInputStream fin = new FileInputStream(filename);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);

            String str = new String(buffer);
            String[] strlist = str.split("\r\n");

            fin.close();


            datastrlist.add("");//添加第一帧传输约定

            int datalength = 0;//数据总字节数


            String datastr = "";
            for (int i = 0; i < strlist.length; i++) {
                strlist[i] = strlist[i].replace(" ", "");

                if (strlist[i].startsWith("@") || strlist[i].startsWith("q")) {

                    if (!datastr.equals("")) {
                        int offset = 0;
                        while (offset < datastr.length()) {

                            //数据帧
                            String stritem;
                            if (datastr.length() - offset >= PACKLENGTH * 2) {
                                stritem = datastr.substring(offset, offset + PACKLENGTH * 2);
                            } else {
                                stritem = datastr.substring(offset, datastr.length());
                            }

                            datalength += stritem.length() / 2;

                            Log.i("ccccccc", "帧序号:" + i + "   " + Tools.Bytes2HexString(new byte[]{(byte) (datastrlist.size() % 256)}, 1));

                            stritem = front + "68" +
                                    Tools.Bytes2HexString(new byte[]{(byte) (datastrlist.size() / 256), (byte) (datastrlist.size() % 256)}, 2) +
                                    "55" +
                                    Tools.Bytes2HexString(new byte[]{(byte) (stritem.length() / 2)}, 1) +
                                    stritem +
                                    "0000";
                            datastrlist.add(stritem);

                            offset += PACKLENGTH * 2;
                        }


                    }
                    datastr = "";
                    //读到文件尾部
                    if (strlist[i].startsWith("q")) {
                        break;
                    } else if (strlist[i].startsWith("@")) {
                        //地址帧
                        String addritem = front + "68" +
                                Tools.Bytes2HexString(new byte[]{(byte) (datastrlist.size() / 256), (byte) (datastrlist.size() % 256)}, 2) +
                                "AA" +
                                "02" +
                                strlist[i].substring(1, 5) +
                                "0000";
                        datastrlist.add(addritem);
                    }
                } else {
                    datastr += strlist[i];
                }
            }

            //第一帧
            String firstitem = front + "68" + "00" + "00" + "5A" + "02" + "0000" + "0000";
            firstitem = firstitem.substring(0, firstitem.length() - 8) + String.format("%04X", datalength) + "0000";
            datastrlist.set(0, firstitem);

            //结束帧
            String lastitem = front + "68" +
                    Tools.Bytes2HexString(new byte[]{(byte) (datastrlist.size() / 256), (byte) (datastrlist.size() % 256)}, 2) +
                    "88" + "00" + "0000";
            datastrlist.add(lastitem);


            //计算crc16校验，并转换成byte数组，添加到datalist
            for (int i = 0; i < datastrlist.size(); i++) {
                String itemstr = datastrlist.get(i);
                String crc16 = String.format("%04X", CRC16.calcCrc16(Tools.HexString2Bytes(itemstr.substring(front.length(), itemstr.length() - 4))));
                itemstr = itemstr.substring(0, itemstr.length() - 4) + crc16.substring(2, 4) + crc16.substring(0, 2);
                datastrlist.set(i, itemstr);
                datalist.add(Tools.HexString2Bytes("AA" + nodeidstr + itemstr));
            }
        } catch (IOException ex) {
            Log.i("ccccccccc", ex.getMessage());
        }

        return datalist;
    }

    /**
     * 返回一个byte数组
     *
     * @param file 文件
     * @return 数组
     * @throws IOException
     */
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        // 获取文件大小
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            // 文件太大，无法读取
            throw new IOException("File is to large " + file.getName());
        }
        // 创建一个数据来保存文件数据
        byte[] bytes = new byte[(int) length];
        // 读取数据到byte数组中
        int offset = 0;
        int numRead;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    /**
     * 利尔达模块数据生成
     *
     * @param map
     * @return
     */
    public static byte[] AssembleCmdLierda(HashMap map) {
        byte[] data;

        String meteridstr = "10" + String.format("%06X", Long.parseLong((String) map.get(KEY_METER_ID)));
        int dataType = (Integer) map.get(KEY_DATA_TYPE);

        String subdatastr;

        switch (dataType) {
            case Const.DATAID_WRITE_VALVE:
                if (map.get(KEY_VALUE) == "0")//关阀
                {
                    String innerData = "0A" + meteridstr;
                    subdatastr = "AA"
                            + String.format("%02X", innerData.length() / 2)
                            + innerData
                            + "00"
                            + "55";

                } else {
                    String innerData = "09" + meteridstr;
                    subdatastr = "AA"
                            + String.format("%02X", innerData.length() / 2)
                            + innerData
                            + "00"
                            + "55";
                }
                break;
            case Const.DATAID_READ_DATA:
                String innerData = "11" + meteridstr;
                subdatastr = "AA"
                        + String.format("%02X", innerData.length() / 2)
                        + innerData
                        + "00"
                        + "55";
                break;
            default:
                return null;
        }

        //计算子数据的校验 完成子数据   子数据一次校验  总数据一次校验
        String strrrr = subdatastr.substring(2, subdatastr.length() - 4);
        byte subdatacs = Tools.CheckSum(Tools.HexString2Bytes(subdatastr.substring(2, subdatastr.length() - 4)));
        String subdatacsstr = String.format("%02X", (subdatacs & 0xff));
        subdatastr = subdatastr.substring(0, subdatastr.length() - 4) + subdatacsstr + subdatastr.substring(subdatastr.length() - 2, subdatastr.length());


        //总数据
        String lengthdata = "F111"
                + meteridstr
                + subdatastr;
        String generaldatastr = "AA"
                + String.format("%02X", lengthdata.length() / 2)
                + lengthdata
                + "00"
                + "55";

        //计算总数据的校验 完成总数据   子数据一次校验  总数据一次校验
        byte generaldatacs = Tools.CheckSum(Tools.HexString2Bytes(generaldatastr.substring(2, generaldatastr.length() - 4)));
        String generaldatacsstr = String.format("%02X", (generaldatacs & 0xff));
        generaldatastr = generaldatastr.substring(0, generaldatastr.length() - 4) + generaldatacsstr + generaldatastr.substring(generaldatastr.length() - 2, generaldatastr.length());

        data = Tools.HexString2Bytes(generaldatastr);

        return data;
    }


    /**
     * 反转16进制字符串
     *
     * @param hexstr
     * @return
     */
    public static String ReversalHexStr(String hexstr) {
        String str = "";
        if (hexstr.length() % 2 == 0) {
            for (int i = hexstr.length() / 2; i > 0; i--) {
                str += hexstr.substring(i * 2 - 2, i * 2);
            }
        }

        return str;
    }

    public static final String KEY_DATA_BYTES_STR = "KEY_DATA_BYTES_STR";
    public static final String KEY_ERR_MESSAGE = "KEY_ERR_MESSAGE";
    public static final String KEY_METER_ID = "KEY_METER_ID";
    public static final String KEY_RELAY_ID = "KEY_RELAY_ID";
    public static final String KEY_NODE_ID = "KEY_NODE_ID";
    public static final String KEY_SER = "KEY_SER";
    public static final String KEY_DATA_TYPE = "KEY_DATA_TYPE";
    public static final String KEY_VALUE = "KEY_VALUE";

    public static final String KEY_NET_ID = "KEY_NET_ID";

    public static final String KEY_RESULT = "KEY_RESULT";
    public static final String KEY_VALUE_NOW = "KEY_VALUE_NOW";
    public static final String KEY_JIE_SUAN_RI = "KEY_JIE_SUAN_RI";
    public static final String KEY_VALVE_STATE = "KEY_VALVE_STATE";
    public static final String KEY_BATTERY_6_STATE = "KEY_BATTERY_6_STATE";
    public static final String KEY_BATTERY_3_6_STATE = "KEY_BATTERY_3_6_STATE";
    public static final String KEY_SUCCESS = "KEY_SUCCESS";
    public static final String KEY_BATTERY_VALUE = "KEY_BATTERY_VALUE";
    public static final String KEY_TEMPERATURE = "KEY_TEMPERATURE";

    public static final String KEY_RF_MODULE_TYPE = "KEY_RF_MODULE_TYPE";


    public static final String key_meter_rf_baudrate = "meter_rf_baudrate";
    public static final String key_meter_rf_frequency = "meter_rf_frequency";
    public static final String key_meter_rf_factor = "meter_rf_factor";
    public static final String key_meter_rf_bw = "meter_rf_bw";
    public static final String key_meter_rf_new_nodeid = "meter_rf_new_nodeid";
    public static final String key_meter_rf_new_netid = "meter_rf_new_netid";
    public static final String key_meter_rf_power = "meter_rf_power";
    public static final String key_meter_rf_breath = "key_meter_rf_breath";

    public static final String key_handle_rf_baudrate = "handle_rf_baudrate";
    public static final String key_handle_rf_frequency = "handle_rf_frequency";
    public static final String key_handle_rf_factor = "handle_rf_factor";
    public static final String key_handle_rf_bw = "handle_rf_bw";
    public static final String key_handle_rf_new_nodeid = "handle_rf_new_nodeid";
    public static final String key_handle_rf_new_netid = "handle_rf_new_netid";
    public static final String key_handle_rf_power = "handle_rf_power";
    public static final String key_handle_rf_breath = "handle_rf_breath";


    public static final String key_rf_transmission_type = "key_rf_transmission_type";
    public static final String key_rf_relay_id = "key_rf_relay_id";

    public static final String key_rf_node_id_type = "key_rf_node_id_type";


    /**
     * 在此定义一个静态变量 为当前掌机模块的NETID 若表的netid与之相等则不再进行设置 而是直接操作表
     */
    private static int handle_rf_net_id = 170;
}