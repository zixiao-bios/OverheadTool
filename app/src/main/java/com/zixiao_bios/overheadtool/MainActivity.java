package com.zixiao_bios.overheadtool;

import androidx.appcompat.app.AppCompatActivity;
import android.app.usage.NetworkStats;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.app.usage.NetworkStatsManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // 绑定MonitorService
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            // 服务绑定成功
            Log.i(tag, "MonitorService绑定成功");
            MonitorService.MonitorServiceBinder binder = (MonitorService.MonitorServiceBinder) iBinder;
            monitorService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 服务断开
            Log.i(tag, "MonitorService断开");
        }
    };

    private MonitorService monitorService;
    private final String tag = "MainActivity";

    // UI组件
    private EditText uidInput, durationInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uidInput = findViewById(R.id.uidInputText);
        durationInput = findViewById(R.id.durationInputText);

        if (!NetworkStatsHelper.hasPermissionToReadNetworkHistory(this)){
            // 没有读取网络使用情况权限
            Toast.makeText(this, "请开启此应用的权限，然后重启应用", Toast.LENGTH_LONG).show();
            NetworkStatsHelper.requestReadNetworkHistoryAccess(this);
            return;
        }

        // 有权限
        Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();

        // 绑定MonitorService
        Intent intent = new Intent(this, MonitorService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    // 开始测试按钮监听
    public void clickStartTest(View view) {
        if (uidInput.getText().toString().length() > 0 && durationInput.getText().toString().length() > 0) {
            int uid = Integer.parseInt(uidInput.getText().toString());
            double min = Double.parseDouble(durationInput.getText().toString());
            long duration = (long)(min * 60.0 * 1000.0);

            new Thread(){
                @Override
                public void run() {
                    monitorService.runMonitor(duration, uid);
                }
            }.start();
        } else {
            Toast.makeText(this, "请正确输入！", Toast.LENGTH_SHORT).show();
        }
    }
}