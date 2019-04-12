package com.skloda.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @Author: jiangkun
 * @Description: 使用原生java.io.stream和java.nio.channel操作文件，底层其实是一样的实现
 * @Date: Created in 2019-03-29 23:47
 */
public class FileOperationUtil {

    /**
     * 使用NIO提供的FileChannel读写文件
     * @param source 原路径
     * @param target 目标路径
     */
    public static void copyFileByChannel(String source, String target) {
        //1 声明源文件和目标文件
        try (FileChannel inChannel = FileChannel.open(Paths.get(source), StandardOpenOption.READ);
             FileChannel outChannel = FileChannel.open(Paths.get(target), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            //2 获得容器buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            //3 判断是否读完文件，最后一次读到EOF文件末尾返回-1
            while (inChannel.read(buffer) != -1) {
                //4 重设一下buffer的position=0，limit=position
                buffer.flip();
                //5 开始写
                outChannel.write(buffer);
                //6 写完要重置buffer，重设position=0,limit=capacity
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用NIO提供的Stream读写文件
     * @param source 原路径
     * @param target 目标路径
     */
    public static void copyFileByStream(String source, String target) {
        //1 声明源文件和目标文件
        try (FileInputStream fis = new FileInputStream(new File(source));
             FileOutputStream fos = new FileOutputStream(new File(target))) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String source = System.getProperty("user.dir") + "/src/main/resources/my.txt";
        String target1 = System.getProperty("user.dir") + "/src/main/resources/my-copy1.txt";
        String target2 = System.getProperty("user.dir") + "/src/main/resources/my-copy2.txt";

        copyFileByChannel(source, target1);
        copyFileByStream(source, target2);
    }
}
