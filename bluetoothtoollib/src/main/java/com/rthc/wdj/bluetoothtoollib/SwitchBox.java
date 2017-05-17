package com.rthc.wdj.bluetoothtoollib;

import android.content.Context;
import android.util.Log;

import com.rthc.wdj.bluetoothtoollib.cmd.BleCmd;
import com.rthc.wdj.bluetoothtoollib.cmd.Cmd;
import com.rthc.wdj.bluetoothtoollib.cmd.Const;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Administrator on 2015/12/4.
 */
public class SwitchBox implements MeterController {

    BleManager bleManager;

    MeterHandler meterHandler;
    ValveHandler valveHandler;
    UpdateHandler updateHandler;


    BleCmd bleCmd;

    Context context;

    HashMap<String, Object> resultMap;

    Thread thread;


    enum RESULT {
        FAIL_BLE_DIS_CONNECT,
        FAIL_THREAD_RUNNING,
        SUCCESS
    }


    /**
     * 升级命令队列
     */
    ArrayList<byte[]> arrayList;

    /**
     * 每次通讯等待表回应超时时间
     */
    int timeout = 20000;


    /**
     * 断开连接时是否自动连接
     */
    boolean autoConnect = false;
    String deviceNameTemp = "";
    int scanTimeoutTemp = 0;

