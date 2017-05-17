package com.rthc.wdj.bluetoothtoollib.cmd;

import com.rthc.wdj.bluetoothtoollib.util.CRC16;
import com.rthc.wdj.bluetoothtoollib.util.StringTool;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Administrator on 2015/12/4.
 */
public class BleCmd {

    public static final int CTR_DIRECTION_MSK = 0x01 << 7;
    public static final int CTR_DIRECTION_REQUEST = 0x00;
    public static final int CTR_DIRECTION_RESPONSE = 0x01 << 7;


    public static final int CTR_NEED_RESPONSE_MSK = 0x01 << 6;
    public static final int CTR_NEED_RESPONSE = 0x01 << 6;
    public static final int CTR_NO_NEED_RESPONSE = 0x00;

    public static final int CTR_RESPONSE_MSK = 0x01 << 5;
    public static final int CTR_RESPONSE_OK = 0x00;
    public static final int CTR_RESPONSE_ERROR = 0x01 << 5;

    public static final int CTR_TRANS_MSK = 0x01 << 4;
    public static final int CTR_TRANS_Y = 0x00;
    public static final int CTR_TRANS_N = 0x01 << 4;

    public static final int CTR_MODULE_ID_MSK = 0x0f;
    public static final int CTR_MODULE_ID_BOX = 0x00;
    public static final int CTR_MODULE_ID_JIEXUN = 0x01;
    public static final int CTR_MODULE_ID_SKYSHOOT = 0x02;
    public static final int CTR_MODULE_ID_LIERDA = 0x03;


    public static final int TAIL = 0x55;
    public static final int HEAD = 0xAA;

    public static final int SYNC_BYTE = 0xFE;

    //设置盒子接收表端回复的超时时间
    private static int TIME_OUT = 20000;

    public BleCmd() {

    }


    private byte[] AssembleBleCmd(HashMap<String, Object> hashMap) {

        byte[] bleCmd;


        int direction = CTR_DIRECTION_REQUEST;//请求帧
        int isNeedResponse = CTR_NEED_RESPONSE;//需响应
        int isResponseOk = CTR_RESPONSE_OK;//正常响应
        int isTrans = CTR_TRANS_Y;//透传
        int moduleID = Integer.parseInt(hashMap.get(KEY_BLE_MODULE_ID).toString());
        if (moduleID == CTR_MODULE_ID_BOX) {
            isTrans = CTR_TRANS_N;
        }

        //控制字
        int control = direction + isNeedResponse + isResponseOk + isTrans + moduleID;


        //Data
        byte[] cmd = new byte[]{};
        switch (moduleID) {
            case CTR_MODULE_ID_BOX:
                cmd = BoxCmd.AssembleBoxCmd(hashMap);
                break;
            case CTR_MODULE_ID_JIEXUN:
                cmd = Cmd.AssembleCmd(hashMap);
                break;
            case CTR_MODULE_ID_SKYSHOOT:
                cmd = Cmd.AssembleCmd(hashMap);
                break;
            case CTR_MODULE_ID_LIERDA:
                cmd = Cmd.AssembleCmdLierda(hashMap);
                break;
        }

        if (cmd == null) return null;
        else if (cmd.length == 0) return null;

        //Length
        int dataLength = cmd.length + 8; //数据总长度

        String bleCmdStr = String.format("%02X", SYNC_BYTE)
                + String.format("%02X", SYNC_BYTE)
                + String.format("%02X", HEAD)
                + String.format("%02X", dataLength)
                + String.format("%02X", control)
                + StringTool.reversalHexString(String.format("%04X", TIME_OUT))
                + Tools.Bytes2HexString(cmd, cmd.length)
                + "0000"
                + String.format("%02X", TAIL);

        int crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(bleCmdStr.substring(4, bleCmdStr.length() - 6)));

        bleCmdStr = bleCmdStr.substring(0, bleCmdStr.length() - 6)
                + StringTool.reversalHexString(String.format("%04X", crc16))
                + bleCmdStr.substring(bleCmdStr.length() - 2, bleCmdStr.length());

