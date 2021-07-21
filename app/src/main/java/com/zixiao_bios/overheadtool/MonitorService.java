package com.zixiao_bios.overheadtool;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
    public void runTest(long duration, int uid){
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;
        Log.e("test", "duration=" + duration + "\nuid=" + uid);

        // todo 添加测试内容
    }
}