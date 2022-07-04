/*
 * Copyright 2011 Thilo Planz
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.rsk.bitcoinj.core;

import java.math.BigInteger;
import java.util.Date;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void testReverseBytes() {
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Utils.reverseBytes(new byte[]{5, 4, 3, 2, 1}));
    }

    @Test
    public void testReverseDwordBytes() {
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, Utils.reverseDwordBytes(new byte[]{4, 3, 2, 1, 8, 7, 6, 5}, -1));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, Utils.reverseDwordBytes(new byte[]{4, 3, 2, 1, 8, 7, 6, 5}, 4));
        assertArrayEquals(new byte[0], Utils.reverseDwordBytes(new byte[]{4, 3, 2, 1, 8, 7, 6, 5}, 0));
        assertArrayEquals(new byte[0], Utils.reverseDwordBytes(new byte[0], 0));
    }

    @Test
    public void testMaxOfMostFreq() throws Exception {
        assertEquals(0, Utils.maxOfMostFreq());
        assertEquals(0, Utils.maxOfMostFreq(0, 0, 1));
        assertEquals(2, Utils.maxOfMostFreq(1, 1, 2, 2));
        assertEquals(1, Utils.maxOfMostFreq(1, 1, 2, 2, 1));
        assertEquals(-1, Utils.maxOfMostFreq(-1, -1, 2, 2, -1));
    }

    @Test
    public void compactEncoding() throws Exception {
        assertEquals(new BigInteger("1234560000", 16), Utils.decodeCompactBits(0x05123456L));
        assertEquals(new BigInteger("c0de000000", 16), Utils.decodeCompactBits(0x0600c0de));
        assertEquals(0x05123456L, Utils.encodeCompactBits(new BigInteger("1234560000", 16)));
        assertEquals(0x0600c0deL, Utils.encodeCompactBits(new BigInteger("c0de000000", 16)));
    }

    @Test
    public void dateTimeFormat() {
        assertEquals("2014-11-16T10:54:33Z", Utils.dateTimeFormat(1416135273781L));
        assertEquals("2014-11-16T10:54:33Z", Utils.dateTimeFormat(new Date(1416135273781L)));
    }

    @Test
    public void unsignedLongToByteArrayBE_twoBytes() {
        final int numBytes = 2;

        long value = 1L;
        byte[] conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        long obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(value, obtainedValue);

        value = 255L;
        conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(value, obtainedValue);

        value = 256L;
        conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(value, obtainedValue);

        value = 65535L;
        conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(value, obtainedValue);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayBE_insufficientNumBytesOf1() {
        Utils.unsignedLongToByteArrayBE(260, 1);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayBE_insufficientNumBytesOf2() {
        Utils.unsignedLongToByteArrayBE(65536, 2);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayBE_negativeNumber() {
        Utils.unsignedLongToByteArrayBE(-100, 2);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayBE_negativeLength() {
        Utils.unsignedLongToByteArrayBE(260, -1);
    }

    @Test
    public void unsignedLongToByteArrayLE_twoBytes() {
        final int numBytes = 2;

        long valueInBE = 1L;
        long valueInLE = 256;
        byte[] conversion = Utils.unsignedLongToByteArrayLE(valueInBE, numBytes);
        byte[] reversed = Utils.reverseBytes(conversion);
        long obtainedValueInLE = Long.parseLong(Hex.toHexString(conversion), 16);
        long obtainedValueInBE = Long.parseLong(Hex.toHexString(reversed), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(valueInLE, obtainedValueInLE);
        assertEquals(valueInBE, obtainedValueInBE);

        valueInBE = 255L;
        valueInLE = 65_280;
        conversion = Utils.unsignedLongToByteArrayLE(valueInBE, numBytes);
        reversed = Utils.reverseBytes(conversion);
        obtainedValueInLE = Long.parseLong(Hex.toHexString(conversion), 16);
        obtainedValueInBE = Long.parseLong(Hex.toHexString(reversed), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(valueInLE, obtainedValueInLE);
        assertEquals(valueInBE, obtainedValueInBE);

        valueInBE = 256L;
        valueInLE = 1L;
        conversion = Utils.unsignedLongToByteArrayLE(valueInBE, numBytes);
        reversed = Utils.reverseBytes(conversion);
        obtainedValueInLE = Long.parseLong(Hex.toHexString(conversion), 16);
        obtainedValueInBE = Long.parseLong(Hex.toHexString(reversed), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(valueInLE, obtainedValueInLE);
        assertEquals(valueInBE, obtainedValueInBE);

        valueInBE = 65_535L;
        valueInLE = 65_535L;
        conversion = Utils.unsignedLongToByteArrayLE(valueInBE, numBytes);
        reversed = Utils.reverseBytes(conversion);
        obtainedValueInLE = Long.parseLong(Hex.toHexString(conversion), 16);
        obtainedValueInBE = Long.parseLong(Hex.toHexString(reversed), 16);
        assertEquals(numBytes, conversion.length);
        assertEquals(valueInLE, obtainedValueInLE);
        assertEquals(valueInBE, obtainedValueInBE);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayLE_insufficientNumBytesOf1() {
        Utils.unsignedLongToByteArrayLE(260, 1);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayLE_insufficientNumBytesOf2() {
        Utils.unsignedLongToByteArrayLE(65536, 2);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayLE_negativeNumber() {
        Utils.unsignedLongToByteArrayLE(-100, 2);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArrayLE_negativeLength() {
        Utils.unsignedLongToByteArrayLE(260, -1);
    }
}
