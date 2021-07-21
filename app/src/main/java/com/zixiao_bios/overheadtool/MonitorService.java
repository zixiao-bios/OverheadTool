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

    /**
     * 对一个指定uid的程序进行指定时长的性能测试
     * @param duration 测试时长(ms)
     * @param uid 待测程序的uid
     */
    public void runMonitor(long duration, int uid){
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;
        Log.e("test", "duration=" + duration + "\nuid=" + uid);

        while (System.currentTimeMillis() < endTime){
            // 测试期间
        }

        // 测试时间结束
        Log.e("test", "测试结束");

        // 测试结束后，统计网络使用情况
        try {
            NetworkStatsManager networkStatsManager = getSystemService(NetworkStatsManager.class);
            NetworkStats networkStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    null,
                    startTime,
                    endTime,
                    uid
            );
            NetworkStatsHelper.statisticNetworkStats(networkStats);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "统计网络数据时出错！", Toast.LENGTH_LONG).show();
        }
    }
}