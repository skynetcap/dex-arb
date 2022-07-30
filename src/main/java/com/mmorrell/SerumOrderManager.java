package com.mmorrell;

import ch.openserum.serum.manager.SerumManager;
import ch.openserum.serum.model.*;
import ch.openserum.serum.program.SerumProgram;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.programs.MemoProgram;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.AccountInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Slf4j
public class SerumOrderManager {

    private final RpcClient client;
    private final SerumManager serumManager;
    private final Account account;
    private float srmAmount;

    // sol/usdc
    private final PublicKey srmUsdcMarketPubkey = PublicKey.valueOf("ByRys5tuUWDgL73G8JBAEfkdFf8JWBzPBDHsBVQ5vbQA");
    private final Market srmUsdcMarket;

    // sol/usdt
    private final PublicKey srmUsdtMarketPubkey = PublicKey.valueOf("AtNnsY1AyRERWJ8xCskfz38YdvruWVJQUVXgScC1iPb");
    private final Market srmUsdtMarket;

    // finals
    private final PublicKey srmUsdcOoa = PublicKey.valueOf("G2VagULnBacMoQ3Umc12ut9fs5kGGyYr92LwUUbvhhe7");
    private final PublicKey srmUsdtOoa = PublicKey.valueOf("7nwSNT96eeVoTMA3f1tPDjtcrgqHLduESorpmU3qWXbv");

    private final PublicKey usdcWallet = PublicKey.valueOf("4oKFPF8pAELch6P1owmTgk8C5oweVEMjN6qbS1YZKHZu");
    private final PublicKey srmWallet = PublicKey.valueOf("3Kdg8eX62TMLN74jgndMHDkipXC7QrXcqxiWhwnPEvBq");
    private final PublicKey usdtWallet = PublicKey.valueOf("AdREyCYJXzJEdz5oFgPLMTj4cBmFcoMEsva5diaeUUaH");

