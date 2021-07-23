package com.zixiao_bios.overheadtool;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
    private long startTime, endTime;

    private final String tag = "MonitorService";

    private int uid, pid;
    private long duration;

    // 测试任务信息
    public String taskMessage = "暂无信息";

    // 测试期间的输出信息
    public String testingMessage = "暂无信息";

    // 测试完成的输出信息
    public String reportMessage = "等待测试结束……";

    public boolean testRun = false;

    /**
     * 对一个指定pid的程序进行指定时长的性能测试
     *
     * @param duration 测试时长(ms)
     * @param pid      待测程序的pid
     */
    public void runMonitor(long duration, int pid) {
        testRun = true;
        this.pid = pid;
        this.duration = duration;

        // 获取uid
        uid = Tools.findUidByPid(pid);
        if (uid == -1) {
            return;
        }

        // 获取起止时间
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;

        // CPU、内存总用量
        double cpuTot = 0, memTot = 0;

        // CPU测量次数
        int testNum = 0;

        // 用电量（J）
        double powerUse = 0;

        // 上一次、本次测量功率的时间戳
        long lastPowerTime = startTime, thisPowerTime;

        // 每次测量的resMap、powerMap
        HashMap<String, Double> resMapEach, powerMap;

        // 测试开始
        Log.e(tag, "-----------------------------------------\nduration=" + duration + "\nuid=" + uid);

        // 统计开始时网络用量
        HashMap<String, Long> netstatsMapStart = Tools.getUidNetstats(uid);

        // 测试期间
        while (System.currentTimeMillis() < endTime) {
            // 每1秒测一次
            // CPU和内存用量
            resMapEach = Tools.getPidCpuMemStats(pid);
            if (resMapEach != null) {
                cpuTot += resMapEach.get("cpu");
                memTot += resMapEach.get("mem");
                testNum++;
            } else {
                Log.e(tag, "单次CPU和内存读取失败！");
                MyDisplay.toast("单次CPU和内存读取失败！");
            }

            // 耗电量
            powerMap = Tools.getUsbPowerMap();
            if (powerMap != null) {
                thisPowerTime = System.currentTimeMillis();
                powerUse += powerMap.get("usbP") * ((double) (thisPowerTime - lastPowerTime) / 1000);
                lastPowerTime = thisPowerTime;
            } else {
                Log.e(tag, "单次电源信息读取失败！");
                MyDisplay.toast("单次电源信息读取失败！");
            }

            // 输出过程信息

            // 睡眠，且按时退出循环
            try {
                if (endTime - System.currentTimeMillis() >= 1000) {
                    // 剩余时间大于1秒，则睡1秒
                    Thread.sleep(1000);
                } else {
                    // 剩余时间不足1秒，则睡到endTime
                    Thread.sleep(endTime - System.currentTimeMillis());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 补上最后一个时间片的耗电量
        powerMap = Tools.getUsbPowerMap();
        if (powerMap != null) {
            powerUse += powerMap.get("usbP") * ((double) (endTime - lastPowerTime));
        } else {
            Log.e(tag, "单次电源信息读取失败！");
            MyDisplay.toast("单次电源信息读取失败！");
        }

        // 结束时的网络用量
        HashMap<String, Long> netstatsMapEnd = Tools.getUidNetstats(uid);

        // ------------------------------------------测试结束---------------------------------------------------
        Log.e(tag, "测试结束");
        testRun = false;

        // 网络总用量
        HashMap<String, Double> netUseMap = Tools.getNetUseMap(netstatsMapEnd, netstatsMapStart);
        if (netUseMap == null) {
            Log.e(tag, "网络统计失败");
        } else {
            Log.e(tag, netUseMap.toString());
        }

        // CPU(%)、内存(MB)平均用量
        if (testNum == 0) {
            Log.e(tag, "CPU、内存统计失败");
        } else {
            HashMap<String, Double> resMapAve = new HashMap<>();
            resMapAve.put("cpu", cpuTot / testNum);
            resMapAve.put("mem", memTot / testNum);
            Log.e(tag, resMapAve.toString());
        }
    }

    private void makeTestingMessage(HashMap<String, Double> resMap, HashMap<String, Double> powerMap) {
        String msg;
        if (testRun) {
            msg = "测试中，还剩 " + (double) (endTime - System.currentTimeMillis()) / 1000 + " 秒……\n";
            msg += "CPU:\t" + resMap.get("cpu") + "%\n";
            msg += "内存:\t" + resMap.get("mem") + "MB\n";
            msg += "USB电流:\t" + powerMap.get("usbI") * 1000 + "mA\n";
            msg += "USB电压:\t" + powerMap.get("usbV") + "V\n";
            msg += "USB功率:\t" + powerMap.get("usbP") + "W\n";
            msg += "网络信息将在测试完成后显示……\n";
        } else {
            msg = "测试完成\n";
        }
        testingMessage = msg;
    }

    private void makeTaskMessage() {
        String msg = "PID:\t" + pid + "\n";
        msg += "UID:\t" + uid + "\n";
        msg += "测试时长:\t" + (double) duration / 1000 + "秒\n";
        taskMessage = msg;
    }

    private void makeReportMessage(HashMap<String, Double> netUseMap, HashMap<String, Double> resMap, double powerUse) {
        String msg = "";
        if (testRun) {
            msg = "等待测试结束……";
        } else {
            if (resMap == null) {
                msg += "CPU和内存检测失败！\n";
            } else {
                msg += "CPU:\t" + resMap.get("cpu") + "%\n";
                msg += "内存:\t" + resMap.get("mem") + "MB\n";
            }

            if (powerUse == -1) {
                msg += "USB供电量检测失败！\n";
            } else {
                msg += "USB供电量:\t" + powerUse + "J\n";
            }

            if (netUseMap == null) {
                msg += "网络流量检测失败！\n";
            } else {
                msg += "网络流量:\t" + netUseMap.get("totKB") + "KB\n";
                msg += "接收流量:\t" + netUseMap.get("rKB") + "KB\n";
                msg += "接收数据包:\t" + netUseMap.get("rp").intValue() + "\n";
                msg += "发送流量:\t" + netUseMap.get("tKB") + "KB\n";
                msg += "发送数据包:\t" + netUseMap.get("tp").intValue() + "\n";
            }
        }
        reportMessage = msg;
    }
}