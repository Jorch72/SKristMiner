package com.sci.skristminer;

/**
 * @author sci4me
 */
public final class Miner implements Runnable
{
    private final MinerListener listener;
    private final int id;

    private final String block;
    private final String minerID;
    private final long startNonce;
    private final int nonces;

    private volatile long nonce;
    private volatile boolean isComplete;
    private volatile boolean solvedBlock;

    private volatile boolean stop;

    public Miner(final MinerListener listener, final int id, final String block, final String minerID, final long startNonce)
    {
        this.listener = listener;
        this.id = id;
        this.block = block;
        this.minerID = minerID;
        this.startNonce = startNonce;
        this.nonce = this.startNonce;
        this.nonces = SKristMiner.instance().nonces();
        this.isComplete = false;
        this.solvedBlock = false;
    }

    @Override
    public void run()
    {
        final String minerBlock = this.minerID + this.block;
        String newBlock = Utils.subSHA256(minerBlock + this.nonce, 12);

        for (int hash = 0; hash < this.nonces && newBlock.compareTo(this.block) >= 0; hash++, this.nonce++)
        {
            if (this.stop)
                return;

            newBlock = Utils.subSHA256(minerBlock + this.nonce, 12);
        }

        if (newBlock.compareTo(this.block) < 0)
        {
            Utils.submitSolution(this.minerID, this.nonce - 1);
            this.solvedBlock = true;
        }

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

    public void stop()
    {
        this.stop = true;
    }
}