package com.zixiao_bios.overheadtool;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;

public class MonitorService extends Service {
    // 用于Activity和service通讯
    private final MonitorServiceBinder monitorServiceBinder = new MonitorServiceBinder();

    public class MonitorServiceBinder extends Binder {
        public MonitorService getService() {
            return MonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return monitorServiceBinder;
    }

    // 测试开始、结束时间戳
    long startTime, endTime;

    private final String tag = "MonitorService";

    /**
     * 对一个指定pid的程序进行指定时长的性能测试
     *
     * @param duration 测试时长(ms)
     * @param pid      待测程序的pid
     */
    public void runMonitor(long duration, int pid) {
        // 获取uid
        int uid = CmdTool.findUidByPid(pid);
        if (uid == -1){
            return;
        }

        // 获取起止时间
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;

        // CPU、内存总用量
        double cpuTot = 0, memTot = 0;

        // CPU测量次数
        int testNum = 0;

        // 每次测量的resMap
        HashMap<String, Double> resMapEach;

        // 测试开始
        Log.e(tag, "-----------------------------------------\nduration=" + duration + "\nuid=" + uid);

        // 统计开始时网络用量
        HashMap<String, Long> netstatsMapStart = CmdTool.findUidNetstats(uid);

        // 测试期间
        while (System.currentTimeMillis() < endTime) {
            // 每5秒测一次CPU和内存
            resMapEach = CmdTool.findPidCpuMemStats(pid);
            if (resMapEach == null) {
                continue;
            }
            cpuTot += resMapEach.get("cpu");
            memTot += resMapEach.get("mem");
            testNum ++;

            // 睡眠，且按时退出循环
            try {
                if (endTime - System.currentTimeMillis() >= 5000) {
                    // 剩余时间大于5秒，则睡5秒
                    Thread.sleep(5000);
                } else {
                    // 剩余时间不足5秒，则睡到endTime
                    Thread.sleep(endTime - System.currentTimeMillis());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 测试结束
        Log.e(tag, "测试结束");

        // 结束时网络用量
        HashMap<String, Long> netstatsMapEnd = CmdTool.findUidNetstats(uid);

        // 网络总用量
        HashMap<String, Long> netstatsMapUse = CmdTool.subNetstatsMap(netstatsMapEnd, netstatsMapStart);
        if (netstatsMapUse == null) {
            Log.e(tag, "网络统计失败");
        } else {
            Log.e(tag, netstatsMapUse.toString());
        }

        // CPU(%)、内存(MB)平均用量
        if (testNum == 0){
            Log.e(tag, "CPU、内存统计失败");
        } else {
            HashMap<String, Double> resMapAve = new HashMap<>();
            resMapAve.put("cpu", cpuTot / testNum);
            resMapAve.put("mem", memTot / testNum);
            Log.e(tag, resMapAve.toString());
        }

        Log.e(tag, "-----------------------------------------------");
    }
}