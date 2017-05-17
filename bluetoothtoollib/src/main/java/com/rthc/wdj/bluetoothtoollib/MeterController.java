package com.rthc.wdj.bluetoothtoollib;

/**
 * Created by Administrator on 2015/12/4.
 */
public interface MeterController {
    /**
     * 抄表
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    void readMeter(String meterId, int bleModuleId, MeterHandler handler);

    /**
     * 开阀
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    void openValve(String meterId, int bleModuleId, ValveHandler handler);

    /**
     * 关阀
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    void closeValve(String meterId, int bleModuleId, ValveHandler handler);


    /**
     * 写表ID
     * @param oldMeterId 原表ID
     * @param newMeterId 新表ID
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterId(String oldMeterId, String newMeterId, int bleModuleId, MeterHandler handler);


    /**
     * 写表底数
     * @param meterId 表ID
     * @param meterValue 底数
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterValue(String meterId,float meterValue, int bleModuleId, MeterHandler handler);

    /**
     * 读表状态
     * @param meterId 表ID
     * @param bleModuleId 模块选择
     * @param handler
     */
    void readMeterState(String meterId, int bleModuleId, MeterHandler handler);

    /**
     * 写表状态 默认为正常关阀
     * @param meterId 表ID
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterState(String meterId, int bleModuleId, MeterHandler handler);

    /**
     * 写表NETID
     * @param meterId 表ID
     * @param newNetId 新NETID
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterNetId(String meterId, int newNetId, int bleModuleId, MeterHandler handler);


    /**
     * 复位表
     * @param meterId
     * @param handler
     */
    void resetMeter(String meterId, int bleModuleId, MeterHandler handler);


    /**
     * 抄表 2ID
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    void readMeter2(String meterId, int bleModuleId, MeterHandler handler);

    /**
     * 开阀 2ID
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    void openValve2(String meterId, int bleModuleId, ValveHandler handler);

    /**
     * 关阀 2ID
     *
     * @param meterId 表ID
     * @param handler 回调
     */
    void closeValve2(String meterId, int bleModuleId, ValveHandler handler);


    /**
     * 写表ID
     * @param oldMeterId 原表ID
     * @param newMeterId 新表ID
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterId2(String oldMeterId, String newMeterId, int bleModuleId, MeterHandler handler);


    /**
     * 写表底数
     * @param meterId 表ID
     * @param meterValue 底数
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterValue2(String meterId,float meterValue, int bleModuleId, MeterHandler handler);

    /**
     * 读表状态
     * @param meterId 表ID
     * @param bleModuleId 模块选择
     * @param handler
     */
    void readMeterState2(String meterId, int bleModuleId, MeterHandler handler);

    /**
     * 写表状态 默认为正常关阀
     * @param meterId 表ID
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterState2(String meterId, int bleModuleId, MeterHandler handler);

    /**
     * 写表NETID
     * @param meterId 表ID
     * @param newNetId 新NETID
     * @param bleModuleId 模块选择
     * @param handler 回调
     */
    void writeMeterNetId2(String meterId, int newNetId, int bleModuleId, MeterHandler handler);

    /**
     * 复位表
     * @param meterId
     * @param handler
     */
    void resetMeter2(String meterId, int bleModuleId, MeterHandler handler);
}
