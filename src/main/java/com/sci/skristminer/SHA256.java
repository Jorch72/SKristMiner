package com.sci.skristminer;

/**
 * @author sci4me
 */
public final class SHA256
{
    private static final int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};

    private SHA256()
    {
    }

    public static byte[] digest(final byte[] message)
    {
        byte[] hashed = new byte[32], block = new byte[64], padded = padMessage(message);

        int[] H = {0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19};

        for (int i = 0; i < padded.length / 64; i++)
        {
            int[] words = new int[64];
            int a = H[0], b = H[1], c = H[2], d = H[3], e = H[4], f = H[5], g = H[6], h = H[7], s0, s1, maj, t1, t2, ch;

            System.arraycopy(padded, 64 * i, block, 0, 64);
            for (int j = 0; j < 16; j++)
            {
                final int j4 = j * 4;
                words[j] |= ((block[j4] & 0x000000FF) << 24);
                words[j] |= ((block[j4 + 1] & 0x000000FF) << 16);
                words[j] |= ((block[j4 + 2] & 0x000000FF) << 8);
                words[j] |= (block[j4 + 3] & 0x000000FF);
            }

            for (int j = 16; j < 64; j++)
            {
                final int sa = words[j - 15];
                final int sb = words[j - 2];
                s0 = Integer.rotateRight(sa, 7) ^ Integer.rotateRight(sa, 18) ^ (sa >>> 3);
                s1 = Integer.rotateRight(sb, 17) ^ Integer.rotateRight(sb, 19) ^ (sb >>> 10);
                words[j] = words[j - 16] + s0 + words[j - 7] + s1;
            }

            for (int j = 0; j < 64; j++)
            {
                s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
                maj = (a & b) ^ (a & c) ^ (b & c);
                t2 = s0 + maj;
                s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
                ch = (e & f) ^ (~e & g);
                t1 = h + s1 + ch + SHA256.K[j] + words[j];

                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
            }

            H[0] += a;
            H[1] += b;
            H[2] += c;
            H[3] += d;
            H[4] += e;
            H[5] += f;
            H[6] += g;
            H[7] += h;
        }

        for (int i = 0; i < 8; i++)
        {
            final int h = H[i];
            final int i4 = i * 4;
            hashed[i4] = (byte) ((h >>> 56) & 0xff);
            hashed[i4 + 1] = (byte) ((h >>> 48) & 0xff);
            hashed[i4 + 2] = (byte) ((h >>> 40) & 0xff);
            hashed[i4 + 3] = (byte) ((h >>> 32) & 0xff);
        }

        return hashed;
    }

    private static byte[] padMessage(final byte[] data)
    {
        final int origLength = data.length;
        final int tailLength = origLength % 64;
        final int padLength;
        if ((64 - tailLength >= 9))
            padLength = 64 - tailLength;
        else
            padLength = 128 - tailLength;

        final byte[] thePad = new byte[padLength];
        thePad[0] = (byte) 0x80;

        final long lengthInBits = origLength * 8;

        final int lm1 = thePad.length - 1;
        thePad[lm1] = (byte) (lengthInBits & 0xFF);
        thePad[lm1 - 1] = (byte) ((lengthInBits >>> 8) & 0xFF);
        thePad[lm1 - 2] = (byte) ((lengthInBits >>> 16) & 0xFF);
        thePad[lm1 - 3] = (byte) ((lengthInBits >>> 24) & 0xFF);
        thePad[lm1 - 4] = (byte) ((lengthInBits >>> 32) & 0xFF);
        thePad[lm1 - 5] = (byte) ((lengthInBits >>> 40) & 0xFF);
        thePad[lm1 - 6] = (byte) ((lengthInBits >>> 48) & 0xFF);
        thePad[lm1 - 7] = (byte) ((lengthInBits >>> 56) & 0xFF);

        final byte[] output = new byte[origLength + padLength];

        System.arraycopy(data, 0, output, 0, origLength);
        System.arraycopy(thePad, 0, output, origLength, thePad.length);

        return output;
    }
}