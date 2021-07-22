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
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;
        int uid = CmdTool.findUidByPid(pid);
        if (uid == -1){
            return;
        }

        // 测试开始
        Log.e(tag, "-----------------------------------------\nduration=" + duration + "\nuid=" + uid);

        // 统计开始时网络用量
        HashMap<String, Long> netstatsMapStart = CmdTool.findUidNetstats(uid);

        while (System.currentTimeMillis() < endTime) {
            // 测试期间
        }

        // 测试结束
        Log.e(tag, "测试结束");

        // 统计结束时网络用量
        HashMap<String, Long> netstatsMapEnd = CmdTool.findUidNetstats(uid);

        // 测试期间网络用量
        HashMap<String, Long> netstatsMapUse = CmdTool.subNetstatsMap(netstatsMapEnd, netstatsMapStart);
        if (netstatsMapUse == null) {
            Log.e(tag, "网络统计失败");
        } else {
            Log.e(tag, netstatsMapUse.toString());
        }

        // test top
        HashMap<String, Double> resMap = CmdTool.findPidCpuMemStats(pid);
        if (resMap == null) {
            Log.e(tag, "CPU和内存统计失败！");
        } else {
            Log.e(tag, resMap.toString());
        }

        Log.e(tag, "-----------------------------------------------");
    }
}