package com.sci.skristminer.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sci4me
 */
public final class Utils
{
    public static final String KRIST_SYNC_LINK = get("https://raw.githubusercontent.com/BTCTaras/kristwallet/master/staticapi/syncNode").get(0) + "?";
    public static final String LAST_BLOCK_LINK = KRIST_SYNC_LINK + "lastblock";
    public static final String GET_WORK_LINK = KRIST_SYNC_LINK + "getwork";
    public static final String BALANCE_LINK_BASE = KRIST_SYNC_LINK + "getbalance=";
    private static final DecimalFormat format = new DecimalFormat("0.00");

    public static String formatSpeed(final long rawSpeed)
    {
        String result;

        if (rawSpeed > 1000000000)
        {
            final double speed = (double) rawSpeed / 1000000000;
            result = Utils.format.format(speed) + " GH/s";
        }
        else if (rawSpeed > 1000000)
        {
            final double speed = (double) rawSpeed / 1000000;
            result = Utils.format.format(speed) + " MH/s";
        }
        else if (rawSpeed > 1000)
        {
            final double speed = (double) rawSpeed / 1000;
            result = Utils.format.format(speed) + " KH/s";
        }
        else
        {
            result = rawSpeed + " H/s";
        }

        return result;
    }

    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(final byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            final int v = bytes[j] & 0xFF;
            final int j2 = j * 2;
            hexChars[j2] = hexArray[v >>> 4];
            hexChars[j2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] strToBytes(final String data)
    {
        final byte[] bytes = new byte[data.length()];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) data.charAt(i);
        return bytes;
    }

    public static String subSHA256(final String data, final int endIndex)
    {
        return bytesToHex(SHA256.digest(strToBytes(data))).substring(0, endIndex);
    }

    public static String getWork()
    {
        final List<String> lastBlockPageData = get(GET_WORK_LINK);
        return lastBlockPageData == null ? null : lastBlockPageData.get(0);
    }

    public static String getLastBlock()
    {
        final List<String> lastBlockPageData = get(LAST_BLOCK_LINK);
        return lastBlockPageData == null ? null : lastBlockPageData.get(0);
    }

    public static long getBalance(final String userAddress)
    {
        final List<String> balanceData = get(BALANCE_LINK_BASE + userAddress);
        try
        {
            return userAddress == null ? -1 : Long.parseLong(balanceData.get(0));
        }
        catch (final NumberFormatException e)
        {
            System.out.println(balanceData.get(0));
            return -1;
        }
    }

    public static void submitSolution(final String minerID, final long nonce)
    {
        get(KRIST_SYNC_LINK + "submitblock&address=" + minerID + "&nonce=" + nonce);
    }

    public static boolean isMinerValid(final String minerID)
    {
        final List<String> minerValidity = get(BALANCE_LINK_BASE + minerID);
        return minerValidity != null && !minerValidity.isEmpty();
    }

    public static List<String> get(final String url)
    {
        try
        {
            final URL urlObj = new URL(url);
            final HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "SKristMiner");

            final int responseCode = conn.getResponseCode();
            if (responseCode != 200)
                return null;

            final List<String> result = new ArrayList<>();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
                result.add(line);
            reader.close();

            return result;
        }
        catch (final Throwable t)
        {
            return null;
        }
    }

    private Utils()
    {
    }
}