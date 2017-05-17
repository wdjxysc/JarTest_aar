package com.rthc.wdj.bluetoothtoollib.cmd;


import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.HashMap;

/**
 * Created by Administrator on 2015/7/18 0018.
 */
public class ParseData {


    /**
     * 对接收到的数据进行解析，返回map对象
     *
     * @param data 串口接收到的byte数组
     * @return map对象
     */
    public static HashMap<String, Object> ParseDataToMap(byte[] data) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(Cmd.KEY_DATA_BYTES_STR, Tools.Bytes2HexString(data, data.length));

        if (data == null) return map;

        String datastr = Tools.Bytes2HexString(data, data.length);


        //去掉数据头获取数据主体部分
        int rf_relay_id_length = 0;
        switch (Cmd.rf_transmission_type) {
            case NO_RELAY:
                rf_relay_id_length = 0;
                break;
            case RELAY:
                rf_relay_id_length = Const.RF_RELAY_ID_LENGTH;
                break;
        }

        int head_length = (Const.IN_HEAD_LENGTH_4_BYTES + rf_relay_id_length) * 2;
        switch (Cmd.rf_node_id_type) {
            case NODE_ID_2_BYTES:
                head_length = (Const.IN_HEAD_LENGTH_2_BYTES + rf_relay_id_length) * 2;
                break;
            case NODE_ID_4_BYTES:
                head_length = (Const.IN_HEAD_LENGTH_4_BYTES + rf_relay_id_length) * 2;
                break;
        }

        String realdatastr = datastr.substring(head_length, datastr.length());


        data = Tools.HexString2Bytes(realdatastr);

