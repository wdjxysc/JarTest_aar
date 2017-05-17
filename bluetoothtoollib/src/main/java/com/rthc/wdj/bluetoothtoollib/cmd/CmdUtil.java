package com.rthc.wdj.bluetoothtoollib.cmd;

import android.util.Log;

import com.rthc.wdj.bluetoothtoollib.util.StringTool;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.Arrays;
import java.util.HashMap;

import static android.content.ContentValues.TAG;
import static com.rthc.wdj.bluetoothtoollib.cmd.BleCmd.isFirstFrame;

/**
 * Created by Administrator on 2016/11/18.
 *
 * @author wdjxysc
 */
public class CmdUtil {
    /**
     * 去掉数据头之前的同步头字节  0xFE  首个有效字节为0xAA
     * @param data
     * @return
     */
    public static byte[] cutData(byte[] data){

        //去掉同步头
        int headIndex = isFirstFrame(data);
        if (headIndex != -1) {
            data = Arrays.copyOfRange(data, headIndex, data.length);
        }

        return data;
    }



    /**
     * 去掉数据头之前的所有字节  首个有效字节为0x68
     * @param data
     * @return
     */
    public static byte[] cutData68(byte[] data) {

        int index = -1;
        //去掉同步头
        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xff) == 0x68 && (data[i + 1] & 0xff) == 0x30) {

                if ((data[i + 2] & 0xff) == 0x68 && (data[i + 3] & 0xff) == 0x30) {
                    if((data[i + 4] & 0xff) == 0x30 && (data[i + 5] & 0xff) == 0x68){
                        index = i + 2;
                        break;
                    }else {
                        index = i;
                        break;
                    }
                } else {
                    if (!((data[i + 2] & 0xff) == 0xfe && (data[i + 3] & 0xff) == 0xfe)) {
                        index = i;
                        break;
                    }
                }
            }
        }
        Log.i("BLE_BOX", "cutData68: index = " + index);
        if (index != -1) {
            byte[] result = new byte[data.length - index];
            System.arraycopy(data, index, result, 0, result.length);
            return result;
        } else {
            return data;
        }
    }


    /**
     * 判断这组数据是否是同一块表的操作
     * @param sendData
     * @param recvData
     * @return
     */
    public static boolean isResponseData(byte[] sendData, byte[] recvData){

        Log.i(TAG, "isResponseData: send" + Tools.Bytes2HexString(sendData,sendData.length));
        Log.i(TAG, "isResponseData: recv" + Tools.Bytes2HexString(recvData,recvData.length));

        BleCmd bleCmd = new BleCmd();

        HashMap<String,Object> mapRecv = bleCmd.parseBleData(recvData);

        if((int)mapRecv.get(Cmd.KEY_SUCCESS) != 1){
            //数据校验错误 无法判断是否是对应表返回数据 直接返回
            return true;
        }

        if((int)mapRecv.get(BleCmd.KEY_BLE_MODULE_ID) == BleCmd.CTR_MODULE_ID_BOX){
            return true;
        }else if((int)mapRecv.get(BleCmd.KEY_BLE_MODULE_ID) == BleCmd.CTR_MODULE_ID_LIERDA){
            byte[] sendCtrlBytesData = new byte[2];
            System.arraycopy(sendData, 7, sendCtrlBytesData, 0, sendCtrlBytesData.length);

            byte[] recvCtrlBytesData = new byte[2];
            System.arraycopy(recvData, 7, recvCtrlBytesData, 0, recvCtrlBytesData.length);

            String sendCtrl = Tools.Bytes2HexString(sendCtrlBytesData,sendCtrlBytesData.length);
            String recvCtrl = Tools.Bytes2HexString(recvCtrlBytesData,recvCtrlBytesData.length);
            if(sendCtrl.equals(recvCtrl)){

                byte[] sendMeterIdBytesData = new byte[4];
                System.arraycopy(sendData, 9, sendCtrlBytesData, 0, sendCtrlBytesData.length);

                byte[] recvMeterIdBytesData = new byte[4];
                System.arraycopy(recvData, 9, recvCtrlBytesData, 0, recvCtrlBytesData.length);
                String sendMeterId = Tools.Bytes2HexString(sendMeterIdBytesData,sendMeterIdBytesData.length);
                String recvMeterId = Tools.Bytes2HexString(recvMeterIdBytesData,recvMeterIdBytesData.length);

                return sendMeterId.equals(recvMeterId);
            }else {
                return false;
            }
        }else if(recvData.length > 20) {

            byte[] sendMeterBytesData = new byte[sendData.length - 8];
            System.arraycopy(sendData, 8, sendMeterBytesData, 0, sendMeterBytesData.length);

            byte[] recvMeterBytesData = new byte[recvData.length - 8];
            System.arraycopy(recvData, 8, recvMeterBytesData, 0, recvMeterBytesData.length);

            sendMeterBytesData = cutData68(sendMeterBytesData);
            recvMeterBytesData = cutData68(recvMeterBytesData);

//            String sendDataStr = Tools.Bytes2HexString(sendMeterBytesData,sendMeterBytesData.length);
//            String  recvDataStr = Tools.Bytes2HexString(recvMeterBytesData,recvMeterBytesData.length);
//            Log.i(TAG, "isResponseData: sendcutted----" + sendDataStr);
//            Log.i(TAG, "isResponseData: recvcutted----" + recvDataStr);

            byte[] meterIdDataSend = new byte[7];
            byte[] meterIdDataRecv = new byte[7];
            System.arraycopy(sendMeterBytesData, 2, meterIdDataSend, 0, meterIdDataSend.length);
            System.arraycopy(recvMeterBytesData, 2, meterIdDataRecv, 0, meterIdDataRecv.length);
            String sendMeterId = StringTool.reversalHexString(Tools.Bytes2HexString(meterIdDataSend, meterIdDataSend.length));
            String recvMeterId = StringTool.reversalHexString(Tools.Bytes2HexString(meterIdDataRecv, meterIdDataRecv.length));


            int senddatatype = ((sendMeterBytesData[12] & 0xff) << 8) + (sendMeterBytesData[11] & 0xff);
            int recvdatatype = ((recvMeterBytesData[12] & 0xff) << 8) + (recvMeterBytesData[11] & 0xff);

            int sendSer = sendMeterBytesData[13]&0xff;
            int recvSer = recvMeterBytesData[13]&0xff;
            //命令datatype一致 且 序列号ser一致
            if(senddatatype == recvdatatype && sendSer == recvSer) {

                //写ID返回的表号是新表号 所以收发表ID必然不同 就直接返回true
                if((int)mapRecv.get(Cmd.KEY_DATA_TYPE) == Const.DATAID_WRITE_ADDRESS){
                    byte[] sendNewMeterIdBytes = new byte[7];
                    System.arraycopy(sendMeterBytesData, 14, sendNewMeterIdBytes, 0, sendNewMeterIdBytes.length);
                    String sendNewMeterId = StringTool.reversalHexString(Tools.Bytes2HexString(sendNewMeterIdBytes, sendNewMeterIdBytes.length));
                    return sendNewMeterId.equals(recvMeterId);
                }

                /*如果是广播指令则直接返回true*/
                return sendMeterId.toUpperCase().equals("AAAAAAAAAAAAAA") || sendMeterId.equals(recvMeterId);
            }else {
                return false;
            }
        }


        return true;
    }
}
