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

    /**
     * 对一个指定uid的程序进行指定时长的性能测试
     *
     * @param duration 测试时长(ms)
     * @param uid      待测程序的uid
     */
    public void runMonitor(long duration, int uid) {
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;

        // 测试开始
        Log.e("test", "-----------------------------------------\nduration=" + duration + "\nuid=" + uid);
        HashMap<String, Long> startNetStats = NetworkStatsHelper.getUidNetworkStats(uid);
        long startB = NetworkStatsHelper.getTotalBytesManual(uid);

        while (System.currentTimeMillis() < endTime) {
            // 测试期间
        }

        // 测试结束
        Log.e("test", "测试结束");
        HashMap<String, Long> endNetStats = NetworkStatsHelper.getUidNetworkStats(uid);
        HashMap<String, Long> netUse = NetworkStatsHelper.countNetworkUse(startNetStats, endNetStats);

        long endB = NetworkStatsHelper.getTotalBytesManual(uid);
        long useB = endB - startB;

        Log.e("test", String.valueOf(useB));
//        Log.e("test", endNetStats.toString());
        Log.e("test", "-----------------------------------------------");

        // 测试结束后，统计网络使用情况
//        try {
//            Calendar endCalendar = Calendar.getInstance();
//            endCalendar.add(Calendar.DATE, 1);
//
//            Calendar startCalendar = Calendar.getInstance();
//            startCalendar.add(Calendar.HOUR, -1);
//
////            Log.e("test", startCalendar.getTimeInMillis() + "\t" + endCalendar.getTimeInMillis());
////            Log.e("test", String.valueOf((endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis())/(1000 * 3600)));
//
//            NetworkStatsManager networkStatsManager = getSystemService(NetworkStatsManager.class);
////            NetworkStats networkStats = networkStatsManager.queryDetailsForUid(
////                    ConnectivityManager.TYPE_WIFI,
////                    "",
////                    0L,
////                    System.currentTimeMillis() + 3600 * 1000,
////                    uid
////            );
//
//            NetworkStats networkStats = networkStatsManager.querySummary(
//                    ConnectivityManager.TYPE_WIFI,
//                    "",
//                    startTime,
//                    startTime + 1
//            );
//
//            Log.e("test", String.valueOf((endTime - startTime) / 1000));
//
//            NetworkStatsHelper.statisticNetworkStats(networkStats, uid);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "统计网络数据时出错！", Toast.LENGTH_LONG).show();
//        }
    }
}