        map.put(Cmd.KEY_RESULT, -1);
        map.put(Cmd.KEY_SUCCESS, -1);
        if (data[0] == Const.HEAD && data[data.length - 1] == Const.TAIL) {

            int sum = 0;
            for (int i = 0; i < data.length - 2; i++) {
                sum += data[i] & 0xff;
            }
            if ((data[data.length - 2] & 0xff) != sum % 256) {//和校验
                map.put(Cmd.KEY_ERR_MESSAGE, "和校验错误");
                map.put(Cmd.KEY_RESULT, -1);
                map.put(Cmd.KEY_SUCCESS, -1);
                return map;
            }

            byte[] meteriddata = new byte[]{data[8], data[7], data[6], data[5], data[4], data[3], data[2]};
            map.put(Cmd.KEY_METER_ID, Tools.Bytes2HexString(meteriddata, meteriddata.length));
            map.put(Cmd.KEY_SER, data[13] & 0xff);
            int datatype = ((data[12] & 0xff) << 8) + (data[11] & 0xff);
            map.put(Cmd.KEY_DATA_TYPE, datatype);
            int ctr = data[9] & 0xff;

            map.put(Cmd.KEY_RESULT, -1);
            switch (ctr) {
                case Const.CTR_1_READ_DATA://读数据正常应答
                    map.put(Cmd.KEY_RESULT, 1);
                    if (datatype == Const.DATAID_READ_DATA) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                        System.out.println("接收到实时抄表数据");
                        byte[] valuenowdata = new byte[]{data[18], data[17], data[16], data[15], data[14]};
                        String valuenowstr = Tools.Bytes2HexString(valuenowdata, valuenowdata.length);
                        map.put(Cmd.KEY_VALUE_NOW, Float.parseFloat(valuenowstr) / 10);

                        byte[] valuejiesuanri = new byte[]{data[23], data[22], data[21], data[20], data[19]};
                        String valuejiesuanristr = Tools.Bytes2HexString(valuejiesuanri, valuejiesuanri.length);
                        map.put(Cmd.KEY_JIE_SUAN_RI, Float.parseFloat(valuejiesuanristr) / 10);

                        byte[] meterState = new byte[]{data[data.length - 4], data[data.length - 3]};

                        MeterStateConst.STATE_VALVE state_valve = MeterStateConst.STATE_VALVE.CLOSE;
                        if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_CLSD) {
                            state_valve = MeterStateConst.STATE_VALVE.CLOSE;
                        } else if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_OPND) {
                            state_valve = MeterStateConst.STATE_VALVE.OPEN;
                        } else if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_ERR) {
                            state_valve = MeterStateConst.STATE_VALVE.ERROR;
                        }
                        map.put(Cmd.KEY_VALVE_STATE, state_valve);


                        MeterStateConst.STATE_POWER_3_6_V state_power_3_6_v;
                        if ((meterState[0] & MeterStateConst.S_BAT_3_6V_MSK) == MeterStateConst.S_BAT_3_6V_LO) {
                            state_power_3_6_v = MeterStateConst.STATE_POWER_3_6_V.LOW;
                        } else {
                            state_power_3_6_v = MeterStateConst.STATE_POWER_3_6_V.OK;
                        }
                        map.put(Cmd.KEY_BATTERY_3_6_STATE, state_power_3_6_v);

                        MeterStateConst.STATE_POWER_6_V state_power_6_v;
                        if ((meterState[0] & MeterStateConst.S_BAT_6V_MSK) == MeterStateConst.S_BAT_6V_LO) {
                            state_power_6_v = MeterStateConst.STATE_POWER_6_V.LOW;
                        } else {
                            state_power_6_v = MeterStateConst.STATE_POWER_6_V.OK;
                        }
                        map.put(Cmd.KEY_BATTERY_6_STATE, state_power_6_v);

                    } else if (datatype == Const.DATAID_READ_METER_STATE) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                        byte[] meterState = new byte[]{data[data.length - 4], data[data.length - 3]};

                        MeterStateConst.STATE_VALVE state_valve = MeterStateConst.STATE_VALVE.CLOSE;
                        if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_CLSD) {
                            state_valve = MeterStateConst.STATE_VALVE.CLOSE;
                        } else if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_OPND) {
                            state_valve = MeterStateConst.STATE_VALVE.OPEN;
                        } else if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_ERR) {
                            state_valve = MeterStateConst.STATE_VALVE.ERROR;
                        }
                        map.put(Cmd.KEY_VALVE_STATE, state_valve);


                        MeterStateConst.STATE_POWER_3_6_V state_power_3_6_v;
                        if ((meterState[0] & MeterStateConst.S_BAT_3_6V_MSK) == MeterStateConst.S_BAT_3_6V_LO) {
                            state_power_3_6_v = MeterStateConst.STATE_POWER_3_6_V.LOW;
                        } else {
                            state_power_3_6_v = MeterStateConst.STATE_POWER_3_6_V.OK;
                        }
                        map.put(Cmd.KEY_BATTERY_3_6_STATE, state_power_3_6_v);

                        MeterStateConst.STATE_POWER_6_V state_power_6_v;
                        if ((meterState[0] & MeterStateConst.S_BAT_6V_MSK) == MeterStateConst.S_BAT_6V_LO) {
                            state_power_6_v = MeterStateConst.STATE_POWER_6_V.LOW;
                        } else {
                            state_power_6_v = MeterStateConst.STATE_POWER_6_V.OK;
                        }
                        map.put(Cmd.KEY_BATTERY_6_STATE, state_power_6_v);

                    } else if (datatype == Const.DATAID_READ_TIME) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                    }
                    break;
                case Const.CTR_2_READ_DATA://读数据异常应答
                    map.put(Cmd.KEY_RESULT, -1);
                    break;
                case Const.CTR_1_READ_KEY_VERSION://读秘钥版本号正常应答
                    map.put(Cmd.KEY_RESULT, 1);
                    break;
                case Const.CTR_2_READ_KEY_VERSION://读秘钥版本号异常应答
                    map.put(Cmd.KEY_RESULT, -1);
                    break;
                case Const.CTR_1_READ_ADRRESS://读地址正常应答
                    map.put(Cmd.KEY_RESULT, 1);
                    break;
                case Const.CTR_2_READ_ADRRESS://读地址异常应答
                    map.put(Cmd.KEY_RESULT, -1);
                    break;
                case Const.CTR_4_WRITE_DATA://写数据正常应答
                    map.put(Cmd.KEY_RESULT, 1);
                    if (datatype == Const.DATAID_WRITE_VALVE) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                        byte[] meterState = new byte[]{data[data.length - 4], data[data.length - 3]};


                        MeterStateConst.STATE_VALVE state_valve = MeterStateConst.STATE_VALVE.CLOSE;
                        if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_CLSD) {

                            state_valve = MeterStateConst.STATE_VALVE.CLOSE;
                        } else if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_OPND) {

                            state_valve = MeterStateConst.STATE_VALVE.OPEN;
                        } else if ((meterState[0] & MeterStateConst.S_VALVE_MSK) == MeterStateConst.S_VALVE_ERR) {

                            state_valve = MeterStateConst.STATE_VALVE.ERROR;
                        }

                        map.put(Cmd.KEY_VALVE_STATE, state_valve);

                        MeterStateConst.STATE_POWER_3_6_V state_power_3_6_v;
                        if ((meterState[0] & MeterStateConst.S_BAT_3_6V_MSK) == MeterStateConst.S_BAT_3_6V_LO) {
                            state_power_3_6_v = MeterStateConst.STATE_POWER_3_6_V.LOW;
                        } else {
                            state_power_3_6_v = MeterStateConst.STATE_POWER_3_6_V.OK;
                        }
                        map.put(Cmd.KEY_BATTERY_3_6_STATE, state_power_3_6_v);

                        MeterStateConst.STATE_POWER_6_V state_power_6_v;
                        if ((meterState[0] & MeterStateConst.S_BAT_6V_MSK) == MeterStateConst.S_BAT_6V_LO) {
                            state_power_6_v = MeterStateConst.STATE_POWER_6_V.LOW;
                        } else {
                            state_power_6_v = MeterStateConst.STATE_POWER_6_V.OK;
                        }
                        map.put(Cmd.KEY_BATTERY_6_STATE, state_power_6_v);

                    } else if (datatype == Const.DATAID_WRITE_TIME) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                    } else if (datatype == Const.DATAID_WRITE_METER_STATE) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                    } else if (datatype == Const.DATAID_SET_METER_RF_PARAM) {
                        map.put(Cmd.KEY_SUCCESS, 1);
//                        if (data[14] == 0) {
//                            map.put("success", 1);
//                        } else {
//                            map.put("success", -1);
//                        }
                    }
                    break;
                case Const.CTR_5_WRITE_DATA://写数据异常应答
                    map.put(Cmd.KEY_RESULT, -1);
                    break;
                case Const.CTR_4_WRITE_ADRRESS://写地址正常应答
                    if (datatype == Const.DATAID_WRITE_ADDRESS) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                    }
                    map.put(Cmd.KEY_RESULT, 1);
                    break;
                case Const.CTR_5_WRITE_ADRRESS://写地址异常应答
                    map.put(Cmd.KEY_RESULT, -1);
                    break;
                case Const.CTR_4_WRITE_SYS_DATA://写表底数正常应答
                    if (datatype == Const.DATAID_WRITE_DATA) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                    }
                    map.put(Cmd.KEY_RESULT, 1);
                    break;
                case Const.CTR_5_WRITE_SYS_DATA://写表底数异常应答
                    map.put(Cmd.KEY_RESULT, -1);
                    break;
                case Const.CTR_4_UPDATE_FIRMWARE://固件升级正常应答
                    if (datatype == Const.DATAID_UPDATE_FIRMWARE_RESET) {
                        map.put(Cmd.KEY_SUCCESS, 1);
                    }
                    map.put(Cmd.KEY_RESULT, 1);
                    break;
                default:
                    break;
            }
        } else {
            map.put(Cmd.KEY_ERR_MESSAGE, "数据错误");
        }

        return map;
    }


    /**
     * 利尔达协议解析
     *
     * @param data
     * @return
     */
    public static HashMap<String, Object> ParseDataToMapLierda(byte[] data) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(Cmd.KEY_SUCCESS, -1);

