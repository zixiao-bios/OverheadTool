package com.zixiao_bios.overheadtool;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.app.usage.NetworkStatsManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NetworkStatsManager networkStatsManager = getSystemService(NetworkStatsManager.class);
//        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
        if (!NetworkStatsHelper.hasPermissionToReadNetworkHistory(this)){
            // 没有读取网络使用情况权限
            Toast.makeText(this, "请开启此应用的权限，然后重启应用", Toast.LENGTH_LONG).show();
            NetworkStatsHelper.requestReadNetworkHistoryAccess(this);
            return;
        }

        // 有权限
        Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
        try {
            NetworkStats networkStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    null,
                    0,
                    System.currentTimeMillis(),
                    1000
            );
            NetworkStatsHelper.testNetworkStats(networkStats);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}