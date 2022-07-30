package com.mmorrell;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class App {
    public static void main(String[] args) {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        // create order manager (pkey, rpcclient)
        SerumOrderManager orderManager = new SerumOrderManager();
        orderManager.setSrmAmount(50f);
        executor.submit(() -> {
            while (true) {
                orderManager.executeArb();
                Thread.sleep(400L);
            }
        });

        log.info("Serum arb bot started.");
    }
}