        bleCmd = Tools.HexString2Bytes(bleCmdStr);
        return bleCmd;
    }

    /**
     * 获取读数据命令
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getReadDataCmd(String meterId, int bleModuleId, boolean useRelay, int relayId) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);

        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_READ_DATA);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if (meterId.length() == 14) {
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取读数据命令 2ID
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getReadDataCmd2(String meterId, int bleModuleId, boolean useRelay, int relayId) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);

        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_READ_DATA);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if (meterId.length() == 14) {
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取开阀命令
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getOpenValveCmd(String meterId, int bleModuleId, boolean useRelay, int relayId) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_VALVE);
        map.put(Cmd.KEY_VALUE, "1");
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取开阀命令 2ID
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getOpenValveCmd2(String meterId, int bleModuleId, boolean useRelay, int relayId) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_VALVE);
        map.put(Cmd.KEY_VALUE, "1");
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取关阀命令
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getCloseValveCmd(String meterId, int bleModuleId, boolean useRelay, int relayId) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_VALVE);
        map.put(Cmd.KEY_VALUE, "0");
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取关阀命令 2ID
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getCloseValveCmd2(String meterId, int bleModuleId, boolean useRelay, int relayId) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_VALVE);
        map.put(Cmd.KEY_VALUE, "0");
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取写表ID命令
     * @param oldMeterId
     * @param newMeterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterIdCmd(String oldMeterId, String newMeterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, oldMeterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_ADDRESS);
        map.put(Cmd.KEY_VALUE, newMeterId);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (oldMeterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(oldMeterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(oldMeterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }



    /**
     * 获取写表ID命令 2ID
     * @param oldMeterId
     * @param newMeterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterIdCmd2(String oldMeterId, String newMeterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, oldMeterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_ADDRESS);
        map.put(Cmd.KEY_VALUE, newMeterId);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (oldMeterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(oldMeterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(oldMeterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取广播方式写表ID命令  原表ID为 AAAAAAAAAAAAAA  nodeId 为FFFFFFFF
     * @param oldMeterId
     * @param nodeId
     * @param newMeterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterIdByBroadcastCmd(String oldMeterId, String nodeId, String newMeterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;

        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, oldMeterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_ADDRESS);
        map.put(Cmd.KEY_VALUE, newMeterId);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (oldMeterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(oldMeterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(nodeId, 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取广播方式写表ID命令  原表ID为 AAAAAAAAAAAAAA  nodeId 为FFFF    2ID
     * @param oldMeterId
     * @param nodeId
     * @param newMeterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterIdByBroadcastCmd2(String oldMeterId, String nodeId, String newMeterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;

        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, oldMeterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_ADDRESS);
        map.put(Cmd.KEY_VALUE, newMeterId);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (oldMeterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(oldMeterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(nodeId, 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取写表底数命令
     * @param meterId
     * @param meterValue
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterValueCmd(String meterId, float meterValue, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_DATA);
        map.put(Cmd.KEY_VALUE, meterValue + "");
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }


        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取写表底数命令  2ID
     * @param meterId
     * @param meterValue
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterValueCmd2(String meterId, float meterValue, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_DATA);
        map.put(Cmd.KEY_VALUE, meterValue + "");
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }


        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取读表状态命令
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getReadMeterStateCmd(String meterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_READ_METER_STATE);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取读表状态命令   2ID
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getReadMeterStateCmd2(String meterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_READ_METER_STATE);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取写表状态命令 默认正常关闭
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterStateCmd(String meterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_METER_STATE);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取写表状态命令 默认正常关闭    2ID
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getWriteMeterStateCmd2(String meterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_METER_STATE);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取写表NETID命令
     * @param meterId
     * @param bleModuleId
     * @return
     */
    public byte[] getWriteMeterNetIdCmd(String meterId,int newNetId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_SET_METER_RF_PARAM);
        map.put(Cmd.key_meter_rf_new_netid, newNetId);

        map.put(Cmd.key_meter_rf_baudrate, 4);
        map.put(Cmd.key_meter_rf_factor, 11);
        map.put(Cmd.key_meter_rf_bw, 7);
        map.put(Cmd.key_meter_rf_frequency, 0x7A8CE1);
        map.put(Cmd.key_meter_rf_new_nodeid, "00000000");//此处写nodeid无用  表总是以表ID最后6位为nodeid
        map.put(Cmd.key_meter_rf_power, 7);
        map.put(Cmd.key_meter_rf_breath, 2);

        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);

        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(8, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取写表NETID命令  2ID
     * @param meterId
     * @param bleModuleId
     * @return
     */
    public byte[] getWriteMeterNetIdCmd2(String meterId,int newNetId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_SET_METER_RF_PARAM);
        map.put(Cmd.key_meter_rf_new_netid, newNetId);

        map.put(Cmd.key_meter_rf_baudrate, 4);
        map.put(Cmd.key_meter_rf_factor, 11);
        map.put(Cmd.key_meter_rf_bw, 7);
        map.put(Cmd.key_meter_rf_frequency, 0x7A8CE1);
        map.put(Cmd.key_meter_rf_new_nodeid, "0000");//此处写nodeid无用  表总是以表ID最后4位为nodeid
        map.put(Cmd.key_meter_rf_power, 7);
        map.put(Cmd.key_meter_rf_breath, 2);

        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);

        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong(meterId.substring(10, 14), 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取复位表命令 nodeid为FFFFFFFF
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getResetMeterCmd(String meterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_UPDATE_FIRMWARE_RESET);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_4_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong("FFFFFFFF",16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取复位表命令 nodeid为FFFFFFFF  2ID
     * @param meterId
     * @param bleModuleId
     * @param useRelay
     * @param relayId
     * @return
     */
    public byte[] getResetMeterCmd2(String meterId, int bleModuleId, boolean useRelay, int relayId){
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_METER_ID, meterId);
        map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_UPDATE_FIRMWARE_RESET);
        if(useRelay) {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.RELAY);
            map.put(Cmd.key_rf_relay_id, relayId);
        }else {
            map.put(Cmd.key_rf_transmission_type, Cmd.RF_TRANSMISSION_TYPE.NO_RELAY);
        }
        map.put(Cmd.key_rf_node_id_type, Cmd.RF_NODE_ID_TYPE.NODE_ID_2_BYTES);
        if (meterId.length() == 8) {//利尔达
            map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
        } else if(meterId.length() == 14){
            map.put(KEY_BLE_MODULE_ID, bleModuleId);
            map.put(Cmd.KEY_NODE_ID, Long.parseLong("FFFF", 16));
        } else {
            return null;
        }

        data = this.AssembleBleCmd(map);

        return data;
    }



    /**
     * 获取盒子升级DataList
     * @return
     */
    public ArrayList<byte[]> getUpdateCmd() {
        BoxUpdate boxUpdate = null;
        try {
            boxUpdate = new BoxUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        ArrayList<byte[]> dataList = boxUpdate.getUpdateDataList();
        if (dataList == null) return null;

        int direction = CTR_DIRECTION_REQUEST;//请求帧
        int isNeedResponse = CTR_NEED_RESPONSE;//需响应
        int isResponseOk = CTR_RESPONSE_OK;//正常响应
        int isTrans = CTR_TRANS_N;         //非透传
        int moduleID = CTR_MODULE_ID_BOX; //盒子


        //控制字
        int control = direction + isNeedResponse + isResponseOk + isTrans + moduleID;

        for (int i = 0; i < dataList.size(); i++) {
            byte[] cmd = dataList.get(i);
            //Length
            int dataLength = cmd.length + 8; //数据总长度

            String bleCmdStr = String.format("%02X", SYNC_BYTE)
                    + String.format("%02X", SYNC_BYTE)
                    + String.format("%02X", HEAD)
                    + String.format("%02X", dataLength)
                    + String.format("%02X", control)
                    + String.format("%04X", TIME_OUT)
                    + Tools.Bytes2HexString(cmd, cmd.length)
                    + "0000"
                    + String.format("%02X", TAIL);

            int crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(bleCmdStr.substring(4, bleCmdStr.length() - 6)));

            bleCmdStr = bleCmdStr.substring(0, bleCmdStr.length() - 6)
                    + StringTool.reversalHexString(String.format("%04X", crc16))
                    + bleCmdStr.substring(bleCmdStr.length() - 2, bleCmdStr.length());

            dataList.set(i, Tools.HexString2Bytes(bleCmdStr));
        }

        return dataList;
    }

    /**
     * 获取设置盒子蓝牙名命令
     * @param name
     * @return
     */
    public byte[] getSetBleNameCmd(String name) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.KEY_VALUE, name);
        map.put(Cmd.KEY_DATA_TYPE, BoxCmd.BOX_DATA_ID_SET_BLE_NAME);
        map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_BOX);

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取设置中心模块网络ID命令
     * @param netId
     * @param rfModuleType
     * @return
     */
    public byte[] getSetRFNetIdCmd(long netId, Const.RfModuleType rfModuleType) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.key_handle_rf_new_netid, netId);
        map.put(Cmd.KEY_RF_MODULE_TYPE, rfModuleType);
        map.put(Cmd.key_handle_rf_breath, 1);//默认呼吸时间 1:4s
        map.put(Cmd.KEY_DATA_TYPE, BoxCmd.BOX_DATA_ID_SET_RF_PARAM);
        map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_BOX);

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 获取设置中心模块呼吸周期命令
     * @param breath
     * @param rfModuleType
     * @return
     */
    public byte[] getSetRFBreathCmd(int breath, Const.RfModuleType rfModuleType) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.key_handle_rf_breath, breath);
        map.put(Cmd.KEY_RF_MODULE_TYPE, rfModuleType);
        map.put(Cmd.key_handle_rf_new_netid, 0xAA);//默认netId 0xAA
        map.put(Cmd.KEY_DATA_TYPE, BoxCmd.BOX_DATA_ID_SET_RF_PARAM);
        map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_BOX);

        data = this.AssembleBleCmd(map);

        return data;
    }

    /**
     * 获取设置中心模块呼吸周期命令
     * @param breath
     * @param rfModuleType
     * @return
     */
    public byte[] getSetRFCmd(int netId, int breath, Const.RfModuleType rfModuleType) {
        byte[] data;
        HashMap<String, Object> map = new HashMap<>();
        map.put(Cmd.key_handle_rf_new_netid, netId);
        map.put(Cmd.key_handle_rf_breath, breath);
        map.put(Cmd.KEY_RF_MODULE_TYPE, rfModuleType);
        map.put(Cmd.KEY_DATA_TYPE, BoxCmd.BOX_DATA_ID_SET_RF_PARAM);
        map.put(KEY_BLE_MODULE_ID, CTR_MODULE_ID_BOX);

        data = this.AssembleBleCmd(map);

        return data;
    }


    /**
     * 解析从蓝牙返回的数据
     * @param data
     * @return
     */
    public HashMap<String, Object> parseBleData(byte[] data) {
        HashMap<String, Object> hashMap = new HashMap<>();

        if (data == null) return hashMap;

        if (data.length < 5) return hashMap;

        int revCrc16 = (data[data.length - 3] & 0xff) + ((data[data.length - 2] & 0xff) << 8);//低字节在前 高字节在后

        int realCrc16 = CRC16.calcCrc16(data, 0, data.length - 3);

        if (revCrc16 != realCrc16) {
            hashMap.put(Cmd.KEY_ERR_MESSAGE, "校验错误");
            hashMap.put(Cmd.KEY_SUCCESS, -1);
            return hashMap;
        } else {
            int control = data[2] & 0xff;

            int moduleId = control & CTR_MODULE_ID_MSK;

            byte[] realData = Arrays.copyOfRange(data, 5, data.length - 3);

            switch (moduleId) {
                case CTR_MODULE_ID_BOX:
                    hashMap = BoxCmd.parseData(realData);
                    hashMap.put(BleCmd.KEY_BLE_MODULE_ID, CTR_MODULE_ID_BOX);
                    break;
                case CTR_MODULE_ID_JIEXUN:
                    hashMap = ParseData.ParseDataToMap(realData);
                    hashMap.put(BleCmd.KEY_BLE_MODULE_ID, CTR_MODULE_ID_JIEXUN);
                    break;
                case CTR_MODULE_ID_SKYSHOOT:
                    hashMap = ParseData.ParseDataToMap(realData);
                    hashMap.put(BleCmd.KEY_BLE_MODULE_ID, CTR_MODULE_ID_SKYSHOOT);
                    break;
                case CTR_MODULE_ID_LIERDA:
                    hashMap = ParseData.ParseDataToMapLierda(realData);
                    hashMap.put(BleCmd.KEY_BLE_MODULE_ID, CTR_MODULE_ID_LIERDA);
                    break;
            }

            hashMap.put(BleCmd.KEY_BLE_MODULE_ID, moduleId);
        }

        return hashMap;
    }


    public static final String KEY_BLE_MODULE_ID = "KEY_BLE_MODULE_ID";


    /**
     * 判断是否是第一包 若是则返回包头HEAD位置 若不是返回-1
     *
     * @return
     */
    public static int isFirstFrame(byte[] data) {
        int result = -1;

        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xff) == SYNC_BYTE && (data[i + 1] & 0xff) == HEAD) {
                return i + 1;
            }
        }
        return result;
    }


}