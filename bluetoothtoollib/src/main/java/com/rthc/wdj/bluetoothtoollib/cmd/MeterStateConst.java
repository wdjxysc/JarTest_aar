package com.rthc.wdj.bluetoothtoollib.cmd;

/**
 * Created by Administrator on 2015/9/17.
 * 表状态
 */
public class MeterStateConst {
    /**
     * 错误
     */
    public static final int S_ERROR_MSK = 0xDC;

    //阀门状态
    public static final int S_VALVE_MSK  = 0x03;
    public static final int S_VALVE_OPND = 0x00;
    public static final int S_VALVE_CLSD = 0x01;
    public static final int S_VALVE_ERR  = 0x03;

    //6V电源状态
    public static final int S_BAT_6V_MSK = 0x01<<2;
    public static final int S_BAT_6V_LO  = 0x01<<2;

    //阀门泄露
    public static final int S_VALVE_LEAK_MSK = 0x01<<3;
    public static final int S_VALVE_LEAK     = 0x01<<3;

    //超过15天未用
    public static final int S_UNUSD_OVER_15_DAYS_MSK = 0x01<<4;
    public static final int S_UNUSD_OVER_15_DAYS     = 0x01<<4;

    //3.6V电源状态
    public static final int S_BAT_3_6V_MSK = 0x01<<5;
    public static final int S_BAT_3_6V_LO  = 0x01<<5;

    //强磁干扰
    public static final int S_STRG_MAGIC_MSK = 0x01<<6;
    public static final int S_STRG_MAGIC     = 0x01<<6;

    //干簧管坏
    public static final int S_RDSW_BAD_MSK = 0x01<<7;
    public static final int S_RDSW_BAD     = 0x01<<7;




    public enum STATE_VALVE{
        OPEN,
        CLOSE,
        ERROR
    }

    public enum STATE_POWER_6_V{
        LOW,
        OK
    }

    public enum STATE_POWER_3_6_V{
        LOW,
        OK
    }

    public enum STATE_VALVE_LEAK{
        LEAK,
        OK
    }

    public enum STATE_UNUSD_OVER_15_DAYS{
        UNUSD,
        OK
    }

    public enum STATE_STRG_MAGIC{
        STRG_MAGIC,
        OK
    }

    public enum STATE_RDSW_BAD{
        BAD,
        OK
    }
}
