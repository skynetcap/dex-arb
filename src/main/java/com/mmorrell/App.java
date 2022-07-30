package com.mmorrell;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class App {
    public static void main(String[] args) {

        ExecutorService executor = Executors.newCachedThreadPool();

        // create order manager (pkey, rpcclient)
        SerumOrderManager orderManager = new SerumOrderManager();

        // configure strategy parameters
        orderManager.setSolAmount(0.5);

        // executorservice, start runnable containing loop'
        executor.submit(() -> {
            while (true) {
                orderManager.executeArb();
                Thread.sleep(500L);
            }
        });

        System.out.println("Serum arb bot started.");
    }
}
