package com.mmorrell;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class App {
    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // create order manager (pkey, rpcclient)
        SerumOrderManager orderManager = new SerumOrderManager();
        executor.submit(() -> {
            while (true) {
                // srm strat
                orderManager.executeArb();
                Thread.sleep(400L);
            }
        });
        executor.submit(() -> {
            while (true) {
                // btc start
                orderManager.executeArb2();
                Thread.sleep(400L);
            }
        });

        log.info("Serum arb bot started.");
    }
}