//        byte[] data = new byte[datao.length-5];
//
//        System.arraycopy(datao,5,data,0,data.length);

        if (data != null) {
            if (data.length > 2) {
                int length = data[1] & 0xff;

                if (length == data.length - 4) {
                    if ((data[0] & 0xff) == 0xAA && (data[data.length - 1] & 0xff) == 0x55) {

                        int sum = 0;
                        for (int i = 1; i < data.length - 2; i++) {
                            sum += data[i] & 0xff;
                        }

                        if ((data[data.length - 2] & 0xff) != sum % 256) {//和校验
                            map.put(Cmd.KEY_ERR_MESSAGE, "和校验错误");
                            map.put(Cmd.KEY_SUCCESS, -1);
                            return map;
                        }

                        String strsss = Tools.Bytes2HexString(data, data.length);
                        //AA04C2004F4B6055
                        if (strsss.equals("AA04C2004F4B6055")) {
                            map.put(Cmd.KEY_ERR_MESSAGE, "ACK");
                            map.put(Cmd.KEY_SUCCESS, -1);
                            return map;
                        } else if (data.length < 15) {
                            map.put(Cmd.KEY_SUCCESS, -1);
                            map.put(Cmd.KEY_ERR_MESSAGE, "unknown data:" + strsss);
                            return map;
                        }

                        int datatype = (data[12] & 0xff);

                        byte[] meterstate = new byte[2];

                        if (datatype == 0x91) {//抄表数据
                            map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_READ_DATA);
                            map.put(Cmd.KEY_SUCCESS, 1);
                            byte[] metervaluedata = new byte[]{data[20], data[19], data[18], data[17]};
                            String valuehexstr = Tools.Bytes2HexString(metervaluedata, metervaluedata.length);
                            long valuelong = Long.parseLong(valuehexstr, 16);
                            double value = valuelong * 0.01;
                            map.put(Cmd.KEY_VALUE_NOW, value);

                            meterstate = new byte[]{data[21], data[22]};

                            byte[] batteryv = new byte[]{data[24], data[23]};
                            double batteryvalue = ((batteryv[0] & 0xff) + (batteryv[1] & 0xff) * 256) * 0.01;

                            int temperature = (data[25] & 0xff);
                            map.put(Cmd.KEY_BATTERY_VALUE, batteryvalue);
                            map.put(Cmd.KEY_TEMPERATURE, temperature);
                        } else if (datatype == 0x89) {
                            map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_VALVE);
                            map.put(Cmd.KEY_SUCCESS, 1);
                            meterstate = new byte[]{data[17], data[18]};
                        } else if (datatype == 0x8A) {
                            map.put(Cmd.KEY_DATA_TYPE, Const.DATAID_WRITE_VALVE);
                            map.put(Cmd.KEY_SUCCESS, 1);
                            meterstate = new byte[]{data[17], data[18]};
                        } else { //错误数据返回失败
                            map.put(Cmd.KEY_ERR_MESSAGE, "unknown data:" + strsss);
                            map.put(Cmd.KEY_SUCCESS, -1);
                            return map;
                        }

                        if ((meterstate[0] & 0x04) == 0x04) {
                            map.put(Cmd.KEY_VALVE_STATE, MeterStateConst.STATE_VALVE.CLOSE);
                        } else {
                            map.put(Cmd.KEY_VALVE_STATE, MeterStateConst.STATE_VALVE.OPEN);
                        }

                        if ((meterstate[0] & 0x20) == 0x20) {
                            map.put(Cmd.KEY_VALVE_STATE, MeterStateConst.STATE_VALVE.ERROR);
                        } else {

                        }
                    }
                }
            }
        }

        return map;
    }
}
