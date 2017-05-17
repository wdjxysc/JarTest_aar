package com.rthc.wdj.bluetoothtoollib.cmd;

import android.util.Log;

import com.rthc.wdj.bluetoothtoollib.util.CRC16;
import com.rthc.wdj.bluetoothtoollib.util.FileTool;
import com.rthc.wdj.bluetoothtoollib.util.StringTool;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Administrator on 2015/12/7.
 */
public class BoxUpdate {

    public int frameLength = 128;

    public String fileNamePart = "switchbox";

    public String fileName;

    public String softwareVersion = "0001";
    public String hardwareVersion = "0001";
    public long fileSize = 0;

    public String fileDirectory = "/switchbox/update/";

//    public String fileDirectory = "/hehe/update/";

    public BoxUpdate() throws Exception{

        String wholePath = FileTool.getSDPath() + fileDirectory;
//        String wholePath = FileTool.getExtSDCardPath().get(0) + fileDirectory;

        FileTool.createDir(wholePath);

        File[] files = FileTool.getFiles(wholePath);

        if(files.length == 0){
            throw new Exception("升级文件不存在");
        }

        if (files.length != 0) {
            fileName = files[0].getAbsolutePath();
        }

        getInfo();
    }


    private void getInfo() {
//        String str = "/storage/sdcard0/switchbox_1000_1000_cryp.bin";

        String binFileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());

        String postfix = binFileName.split("\\.")[1];
        String name = binFileName.split("\\.")[0];

        try {
            hardwareVersion = name.split("_")[1];
            softwareVersion = name.split("_")[2];
        } catch (Exception ex) {
            Log.i("wdj", ex.getMessage());
        }


        fileSize = FileTool.getSize(fileName);

    }

    /**
     * 获取升级bin文件 转换为命令组
     *
     * @return 命令组
     */
    public ArrayList<byte[]> GetUpdateDataListFromFile() {
        ArrayList<byte[]> datalist = new ArrayList<byte[]>();

        try {
            FileInputStream fin = new FileInputStream(fileName);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);

            int offset = 0;
            while (offset < buffer.length) {
                byte[] data = new byte[]{};
                if (buffer.length - offset >= frameLength) {
                    data = new byte[frameLength];
                    System.arraycopy(buffer, offset, data, 0, frameLength);
                } else {
                    data = new byte[buffer.length - offset];
                    System.arraycopy(buffer, offset, data, 0, buffer.length - offset);
                }
                offset += frameLength;
                datalist.add(data);
            }

            fin.close();
        } catch (IOException ex) {
            Log.i("ex", ex.getMessage());
        }

        return datalist;
    }


    public ArrayList<byte[]> getUpdateDataList() {
        ArrayList<byte[]> updateDataList = new ArrayList<>();

        //约定帧
        String str = String.format("%02X", BLE_HEAD)
                + "0000"
                + String.format("%02X", DATA_ID_SWITCH_BOX_UPDATE_CONVENT)
                + "08"
                + StringTool.reversalHexString(hardwareVersion)
                + StringTool.reversalHexString(softwareVersion)
                + StringTool.reversalHexString(String.format("%08X", fileSize))
                + "0000"
                + String.format("%02X", BLE_TAIL);
        int crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(str.substring(0, str.length() - 6)));

        str = str.substring(0, str.length() - 6)
                + StringTool.reversalHexString(String.format("%04X", crc16))
                + str.substring(str.length() - 2, str.length());

        byte[] cmd = Tools.HexString2Bytes(str);

        updateDataList.add(cmd);

        //数据帧
        long sum32 = 0;
//        Log.i("wdj","GetUpdateDataListFromFile()" + "    begin---" + new Date());
        ArrayList<byte[]> dataList = GetUpdateDataListFromFile();
//        Log.i("wdj","GetUpdateDataListFromFile()" + "    end---" + new Date());
        if (dataList.size() == 0) {
            return null;
        }