    public SerumOrderManager() {
        try {
            this.account = Account.fromJson(Files.readString(Paths.get("src/main/resources/mainnet.json")));
            log.info("Loaded wallet: " + account.getPublicKey().toBase58());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.client = new RpcClient("https://node.openserum.io/");
        this.srmUsdcMarket = new MarketBuilder()
                .setClient(client)
                .setPublicKey(srmUsdcMarketPubkey)
                .setRetrieveOrderBooks(true)
                .build();
        log.info("Loaded market (SRM/USDC): " + srmUsdcMarket.getOwnAddress().toBase58());
        this.srmUsdtMarket = new MarketBuilder()
                .setClient(client)
                .setPublicKey(srmUsdtMarketPubkey)
                .setRetrieveOrderBooks(true)
                .build();
        log.info("Loaded market (SRM/USDT): " + srmUsdtMarket.getOwnAddress().toBase58());

        this.serumManager = new SerumManager(client);
    }

    public void setSrmAmount(float srmAmount) {
        this.srmAmount = srmAmount;
    }

    // Buy on SRM/USDT
    // Sell on SRM/USDC
    // Convert USDC to USDT
    public void executeArb() {
        // SRM/USDT asks
        AccountInfo obAccount = null;
        try {
            obAccount = client.getApi().getAccountInfoProcessed(
                    srmUsdtMarket.getAsks()
            );
        } catch (RpcException e) {
            log.error("Unable to get ask account.");
        }

        if (obAccount == null) {
            log.error("null ask account.");
            return;
        }

        byte[] askData = Base64.getDecoder().decode(
                obAccount.getValue().getData().get(0)
        );

        OrderBook askOrderBook = OrderBook.readOrderBook(askData);
        askOrderBook.setBaseLotSize(srmUsdtMarket.getBaseLotSize());
        askOrderBook.setQuoteLotSize(srmUsdtMarket.getQuoteLotSize());
        askOrderBook.setBaseDecimals(srmUsdtMarket.getBaseDecimals());
        askOrderBook.setQuoteDecimals(srmUsdtMarket.getQuoteDecimals());

        Order bestAsk = askOrderBook.getBestAsk();

        // SOL/USDC bids
        try {
            obAccount = client.getApi().getAccountInfoProcessed(
                    srmUsdcMarket.getBids()
            );
        } catch (RpcException e) {
            log.error("Unable to get bid account.");
        }

        if (obAccount == null) {
            log.error("null bid account.");
            return;
        }

        byte[] bidData = Base64.getDecoder().decode(
                obAccount.getValue().getData().get(0)
        );

        OrderBook bidOrderBook = OrderBook.readOrderBook(bidData);
        bidOrderBook.setBaseLotSize(srmUsdcMarket.getBaseLotSize());
        bidOrderBook.setQuoteLotSize(srmUsdcMarket.getQuoteLotSize());
        bidOrderBook.setBaseDecimals(srmUsdcMarket.getBaseDecimals());
        bidOrderBook.setQuoteDecimals(srmUsdcMarket.getQuoteDecimals());
        Order bestBid = bidOrderBook.getBestBid();

        float bestBidPrice = bestBid.getFloatPrice();
        float bestAskPrice = bestAsk.getFloatPrice();

        log.info(
                String.format(
                        "$%.4f / $%.4f [%.2f/%.2f], %d",
                        bestBidPrice,
                        bestAskPrice,
                        bestBid.getFloatQuantity(),
                        bestAsk.getFloatQuantity(),
                        obAccount.getContext().getSlot()
                )
        );

        if (bestBidPrice > bestAskPrice) {
            log.info("!!!! ARB DETECTED !!!!");
            log.info("Executing transaction.");
            final Transaction transaction = new Transaction();

            long buyOrderId = 11133711L;
            final Order buyOrder = Order.builder()
                    .floatPrice(bestAskPrice)
                    .floatQuantity(srmAmount)
                    .clientOrderId(buyOrderId)
                    .orderTypeLayout(OrderTypeLayout.IOC)
                    .selfTradeBehaviorLayout(SelfTradeBehaviorLayout.DECREMENT_TAKE)
                    .buy(true).build();
            serumManager.setOrderPrices(buyOrder, srmUsdtMarket);

            long sellOrderId = 1142011L;
            final Order sellOrder = Order.builder()
                    .floatPrice(bestBidPrice)
                    .floatQuantity(srmAmount)
                    .clientOrderId(sellOrderId)
                    .orderTypeLayout(OrderTypeLayout.IOC)
                    .selfTradeBehaviorLayout(SelfTradeBehaviorLayout.DECREMENT_TAKE)
                    .buy(false).build();
            serumManager.setOrderPrices(sellOrder, srmUsdcMarket);

            transaction.addInstruction(
                    SerumProgram.placeOrder(
                            account,
                            usdtWallet,
                            srmUsdtOoa,
                            srmUsdtMarket,
                            buyOrder
                    )
            );

            transaction.addInstruction(
                    SerumProgram.consumeEvents(
                            account.getPublicKey(),
                            List.of(
                                    srmUsdtOoa
                            ),
                            srmUsdtMarket,
                            srmWallet,
                            usdtWallet
                    )
            );

            transaction.addInstruction(
                    SerumProgram.placeOrder(
                            account,
                            srmWallet,
                            srmUsdcOoa,
                            srmUsdcMarket,
                            sellOrder
                    )
            );

            transaction.addInstruction(
                    SerumProgram.consumeEvents(
                            account.getPublicKey(),
                            List.of(
                                    srmUsdcOoa
                            ),
                            srmUsdcMarket,
                            srmWallet,
                            usdcWallet
                    )
            );

            transaction.addInstruction(
                    MemoProgram.writeUtf8(
                            account.getPublicKey(),
                            "Liquidity by openserum.io"
                    )
            );

            log.info("Sending TX.");

            try {
                log.info("TX: " + client.getApi().sendTransaction(transaction, account));
            } catch (RpcException ex) {
                log.error(ex.getMessage());
            }

            // CONVERT USDC BACK INTO USDT
            // let's test this first.
        }

    }
}
