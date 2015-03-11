package com.sci.skristminer;

import com.sci.skristminer.mine.Miner;
import com.sci.skristminer.mine.MinerListener;
import com.sci.skristminer.util.Utils;
import org.apache.commons.cli.*;

import java.text.DateFormat;
import java.text.NumberFormat;
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

    private boolean stopMining;
    private boolean startMining;
    private long nonce;

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
        System.out.println(this.date() + ":  Mining on address " + address + " with " + threads + " thread" + (threads > 1 ? "s" : "") + " (" + this.block + " " + this.work + ")");

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

                    if (this.stopMining)
                    {
                        this.stopMining = false;
                        this.stopMining();
                        continue;
                    }

                    if (this.miners.size() == this.threads)
                    {
                        if (!this.block.equals(Utils.getLastBlock()) || !this.work.equals(Utils.getWork()))
                        {
                            this.updateBalance();
                            this.stopMining();
                            this.startMining(0);
                            continue;
                        }

                        long rawSpeed = 0;
                        for (final Miner miner : this.miners)
                        {
                            if (!miner.isComplete())
                                rawSpeed += (miner.getNonce() - miner.getStartNonce()) / (this.secondsElapsed == 0 ? 1 : this.secondsElapsed);
                        }

                        this.totalSpeed += rawSpeed;

                        if (rawSpeed > this.highestSpeed)
                            this.highestSpeed = rawSpeed;

                        //@formatter:off
                        System.out.println(
                                this.date() + ":  " +
                                this.balance + " KST  " +
                                this.blocksMined + " Blocks Mined  " +
                                "(" + Utils.formatSpeed(rawSpeed) + " now, " +
                                Utils.formatSpeed(this.totalSpeed / (this.secondsElapsed == 0 ? 1 : this.secondsElapsed)) + " avg, " +
                                Utils.formatSpeed(this.highestSpeed) + " high)"
                        );
                        //@formatter:on
                    }
                }
            }
            else
            {
                if (this.startMining)
                {
                    this.startMining = false;
                    this.startMining(this.nonce);
                    this.nonce = 0;
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
            this.finishedMiners = 0;

            this.updateBalance();

            if (this.block == null || !this.block.equals(Utils.getLastBlock()))
            {
                this.block = Utils.getLastBlock();
                this.work = Utils.getWork();
                startingNonce = 0;
                System.out.println(this.date() + ":  New block (" + this.block + " " + this.work + ")");
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

            this.isMining = true;
        }
    }

    private void killThreads()
    {
        if (this.miners.size() > 0)
        {
            for (final Miner miner : this.miners)
                miner.stopMining();

            try
            {
                for (final Miner miner : this.miners)
                    miner.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            this.miners.clear();
        }
    }

    private void stopMining()
    {
        if (this.isMining)
        {
            this.isMining = false;
            this.killThreads();
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
                System.out.println(this.date() + ":  Thread " + finishedMiner.getID() + " solved block with nonce " + finishedMiner.getNonce());

                this.startMining = true;
                this.stopMining = true;
                this.nonce = 0;
                this.blocksMined++;
                return;
            }
        }

        if (this.finishedMiners == this.threads)
        {
            final Miner lastMiner = this.miners.get(this.miners.size() - 1);

            this.stopMining = true;
            this.startMining = true;

            if (this.block.equals(Utils.getLastBlock()))
            {
                System.out.println(this.date() + ":  Threads finished " + this.nonces + " nonces each. Starting at " + lastMiner.getNonce());
                this.nonce = lastMiner.getNonce();
            }
            else
            {
                this.nonce = 0;
            }
        }
    }

    private String date()
    {
        return this.dateFormat.format(new Date());
    }

    public int nonces()
    {
        return this.nonces;
    }
}