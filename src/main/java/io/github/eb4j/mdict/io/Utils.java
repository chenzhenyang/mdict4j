/*
 * MD4J, a parser library for MDict format.
 * Copyright (C) 2021 Hiroshi Miura.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.eb4j.mdict.io;

import io.github.eb4j.mdict.MDException;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.lzo_uintp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class Utils {

    private Utils() {
    }

    public static MDBlockInputStream decompress(final MDInputStream inputStream, final long compSize,
                                                  final long decompSize)
            throws IOException, MDException, DataFormatException {
        int flag = inputStream.read();
        inputStream.skip(3);
        byte[] word = new byte[4];
        inputStream.readFully(word);
        long checksum = byteArrayToInt(word);
        byte[] input;
        byte[] output;
        switch (flag) {
            case 0:
                break;
            case 1:
                input = new byte[(int) compSize - 8];
                output = new byte[(int) decompSize];
                inputStream.readFully(input);
                LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;
                LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
                lzo_uintp outLen = new lzo_uintp();
                decompressor.decompress(input, 0, (int) compSize, output, 0, outLen);
                if (outLen.value != decompSize) {
                    throw new MDException("Decompression size is differ.");
                }
                return new MDBlockInputStream(new ByteArrayInputStream(output));
            case 2:
                input = new byte[(int) compSize - 8];
                output = new byte[(int) decompSize];
                Inflater inflater = new Inflater();
                inputStream.readFully(input);
                inflater.setInput(input);
                int size = inflater.inflate(output);
                if (checksum != inflater.getAdler()) {
                    throw new MDException("checksum error");
                }
                inflater.end();
                if (size != decompSize) {
                    throw new MDException("Decompression error, wrong size.");
                }
                return new MDBlockInputStream(new ByteArrayInputStream(output));
            default:
                throw new MDException(String.format("Unknown compression level: %d", flag));
        }
        throw new MDException("Unsupported data.");
    }

    public static long byteArrayToLong(final byte[] dWord) {
        ByteBuffer buffer = ByteBuffer.wrap(dWord).order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    public static int byteArrayToInt(final byte[] word, final ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.wrap(word).order(byteOrder);
        return buffer.getInt();
    }

    public static int byteArrayToInt(final byte[] word) {
        return byteArrayToInt(word, ByteOrder.BIG_ENDIAN);
    }

    public static short byteArrayToShort(final byte[] hWord) {
        ByteBuffer buffer = ByteBuffer.wrap(hWord).order(ByteOrder.BIG_ENDIAN);
        return buffer.getShort();
    }
}