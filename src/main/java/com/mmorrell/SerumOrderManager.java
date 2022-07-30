package com.mmorrell;

import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class SerumOrderManager {

    private final Account account;
    private double solAmount;

    public SerumOrderManager() {
        try {
            this.account = Account.fromJson(Files.readString(Paths.get("src/main/resources/mainnet.json")));
            log.info("Loaded wallet: " + account.getPublicKey().toBase58());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSolAmount(double solAmount) {
        this.solAmount = solAmount;
    }

    public void executeArb() {
        // get sol/usdc asks

        // get sol/usdt bids
    }
}
