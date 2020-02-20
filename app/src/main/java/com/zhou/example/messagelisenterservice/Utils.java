package com.zhou.example.messagelisenterservice;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

//    private static final File notifycationFilePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());


    static void putStr(Context context, String value) {
        if (context == null) {
            return;
        }
        File notifycationFilePath = context.getExternalFilesDir("log");
        if (notifycationFilePath == null || !canWrite(notifycationFilePath)) return;

        String notifycationFileName = "notifycation_file.txt";
        File file = new File(notifycationFilePath, notifycationFileName);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file, true), 1024);
            out.write(value);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean canWrite(File notifycationFilePath) {
        return notifycationFilePath.canWrite();
    }
}
