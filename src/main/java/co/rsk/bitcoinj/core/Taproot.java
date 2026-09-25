/*
 * This file is part of RskJ
 * Copyright (C) 2026 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.bitcoinj.core;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.spongycastle.math.ec.ECPoint;

/**
 * <p>The BIP341 taproot output key derivation, applied with an empty merkle root, which is the
 * single key case BIP86 describes.</p>
 *
 * <pre>
 * t = int(tagged_hash("TapTweak", x_only(P)))     fail if t &gt;= n
 * Q = lift_x(x_only(P)) + t*G
 * output key = x_only(Q)
 * </pre>
 *
 * <p>Not a port. bitcoinj 0.17.1 has the whole address layer but no taproot crypto at all:
 * nothing under {@code org.bitcoinj.crypto} mentions taproot, tweak, schnorr, x-only or lift.
 * This comes from BIP340 and BIP341 directly.</p>
 *
 * <p>No new dependency is needed. {@link BtcECKey#CURVE} and {@link BtcECKey#getPubKeyPoint()}
 * are already public, and SpongyCastle's {@code ECPoint} has the arithmetic.</p>
 */
public final class Taproot {

    private static final String TAP_TWEAK_TAG = "TapTweak";
    private static final int X_ONLY_KEY_LENGTH = 32;

    private Taproot() { }

    /**
     * The 32-byte witness version 1 program a peg-out to this key must pay to.
     *
     * @throws IllegalArgumentException if the key is not compressed, or if the derivation lands
     *                                  on a value the curve cannot represent
     */
    public static byte[] deriveOutputKey(BtcECKey key) {
        if (!key.isCompressed()) {
            throw new IllegalArgumentException("Taproot destinations require a compressed public key");
        }

        ECPoint internalKey = liftX(key.getPubKeyPoint());
        BigInteger tweak = new BigInteger(1, taggedHash(TAP_TWEAK_TAG, toXOnly(internalKey)));

        return applyTweak(internalKey, tweak);
    }

    /**
     * BIP341 {@code taproot_tweak_pubkey}, from the point the tweak has already been derived.
     *
     * <p>Separate from {@link #deriveOutputKey} because the two rejections below cannot be reached
     * through it: the tweak is a hash of the key, so a tweak outside the scalar range means a key
     * whose TapTweak hash lands in the last 2^-128 of the 256-bit range. This is the step BIP341
     * names, not a seam that exists only for the test.</p>
     *
     * @throws IllegalArgumentException if the tweak is not a valid scalar, or the result is the
     *                                  point at infinity
     */
    static byte[] applyTweak(ECPoint internalKey, BigInteger tweak) {
        if (tweak.compareTo(BtcECKey.CURVE.getN()) >= 0) {
            throw new IllegalArgumentException("Taproot tweak is not a valid scalar for this key");
        }

        ECPoint outputKey = internalKey.add(BtcECKey.CURVE.getG().multiply(tweak)).normalize();
        if (outputKey.isInfinity()) {
            throw new IllegalArgumentException("Taproot tweak produced the point at infinity");
        }

        return toXOnly(outputKey);
    }

    /** BIP340 {@code lift_x} applied to a public key, exposed for {@link #applyTweak}. */
    static ECPoint internalKeyOf(BtcECKey key) {
        return liftX(key.getPubKeyPoint());
    }

    /**
     * BIP340 {@code lift_x}: the point with the given x coordinate and an even y. Taproot keys
     * carry no y parity of their own, so a key with an odd y is negated to its even counterpart.
     */
    private static ECPoint liftX(ECPoint point) {
        ECPoint normalized = point.normalize();
        boolean hasOddY = normalized.getAffineYCoord().toBigInteger().testBit(0);

        return hasOddY ? normalized.negate().normalize() : normalized;
    }

    /** BIP340 x-only encoding: the 32-byte x coordinate, with the y parity dropped. */
    private static byte[] toXOnly(ECPoint point) {
        BigInteger x = point.normalize().getAffineXCoord().toBigInteger();

        return toFixedLength(x, X_ONLY_KEY_LENGTH);
    }

    /** BIP340 tagged hash: {@code SHA256(SHA256(tag) || SHA256(tag) || message)}. */
    private static byte[] taggedHash(String tag, byte[] message) {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes(StandardCharsets.UTF_8));
        byte[] preimage = new byte[tagHash.length * 2 + message.length];

        System.arraycopy(tagHash, 0, preimage, 0, tagHash.length);
        System.arraycopy(tagHash, 0, preimage, tagHash.length, tagHash.length);
        System.arraycopy(message, 0, preimage, tagHash.length * 2, message.length);

        return Sha256Hash.hash(preimage);
    }

    /** Big-endian, left-padded with zeros. BigInteger drops leading zero bytes on its own. */
    private static byte[] toFixedLength(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == length) {
            return bytes;
        }

        byte[] padded = new byte[length];
        if (bytes.length > length) {
            // toByteArray() prepends a zero byte when the high bit is set
            System.arraycopy(bytes, bytes.length - length, padded, 0, length);
        } else {
            System.arraycopy(bytes, 0, padded, length - bytes.length, bytes.length);
        }

        return padded;
    }
}
