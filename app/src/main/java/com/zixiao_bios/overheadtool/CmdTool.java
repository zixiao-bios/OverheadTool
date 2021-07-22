package com.zixiao_bios.overheadtool;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;

public class CmdTool {
    public static final String tag = "cmd";

    /**
     * 向命令行中输入命令，并返回结果。自动向输入命令的末尾添加换行符，命令以root权限执行
     *
     * @param cmdStr 命令行输入的命令
     * @return 命令的执行结果
     */
    public static String cmd(String cmdStr) {
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            Process p = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());

            // 命令行输入
            dos.writeBytes(cmdStr + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();

            // 命令行输出
            StringBuilder res = new StringBuilder();
            String line = dis.readLine();
            while (line != null) {
                res.append(line).append("\n");
                line = dis.readLine();
            }
            return res.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }
                if (dis != null) {
                    dis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 使用 dumpsys netstats detail 命令查询指定uid进程的网络使用情况，返回netstatsMap
     *
     * @param uid     要查询的进程的uid
     * @return netstatsMap；若uid该不存在，则为null
     */
    public static HashMap<String, Long> findUidNetstats(int uid) {
        HashMap<String, Long> setDefault = findUidSetNetStats(uid, "DEFAULT");
        HashMap<String, Long> setForeground = findUidSetNetStats(uid, "FOREGROUND");

        if (setDefault == null && setForeground == null) {
            MyDisplay.toast("错误！指定进程不存在！");
            Log.e(tag, "错误！指定进程不存在！");
            return null;
        } else if (setDefault == null) {
            return setForeground;
        } else if (setForeground == null) {
            return setDefault;
        } else {
            return plusNetstatsMap(setDefault, setForeground);
        }
    }

    /**
     * 查找给定uid和set的网络使用情况，返回netstatsMap
     * @param uid uid
     * @param set set，取值为"DEFAULT"或”FOREGROUND“
     * @return netstatsMap
     */
    public static HashMap<String, Long> findUidSetNetStats(int uid, String set){
        String originData = cmd("dumpsys netstats detail");
        if (originData == null) {
            // 命令执行结果为空
            MyDisplay.toast("错误！命令 \"dumpsys netstats detail\" 无返回结果！");
            Log.e(tag, "命令 \"dumpsys netstats detail\" 无返回结果");
            return null;
        }

        int startIndex, endIndex, uidIndex, endUidIndex;
        endIndex = originData.indexOf("UID tag stats");

        // 剩余待处理字符串
        String left = originData.substring(0, endIndex);
        String working;

        uidIndex = left.indexOf("uid=" + uid + " set=" + set);
        if (uidIndex == -1) {
            // 字符串中uid、set不存在
//            MyDisplay.toast("错误！ uid=" + uid + "且set=" + set + "的进程不存在！");
//            Log.e(tag, "错误！ uid=" + uid + "且set=" + set + "的进程不存在！");
            return null;
        }

        // 初始化存储数据的netstatsMap
        HashMap<String, Long> netstatsMap = new HashMap<>();
        netstatsMap.put("rb", 0L);
        netstatsMap.put("rp", 0L);
        netstatsMap.put("tb", 0L);
        netstatsMap.put("tp", 0L);

        // 开始处理
        while (true) {
            // 查找剩余字符串
            uidIndex = left.indexOf("uid=" + uid + " set=" + set);
            if (uidIndex == -1) {
                // 剩余字符串中uid、set不存在，说明处理完成，跳出循环
                break;
            }

            // 剩余字符串包含字符

            // 剩余字符串中匹配项的第一行
            startIndex = left.substring(0, uidIndex).lastIndexOf("\n") + 1;

            // 剩余字符串中匹配项的下一项
            endUidIndex = left.substring(uidIndex + 1).indexOf("uid=");

            if (endUidIndex == -1) {
                // 没有下一项了
                working = left.substring(startIndex);

                // 此次循环后退出
                left = "";
            } else {
                // 有下一项
                // 剩余字符串中匹配项的最后一行，即下一项的上一行
                endIndex = left.substring(0, endUidIndex).lastIndexOf("\n");

                // 从剩余字符串中取出本次处理的字符串
                working = left.substring(startIndex, endIndex);

                // 从剩余字符串中删除本次处理的字符串
                left = left.substring(endIndex);
            }

//            Log.e(tag, "working:\n" + working);
            netstatsMap = plusNetstatsMap(netstatsMap, countWorkingString(working));
        }
        return netstatsMap;
    }

    /**
     * 将workingString转为Map数据
     * ------------------------------workingString示例----------------------------------------------
     *       ident=[{type=WIFI, subType=COMBINED, networkId="1107", metered=false, defaultNetwork=true}] uid=10142 set=FOREGROUND tag=0x0
     *         NetworkStatsHistory: bucketDuration=7200
     *           st=1626933600 rb=3533 rp=60 tb=6247 tp=106 op=0
     *           st=1626940800 rb=11127 rp=200 tb=22005 tp=389 op=0
     * ---------------------------------------------------------------------------------------------
     * @param working workingString
     * @return HashMap<String, Long>
     */
    private static HashMap<String, Long> countWorkingString(String working){
        long rb = 0, rp = 0, tb = 0, tp = 0;

        // 去除前两行
        working = working.substring(working.indexOf("\n") + 1);
        working = working.substring(working.indexOf("\n") + 1);

        // 循环处理每一行
        String line;
        while (working.contains("\n")) {
            line = working.substring(0, working.indexOf("\n"));
            working = working.substring(working.indexOf("\n") + 1);

            // 从每一行中读取数据
            rb += countKeyValue(line.substring(line.indexOf("rb")));
            rp += countKeyValue(line.substring(line.indexOf("rp")));
            tb += countKeyValue(line.substring(line.indexOf("tb")));
            tp += countKeyValue(line.substring(line.indexOf("tp")));
        }

        HashMap<String, Long> valueMap = new HashMap<>();
        valueMap.put("rb", rb);
        valueMap.put("rp", rp);
        valueMap.put("tb", tb);
        valueMap.put("tp", tp);

//        Log.e("rb", valueMap.toString());
        return valueMap;
    }

    /**
     * 从符合"a=xxx ..."格式的字符串中，以long格式读取xxx
     * @param keyValue "a=xxx ..."
     * @return xxx
     */
    private static long countKeyValue(String keyValue){
        int startIndex = keyValue.indexOf("=") + 1;
        int endIndex = keyValue.indexOf(" ");
        return Long.parseLong(keyValue.substring(startIndex, endIndex));
    }

    // 计算两个netstatsMap的和
    public static HashMap<String, Long> plusNetstatsMap(HashMap<String, Long> map1, HashMap<String, Long> map2) {
        if (map1 == null || map2 == null) {
            return null;
        }

        HashMap<String, Long> res = new HashMap<>();
        res.put("rb", map1.get("rb") + map2.get("rb"));
        res.put("rp", map1.get("rp") + map2.get("rp"));
        res.put("tb", map1.get("tb") + map2.get("tb"));
        res.put("tp", map1.get("tp") + map2.get("tp"));
        return res;
    }

    // 计算两个netstatsMap的差，map1-map2
    public static HashMap<String, Long>subNetstatsMap(HashMap<String, Long> map1, HashMap<String, Long> map2){
                if (map1 == null || map2 == null) {
            return null;
        }

        HashMap<String, Long> res = new HashMap<>();
        res.put("rb", map1.get("rb") - map2.get("rb"));
        res.put("rp", map1.get("rp") - map2.get("rp"));
        res.put("tb", map1.get("tb") - map2.get("tb"));
        res.put("tp", map1.get("tp") - map2.get("tp"));
        return res;
    }

    /**
     * 通过pid查找uid，失败时返回-1
     * @param pid pid
     * @return uid
     */
    public static int findUidByPid(int pid) {
        String originData = cmd("cat /proc/" + pid + "/status");
        if (originData == null) {
            MyDisplay.toast("错误！uid查找失败！");
            Log.e(tag, "错误！uid查找失败！");
            return -1;
        }

        int startIndex = originData.indexOf("Uid");
        String res = originData.substring(startIndex);
        startIndex = res.indexOf("\t") + 1;
        res = res.substring(startIndex);
        int endIndex = res.indexOf("\t");
        res = res.substring(0, endIndex);

        return Integer.parseInt(res);
    }
}
