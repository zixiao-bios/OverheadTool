package com.zixiao_bios.overheadtool;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class NetworkStatsHelper {
    /**
     * 判断当前context是否有读取网络使用情况的权限
     * @param context context
     * @return 该context是否有读取网络使用情况的权限
     */
    public static boolean hasPermissionToReadNetworkHistory(Context context) {
        final AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * 打开设置权限的窗口，并提示用户设置权限
     * @param context context
     */
    public static void requestReadNetworkHistoryAccess(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        context.startActivity(intent);
    }

    /**
     * 统计NetworkStats中所有Bucket的rxBytes、txBytes、rxPackets、txPackets、bucketNum
     * @param networkStats 被统计的NetworkStats
     * @return 统计后生成的HashMap<String, Long>
     */
    public static HashMap<String, Long> statisticNetworkStats(NetworkStats networkStats){
        long rxBytes = 0, txBytes = 0, rxPackets = 0, txPackets = 0, bucketNum = 0;
        while (networkStats.hasNextBucket()) {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            rxBytes += bucket.getRxBytes();
            txBytes += bucket.getTxBytes();
            rxPackets += bucket.getRxPackets();
            txPackets += bucket.getTxPackets();
            bucketNum++;
        }

        HashMap<String, Long> networkStatsMap = new HashMap<>();
        networkStatsMap.put("rxBytes", rxBytes);
        networkStatsMap.put("txBytes", txBytes);
        networkStatsMap.put("rxPackets", rxPackets);
        networkStatsMap.put("txPackets", txPackets);
        Log.e("test", networkStatsMap.toString());
        return networkStatsMap;
    }
}
