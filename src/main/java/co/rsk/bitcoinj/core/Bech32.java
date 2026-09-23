/*
 * Copyright 2018 Coinomi Ltd
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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p>Implementation of the bech32 encoding, as defined by BIP173, and of bech32m, as defined by
 * BIP350. The two are the same algorithm and differ only in a constant that is XORed into the
 * checksum.</p>
 *
 * <p>Ported from bitcoinj 0.17.1, {@code core/src/main/java/org/bitcoinj/base/Bech32.java}. The
 * only structural difference is that upstream backs {@link Bech32Bytes} with its own
 * {@code ByteArray} type, which this fork does not have, so the array is held directly.</p>
 *
 * <p>The caller says which encoding to use, and {@link #decode(String)} reports which one it
 * found. Deciding that witness version 0 uses bech32 and versions 1 and above use bech32m belongs
 * to the caller that understands witness versions.</p>
 */
public final class Bech32 {

    /** The bech32 character set for encoding. */
    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    /** The bech32 character set for decoding. */
    private static final byte[] CHARSET_REV = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        15, -1, 10, 17, 21, 20, 26, 30,  7,  5, -1, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25,  9,  8, 23, -1, 18, 22, 31, 27, 19, -1,
         1,  0,  3, 16, 11, 28, 12, 14,  6,  4,  2, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25,  9,  8, 23, -1, 18, 22, 31, 27, 19, -1,
         1,  0,  3, 16, 11, 28, 12, 14,  6,  4,  2, -1, -1, -1, -1, -1
    };

    private static final int BECH32_CONST = 1;
    private static final int BECH32M_CONST = 0x2bc830a3;

    /**
     * The two checksum variants. Same algorithm, different final constant. BIP350 introduced
     * bech32m and left bech32 in place for what already used it.
     */
    public enum Encoding { BECH32, BECH32M }

    /**
     * The 5-bit payload of a bech32 string.
     */
    public static class Bech32Bytes {
        protected final byte[] bytes;

        protected Bech32Bytes(byte[] bytes) {
            this.bytes = bytes;
        }

        private Bech32Bytes(byte first, byte[] rest) {
            this.bytes = new byte[rest.length + 1];
            this.bytes[0] = first;
            System.arraycopy(rest, 0, this.bytes, 1, rest.length);
        }

        /** Wraps 8-bit data, converting it to the 5-bit groups bech32 encodes. */
        static Bech32Bytes ofBytes(byte[] data) {
            return new Bech32Bytes(encode8to5(data));
        }

        /** Wraps a witness version and program, as a segwit address needs them laid out. */
        static Bech32Bytes ofSegwit(short witnessVersion, byte[] witnessProgram) {
            return new Bech32Bytes((byte) witnessVersion, encode8to5(witnessProgram));
        }

        private static byte[] encode8to5(byte[] data) {
            return convertBits(data, 0, data.length, 8, 5, true);
        }

        /** The payload widened back to 8 bits per byte. */
        public byte[] decode5to8() {
            return convertBits(bytes, 0, bytes.length, 5, 8, false);
        }

        /** The raw 5-bit payload. Upstream inherits this accessor from its ByteArray type. */
        byte[] bytes() {
            return Arrays.copyOf(bytes, bytes.length);
        }

        /** The first 5-bit symbol, which for a segwit address is the witness version. */
        short witnessVersion() {
            return bytes[0];
        }

        private Bech32Bytes stripFirst() {
            return new Bech32Bytes(Arrays.copyOfRange(bytes, 1, bytes.length));
        }

        /** Everything after the witness version, widened back to 8 bits per byte. */
        byte[] witnessProgram() {
            return stripFirst().decode5to8();
        }
    }

    /** A decoded bech32 string: which encoding it used, its human-readable part, and its payload. */
    public static class Bech32Data extends Bech32Bytes {
        public final Encoding encoding;
        public final String hrp;

        private Bech32Data(final Encoding encoding, final String hrp, final byte[] data) {
            super(data);
            this.encoding = encoding;
            this.hrp = hrp;
        }
    }

    private Bech32() { }

    /** Encodes 8-bit bytes. For a payload that is already in 5-bit groups, use {@link #encode}. */
    public static String encodeBytes(Encoding encoding, String hrp, byte[] bytes) {
        return encode(encoding, hrp, Bech32Bytes.ofBytes(bytes));
    }

    /**
     * Decodes to 8-bit bytes, checking that the human-readable part and the encoding are the ones
     * expected. For a payload that should stay in 5-bit groups, use {@link #decode}.
     */
    public static byte[] decodeBytes(String bech32, String expectedHrp, Encoding expectedEncoding) {
        Bech32Data decoded = decode(bech32);
        if (!decoded.hrp.equals(expectedHrp)) {
            throw new AddressFormatException(
                "Unexpected human-readable part: " + decoded.hrp + ", expected " + expectedHrp);
        }
        if (decoded.encoding != expectedEncoding) {
            throw new AddressFormatException(
                "Unexpected encoding: " + decoded.encoding + ", expected " + expectedEncoding);
        }
        return decoded.decode5to8();
    }

    /** Re-encodes a decoded string, using the encoding and human-readable part it came with. */
    public static String encode(final Bech32Data bech32) {
        return encode(bech32.encoding, bech32.hrp, bech32);
    }

    /** Encodes a payload that is already in 5-bit groups. */
    public static String encode(Encoding encoding, String hrp, Bech32Bytes values) {
        if (hrp.length() < 1 || hrp.length() > 83) {
            throw new AddressFormatException("Invalid human-readable part length: " + hrp.length());
        }

        String lowerCaseHrp = hrp.toLowerCase(Locale.ROOT);
        byte[] data = values.bytes;
        byte[] checksum = createChecksum(encoding, lowerCaseHrp, data);

        StringBuilder sb = new StringBuilder(lowerCaseHrp.length() + 1 + data.length + checksum.length);
        sb.append(lowerCaseHrp).append('1');
        for (byte value : data) {
            sb.append(CHARSET.charAt(value));
        }
        for (byte value : checksum) {
            sb.append(CHARSET.charAt(value));
        }

        String encoded = sb.toString();
        if (encoded.length() > 90) {
            throw new AddressFormatException("Output too long: " + encoded.length());
        }
        return encoded;
    }

    /**
     * Decodes a bech32 or bech32m string. The encoding is worked out from which constant the
     * checksum validates against, and reported on the result.
     */
    public static Bech32Data decode(String str) {
        if (str.length() < 8) {
            throw new AddressFormatException("Input too short: " + str.length());
        }
        if (str.length() > 90) {
            throw new AddressFormatException("Input too long: " + str.length());
        }

        boolean lower = false;
        boolean upper = false;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < 33 || c > 126) {
                throw new AddressFormatException("Invalid character at index " + i);
            }
            if (c >= 'a' && c <= 'z') {
                if (upper) {
                    throw new AddressFormatException("Mixed case string");
                }
                lower = true;
            }
            if (c >= 'A' && c <= 'Z') {
                if (lower) {
                    throw new AddressFormatException("Mixed case string");
                }
                upper = true;
            }
        }

        int separatorIndex = str.lastIndexOf('1');
        if (separatorIndex < 1) {
            throw new AddressFormatException("Missing human-readable part");
        }
        int dataPartLength = str.length() - 1 - separatorIndex;
        if (dataPartLength < 6) {
            throw new AddressFormatException("Data part too short: " + dataPartLength);
        }

        byte[] values = new byte[dataPartLength];
        for (int i = 0; i < dataPartLength; ++i) {
            char c = str.charAt(i + separatorIndex + 1);
            if (c > CHARSET_REV.length - 1 || CHARSET_REV[c] == -1) {
                throw new AddressFormatException("Invalid character at index " + (i + separatorIndex + 1));
            }
            values[i] = CHARSET_REV[c];
        }

        String hrp = str.substring(0, separatorIndex).toLowerCase(Locale.ROOT);
        Encoding encoding = verifyChecksum(hrp, values);
        if (encoding == null) {
            throw new AddressFormatException("Invalid checksum");
        }

        return new Bech32Data(encoding, hrp, Arrays.copyOfRange(values, 0, values.length - 6));
    }

    /** Re-groups the bits of {@code in} from {@code fromBits}-wide groups to {@code toBits}-wide ones. */
    private static byte[] convertBits(byte[] in, int inStart, int inLen, int fromBits, int toBits, boolean pad) {
        int acc = 0;
        int bits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        final int maxv = (1 << toBits) - 1;
        final int maxAcc = (1 << (fromBits + toBits - 1)) - 1;

        for (int i = 0; i < inLen; i++) {
            int value = in[i + inStart] & 0xff;
            if ((value >>> fromBits) != 0) {
                throw new AddressFormatException(
                    String.format("Input value '%X' exceeds '%d' bit size", value, fromBits));
            }
            acc = ((acc << fromBits) | value) & maxAcc;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                out.write((acc >>> bits) & maxv);
            }
        }

        if (pad) {
            if (bits > 0) {
                out.write((acc << (toBits - bits)) & maxv);
            }
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new AddressFormatException("Could not convert bits, invalid padding");
        }

        return out.toByteArray();
    }

    /** Finds the polynomial with value coefficients mod the generator as 30-bit. */
    private static int polymod(byte[] values) {
        int c = 1;
        for (byte value : values) {
            int c0 = (c >>> 25) & 0xff;
            c = ((c & 0x1ffffff) << 5) ^ (value & 0xff);
            if ((c0 & 1) != 0) {
                c ^= 0x3b6a57b2;
            }
            if ((c0 & 2) != 0) {
                c ^= 0x26508e6d;
            }
            if ((c0 & 4) != 0) {
                c ^= 0x1ea119fa;
            }
            if ((c0 & 8) != 0) {
                c ^= 0x3d4233dd;
            }
            if ((c0 & 16) != 0) {
                c ^= 0x2a1462b3;
            }
        }
        return c;
    }

    /** Expands a human-readable part for use in checksum computation. */
    private static byte[] expandHrp(String hrp) {
        int hrpLength = hrp.length();
        byte[] ret = new byte[hrpLength * 2 + 1];
        for (int i = 0; i < hrpLength; ++i) {
            int c = hrp.charAt(i) & 0x7f; // Limit to standard 7-bit ASCII
            ret[i] = (byte) ((c >>> 5) & 0x07);
            ret[i + hrpLength + 1] = (byte) (c & 0x1f);
        }
        ret[hrpLength] = 0;
        return ret;
    }

    /** Returns the encoding whose constant the checksum matches, or null if it matches neither. */
    private static Encoding verifyChecksum(final String hrp, final byte[] values) {
        byte[] hrpExpanded = expandHrp(hrp);
        byte[] combined = new byte[hrpExpanded.length + values.length];
        System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.length);
        System.arraycopy(values, 0, combined, hrpExpanded.length, values.length);
        final int check = polymod(combined);
        if (check == BECH32_CONST)
            return Encoding.BECH32;
        else if (check == BECH32M_CONST)
            return Encoding.BECH32M;
        else
            return null;
    }

    private static byte[] createChecksum(final Encoding encoding, final String hrp, final byte[] values) {
        byte[] hrpExpanded = expandHrp(hrp);
        byte[] enc = new byte[hrpExpanded.length + values.length + 6];
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.length);
        System.arraycopy(values, 0, enc, hrpExpanded.length, values.length);
        int mod = polymod(enc) ^ (encoding == Encoding.BECH32 ? BECH32_CONST : BECH32M_CONST);
        byte[] ret = new byte[6];
        for (int i = 0; i < 6; ++i) {
            ret[i] = (byte) ((mod >>> (5 * (5 - i))) & 31);
        }
        return ret;
    }
}