    public SwitchBox(final Context context) {
        this.context = context;

        try {
            bleManager = new BleManager(context, new BleManager.ConnectStatusHandler() {
                @Override
                public void connectStatusChanged(int oldStatus, int newStatus) {
                    Log.i("ble_box", "connectStatusChanged: 连接状态发生变化！！！！！" + oldStatus + "----" + newStatus);
                    //0 未连接  1 正在连接中  2 已连接
                    if (newStatus == 0) {
                        if (autoConnect) {
                            boolean success = false;
                            while (!success) {
                                for (int i = 0; i < 5; i++) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    bleManager.close();
                                }

                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                success = bleManager.scan(deviceNameTemp, scanTimeoutTemp);
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e("wdj", e.getMessage());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {


                bleCmd = new BleCmd();

                arrayList = bleCmd.getUpdateCmd();

            }
        }).start();
    }

    /**
     * 扫描设备
     *
     * @param deviceName  设备名字
     * @param scanTimeout 超时时间
     * @return 返回是否扫描到设备并且准备就绪
     */
    public boolean scanDevice(String deviceName, int scanTimeout) {
        deviceNameTemp = deviceName;
        scanTimeoutTemp = scanTimeout;
        autoConnect = true;
        return bleManager.scan(deviceName, scanTimeout);
    }

    /**
     * 关闭蓝牙
     */
    public void close() {
        autoConnect = false;
        bleManager.close();
    }

    /**
     * 设置每分包数据发送间隔时间
     *
     * @param time
     */
    public void setPackageItemsIntervalTime(int time) {
        bleManager.setPackageItemsIntervalTime(time);
    }


    /**
     * 是否使用中继
     */
    private boolean useRelay = false;

    /**
     * 中继ID
     */
    private int relayId = 0;

    /**
     * 设置使用中继信息
     *
     * @param isUseRelay 是否使用中继
     * @param relayId    中继号
     */
    public void setRelayInfo(boolean isUseRelay, int relayId) {
        this.useRelay = isUseRelay;
        this.relayId = relayId;
    }


    /**
     * 抄表
     *
     * @param meterId     表ID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void readMeter(String meterId, int bleModuleId, final MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在抄表...");

        byte[] sendData = bleCmd.getReadDataCmd(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 抄表 2ID
     *
     * @param meterId     表ID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void readMeter2(String meterId, int bleModuleId, final MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在抄表...");

        byte[] sendData = bleCmd.getReadDataCmd2(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        final HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        meterHandler.callback(-1, map);
                    }
                }).start();

                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        meterHandler.callback(-1, map);
                    }
                }).start();
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 开阀
     *
     * @param meterId     表ID
     * @param bleModuleId
     * @param handler     回调
     */
    @Override
    public void openValve(String meterId, int bleModuleId, ValveHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.valveHandler = handler;
        Log.i(Const.TAG_DATA, "正在开阀...");
        byte[] sendData = bleCmd.getOpenValveCmd(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_WRITE_VALVE);

        switch (result) {
            case FAIL_THREAD_RUNNING:
                valveHandler.callback(false);
                break;
            case FAIL_BLE_DIS_CONNECT:
                valveHandler.callback(false);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 开阀 2ID
     *
     * @param meterId     表ID
     * @param bleModuleId
     * @param handler     回调
     */
    @Override
    public void openValve2(String meterId, int bleModuleId, ValveHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.valveHandler = handler;
        Log.i(Const.TAG_DATA, "正在开阀...");
        byte[] sendData = bleCmd.getOpenValveCmd2(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_WRITE_VALVE);

        switch (result) {
            case FAIL_THREAD_RUNNING:
                valveHandler.callback(false);
                break;
            case FAIL_BLE_DIS_CONNECT:
                valveHandler.callback(false);
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 关阀
     *
     * @param meterId     表ID
     * @param bleModuleId
     * @param handler     回调
     */
    @Override
    public void closeValve(String meterId, int bleModuleId, ValveHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.valveHandler = handler;
        Log.i(Const.TAG_DATA, "正在关阀...");
        byte[] sendData = bleCmd.getCloseValveCmd(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_WRITE_VALVE);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }

    }

    /**
     * 关阀 2ID
     *
     * @param meterId     表ID
     * @param bleModuleId
     * @param handler     回调
     */
    @Override
    public void closeValve2(String meterId, int bleModuleId, ValveHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.valveHandler = handler;
        Log.i(Const.TAG_DATA, "正在关阀...");
        byte[] sendData = bleCmd.getCloseValveCmd2(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_WRITE_VALVE);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }

    }


    /**
     * 写表ID
     *
     * @param oldMeterId  原表ID
     * @param newMeterId  新表ID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterId(String oldMeterId, String newMeterId, int bleModuleId, MeterHandler handler) {
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表ID...");
        byte[] sendData = bleCmd.getWriteMeterIdCmd(oldMeterId, newMeterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 写表ID   2ID
     *
     * @param oldMeterId  原表ID
     * @param newMeterId  新表ID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterId2(String oldMeterId, String newMeterId, int bleModuleId, MeterHandler handler) {
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表ID...");
        byte[] sendData = bleCmd.getWriteMeterIdCmd2(oldMeterId, newMeterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 备用方法
     * 通过广播方式写表ID  原表号 AAAAAAAAAAAAAA  nodeid FFFFFFFF
     *
     * @param newMeterId  新表号
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    public void writeMeterIdByBroadcast(String newMeterId, int bleModuleId, MeterHandler handler) {
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在广播写表ID...");
        byte[] sendData = bleCmd.getWriteMeterIdByBroadcastCmd("AAAAAAAAAAAAAA", "FFFFFFFF", newMeterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 备用方法
     * 通过广播方式写表ID  原表号 AAAAAAAAAAAAAA  nodeid FFFF    2ID
     *
     * @param newMeterId  新表号
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    public void writeMeterIdByBroadcast2(String newMeterId, int bleModuleId, MeterHandler handler) {
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在广播写表ID...");
        byte[] sendData = bleCmd.getWriteMeterIdByBroadcastCmd2("AAAAAAAAAAAAAA", "FFFF", newMeterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 写底数
     *
     * @param meterId     表ID
     * @param meterValue  底数
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterValue(String meterId, float meterValue, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表底数...");
        byte[] sendData = bleCmd.getWriteMeterValueCmd(meterId, meterValue, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 写底数    2ID
     *
     * @param meterId     表ID
     * @param meterValue  底数
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterValue2(String meterId, float meterValue, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表底数...");
        byte[] sendData = bleCmd.getWriteMeterValueCmd2(meterId, meterValue, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 读表状态
     *
     * @param meterId     表ID
     * @param bleModuleId 模块选择
     * @param handler
     */
    @Override
    public void readMeterState(String meterId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在读表状态...");
        byte[] sendData = bleCmd.getReadMeterStateCmd(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 读表状态   2ID
     *
     * @param meterId     表ID
     * @param bleModuleId 模块选择
     * @param handler
     */
    @Override
    public void readMeterState2(String meterId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在读表状态...");
        byte[] sendData = bleCmd.getReadMeterStateCmd2(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * 写表状态
     *
     * @param meterId     表ID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterState(String meterId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表状态...");
        byte[] sendData = bleCmd.getWriteMeterStateCmd(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 写表状态   2ID
     *
     * @param meterId     表ID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterState2(String meterId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表状态...");
        byte[] sendData = bleCmd.getWriteMeterStateCmd2(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 写表NETID
     *
     * @param meterId     表ID
     * @param newNetId    新NETID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterNetId(String meterId, int newNetId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表NETID...");
        byte[] sendData = bleCmd.getWriteMeterNetIdCmd(meterId, newNetId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 写表NETID      2ID
     *
     * @param meterId     表ID
     * @param newNetId    新NETID
     * @param bleModuleId 模块选择
     * @param handler     回调
     */
    @Override
    public void writeMeterNetId2(String meterId, int newNetId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在写表NETID...");
        byte[] sendData = bleCmd.getWriteMeterNetIdCmd2(meterId, newNetId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 复位表 以解决写ID成功后不能生效的问题
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    @Override
    public void resetMeter(String meterId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在复位表...");
        byte[] sendData = bleCmd.getResetMeterCmd(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 复位表 以解决写ID成功后不能生效的问题     2ID
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    @Override
    public void resetMeter2(String meterId, int bleModuleId, MeterHandler handler) {
        setCenterBreathByMeterId(meterId);//设置盒子中心呼吸周期
        this.meterHandler = handler;
        Log.i(Const.TAG_DATA, "正在复位表...");
        byte[] sendData = bleCmd.getResetMeterCmd2(meterId, bleModuleId, useRelay, relayId);
        if (sendData == null) return;
        RESULT result = writeData(sendData, Const.DATAID_READ_DATA);

        HashMap<String, Object> map = new HashMap<>();
        switch (result) {
            case FAIL_THREAD_RUNNING:
                map.put(Cmd.KEY_ERR_MESSAGE, "正在通讯，取消当前请求");
                meterHandler.callback(-1, map);
                break;
            case FAIL_BLE_DIS_CONNECT:
                map.put(Cmd.KEY_ERR_MESSAGE, "蓝牙未连接");
                meterHandler.callback(-1, map);
                break;
            case SUCCESS:
                break;
        }
    }


    /**
     * 解析数据并执行回调返回结果
     *
     * @param revData
     */
    public void parseData(byte[] revData) {
        if (revData == null) return;
        String dataStr = Tools.Bytes2HexString(revData, revData.length);
        Log.i(Const.TAG_DATA, "rev----" + dataStr);

        //收到数据进行解析
        HashMap<String, Object> hashMap = bleCmd.parseBleData(revData);


        if (hashMap.containsKey(Cmd.KEY_ERR_MESSAGE)) { //利尔达模块回执数据不予反应
            if (hashMap.get(Cmd.KEY_ERR_MESSAGE).toString().equals("ACK")) {
                Log.i("wdj", hashMap.get(Cmd.KEY_ERR_MESSAGE).toString());
                return;
            }
        }

        switch (Integer.parseInt(hashMap.get(BleCmd.KEY_BLE_MODULE_ID).toString())) {
            case BleCmd.CTR_MODULE_ID_BOX:
                //对盒子的操作数据不在这里做处理 由单独的同步方法处理
                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {

                } else {

                }
                break;
            default:
                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) != 1) {
                    if (meterHandler != null) {
                        meterHandler.callback(-1, hashMap);
                    } else {
                        if (valveHandler != null) {
                            valveHandler.callback(false);
                        }
                    }
                } else {
                    switch (Integer.parseInt(hashMap.get(Cmd.KEY_DATA_TYPE).toString())) {
                        case Const.DATAID_READ_DATA:
                            if (meterHandler != null) {
                                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                                    int result = meterHandler.callback(Float.parseFloat(hashMap.get(Cmd.KEY_VALUE_NOW).toString()), hashMap);
                                }
                            }
                            break;
                        case Const.DATAID_WRITE_VALVE:
                            if (valveHandler != null) {
                                boolean b = false;
                                if ((Integer) hashMap.get(Cmd.KEY_SUCCESS) == 1) {
                                    b = true;
                                }
                                int result = valveHandler.callback(b);
                            }
                            break;
                        default:
                            if (meterHandler != null) {
                                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                                    int result = meterHandler.callback(0, hashMap);
                                }
                            }
                            break;
                    }
                }

                break;
        }
    }

    /**
     * 同步方式发送数据 用于操作盒子
     *
     * @param sendData
     * @param timeout
     * @return
     */
    private byte[] sendAndRev(byte[] sendData, int timeout) {
        byte[] revData;

        revData = bleManager.writeAndRev(sendData, timeout);

        return revData;
    }

    /**
     * 盒子固件升级
     *
     * @param updateHandler
     */
    public void updateSwitchBox(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;

        if (arrayList == null) return;
        for (int i = 0; i < arrayList.size(); i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            resultMap = null;

            byte[] revData;
            for (int j = 0; j < 6; j++) {
                Log.i("wdj", "write package:  " + i);
                revData = sendAndRev(arrayList.get(i), 2000);

                if (resultMap != null) {
                    if (Integer.parseInt(resultMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                        break;
                    }
                }

                if (j == 5) {
                    Log.i("wdj", "超时");
                    updateHandler.timeout();
                    return;
                }
            }
        }
    }

    /**
     * 设置盒子蓝牙名称
     *
     * @param name
     * @return
     */
    public boolean setBleName(String name) {
        boolean b = false;
        byte[] cmd = bleCmd.getSetBleNameCmd(name);
        if (cmd == null) return false;
        byte[] revData = sendAndRev(cmd, 2000);

        HashMap<String, Object> hashMap = bleCmd.parseBleData(revData);

        if (hashMap != null) {
            if (hashMap.containsKey(Cmd.KEY_SUCCESS)) {
                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                    b = true;
                }
            }
        }

        return b;
    }


    /**
     * 设置盒子中心模块NETID
     *
     * @param netId
     * @param rfModuleType
     * @return
     */
    public boolean setNetId(long netId, Const.RfModuleType rfModuleType) {
        if (netId > 255 || netId < 0) return false;
        boolean b = false;
        byte[] cmd = bleCmd.getSetRFNetIdCmd(netId, rfModuleType);
        if (cmd == null) return false;
        byte[] revData = sendAndRev(cmd, 2000);

        HashMap<String, Object> hashMap = bleCmd.parseBleData(revData);

        if (hashMap != null) {
            if (hashMap.containsKey(Cmd.KEY_SUCCESS)) {
                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                    b = true;
                }
            }
        }

        return b;
    }

    /**
     * 设置盒子中心模块呼吸周期
     *
     * @param breathCode
     * @param rfModuleType
     * @return
     */
    public boolean setBoxCenterBreath(int breathCode, Const.RfModuleType rfModuleType) {
        if (breathCode != 0 && breathCode != 1 && breathCode != 2) return false;
        boolean b = false;
        byte[] cmd = bleCmd.getSetRFBreathCmd(breathCode, rfModuleType);
        if (cmd == null) return false;
        byte[] revData = sendAndRev(cmd, 2000);

        HashMap<String, Object> hashMap = bleCmd.parseBleData(revData);

        if (hashMap != null) {
            if (hashMap.containsKey(Cmd.KEY_SUCCESS)) {
                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                    b = true;
                }
            }
        }

        return b;
    }


    /**
     * 设置盒子中心模块参数
     *
     * @param netId
     * @param breathCode
     * @param rfModuleType
     * @return
     */
    public boolean setBoxCenterParam(int netId, int breathCode, Const.RfModuleType rfModuleType) {
        if (breathCode != 0 && breathCode != 1 && breathCode != 2) return false;
        boolean b = false;
        byte[] cmd = bleCmd.getSetRFCmd(netId, breathCode, rfModuleType);
        if (cmd == null) return false;
        byte[] revData = sendAndRev(cmd, 2000);

        HashMap<String, Object> hashMap = bleCmd.parseBleData(revData);

        if (hashMap != null) {
            if (hashMap.containsKey(Cmd.KEY_SUCCESS)) {
                if (Integer.parseInt(hashMap.get(Cmd.KEY_SUCCESS).toString()) == 1) {
                    b = true;
                }
            }
        }

        return b;
    }

    /**
     * 异步方式发送数据
     *
     * @param data
     * @param cmdType
     * @return
     */
    RESULT writeData(final byte[] data, final int cmdType) {
        if (thread != null) {
            if (thread.getState() != Thread.State.TERMINATED) {
                Log.i("blebox_data", "正在通讯，取消当前请求");
                //Toast.makeText(context,"正在通讯，请求无效",Toast.LENGTH_SHORT).show();
                return RESULT.FAIL_THREAD_RUNNING;
            }
        }

        if (!bleManager.isContact) {
            Log.i("blebox_data", "蓝牙未连接");
//            Toast.makeText(context,"蓝牙未连接", Toast.LENGTH_SHORT).show();
            return RESULT.FAIL_BLE_DIS_CONNECT;
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] revData = bleManager.writeAndRev(data, timeout);
                if (revData == null) {
                    switch (cmdType) {
                        case Const.DATAID_READ_DATA:
                            meterHandler.timeOut();
                            break;
                        case Const.DATAID_WRITE_VALVE:
                            valveHandler.timeOut();
                            break;
                    }
                } else {
                    parseData(revData);
                }

            }
        });

        thread.start();

        return RESULT.SUCCESS;
    }


    //是否使用根据表ID来设置中心模块呼吸周期这种方案 目前设为false 待启用
    boolean isUseSetCenterParamByMeterId = false;
    int breathCodeNow = 0;

    /**
     * 根据表ID设置 中心模块呼吸周期 230515 230516开头设为6s  其他以后的设为4s
     *
     * @param meterId
     * @return
     */
    private boolean setCenterBreathByMeterId(String meterId) {
        boolean b = false;
        if (isUseSetCenterParamByMeterId) {
            String sub1 = meterId.substring(0, 4);
            if (sub1.equals("2305")) {
                String sub2 = meterId.substring(0, 6);
                int breathCode;
                if (sub2.equals("230515") || sub2.equals("230516")) {
                    breathCode = 2;
                } else {
                    breathCode = 1;
                }
                if (breathCode != breathCodeNow) {
                    boolean b1 = setBoxCenterParam(0xAA, breathCode, Const.RfModuleType.JIEXUN);
                    boolean b2 = setBoxCenterParam(0xAA, breathCode, Const.RfModuleType.SKY_SHOOT);

                    if (b1 && b2) {
                        breathCodeNow = breathCode;
                        b = true;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return b;
    }
}
