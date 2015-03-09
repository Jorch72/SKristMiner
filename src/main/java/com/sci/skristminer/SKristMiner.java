package com.sci.skristminer;

import com.sci.skristminer.mine.Miner;
import com.sci.skristminer.mine.MinerListener;
import com.sci.skristminer.util.Utils;
import org.apache.commons.cli.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author sci4me
 */
public final class SKristMiner implements MinerListener, Runnable
{
    public static void main(final String[] args) throws ParseException
    {
        final Options options = new Options();

        final Option threads = new Option("threads", true, "number of threads");
        final Option nonces = new Option("nonces", true, "number of nonces per thread");
        options.addOption(threads);
        options.addOption(nonces);

        final CommandLineParser parser = new BasicParser();
        final CommandLine cmd = parser.parse(options, args);

        final String[] fArgs = cmd.getArgs();

        if (fArgs.length < 1)
        {
            System.out.println("Usage: skristminer <address> [--threads=n, --nonce-offset=n]");
            return;
        }

        SKristMiner.instance().start(fArgs[0], Integer.valueOf(cmd.getOptionValue("threads", "1")), Integer.valueOf(cmd.getOptionValue("nonces", "10000000")));
    }

    private static SKristMiner instance;

    public static SKristMiner instance()
    {
        if (SKristMiner.instance == null)
            SKristMiner.instance = new SKristMiner();
        return SKristMiner.instance;
    }

    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private String address;
    private int threads;
    private int nonces;
    private long highestSpeed;
    private long totalSpeed;

    private long secondsElapsed;
    private String block;
    private String work;
    private int finishedMiners;
    private int blocksMined;
    private volatile boolean isMining;
    private List<Miner> miners;

    private long balance;

    private SKristMiner()
    {
        this.miners = new ArrayList<>();
    }

    private void start(final String address, final int threads, final int nonces)
    {
        if (!Utils.isMinerValid(address))
        {
            System.out.println("Invalid address " + address);
            return;
        }

        this.address = address;
        this.threads = threads;
        this.nonces = nonces;

        this.block = Utils.getLastBlock();
        this.work = Utils.getWork();
        System.out.println("Mining on address " + address + " with " + threads + " thread" + (threads > 1 ? "s" : "") + " (" + this.block + " " + this.work + ")");

        this.startMining(0);
        final Thread main = new Thread(this);
        main.setPriority(Thread.NORM_PRIORITY);
        main.start();
    }

    @Override
    public void run()
    {
        long timer = System.currentTimeMillis();
        while (true)
        {
            if (this.isMining)
            {
                final long diff = System.currentTimeMillis() - timer;
                if (diff >= 1000)
                {
                    timer += diff;
                    this.secondsElapsed++;

                    if (this.miners.size() == this.threads)
                    {
                        long rawSpeed = 0;
                        for (final Miner miner : this.miners)
                        {
                            if (!miner.isComplete())
                                rawSpeed += (miner.getNonce() - miner.getStartNonce()) / this.secondsElapsed;
                        }

                        this.totalSpeed += rawSpeed;

                        if (rawSpeed > this.highestSpeed)
                            this.highestSpeed = rawSpeed;

                        //@formatter:off
                        final Date date = new Date();
                        System.out.println(
                                this.dateFormat.format(date) + ":  " +
                                this.balance + " KST  " +
                                this.blocksMined + " Blocks Mined  " +
                                "(" + Utils.formatSpeed(rawSpeed) + " now, " +
                                Utils.formatSpeed(this.totalSpeed / this.secondsElapsed) + " avg, " +
                                Utils.formatSpeed(this.highestSpeed) + " high)"
                        );
                        //@formatter:on
                    }
                }
            }
        }
    }

    private void updateBalance()
    {
        this.balance = Utils.getBalance(this.address);
    }

    private void startMining(final long startNonce)
    {
        if (!this.isMining)
        {
            long startingNonce = startNonce;

            this.secondsElapsed = 0;
            this.totalSpeed = 0;
            this.updateBalance();

            this.isMining = true;
            this.finishedMiners = 0;

            if (this.block == null || !this.block.equals(Utils.getLastBlock()))
            {
                this.block = Utils.getLastBlock();
                this.work = Utils.getWork();
                startingNonce = 0;
                System.out.println("New block: " + this.block + "  work: " + this.work);
            }

            for (int miner = 0; miner < this.threads; miner++)
            {
                final Miner miner_ = new Miner(this, miner, this.block, this.work, this.address, startingNonce + this.nonces * miner);
                this.miners.add(miner_);
                final Thread minerThread = new Thread(miner_);
                minerThread.setPriority(Thread.MAX_PRIORITY);
                minerThread.setDaemon(true);
                minerThread.start();
            }
        }
    }

    private void stopMining()
    {
        if (this.isMining)
        {
            this.isMining = false;

            for (final Miner miner : this.miners)
                miner.stop();

            this.miners.clear();
        }
    }

    @Override
    public void onFinish(final Miner finishedMiner)
    {
        this.finishedMiners++;

        for (int i = 0; i < this.miners.size(); i++)
        {
            final Miner miner = this.miners.get(i);
            if (miner.didSolveBlock())
            {
                System.out.println("Thread " + finishedMiner.getID() + " solved block with nonce " + finishedMiner.getNonce());

                this.stopMining();
                this.blocksMined++;
                this.startMining(0);
                return;
            }
        }

        if (this.finishedMiners == this.threads)
        {
            final Miner lastMiner = this.miners.get(this.miners.size() - 1);

            this.stopMining();

            if (this.block.equals(Utils.getLastBlock()))
            {
                System.out.println("Threads finished " + this.nonces + " nonces each. Starting at " + lastMiner.getNonce());
                this.startMining(lastMiner.getNonce());
            }
            else
            {
                this.startMining(0);
            }
        }
    }

    public int nonces()
    {
        return this.nonces;
    }
}