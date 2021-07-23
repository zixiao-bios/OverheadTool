package com.zixiao_bios.overheadtool;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

public class TaskInfoActivity extends AppCompatActivity {
    // 绑定MonitorService
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            // 服务绑定成功
            Log.i(tag, "MonitorService绑定成功");
            MonitorService.MonitorServiceBinder binder = (MonitorService.MonitorServiceBinder) iBinder;
            monitorService = binder.getService();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    refreshUI();
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 服务断开
            Log.i(tag, "MonitorService断开");
        }
    };

    private MonitorService monitorService;
    String tag = "TaskInfoActivity";

    private TextInputEditText taskMessageText, testingMessageText, reportMessageText;
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_info);

        taskMessageText = findViewById(R.id.taskMassageText);
        testingMessageText = findViewById(R.id.testingMassageText);
        reportMessageText = findViewById(R.id.reportMassageText);
        finishButton = findViewById(R.id.finishButton);

        // 绑定MonitorService
        Intent intent = new Intent(this, MonitorService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    private void refreshUI() {
        // 设置初始信息
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                taskMessageText.setText(monitorService.taskMessage);
                testingMessageText.setText(monitorService.testingMessage);
                reportMessageText.setText(monitorService.reportMessage);
            }
        });

        // 设置任务信息
        while (!monitorService.taskMessageDone);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                taskMessageText.setText(monitorService.taskMessage);
            }
        });

        while (monitorService.testRun) {
            // 测试中
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    testingMessageText.setText(monitorService.testingMessage);
                }
            });
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 测试完成
        while (!monitorService.reportDone);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reportMessageText.setText(monitorService.reportMessage);
                finishButton.setVisibility(View.VISIBLE);
            }
        });
    }

    public void clickFinish(View view){
        monitorService.serviceInit();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }
}