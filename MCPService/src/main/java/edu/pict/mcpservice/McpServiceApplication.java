package edu.pict.mcpservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

//@SpringBootApplication
//@EnableFeignClients
//public class McpServiceApplication {
//
//    public static void main(String[] args) {
//        SpringApplication.run(McpServiceApplication.class, args);
//    }
//}


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/******************************************************************************
 *
 * Online Java Compiler.
 * Code, Compile, Run and Debug java program online.
 * Write your code in this editor and press "Run" button to execute it.
 *
 *******************************************************************************/

public class Main {

    // private synchronized int

    private static ExecutorService poolExecutor = Executors.newFixedThreadPool(100);
    private static void func(int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(port));
        s.close();
    }

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < 100000; i++) {
            list.add(i);
        }

        for (int port : list) {
            poolExecutor.submit(
                    new Runnable() {

                        @Override
                        public void run() {
                            try {
                                func(port);
                            } catch (Exception ioe) {
                                System.out.println("Port: " + port + " Is closed ...");
                            }
                        }

                    }
            );
        }
        poolExecutor.shutdown();
    }
}