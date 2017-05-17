package com.rthc.wdj.bluetoothtoollib.cmd;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Administrator on 2015/7/20 0020.
 */
public class Const {

    /**
     * rf模块类型
     */
    public enum RfModuleType{
        JIEXUN,
        SKY_SHOOT,
        LIERDA
    }

    /**
     * 时间格式
     */
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());


    /**
     * 命令头
     */
    public static final int HEAD       = 0x68;
    /**
     * 表类型
     */
    public static final int METERTYPE  = 0x30;
    /**
     * 命令尾
     */
    public static final int TAIL       = 0x16;

    //读数据
    //CTR_0：控制码  CTR_1 正常应答码  CTR_2 异常应答码
    /**
     * 读数据 CTR_0：控制码
     */
    public static final int CTR_0_READ_DATA = 0x01;
    /**
     * 读数据 CTR_1正常应答码
     */
    public static final int CTR_1_READ_DATA = 0x81;
    /**
     * 读数据 CTR_2异常应答码
     */
    public static final int CTR_2_READ_DATA = 0xC1;

    //读秘钥版本号
    //CTR_0：控制码  CTR_1正常应答码  CTR_2异常应答码
    public static final int CTR_0_READ_KEY_VERSION = 0x09;
    public static final int CTR_1_READ_KEY_VERSION = 0x89;
    public static final int CTR_2_READ_KEY_VERSION = 0xC9;

    //读地址
    //CTR_0：控制码  CTR_1正常应答码  CTR_2异常应答码
    public static final int CTR_0_READ_ADRRESS = 0x03;
    public static final int CTR_1_READ_ADRRESS = 0x83;
    public static final int CTR_2_READ_ADRRESS = 0xC3;

    //写数据
    //CTR_3：控制码  CTR_4正常应答码  CTR_5异常应答码
    public static final int CTR_3_WRITE_DATA = 0x04;
    public static final int CTR_4_WRITE_DATA = 0x84;
    public static final int CTR_5_WRITE_DATA = 0xC4;

    //写地址
    //CTR_3：控制码  CTR_4正常应答码  CTR_5异常应答码
    public static final int CTR_3_WRITE_ADRRESS = 0x15;
    public static final int CTR_4_WRITE_ADRRESS = 0x95;
    public static final int CTR_5_WRITE_ADRRESS = 0xD5;

    //写机电同步数据
    //CTR_3：控制码  CTR_4正常应答码  CTR_5异常应答码
    public static final int CTR_3_WRITE_SYS_DATA = 0x16;
    public static final int CTR_4_WRITE_SYS_DATA = 0x96;
    public static final int CTR_5_WRITE_SYS_DATA = 0xD6;

    //固件升级
    //CTR_3：控制码  CTR_4正常应答码
    public static final int CTR_3_UPDATE_FIRMWARE = 0x2A;
    public static final int CTR_4_UPDATE_FIRMWARE = 0xAA;

    //组网命令
    //CTR_3：控制码  CTR_4正常应答码
    public static final int CTR_3_SCAN_ALL_METER_ID = 0x22;
    public static final int CTR_4_SCAN_ALL_METER_ID = 0xA2;


    //数据标识
    public static final int DATAID_WRITE_VALVE   = 0xA017;//写阀门控制
    public static final int DATAID_WRITE_ADDRESS = 0xA018;//写表地址
    public static final int DATAID_WRITE_DATA    = 0xA016;//写表底数 写机电同步数据
    public static final int DATAID_WRITE_TIME    = 0xA015;//写标准时间
    public static final int DATAID_READ_TIME     = 0x8515;//读表时间
    public static final int DATAID_READ_DATA     = 0x901F;//实时抄表
    public static final int DATAID_WRITE_START_USE = 0xA019;//出厂启用
    public static final int DATAID_WRITE_METER_STATE     = 0xFF80;//写表状态
    public static final int DATAID_READ_METER_STATE      = 0xFF00;//读表状态
    public static final int DATAID_WRITE_JIESUANRI  = 0xA011;//写结算日
    public static final int DATAID_WRITE_CHAOBIAORI = 0xA012;//写抄表日
    public static final int DATAID_READ_JIESUANRI   = 0x8103;//读结算日
    public static final int DATAID_READ_CHAOBIAORI  = 0x8104;//读抄表日


    public static final int DATAID_READ_HISTORY_M_DATA_1     = 0x903F;//读 1-6 月 历史数据
    public static final int DATAID_READ_HISTORY_M_DATA_2     = 0x904F;//读 7-12月 历史数据

    public static final int DATAID_READ_HISTORY_D_DATA_1     = 0x913F;//读前1-5天 数据
    public static final int DATAID_READ_HISTORY_D_DATA_2     = 0x914F;//读前6-10天 数据
    public static final int DATAID_READ_HISTORY_D_DATA_3     = 0x915F;//读前11-15天 数据
    public static final int DATAID_READ_HISTORY_D_DATA_4     = 0x916F;//读前16-20天 数据
    public static final int DATAID_READ_HISTORY_D_DATA_5     = 0x917F;//读前21-25天 数据
    public static final int DATAID_READ_HISTORY_D_DATA_6     = 0x918F;//读前26-30天 数据



    /**
     * 组网命令 广播命令获取在网所有表ID
     */
    public static final int DATAID_SCAN_ALL_METER_ID = 0xFF55;


    public static final int DATAID_UPDATE_FIRMWARE       = 0x55AA;//表固件升级
    public static final int DATAID_UPDATE_FIRMWARE_RESET = 0x55CC;//表复位

    /**
     * 设置掌机NETID 命令ID
     */
    public static final int DATAID_SET_HANDLE_RF_PARAM = 0xBB01;

    /**
     * 设置气表rf模块参数
     */
    public static final int DATAID_SET_METER_RF_PARAM = 0x5599;

    /**
     * 升级文件路径
     */
    public static final String UPDATE_BIN_PATH = "/storage/sdcard0/Y.txt";


    /**
     * 升级文件路径 txt
     */
    public static final String UPDATE_TXT_PATH = "/storage/sdcard0/application_meter.txt";

    /**
     * 发出数据头部长度(字节数) 根据模块变化
     */
    public static final int OUT_HEAD_LENGTH = 2;

    /**
     * 接收数据头部长度(字节数) 4bytesID的模块 根据模块变化
     */
    public static final int IN_HEAD_LENGTH_4_BYTES = 5;

    /**
     * 接收数据头部长度(字节数) 2bytesID的模块 根据模块变化
     */
    public static final int IN_HEAD_LENGTH_2_BYTES = 3;



    /**
     * 中继模块id长度
     */
    public static final int RF_RELAY_ID_LENGTH = 0x03;


    /**
     * 表广播地址
     */
    public static final String METER_ID_BROADCAST = "AAAAAAAAAAAAAA";

    /**
     * 模块广播地址
     */
    public static final String RF_BROADCAST = "FFFFFFFF";


    public static final String TAG_DATA = "blebox_data";

}
