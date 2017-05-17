package com.rthc.wdj.jartest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rthc.wdj.bluetoothtoollib.MeterHandler;
import com.rthc.wdj.bluetoothtoollib.SwitchBox;
import com.rthc.wdj.bluetoothtoollib.ValveHandler;
import com.rthc.wdj.bluetoothtoollib.cmd.BleCmd;
import com.rthc.wdj.bluetoothtoollib.cmd.Cmd;
import com.rthc.wdj.bluetoothtoollib.cmd.Const;
import com.rthc.wdj.bluetoothtoollib.cmd.MeterStateConst;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity {
    EditText meterIdEditText;

    EditText scanNameEditText;
    Button scanBtn;
    Button stopBtn;

    Button readBtn;
    Button openBtn;
    Button closeBtn;
    Button writeMeterNetIdBtn;
    Button writeMeterIdBtn;
    Button readMeterStateBtn;
    Button writeMeterStateBtn;
    Button writeMeterValueBtn;

    Spinner moduleIdSpinner;

    Spinner breathSpinner;
    Button setBreathBtn;

    Context context;

    SwitchBox switchBox;

    Button testBtn;
    TextView testTextView;


    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testTextView = (TextView) findViewById(R.id.testTextView);
        testBtn = (Button) findViewById(R.id.testBtn);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
            }
        });

        scanNameEditText = (EditText) findViewById(R.id.scanNameEditText);
        scanBtn = (Button) findViewById(R.id.scanBtn);

        meterIdEditText = (EditText) findViewById(R.id.meterIdEditText);
        readBtn = (Button) findViewById(R.id.readBtn);
        openBtn = (Button) findViewById(R.id.openBtn);
        closeBtn = (Button) findViewById(R.id.closeBtn);
        writeMeterNetIdBtn = (Button) findViewById(R.id.writeMeterNetIdBtn);
        writeMeterIdBtn = (Button) findViewById(R.id.writeMeterIdBtn);
        readMeterStateBtn = (Button) findViewById(R.id.readMeterStateBtn);
        writeMeterStateBtn = (Button) findViewById(R.id.writeMeterStateBtn);
        writeMeterValueBtn = (Button) findViewById(R.id.writeMeterValueBtn);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        setBreathBtn = (Button) findViewById(R.id.setCenterBreathBtn);

        moduleIdSpinner = (Spinner) findViewById(R.id.moduleIdSpinner);



        ArrayList<String> arrayList = new ArrayList<String>();

        String[] strings = getResources().getStringArray(R.array.moduleArray);


        for (String str : strings) {
            arrayList.add(str);
        }


        moduleIdSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                arrayList));
        moduleIdSpinner.setSelection(1);


        breathSpinner = (Spinner) findViewById(R.id.breathSpinner);
        ArrayList<String> breathArrayList = new ArrayList<String>();
        breathArrayList.add("2s");
        breathArrayList.add("4s");
        breathArrayList.add("6s");
        breathArrayList.add("8s");
        breathArrayList.add("10s");
        breathSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                breathArrayList ));
        breathSpinner.setSelection(1);

        context = this;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            switchBox = new SwitchBox(context);
        }else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                //未授权 请求用户授权
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }else{
                //已授权
                switchBox = new SwitchBox(context);
            }
        }

        initListener();
    }




    @Override
    protected void onDestroy() {
        if (switchBox != null) {
            switchBox.close();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    switchBox = new SwitchBox(context);

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private void initListener() {

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //关闭连接
                        switchBox.close();

                        //连接设备  设备的名字 超时时间
                        final boolean success = switchBox.scanDevice(scanNameEditText.getText().toString(), 15000);
                        switchBox.setPackageItemsIntervalTime(300);
                        //设置使用中继 中继号为1
                        //switchBox.setRelayInfo(true,1);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (success) {
                                    Log.i("blebox_data","已连接设备，可以进行操作");
                                    Toast.makeText(context, "已连接设备，可以进行操作", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.i("blebox_data","连接失败");
                                    Toast.makeText(context, "连接失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }).start();

            }
        });


        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readValue();
            }
        });

        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openValve();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeValve();
            }
        });


        writeMeterNetIdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.writeMeterNetId(getMeterId(), 0xAA, getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return 0;
                    }

                    @Override
                    public void timeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        writeMeterIdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.writeMeterId(getMeterId(), getMeterId(), getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return 0;
                    }

                    @Override
                    public void timeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        readMeterStateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.readMeterState(getMeterId(), getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {
                        Log.i("JarTest", map.size() + "");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return 0;
                    }

                    @Override
                    public void timeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        writeMeterStateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.writeMeterState(getMeterId(), getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        return 0;
                    }

                    @Override
                    public void timeOut() {

                    }
                });
            }
        });

        writeMeterValueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.writeMeterValue(getMeterId(), (float)123.6, getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return 0;
                    }

                    @Override
                    public void timeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        findViewById(R.id.broadcastWriteMeterIdBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.writeMeterIdByBroadcast(getMeterId(), getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return 0;
                    }

                    @Override
                    public void timeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        findViewById(R.id.resetMeterBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchBox.resetMeter(getMeterId(), getModuleId(), new MeterHandler() {
                    @Override
                    public int callback(float result, final HashMap map) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(map.containsKey(Cmd.KEY_SUCCESS) ) {

                                    if((int)map.get(Cmd.KEY_SUCCESS) == 1) {
                                        Toast.makeText(context, "成功" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(context, "失败" + map.get(Cmd.KEY_DATA_BYTES_STR), Toast.LENGTH_SHORT).show();
                                    }
                                }else {
                                    Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        return 0;
                    }

                    @Override
                    public void timeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        setBreathBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean b1 = switchBox.setBoxCenterBreath(breathSpinner.getSelectedItemPosition(), Const.RfModuleType.JIEXUN);
                boolean b2 = switchBox.setBoxCenterBreath(breathSpinner.getSelectedItemPosition(), Const.RfModuleType.SKY_SHOOT);

                boolean b3 = switchBox.setBoxCenterParam(0xAA, breathSpinner.getSelectedItemPosition(), Const.RfModuleType.SKY_SHOOT);

                if(b1 && b2){
                    Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "设置失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * 抄表
     */
    void readValue() {
        final String meterId = getMeterId();

        if (meterId == null) return;

        switchBox.readMeter(meterId, getModuleId(), new MeterHandler() {

            @Override
            public int callback(final float result, final HashMap map) {
                synchronized (object) {
                    object.notify();
                }
                Log.i("wdj", "得到结果" + result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (result < 0) {
                            Toast.makeText(context, "失败：" + map.get(Cmd.KEY_ERR_MESSAGE), Toast.LENGTH_SHORT).show();
                            failtimes++;
                        } else {
                            successtimes++;
                            String valveStateStr = "";
                            if (map.get(Cmd.KEY_VALVE_STATE) == MeterStateConst.STATE_VALVE.OPEN) {
                                valveStateStr = "开";
                            } else if (map.get(Cmd.KEY_VALVE_STATE) == MeterStateConst.STATE_VALVE.CLOSE) {
                                valveStateStr = "关";
                            } else {
                                valveStateStr = "异常";
                            }

                            String power36Str;
                            if (map.get(Cmd.KEY_BATTERY_3_6_STATE) == MeterStateConst.STATE_POWER_3_6_V.LOW) {
                                power36Str = "低";
                            } else {
                                power36Str = "正常";
                            }

                            String power6Str;
                            if (map.get(Cmd.KEY_BATTERY_6_STATE) == MeterStateConst.STATE_POWER_6_V.LOW) {
                                power6Str = "低";
                            } else {
                                power6Str = "正常";
                            }

                            String str = "成功，结果：" + result
                                    + "\n阀门状态:" + valveStateStr
                                    + "\n3.6V电压:" + power36Str
                                    + "\n6V电压:" + power6Str;
                            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                return 0;
            }

            @Override
            public void timeOut() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timeouttimes++;
                        synchronized (object) {
                            object.notify();
                        }
                        Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 开阀
     */
    void openValve() {
        final String meterId = getMeterId();

        if (meterId == null) return;
        switchBox.openValve(meterId, getModuleId(), new ValveHandler() {
            @Override
            public int callback(final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            Toast.makeText(context, "成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return 0;
            }

            @Override
            public void timeOut() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    /**
     * 关阀
     */
    void closeValve() {
        final String meterId = getMeterId();

        if (meterId == null) return;

        switchBox.closeValve(meterId, getModuleId(), new ValveHandler() {
            @Override
            public int callback(final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            Toast.makeText(context, "成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return 0;
            }

            @Override
            public void timeOut() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "超时", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 获取模块ID
     *
     * @return
     */
    int getModuleId() {
        int bleModuleType = BleCmd.CTR_MODULE_ID_JIEXUN;

        int index = moduleIdSpinner.getSelectedItemPosition();
        switch (index) {
            case 0:
                bleModuleType = BleCmd.CTR_MODULE_ID_JIEXUN;
                break;
            case 1:
                bleModuleType = BleCmd.CTR_MODULE_ID_SKYSHOOT;
                break;
            case 2:
                bleModuleType = BleCmd.CTR_MODULE_ID_LIERDA;
                break;
        }

        return bleModuleType;
    }

    /**
     * 获取表ID
     *
     * @return
     */
    String getMeterId() {
        String meterId = meterIdEditText.getText().toString();
        if (meterId.length() != 14 && meterId.length() != 8) {
            Toast.makeText(context, "表号错误", Toast.LENGTH_SHORT).show();
            return null;
        }

        return meterId;
    }




    int timeouttimes = 0;
    int successtimes = 0;
    int failtimes = 0;
    private final Object object = new Object();
    public void test(){
        timeouttimes = 0;
        successtimes = 0;
        failtimes = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0;i<1000;i++){

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            testTextView.setText("S:" + successtimes + "---F:" + failtimes + "---T:" + timeouttimes);
                        }
                    });

                    synchronized (object){
                        readValue();
                        try {
                            object.wait(20000);
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }).start();

    }
}