//        Log.i("wdj","开始获取数据帧 for()  "+ dataList.size() + "    begin---" + new Date());
        for (int i = 0; i < dataList.size(); i++) {
            str = String.format("%02X", BLE_HEAD)
                    + StringTool.reversalHexString(String.format("%04X", i + 1))
                    + String.format("%02X", DATA_ID_SWITCH_BOX_UPDATE_DATA)
                    + String.format("%02X", dataList.get(i).length)
                    + Tools.Bytes2HexString(dataList.get(i), dataList.get(i).length)
                    + "0000"
                    + String.format("%02X", BLE_TAIL);

            crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(str.substring(0, str.length() - 6)));

            str = str.substring(0, str.length() - 6)
                    + StringTool.reversalHexString(String.format("%04X", crc16))
                    + str.substring(str.length() - 2, str.length());


            cmd = Tools.HexString2Bytes(str);

            updateDataList.add(cmd);
            sum32 += sum(dataList.get(i));
//            Log.i("wdj","" + i);
        }

//        Log.i("wdj","for()  "+ dataList.size() + "    end---" + new Date());

        //校验帧
        str = String.format("%02X", BLE_HEAD)
                + StringTool.reversalHexString(String.format("%04X", dataList.size() + 1))
                + String.format("%02X", DATA_ID_SWITCH_BOX_UPDATE_CHECK)
                + "04"
                + StringTool.reversalHexString(String.format("%08X", sum32))
                + "0000"
                + String.format("%02X", BLE_TAIL);

        crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(str.substring(0, str.length() - 6)));

        str = str.substring(0, str.length() - 6)
                + StringTool.reversalHexString(String.format("%04X", crc16))
                + str.substring(str.length() - 2, str.length());

        cmd = Tools.HexString2Bytes(str);


        updateDataList.add(cmd);


        //复位帧
        str = String.format("%02X", BLE_HEAD)
                + StringTool.reversalHexString(String.format("%04X", dataList.size() + 2))
                + String.format("%02X", DATA_ID_SWITCH_BOX_UPDATE_RESET)
                + "00"
                + "0000"
                + String.format("%02X", BLE_TAIL);

        crc16 = CRC16.calcCrc16(Tools.HexString2Bytes(str.substring(0, str.length() - 6)));

        str = str.substring(0, str.length() - 6)
                + StringTool.reversalHexString(String.format("%04X", crc16))
                + str.substring(str.length() - 2, str.length());

//        Log.i("wdj", str);

        cmd = Tools.HexString2Bytes(str);

        updateDataList.add(cmd);

        return updateDataList;
    }


    private long sum(byte[] data) {
        long result = 0;

        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                result += (data[i] & 0xff);
            }
        }

        return result;
    }


    public HashMap<String, Object> parseUpdateData(byte[] revData) {
        HashMap<String, Object> hashMap = new HashMap<>();

        if (revData.length < 5) return hashMap;

        int dataType = revData[3] & 0xff;
        int resp = revData[5] & 0xff;
        hashMap.put(Cmd.KEY_DATA_TYPE, dataType);
        switch (dataType) {
            case DATA_ID_SWITCH_BOX_UPDATE_CONVENT:
                break;
            case DATA_ID_SWITCH_BOX_UPDATE_DATA:
                break;
            case DATA_ID_SWITCH_BOX_UPDATE_ADDRESS:
                break;
            case DATA_ID_SWITCH_BOX_UPDATE_OVER:
                break;
            case DATA_ID_SWITCH_BOX_UPDATE_CHECK:
                break;
        }

        return hashMap;
    }


    public static final int BLE_HEAD = 0x68;
    public static final int BLE_TAIL = 0x16;


    public static final int DATA_ID_SWITCH_BOX_UPDATE_CONVENT = 0x81;

    public static final int DATA_ID_SWITCH_BOX_UPDATE_DATA = 0x82;

    public static final int DATA_ID_SWITCH_BOX_UPDATE_ADDRESS = 0x83;

    public static final int DATA_ID_SWITCH_BOX_UPDATE_OVER = 0x84;

    public static final int DATA_ID_SWITCH_BOX_UPDATE_CHECK = 0x85;

    public static final int DATA_ID_SWITCH_BOX_UPDATE_RESET = 0x86;

}
