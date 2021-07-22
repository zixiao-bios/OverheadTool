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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import android.net.TrafficStats;
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
    public static HashMap<String, Long> statisticNetworkStats(NetworkStats networkStats, int uid){
        long rxBytes = 0, txBytes = 0, rxPackets = 0, txPackets = 0, bucketNum = 0;
        while (networkStats.hasNextBucket()) {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            if (bucket.getUid() == uid) {
                rxBytes += bucket.getRxBytes();
                txBytes += bucket.getTxBytes();
                rxPackets += bucket.getRxPackets();
                txPackets += bucket.getTxPackets();
                bucketNum++;
            }
        }

//        do {
//            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
//            networkStats.getNextBucket(bucket);
//            rxBytes += bucket.getRxBytes();
//            txBytes += bucket.getTxBytes();
//            rxPackets += bucket.getRxPackets();
//            txPackets += bucket.getTxPackets();
//            bucketNum++;
//        } while (networkStats.hasNextBucket());

        HashMap<String, Long> networkStatsMap = new HashMap<>();
        networkStatsMap.put("rxBytes", rxBytes);
        networkStatsMap.put("txBytes", txBytes);
        networkStatsMap.put("rxPackets", rxPackets);
        networkStatsMap.put("txPackets", txPackets);
        networkStatsMap.put("bucketNum", bucketNum);
        Log.e("test", networkStatsMap.toString());
        return networkStatsMap;
    }

    public static HashMap<String, Long> getUidNetworkStats(int uid){
        HashMap<String, Long> networkStatsMap = new HashMap<>();
        networkStatsMap.put("rxBytes", TrafficStats.getUidRxBytes(uid));
        networkStatsMap.put("txBytes", TrafficStats.getUidTxBytes(uid));
        networkStatsMap.put("rxPackets", TrafficStats.getUidRxPackets(uid));
        networkStatsMap.put("txPackets", TrafficStats.getUidTxPackets(uid));
        return networkStatsMap;
    }

    public static HashMap<String, Long> countNetworkUse(HashMap<String ,Long> start, HashMap<String, Long> end){
        HashMap<String, Long> networkStatsMap = new HashMap<>();
        try {
            networkStatsMap.put("rxBytes", end.get("rxBytes") - start.get("rxBytes"));
            networkStatsMap.put("txBytes", end.get("txBytes") - start.get("txBytes"));
            networkStatsMap.put("rxPackets", end.get("rxPackets") - start.get("rxPackets"));
            networkStatsMap.put("txPackets", end.get("txPackets") - start.get("txPackets"));
        } catch (Exception e){
            e.printStackTrace();
        }
        return networkStatsMap;
    }

    public static Long getTotalBytesManual(int localUid) {
//        Log.e("BytesManual*****", "localUid:" + localUid);
        File dir = new File("/proc/uid_stat/");
        String[] children = dir.list();
        if (children == null) {
            Log.e("test", "null!!!!");
        }
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
            stringBuffer.append(children[i]);
            stringBuffer.append("   ");
        }
//        Log.e("children*****", children.length + "");
//        Log.e("children22*****", stringBuffer.toString());
        if (!Arrays.asList(children).contains(String.valueOf(localUid))) {
            return 0L;
        }
        File uidFileDir = new File("/proc/uid_stat/" + String.valueOf(localUid));
        File uidActualFileReceived = new File(uidFileDir, "tcp_rcv");
        File uidActualFileSent = new File(uidFileDir, "tcp_snd");
        String textReceived = "0";
        String textSent = "0";
        try {
            BufferedReader brReceived = new BufferedReader(new FileReader(uidActualFileReceived));
            BufferedReader brSent = new BufferedReader(new FileReader(uidActualFileSent));
            String receivedLine;
            String sentLine;

            if ((receivedLine = brReceived.readLine()) != null) {
                textReceived = receivedLine;
//                Log.e("receivedLine*****", "receivedLine:" + receivedLine);
            }
            if ((sentLine = brSent.readLine()) != null) {
                textSent = sentLine;
//                Log.e("sentLine*****", "sentLine:" + sentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
//            Log.e("IOException*****", e.toString());
        }
//        Log.e("BytesManualEnd*****", "localUid:" + localUid);
        return Long.parseLong(textReceived) + Long.parseLong(textSent);
    }
}
