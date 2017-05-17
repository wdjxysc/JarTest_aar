package com.rthc.wdj.bluetoothtoollib.cmd;

import android.util.Log;


import com.rthc.wdj.bluetoothtoollib.util.StringTool;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Administrator on 2015/9/22.
 */
public class RfCmd {
    /**
     * 根据map获取对应的设置模块的命令
     * @param map
     * @return
     */
    public static byte[] AssembleRFCmd(HashMap map){

        //orderstr = "AF AF 00 00 AF 80 01 0D 04 00 7A 8C E1 0B 01 07 00 06 08 07 02 C6 0D 0A";  //490.2MHz
        //orderstr = "AF AF 00 00 AF 80 01 0D 04 00 6C 80 12 0B 01 07 00 06 08 07 02 C6 0D 0A";   //434MHz
        byte[] data;

        int dataType = (Integer) map.get(Cmd.KEY_DATA_TYPE);
        long netid = (Long) map.get(Cmd.KEY_NET_ID);

        byte netidbyte = (byte)netid;
        String orderstr = "";
        if (dataType == Const.DATAID_SET_HANDLE_RF_PARAM) {
            String formatdata = "";//转换格式后的数据

            String baudratestr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_baudrate)),2,'0');
            String factorstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_factor)), 2, '0');
            String bwstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_bw)), 2, '0');
            String netidstr = StringTool.padLeft(Long.toHexString((Long) map.get(Cmd.key_handle_rf_new_netid)),2,'0');
            String powerstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_power)),2,'0');
            String breathstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_breath)),2,'0');

            formatdata = baudratestr.toUpperCase()  //波特率 9600 串口速率： 1=1200， 2=2400， 3=4800， 4=9600， 5=19200， 6=38400， 7=57600
                    + "00"    //校验位 校验： 0=无 ， 1=奇校验， 2=偶校验
                    + "7A8CE1"//频率 490MHz 发射频率： 如： 433M， 433000000/61.035等于的值就是三个数值
                    + factorstr.toUpperCase()     //扩频因子 扩频因子： 7=128， 8=256， 9=512， 10=1024， 11=2048， 12=4096
                    + "01"                        //模式选择： 0=正常模式， 1=低功耗模式， 2=休眠模式。
                    + bwstr.toUpperCase()        //扩频带宽 扩频带宽： 6=62.5K， 7=125K， 8=256K， 9=512K
                    + "0000"     //节点ID  串口写中心模块nodeID时仍然为2ID
                    + netidstr.toUpperCase()      //网络ID
                    + powerstr.toUpperCase()       //发射功率
                    + breathstr.toUpperCase();       //呼吸周期： 0=2S, 1=4S, 2=6S , 3=8S , 4=10S


            String length = StringTool.padLeft(Integer.toHexString(formatdata.length() / 2).toUpperCase(), 2, '0');
            orderstr = "AF AF 00 00 AF 80 01"
                    + length
                    + formatdata
                    + "00"
                    + "0D 0A";

            orderstr = orderstr.replace(" ", "");
        }

        byte checksum = Tools.CheckSum(Tools.HexString2Bytes(orderstr.substring(0, orderstr.length() - 6)));
        String checksumstr = StringTool.padLeft(Tools.Bytes2HexString(new byte[]{checksum}, 1).toUpperCase(), 2, '0');
        orderstr = orderstr.substring(0, orderstr.length() - 6) + checksumstr + orderstr.substring(orderstr.length() - 4, orderstr.length());

        data = Tools.HexString2Bytes(orderstr);

        return data;
    }

    /**
     * 解析数据
     * @param data
     * @return
     */
    public static HashMap ParseDataToMap(byte[] data) {
        HashMap<String, Object> map = new HashMap<String, Object>();



        return map;
    }
}
