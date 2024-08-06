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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    @Test(expected = IllegalArgumentException.class)
    public void bigIntegerToBytes_convertNegativeNumber() {
        BigInteger b = BigInteger.valueOf(-1);
        Utils.bigIntegerToBytes(b, 32);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigIntegerToBytes_convertWithNegativeLength() {
        BigInteger b = BigInteger.valueOf(10);
        Utils.bigIntegerToBytes(b, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigIntegerToBytes_convertWithZeroLength() {
        BigInteger b = BigInteger.valueOf(10);
        Utils.bigIntegerToBytes(b, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bigIntegerToBytes_insufficientLength() {
        BigInteger b = BigInteger.valueOf(0b1000__0000_0000);   // base 2
        Utils.bigIntegerToBytes(b, 1);
    }

    @Test
    public void bigIntegerToBytes_convertZero() {
        BigInteger b = BigInteger.valueOf(0);
        byte[] expected = new byte[]{0b0000_0000};
        byte[] actual = Utils.bigIntegerToBytes(b, 1);
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void bigIntegerToBytes_singleByteSignFit() {
        BigInteger b = BigInteger.valueOf(0b0000_1111);
        byte[] expected = new byte[]{0b0000_1111};
        byte[] actual = Utils.bigIntegerToBytes(b, 1);
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void bigIntegerToBytes_paddedSingleByte() {
        BigInteger b = BigInteger.valueOf(0b0000_1111);
        byte[] expected = new byte[]{0, 0b0000_1111};
        byte[] actual = Utils.bigIntegerToBytes(b, 2);
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void bigIntegerToBytes_singleByteSignDoesNotFit() {
        BigInteger b = BigInteger.valueOf(0b1000_0000);     // 128 (2-compl does not fit in one byte)
        byte[] expected = new byte[]{-128};                 // -128 == 1000_0000 (compl-2)
        byte[] actual = Utils.bigIntegerToBytes(b, 1);
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void unsignedLongToByteArrayBE_twoBytes() {
        final int numBytes = 2;

        long value = 1L;
        byte[] valueSerializedAsBE = Hex.decode("0001");
        byte[] conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        long obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsBE, conversion);
        assertEquals(value, obtainedValue);

        value = 255L;
        valueSerializedAsBE = Hex.decode("00FF");
        conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsBE, conversion);
        assertEquals(value, obtainedValue);

        value = 256L;
        valueSerializedAsBE = Hex.decode("0100");
        conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsBE, conversion);
        assertEquals(value, obtainedValue);

        value = 65535L;
        valueSerializedAsBE = Hex.decode("FFFF");
        conversion = Utils.unsignedLongToByteArrayBE(value, numBytes);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsBE, conversion);
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

        long value = 1L;
        byte[] valueSerializedAsLE = Hex.decode("0100");
        byte[] conversion = Utils.unsignedLongToByteArrayLE(value, numBytes);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsLE, conversion);

        value = 255L;
        valueSerializedAsLE = Hex.decode("FF00");
        conversion = Utils.unsignedLongToByteArrayLE(value, numBytes);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsLE, conversion);

        value = 256L;
        valueSerializedAsLE = Hex.decode("0001");
        conversion = Utils.unsignedLongToByteArrayLE(value, numBytes);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsLE, conversion);

        value = 65_535L;
        valueSerializedAsLE = Hex.decode("FFFF");
        conversion = Utils.unsignedLongToByteArrayLE(value, numBytes);
        assertEquals(numBytes, conversion.length);
        assertArrayEquals(valueSerializedAsLE, conversion);
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

    @Test
    public void signedLongToByteArrayLE() {
        long value = 1L;
        byte[] conversion = Utils.signedLongToByteArrayLE(value);
        byte[] reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        long obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 255L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 256L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 65_535L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 65_536L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 5_000_000L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 100_000_000L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = 0L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = -100L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);

        value = -100_000L;
        conversion = Utils.signedLongToByteArrayLE(value);
        reversedConversion = Utils.reverseBytes(conversion); // Turn into BE
        obtainedValue = new BigInteger(reversedConversion).longValue();
        assertEquals(value, obtainedValue);
    };

    @Test
    public void hash160() {

        List<byte[]> inputs = Arrays.asList(
                Hex.decode(""),
                Hex.decode("abcd"),
                Hex.decode("00"),
                Hex.decode("01"),
                Hex.decode("0000"),
                Hex.decode("ffff")
        );
        List<byte[]> expectedHashes = Arrays.asList(
                Hex.decode("b472a266d0bd89c13706a4132ccfb16f7c3b9fcb"),
                Hex.decode("4671c47a9d20c240a291661520d4af51df08fb0b"),
                Hex.decode("9f7fd096d37ed2c0e3f7f0cfc924beef4ffceb68"),
                Hex.decode("c51b66bced5e4491001bd702669770dccf440982"),
                Hex.decode("e6c41bcc570872e88e58db7c940dc8d399e72aef"),
                Hex.decode("e6abebacc6bf964f5131e80b241e3fe14bc3e156")
        );

        for (int i = 0; i < inputs.size(); i++) {
            byte[] input = inputs.get(i);
            byte[] expectedHash = expectedHashes.get(i);

            byte[] hashResult = Utils.hash160(input);
            assertArrayEquals(expectedHash, hashResult);
        }
    };
}
