package com.app.fizido_pay.fizido_pay.Sunyard.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class SilenceInstall {
    // 静默安装，1-安装成功，或没有升级文件，2-升级安装出现异常，-1-程序异常
    public static int silentInstall(File file) {
        int result;
        try {
            if (file.length() < 0 || !file.exists() || !file.isFile()) {
                return -1;
            }
            String[] args = {"pm", "install", "-i", "com.sunyard.i80newsdk", "-r",
                    file.getAbsolutePath()};
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            Process process = null;
            BufferedReader successResult = null;
            BufferedReader errorResult = null;
            StringBuilder successMsg = new StringBuilder();
            StringBuilder errorMsg = new StringBuilder();
            try {
                process = processBuilder.start();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = 2;
            } finally {
                try {
                    if (successResult != null) {
                        successResult.close();
                    }
                    if (errorResult != null) {
                        errorResult.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (process != null) {
                    process.destroy();
                }
            }
            if (successMsg.toString().contains("Success") || successMsg.toString().contains("success")) {
                result = 1;
                Log.i("LZF", successMsg.toString());
            } else {
                result = 2;
                Log.i("LZF", errorMsg.toString());
            }
        } catch (Exception e) {
            result = -1;
            e.printStackTrace();
        }
        return result;
    }
}
