package com.zixiao_bios.overheadtool;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;

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
     * 使用 dumpsys netstats detail 命令查询指定uid进程的网络使用情况
     *
     * @param uid     要查询的进程的uid
     * @param context context
     * @return 该进程的网络使用情况。若该uid存在，则为三行数据；若不存在，则为null
     */
    public static String findUidNetstats(int uid, Context context) {
        String originData = cmd("dumpsys netstats detail");
        if (originData == null) {
            // 命令执行结果为空
            MyDisplay.toast("错误！命令 \"dumpsys netstats detail\" 无返回结果！");
            Log.e(tag, "命令 \"dumpsys netstats detail\" 无返回结果");
            return null;
        }

        // 命令执行结果非空
        // uid=uid的索引
        int uidIndex = originData.indexOf("uid=" + uid);
        if (uidIndex == -1) {
            // 该uid不存在
            MyDisplay.toast("错误！ uid=" + uid + "的进程不存在！");
            Log.e(tag, "uid=" + uid + " 的进程不存在");
            return null;
        }

        // 找到该uid的信息
        // 起始字符的索引
        int startIndex = originData.substring(0, uidIndex).lastIndexOf("\n");
        String res = originData.substring(startIndex);
        int endIndex = res.indexOf("UID tag stats");
        res = res.substring(0, endIndex);

        // 下一个uid=xxx的索引
//        int endUidIndex = originData.substring(uidIndex + 1).indexOf("uid=") + uidIndex + 1;

        // 结束字符的索引
//        int endIndex = originData.substring(0, endUidIndex).lastIndexOf("\n");

//        Log.e("test", originData.substring(startIndex, endIndex));
//        Log.i("test", originData.substring(startIndex));

        return res;
    }

    /**
     * 查找给定uid和set的网络使用情况
     * @param uid uid
     * @param set set，取值为"DEFAULT"或”FOREGROUND“
     * @return
     */
    public static String findUidSetNetStatus(int uid, String set){
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
            MyDisplay.toast("错误！ uid=" + uid + "且set=" + set + "的进程不存在！");
            Log.e(tag, "错误！ uid=" + uid + "且set=" + set + "的进程不存在！");
            return null;
        }

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

            Log.e(tag, "working:" + working);
        }
        return null;
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