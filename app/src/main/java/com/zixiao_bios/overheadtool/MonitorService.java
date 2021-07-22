package com.zixiao_bios.overheadtool;

import android.app.Service;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
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

        // 测试开始
        Log.e(tag, "-----------------------------------------\nduration=" + duration + "\nuid=" + uid);

//        while (System.currentTimeMillis() < endTime) {
//            // 测试期间
//        }

        // 测试结束
        Log.e(tag, "测试结束");

//        String s = CmdTool.findUidNetstats(uid, this);
        String s = CmdTool.findUidSetNetStatus(uid, "FOREGROUND");

        if (s == null) {
            Log.e(tag, "结果为空");
        } else {
            Log.e(tag, s);
        }

        Log.e(tag, "-----------------------------------------------");
    }
}