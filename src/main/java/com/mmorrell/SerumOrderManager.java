package com.mmorrell;

import ch.openserum.serum.manager.SerumManager;
import ch.openserum.serum.model.Market;
import ch.openserum.serum.model.MarketBuilder;
import ch.openserum.serum.model.Order;
import ch.openserum.serum.model.OrderBook;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.AccountInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Slf4j
public class SerumOrderManager {

    private final RpcClient client;
    private final Account account;
    private double solAmount;

    // sol/usdc
    private final PublicKey solUsdcMarketPukey = PublicKey.valueOf("9wFFyRfZBsuAha4YcuxcXLKwMxJR43S7fPfQLusDBzvT");
    private final Market solUsdcMarket;

    // sol/usdt
    private final PublicKey solUsdtMarketPukey = PublicKey.valueOf("HWHvQhFmJB3NUcu1aihKmrKegfVxBEHzwVX6yZCKEsi1");
    private final Market solUsdtMarket;

    public SerumOrderManager() {
        try {
            this.account = Account.fromJson(Files.readString(Paths.get("src/main/resources/mainnet.json")));
            log.info("Loaded wallet: " + account.getPublicKey().toBase58());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.client = new RpcClient("https://node.openserum.io/");
        this.solUsdcMarket = new MarketBuilder()
                .setClient(client)
                .setPublicKey(solUsdcMarketPukey)
                .setRetrieveOrderBooks(true)
                .build();
        log.info("Loaded market (SOL/USDC): " + solUsdcMarket.getOwnAddress().toBase58());
        this.solUsdtMarket = new MarketBuilder()
                .setClient(client)
                .setPublicKey(solUsdtMarketPukey)
                .setRetrieveOrderBooks(true)
                .build();
        log.info("Loaded market (SOL/USDT): " + solUsdtMarket.getOwnAddress().toBase58());
    }

    public void setSolAmount(double solAmount) {
        this.solAmount = solAmount;
    }

    public void executeArb() {
        // get sol/usdc asks
        log.info("Getting SOL/USDC asks.");

        AccountInfo askAccount = null;
        try {
            askAccount = client.getApi().getAccountInfo(
                    solUsdcMarket.getAsks()
            );
        } catch (RpcException e) {
            log.error("Unable to get ask account.");
        }

        if (askAccount == null) {
            log.error("null ask account.");
            return;
        }

        byte[] askData = Base64.getDecoder().decode(
                askAccount.getValue().getData().get(0)
        );

        OrderBook askOrderBook = OrderBook.readOrderBook(askData);
        askOrderBook.setBaseLotSize(solUsdcMarket.getBaseLotSize());
        askOrderBook.setQuoteLotSize(solUsdcMarket.getQuoteLotSize());
        askOrderBook.setBaseDecimals(solUsdcMarket.getBaseDecimals());
        askOrderBook.setQuoteDecimals(solUsdcMarket.getQuoteDecimals());

        Order bestAsk = askOrderBook.getBestAsk();
        log.info("Best ask: " + bestAsk.toString() + ", " + askAccount.getContext().getSlot());

        //log.info(Arrays.toString(bidOrderBook.getOrders().toArray()));


        // get sol/usdt bids
    }
}
