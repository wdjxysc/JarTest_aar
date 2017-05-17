package com.rthc.wdj.bluetoothtoollib.cmd;

import com.rthc.wdj.bluetoothtoollib.util.CRC16;
import com.rthc.wdj.bluetoothtoollib.util.StringTool;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.HashMap;

/**
 * Created by Administrator on 2015/12/10.
 */
public class BoxCmd {

    public static final int BOX_DATA_ID_SET_BLE_NAME = 0x01;
    public static final int BOX_DATA_ID_SET_BLE_PARAM = 0x02;
    public static final int BOX_DATA_ID_SET_RF_PARAM = 0x10;


    public static final int NAME_LENGTH_MAX = 18;


    public static byte[] AssembleBoxCmd(HashMap<String, Object> hashMap) {
        byte[] boxCmd;

        int dataType = Integer.parseInt(hashMap.get(Cmd.KEY_DATA_TYPE).toString());

        String str = "";
        switch (dataType) {
            case BOX_DATA_ID_SET_BLE_NAME:
                String name = hashMap.get(Cmd.KEY_VALUE).toString();
                byte[] nameBytes = name.getBytes();
                String nameHexStr = Tools.Bytes2HexString(nameBytes, nameBytes.length);

                if (name.length() > NAME_LENGTH_MAX) return null;

                str = String.format("%02X", BoxUpdate.BLE_HEAD)
                        + "0000"
                        + String.format("%02X", BOX_DATA_ID_SET_BLE_NAME)
                        + String.format("%02X", NAME_LENGTH_MAX)
                        + StringTool.padRight(nameHexStr, NAME_LENGTH_MAX * 2, '0')
                        + "0000"
                        + String.format("%02X", BoxUpdate.BLE_TAIL);
                break;
            case BOX_DATA_ID_SET_BLE_PARAM:
                break;
            case BOX_DATA_ID_SET_RF_PARAM:

                Const.RfModuleType rfModuleType = (Const.RfModuleType) hashMap.get(Cmd.KEY_RF_MODULE_TYPE);
                int moduleId = 0;
                switch (rfModuleType) {
                    case JIEXUN:
                        moduleId = 0;
                        break;
                    case SKY_SHOOT:
                        moduleId = 1;
                        break;
                    case LIERDA:
                        moduleId = 2;
                        break;
                }

                HashMap<String, Object> map = new HashMap<>();
                map.put(Cmd.key_handle_rf_baudrate, 4);
                map.put(Cmd.key_handle_rf_factor, 11);
                map.put(Cmd.key_handle_rf_bw, 7);
                map.put(Cmd.key_handle_rf_new_nodeid, 0);
                map.put(Cmd.key_handle_rf_new_netid, hashMap.get(Cmd.key_handle_rf_new_netid));
                map.put(Cmd.key_handle_rf_power, 7);
                map.put(Cmd.key_handle_rf_breath, hashMap.get(Cmd.key_handle_rf_breath));


                String baudratestr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_baudrate)), 2, '0');
                String factorstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_factor)), 2, '0');
                String bwstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_bw)), 2, '0');
                String netidstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_new_netid)), 2, '0');
                String powerstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_power)), 2, '0');
                String breathstr = StringTool.padLeft(Integer.toHexString((Integer) map.get(Cmd.key_handle_rf_breath)), 2, '0');

                String dataStr = String.format("%02X", moduleId) //模块 0：捷迅 1：skyshoot 2：利尔达
                        + baudratestr.toUpperCase()  //波特率 9600 串口速率： 1=1200， 2=2400， 3=4800， 4=9600， 5=19200， 6=38400， 7=57600
                        + "00"    //校验位 校验： 0=无 ， 1=奇校验， 2=偶校验
                        + "E18C7A"//频率 490MHz 发射频率： 如： 433M， 433000000/61.035等于的值就是三个数值  低位在前
                        + factorstr.toUpperCase()     //扩频因子 扩频因子： 7=128， 8=256， 9=512， 10=1024， 11=2048， 12=4096
                        + "01"                        //模式选择： 0=正常模式， 1=低功耗模式， 2=休眠模式。
                        + bwstr.toUpperCase()        //扩频带宽 扩频带宽： 6=62.5K， 7=125K， 8=256K， 9=512K
                        + "00000000"     //节点ID
                        + netidstr.toUpperCase()      //网络ID
                        + powerstr.toUpperCase()       //发射功率
                        + breathstr.toUpperCase();       //呼吸周期： 0=2S, 1=4S, 2=6S , 3=8S , 4=10S

                str = String.format("%02X", BoxUpdate.BLE_HEAD)
                        + "0000"
                        + String.format("%02X", BOX_DATA_ID_SET_RF_PARAM)
                        + String.format("%02X", 16)
                        + dataStr
                        + "0000"
                        + String.format("%02X", BoxUpdate.BLE_TAIL);
                break;
        }

        if (str.equals("")) return null;

        int crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(str.substring(0, str.length() - 6)));

        str = str.substring(0, str.length() - 6)
                + StringTool.reversalHexString(String.format("%04X", crc16))
                + str.substring(str.length() - 2, str.length());

        boxCmd = Tools.HexString2Bytes(str);

        return boxCmd;
    }


    public static HashMap<String, Object> parseData(byte[] data) {
        HashMap<String, Object> map = new HashMap<String, Object>();

        int dataType = data[3] & 0xff;

        map.put(BleCmd.KEY_BLE_MODULE_ID, BleCmd.CTR_MODULE_ID_BOX);

        if ((data[5] & 0xff) == 0 && (data[6] & 0xff) == 0) {
            map.put(Cmd.KEY_SUCCESS, 1);
        } else {
            map.put(Cmd.KEY_SUCCESS, -1);
            map.put(Cmd.KEY_ERR_MESSAGE, "数据格式错误");
        }

        switch (dataType) {

        }

        return map;
    }
}
