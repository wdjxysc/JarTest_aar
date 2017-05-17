package com.rthc.wdj.bluetoothtoollib;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.rthc.wdj.bluetoothtoollib.cmd.BleCmd;
import com.rthc.wdj.bluetoothtoollib.cmd.CmdUtil;
import com.rthc.wdj.bluetoothtoollib.packet.DataItem;
import com.rthc.wdj.bluetoothtoollib.packet.DataPacket;
import com.rthc.wdj.bluetoothtoollib.util.Tools;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Administrator on 2015/12/4.
 */
public class BleManager {
    Context context;

    private final String TAG = "BLE_BOX";

    BluetoothAdapter bluetoothAdapter;

    BluetoothLeScanner mBluetoothLeScanner;

    boolean scanning;

    /**
     * 是否正在发现服务
     */
    boolean isDiscoveringServices = false;


    public String BLE_NAME = "Skyshoot_3";

    BluetoothGatt mBluetoothGatt;

    public final UUID UUID_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public final UUID UUID_CHARACTERISTIC_1 = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    public final UUID UUID_CHARACTERISTIC_2 = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    public final UUID UUID_CHARACTERISTIC_3 = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    public final UUID UUID_CHARACTERISTIC_4 = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");

    public final UUID UUID_READ_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    BluetoothGattCharacteristic mNotifyCharacteristic = null;
    BluetoothGattCharacteristic mWriteCharacteristic = null;


    private DataPacket meterPacket = null;

    public static final int LENGTH_MAX = 20;
    public static int SLEEP_TIME = 150;

    /**
     * 设置每分包数据发送间隔时间
     *
     * @param time
     */
    public void setPackageItemsIntervalTime(int time) {
        SLEEP_TIME = time;
    }


    final Object mDataLock = new Object();


    final Object mScanLock = new Object();

    /**
     * 发送的数据
     */
    byte[] sendData;

    /**
     * 返回的数据
     */
    byte[] resultData;


    /**
     * 是否连接并且监听成功
     */
    boolean isContact;

    int bleConnectState = BluetoothProfile.STATE_DISCONNECTED;

    ConnectStatusHandler myConnectStatusHandler;

