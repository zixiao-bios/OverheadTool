package com.zixiao_bios.overheadtool;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
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
    private EditText pidInput, durationInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pidInput = findViewById(R.id.pidInputText);
        durationInput = findViewById(R.id.durationInputText);

        MyDisplay.createToast(getApplicationContext(), this);

        startForegroundService(new Intent(this, MonitorService.class));

        // 绑定MonitorService
        bindService(new Intent(MainActivity.this, MonitorService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    // 开始测试按钮监听
    public void clickStartTest(View view) {
        if (pidInput.getText().toString().length() > 0 && durationInput.getText().toString().length() > 0) {
            int pid = Integer.parseInt(pidInput.getText().toString());
            double min = Double.parseDouble(durationInput.getText().toString());
            long duration = (long)(min * 60.0 * 1000.0);
            new Thread(){
                @Override
                public void run() {
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    MyDisplay.vibrate(1000);
                    monitorService.runMonitor(duration, pid);
                    MyDisplay.vibrate(3000);
                    monitorService.serviceInit();
                }
            }.start();
//            Intent intent = new Intent(this, TaskInfoActivity.class);
//            startActivity(intent);
            MyDisplay.toast("测试5s后开始");
        } else {
            Toast.makeText(this, "请正确输入！", Toast.LENGTH_SHORT).show();
        }
    }
}