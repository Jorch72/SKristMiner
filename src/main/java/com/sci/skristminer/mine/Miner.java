package com.sci.skristminer.mine;

import com.sci.skristminer.SKristMiner;
import com.sci.skristminer.util.SHA256;
import com.sci.skristminer.util.Utils;

import java.nio.charset.StandardCharsets;

/**
 * @author sci4me
 */
public final class Miner extends Thread
{
    private final MinerListener listener;
    private final int id;

    private final String block;
    private final long work;
    private final String minerID;
    private final long startNonce;
    private final int nonces;
    private final String addr;
    
    private volatile long nonce;
    private volatile boolean isComplete;
    private volatile boolean solvedBlock;

    private volatile boolean stop;

    public Miner(final MinerListener listener, final int id, final String block, final long work, final String minerID, final long startNonce, final String addr)
    {
        this.listener = listener;
        this.id = id;
        this.block = block;
        this.work = work;
        this.minerID = minerID;
        this.startNonce = startNonce;
        this.nonce = this.startNonce;
        this.nonces = SKristMiner.instance().nonces();
        this.isComplete = false;
        this.solvedBlock = false;
        this.addr = addr;
    }

    @Override
    public void run()
    {
        final String minerBlock = this.minerID + this.block;
        long lNewBlock = Utils.hashToLong(SHA256.digest(Utils.getBytes(minerBlock + this.nonce + addr)));

        for (int hash = 0; hash < this.nonces && lNewBlock >= this.work; hash++, this.nonce++)
        {
            if (this.stop)
                return;

            lNewBlock = Utils.hashToLong(SHA256.digest(Utils.getBytes(minerBlock + this.nonce + addr)));
        }

        this.solvedBlock = Utils.submitSolution(this.minerID, this.nonce - 1);
        this.isComplete = true;

        this.listener.onFinish(this);
    }

    public boolean isComplete()
    {
        return this.isComplete;
    }

    public boolean didSolveBlock()
    {
        return this.solvedBlock;
    }

    public String getCurrentBlock()
    {
        return this.block;
    }

    public long getStartNonce()
    {
        return this.startNonce;
    }

    public long getNonce()
    {
        return this.nonce;
    }

    public int getID()
    {
        return this.id;
    }

    public void stopMining()
    {
        this.stop = true;
    }
}