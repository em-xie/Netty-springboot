package com.lld.im.tcp;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.service.LimServer;
import com.lld.im.tcp.service.LimWebSocketServer;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @作者：xie
 * @时间：2023/4/13 21:18
 */
public class Starter {

    public static void main(String[] args) throws FileNotFoundException {

        if(args.length > 0){
            start(args[0]);
        }
    }
    private static void start(String path){
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(path);
            BootstrapConfig bootstrapConfig = yaml.loadAs(inputStream, BootstrapConfig.class);
            new LimServer(bootstrapConfig.getLim()).start();
            new LimWebSocketServer(bootstrapConfig.getLim()).start();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(500);
        }
    }
}