    public BleManager(final Context context, ConnectStatusHandler connectStatusHandler) throws Exception {
        this.context = context;
        this.myConnectStatusHandler = connectStatusHandler;
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("该设备不支持BLE");
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            boolean b = bluetoothAdapter.enable();

            if (!b) throw new Exception("蓝牙初始化失败");

            Log.i(TAG, b + "");
        }
        Log.i(TAG, "已开启蓝牙");
    }

    public void scanLeDevice(final boolean enable) {
        isDiscoveringServices = false;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "scanLeDevice: 蓝牙未开启");
            return;
        }

        if (enable) {
            scanning = true;

            isContact = false;

            bluetoothAdapter.startLeScan(mLeScanCallback);
            Log.i(TAG, "开始扫描" + BLE_NAME);
        } else {
            scanning = false;

            Log.i(TAG, "停止扫描" + BLE_NAME);
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Log.i(TAG, " " + device.getName() + "----" + device.getAddress() + "----" + rssi);
            if (device.getName() != null) {
                if (device.getName().equals(BLE_NAME) && scanning) {
                    //扫描到后停止
                    scanLeDevice(false);

                    mBluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback);
                    Log.i(TAG, "onLeScan: 获得gatt----" + mBluetoothGatt);
                }
            }
        }
    };


    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            myConnectStatusHandler.connectStatusChanged(status, newState);
            Log.i(TAG, "onConnectionStateChange" + "     status-" + status + "    newState-" + newState);

            if (newState == bleConnectState) {
                return;
            } else {
                bleConnectState = newState;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, "Connected to GATT server." + gatt.getDevice().getName());
                if (!isDiscoveringServices) {


                    isDiscoveringServices = mBluetoothGatt.discoverServices();

                    Log.i(TAG, "Attempting to start service discovery:" + isDiscoveringServices);
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isDiscoveringServices = false;
                //断开连接后开始扫描
//                scanLeDevice(true);
                Log.i(TAG, "Disconnected from GATT server." + gatt.getDevice().getName());
                isContact = false;




            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Connecting to GATT server." + gatt.getDevice().getName());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.i(TAG, "Disconnecting to GATT server." + gatt.getDevice().getName());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            final byte[] revData = characteristic.getValue();

            Log.i(TAG, "onCharacteristicChanged----" + Tools.Bytes2HexString(revData, revData.length));

            analysisData(revData);

            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicRead:" + status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicWrite---" + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG, "onDescriptorWrite:" + status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.i(TAG, "onReliableWriteCompleted:" + status);
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered: Service Number-----" + gatt.getServices().size());

            for (int i = 0; i < gatt.getServices().size(); i++) {
                Log.i(TAG, "Services:" + gatt.getServices().get(i).getUuid());
            }

            BluetoothGattService service = gatt.getService(UUID_SERVICE);
            if (service != null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mWriteCharacteristic = service.getCharacteristic(UUID_CHARACTERISTIC_3);
                mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                mNotifyCharacteristic = service.getCharacteristic(UUID_CHARACTERISTIC_4);
                if (mNotifyCharacteristic != null) {
                    //let remote call the onCharacteristicChanged.
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BluetoothGattDescriptor descriptor = mNotifyCharacteristic.getDescriptor(UUID_READ_DESCRIPTOR);
                    if (descriptor != null) {
                        mBluetoothGatt.setCharacteristicNotification(mNotifyCharacteristic, true);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                        boolean isWriteSuccess = mBluetoothGatt.writeDescriptor(descriptor);

                        if (!isWriteSuccess) {
                            mNotifyCharacteristic = null;
                        }
                    }

                    if (mNotifyCharacteristic != null && mWriteCharacteristic != null) {
                        Log.i(TAG, "Init characteristic Success!");

                        //成功连接并打开读写端口
                        contact();
                    }
                }
            }
            super.onServicesDiscovered(gatt, status);
        }
    };


    /**
     * 以同步方式收发数据
     *
     * @param sendData
     * @param timeout
     * @return
     */
    synchronized byte[] writeAndRev(byte[] sendData, int timeout) {

        byte[] revData = null;

        this.sendData = CmdUtil.cutData(sendData);

        if (isContact) {

            Log.i(TAG, "send----" + Tools.Bytes2HexString(sendData, sendData.length));

            try {
                synchronized (mDataLock) {
                    resultData = null;

                    for (int i = 0; i < sendData.length; /*i += lengthMax*/) {
                        int k = (sendData.length - i);
                        k = ((k > LENGTH_MAX) ? LENGTH_MAX : k);

                        byte[] item = Arrays.copyOfRange(sendData, i, i + k);
                        mWriteCharacteristic.setValue(item);
                        if (mWriteCharacteristic != null && mBluetoothGatt != null) {
                            boolean isWrite = false;

                            for (int j = 0; j < 5; j++) {
                                isWrite = mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
                                Thread.sleep(100);
                                Log.i(TAG, "data write isWrite:" + isWrite + "----" + Tools.Bytes2HexString(item, item.length));
                                if (isWrite) break;
                            }

                            if (!isWrite) {
                                return null;
                            }
                        } else {
                            return null;
                        }

                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        i += k;
                    }


                    try {
                        mDataLock.wait(timeout);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    revData = resultData;
                }
            } catch (Exception ex) {
                Log.i(TAG, ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            Log.i(TAG, "蓝牙已断开！");
        }

        return revData;
    }


    /**
     * 关闭会话
     */
    public void close() {

        if (bluetoothAdapter == null) {
            return;

        }

        if (mBluetoothGatt == null) {
            Log.e(TAG, "bluetoothGatt is null");
            return;
        }
        bleConnectState = BluetoothProfile.STATE_DISCONNECTED;
        mBluetoothGatt.disconnect();

        if (mNotifyCharacteristic != null) mNotifyCharacteristic = null;
        if (mWriteCharacteristic != null) mWriteCharacteristic = null;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "关闭连接----" + mBluetoothGatt.getDevice().getName() + "-----" + mBluetoothGatt.getDevice().getAddress());
        mBluetoothGatt.close();

        mBluetoothGatt = null;
        isContact = false;
    }


    /**
     * 组装数据 封包返回
     *
     * @param revData
     */
    void analysisData(byte[] revData) {
        DataItem frame = new DataItem(revData, revData.length);
        //帧头
        int headIndex = BleCmd.isFirstFrame(revData);
        if (headIndex != -1) {
            Log.i(TAG, "frame start");
            int packetLen = (revData[headIndex + 1] & 0xff) + headIndex;
            meterPacket = new DataPacket(packetLen);
        }

        if (meterPacket != null) {
            meterPacket.add(frame);
            if (meterPacket.isGetAllData()) {
                Log.i(TAG, "收到完整的数据包");
                synchronized (mDataLock) {
                    resultData = meterPacket.getAllData();
                    //模拟2id表返回抄表数据
                    //resultData = Tools.HexString2Bytes("AA30420000AA010268300201000616052381161F9001177200000000000000000000120101201635002E1681D655");
                    //模拟2id表返回写ID数据
                    //resultData = Tools.HexString2Bytes("AA1B420000AA0102683002010006160523950318A004331695ED55");
                    if (CmdUtil.isResponseData(sendData, resultData)) {
                        mDataLock.notify();
                    } else {
                        resultData = null;//舍弃当前获取数据
                        Log.d(TAG, "analysisData: 不是目标表返回的数据, 舍弃");
                    }
                }
            }
        }
    }


    /**
     * 同步方式扫描指定名字的设备 返回是否连接成功
     *
     * @param deviceName
     * @param scanTimeout
     * @return
     */
    boolean scan(String deviceName, int scanTimeout) {
        boolean success;
        synchronized (mScanLock) {
            BLE_NAME = deviceName;

            scanLeDevice(true);
            try {
                mScanLock.wait(scanTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            success = isContact;
            if (!success) {
                scanLeDevice(false);
                close();
            }
        }

        Log.i(TAG, "BleManager scan 返回" + success);
        return success;
    }

    /**
     * 成功连接
     */
    void contact() {
        synchronized (mScanLock) {
            isContact = true;
            mScanLock.notify();
        }
    }


    public interface ConnectStatusHandler{
        void connectStatusChanged(int oldStatus, int newStatus);
    }
}
