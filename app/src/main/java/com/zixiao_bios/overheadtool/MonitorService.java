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

    // 每次检测的poserMap
    HashMap<String, Double> powerMapEach;

    HashMap<String, Double> powerMap = null;

    // 测试任务信息
    public String taskMessage = "暂无信息";

    // 测试期间的输出信息
    public String testingMessage = "暂无信息";

    // 测试完成的输出信息
    public String reportMessage = "等待测试结束……";

    public boolean testRun = false, reportDone = false, taskMessageDone = false;

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

        // CPU、内存、功率总量
        double cpuTot = 0, memTot = 0;

        // CPU、功率测量次数
        int cpuNum = 0;

        // 每次测量的resMap、powerMap
        HashMap<String, Double> resMapEach;

        // 测试开始
        makeTaskMessage();
        taskMessageDone = true;
        Log.i(tag, taskMessage);

        // 开启电量统计线程
        Thread powerStatsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(tag, "启动USB电源统计线程");
                usbPowerStats();
            }
        });
        powerStatsThread.start();

        // 统计开始时网络用量
        HashMap<String, Long> netstatsMapStart = Tools.getUidNetstats(uid);
        if (netstatsMapStart == null) {
            Log.e(tag, "起始网络用量读取失败！");
        }

        // 测试期间
        while (System.currentTimeMillis() < endTime) {
            // CPU和内存用量
            resMapEach = Tools.getPidCpuMemStats(pid);
            if (resMapEach != null) {
                cpuTot += resMapEach.get("cpu");
                memTot += resMapEach.get("mem");
                cpuNum++;
            } else {
                Log.e(tag, "单次CPU和内存读取失败！");
            }

            // 生成过程信息
            makeTestingMessage(resMapEach, powerMapEach);
            Log.i(tag, testingMessage);

            // 睡眠，且按时退出循环
//            try {
//                if (endTime - System.currentTimeMillis() >= 1000) {
//                    // 剩余时间大于1秒，则睡1秒
//                    Thread.sleep(1000);
//                } else {
//                    // 剩余时间不足1秒，则睡到endTime
//                    Thread.sleep(endTime - System.currentTimeMillis());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }

        // 结束时的网络用量
        HashMap<String, Long> netstatsMapEnd = Tools.getUidNetstats(uid);
        if (netstatsMapEnd == null) {
            Log.e(tag, "结束网络用量读取失败！");
        }

        // ------------------------------------------测试结束---------------------------------------------------
        testRun = false;

        // 网络总用量
        HashMap<String, Double> netUseMap = Tools.getNetUseMap(netstatsMapEnd, netstatsMapStart);
        if (netUseMap == null) {
            Log.e(tag, "网络统计失败");
        }

        // CPU(%)、内存(MB)平均用量
        HashMap<String, Double> resMapAve = null;
        if (cpuNum == 0) {
            Log.e(tag, "CPU、内存统计失败");
        } else {
            resMapAve = new HashMap<>();
            resMapAve.put("cpu", cpuTot / cpuNum);
            resMapAve.put("mem", memTot / cpuNum);
            resMapAve.put("time", (double) (endTime - startTime) / cpuNum);
        }

        // 等待电源统计线程结束
        Log.i(tag, "等待USB电源统计线程结束……");
        try {
            powerStatsThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 生成测试报告
        makeReportMessage(netUseMap, resMapAve, powerMap);
        reportDone = true;
        Log.i(tag, reportMessage);
    }

    private void makeTestingMessage(HashMap<String, Double> resMap, HashMap<String, Double> powerMap) {
        String msg = "\n";
        if (testRun) {
            long leftTime = Math.round((double) (endTime - System.currentTimeMillis()) / 1000);
            if (leftTime < 0) {
                leftTime = 0;
            }

            msg += "测试中，还剩 " + leftTime + " 秒……\n";
            if (resMap == null) {
                msg += "CPU和内存读取失败！\n";
            } else {
                msg += "CPU:\t" + resMap.get("cpu") + " %\n";
                msg += "内存:\t" + resMap.get("mem") + " MB\n";
            }
            if (powerMap == null) {
                msg += "电源信息读取失败！\n";
            } else {
                msg += "USB电流:\t" + Math.round(powerMap.get("usbI") * 1000 * 100) * 0.01 + " mA\n";
                msg += "USB电压:\t" + Math.round(powerMap.get("usbV") * 100) * 0.01 + " V\n";
                msg += "USB功率:\t" + Math.round(powerMap.get("usbP") * 100) * 0.01 + " W\n";
            }
            msg += "网络信息将在测试完成后显示……\n";
        } else {
            msg = "测试完成\n";
        }
        testingMessage = msg;
    }

    private void makeTaskMessage() {
        String msg = "\nPID:\t" + pid + "\n";
        msg += "UID:\t" + uid + "\n";
        msg += "测试时长:\t" + (double) duration / 1000 + "秒\n";
        taskMessage = msg;
    }

    private void makeReportMessage(HashMap<String, Double> netUseMap, HashMap<String, Double> resMap, HashMap<String, Double> powerMap) {
        String msg = "\n";
        if (testRun) {
            msg = "等待测试结束……";
        } else {
            // cpu、内存信息
            if (resMap == null) {
                msg += "CPU和内存检测失败！\n";
            } else {
                msg += "CPU:\t" + Math.round(resMap.get("cpu") * 100) * 0.01 + " %\n";
                msg += "内存:\t" + Math.round(resMap.get("mem") * 100) * 0.01 + " MB\n";
                msg += "CPU平均检测间隔:\t" + Math.round(resMap.get("time") * 100) * 0.01 + " ms\n";
            }
            msg += "--------------------------\n";

            // USB功率信息
            if (powerMap == null) {
                msg += "USB功率检测失败！\n";
            } else {
                msg += "USB平均功率:\t" + Math.round(powerMap.get("power") * 100) * 0.01 + " W\n";
                msg += "USB平均检测间隔:\t" + Math.round(powerMap.get("time") * 100) * 0.01 + " ms\n";
            }
            msg += "--------------------------\n";

            // 网络信息
            if (netUseMap == null) {
                msg += "网络流量检测失败！\n";
            } else {
                msg += "网络流量:\t" + netUseMap.get("totKB") + " KB\n";
                msg += "接收流量:\t" + netUseMap.get("rKB") + " KB\n";
                msg += "接收数据包:\t" + netUseMap.get("rp").intValue() + "\n";
                msg += "发送流量:\t" + netUseMap.get("tKB") + " KB\n";
                msg += "发送数据包:\t" + netUseMap.get("tp").intValue() + "\n";
            }
        }
        reportMessage = msg;
    }

    public void serviceInit() {
        taskMessage = "暂无信息";
        testingMessage = "暂无信息";
        reportMessage = "等待测试结束……";
        testRun = false;
        reportDone = false;
        taskMessageDone = false;
        powerMap = null;
    }

    private void usbPowerStats() {
        int powerNum = 0;
        double powerTot = 0;
        while (testRun) {
            powerMapEach = Tools.getUsbPowerMap();
            if (powerMapEach != null) {
                powerTot += powerMapEach.get("usbP");
                powerNum ++;
                try {
                    Thread.sleep(45);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(tag, "单次电源信息读取失败！");
            }
        }

        // 平均功率
        if (powerNum == 0) {
            Log.e(tag, "USB功率统计失败");
        } else {
            powerMap = new HashMap<>();
            powerMap.put("power", powerTot / powerNum);
            powerMap.put("time", (double) (endTime - startTime) / powerNum);
        }
    }
}