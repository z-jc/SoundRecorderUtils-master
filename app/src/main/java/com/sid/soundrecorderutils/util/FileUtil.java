package com.sid.soundrecorderutils.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileUtil {
    /**
     * 遍历所有文件
     */
    public static Map<String, String> getFileName(final String fileAbsolutePath) {
        Map<String, String> map = new HashMap<>();
        File file = new File(fileAbsolutePath);
        File[] subFile = file.listFiles();
        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            // 判断是否为文件夹
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                map.put(String.valueOf(iFileLength),filename);
            }
        }
        return map;
    }
}
