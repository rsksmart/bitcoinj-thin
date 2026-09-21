/*
 * Copyright 2018 Andreas Schildbach
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

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>Implementation of native segwit addresses. They are composed of two parts:</p>
 *
 * <ul>
 * <li>A human-readable part (HRP) which is a sequence of ASCII characters.</li>
 * <li>A data part which is a sequence of numbers between 0 and 31 (5 bits).</li>
 * </ul>
 *
 * <p>Ported from bitcoinj 0.17.1, {@code core/src/main/java/org/bitcoinj/base/SegwitAddress.java}.
 * Upstream carries the network as its own {@code Network} type, which this fork does not have, so
 * this holds {@link NetworkParameters} instead and reads the HRP from
 * {@link NetworkParameters#getSegwitHrp()}. {@code getOutputScriptType}, {@code network()} and the
 * comparator are not ported: our {@link Address} interface declares none of them.</p>
 */
public class SegwitAddress implements Address {
    public static final int WITNESS_PROGRAM_LENGTH_PKH = 20;
    public static final int WITNESS_PROGRAM_LENGTH_SH = 32;
    public static final int WITNESS_PROGRAM_LENGTH_TR = 32;
    public static final int WITNESS_PROGRAM_MIN_LENGTH = 2;
    public static final int WITNESS_PROGRAM_MAX_LENGTH = 40;

    protected final NetworkParameters params;
    protected final short witnessVersion;
    protected final byte[] witnessProgram;          // In 8-bits per byte format

    /**
     * Private constructor. Use {@link #fromBech32(NetworkParameters, String)},
     * {@link #fromHash(NetworkParameters, byte[])} or
     * {@link #fromProgram(NetworkParameters, int, byte[])}.
     *
     * @param params
     *            network this address is valid for
     * @param witnessVersion
     *            version number between 0 and 16
     * @param witnessProgram
     *            hash of pubkey, pubkey or script (depending on version) (8-bits per byte)
     * @throws AddressFormatException
     *             if any of the sanity checks fail
     */
    private SegwitAddress(NetworkParameters params, int witnessVersion, byte[] witnessProgram)
            throws AddressFormatException {
        if (witnessVersion < 0 || witnessVersion > 16)
            throw new AddressFormatException("Invalid script version: " + witnessVersion);
        if (witnessProgram.length < WITNESS_PROGRAM_MIN_LENGTH || witnessProgram.length > WITNESS_PROGRAM_MAX_LENGTH)
            throw new AddressFormatException.InvalidDataLength("Invalid length: " + witnessProgram.length);
        // Check script length for version 0:
        // BIP 141:
        // "If the version byte is 0, but the witness program is neither 20 nor 32 bytes, the script must fail."
        // In other words: coins sent to addresses with other lengths will become unspendable.
        if (witnessVersion == 0 && witnessProgram.length != WITNESS_PROGRAM_LENGTH_PKH
                && witnessProgram.length != WITNESS_PROGRAM_LENGTH_SH)
            throw new AddressFormatException.InvalidDataLength(
                    "Invalid length for address version 0: " + witnessProgram.length);
        // Check script length for version 1:
        // BIP 341:
        // "A Taproot output is a native SegWit output (see BIP141) with version number 1, and a 32-byte
        // witness program. Any other outputs, including version 1 outputs with lengths other than 32 bytes,
        // or P2SH-wrapped version 1 outputs, remain unencumbered."
        // In other words: other lengths are not valid Taproot scripts but coins sent there won't be
        // unspendable, quite the contrary, they will be anyone-can-spend. (Not that easy spendable, because still
        // not-standard outputs and therefore not relayed, but a willing miner could easily spend them.)

        // Rationale for still restricting length here: creating anyone-can-spend Taproot addresses is probably
        // not that what callers expect.
        if (witnessVersion == 1 && witnessProgram.length != WITNESS_PROGRAM_LENGTH_TR)
            throw new AddressFormatException.InvalidDataLength(
                    "Invalid length for address version 1: " + witnessProgram.length);
        this.params = Objects.requireNonNull(params);
        this.witnessVersion = (short) witnessVersion;
        this.witnessProgram = Objects.requireNonNull(witnessProgram);
    }

    /**
     * Returns the witness version in decoded form. Only versions 0 and 1 are in use right now.
     *
     * @return witness version, between 0 and 16
     */
    public int getWitnessVersion() {
        return witnessVersion;
    }

    /**
     * Returns the witness program in decoded form.
     *
     * @return witness program
     */
    public byte[] getWitnessProgram() {
        // no version byte
        return witnessProgram;
    }

    @Override
    public byte[] getHash() {
        return getWitnessProgram();
    }

    @Override
    public NetworkParameters getParameters() {
        return params;
    }

    @Override
    public String toString() {
        return toBech32();
    }

    /**
     * Construct a {@link SegwitAddress} from its textual form.
     *
     * @param params  expected network this address is valid for
     * @param bech32  bech32-encoded textual form of the address
     * @return constructed address
     * @throws AddressFormatException if something about the given bech32 address isn't right
     */
    public static SegwitAddress fromBech32(NetworkParameters params, String bech32)
            throws AddressFormatException {
        Bech32.Bech32Data bechData = Bech32.decode(bech32);
        if (bechData.hrp.equals(params.getSegwitHrp()))
            return fromBechData(params, bechData);
        throw new AddressFormatException.WrongNetwork(bechData.hrp);
    }

    static SegwitAddress fromBechData(NetworkParameters params, Bech32.Bech32Data bechData) {
        if (bechData.bytes().length < 1) {
            throw new AddressFormatException.InvalidDataLength("invalid address length (0)");
        }
        final int witnessVersion = bechData.witnessVersion();
        final SegwitAddress address = new SegwitAddress(params, witnessVersion, bechData.witnessProgram());
        if ((witnessVersion == 0 && bechData.encoding != Bech32.Encoding.BECH32) ||
                (witnessVersion != 0 && bechData.encoding != Bech32.Encoding.BECH32M))
            throw new AddressFormatException.UnexpectedWitnessVersion("Unexpected witness version: " + witnessVersion);
        return address;
    }

    /**
     * Construct a {@link SegwitAddress} that represents the given hash, which is either a pubkey hash or a script hash.
     * The resulting address will be either a P2WPKH or a P2WSH type of address.
     *
     * @param params network this address is valid for
     * @param hash 20-byte pubkey hash or 32-byte script hash
     * @return constructed address
     */
    public static SegwitAddress fromHash(NetworkParameters params, byte[] hash) {
        return new SegwitAddress(params, 0, hash);
    }

    /**
     * Construct a {@link SegwitAddress} that represents the given program, which is either a pubkey, a pubkey hash
     * or a script hash - depending on the script version. The resulting address will be either a P2WPKH, a P2WSH or
     * a P2TR type of address.
     *
     * @param params network this address is valid for
     * @param witnessVersion version number between 0 and 16
     * @param witnessProgram version dependent witness program
     * @return constructed address
     */
    public static SegwitAddress fromProgram(NetworkParameters params, int witnessVersion, byte[] witnessProgram) {
        return new SegwitAddress(params, witnessVersion, witnessProgram);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, witnessVersion, Arrays.hashCode(witnessProgram));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SegwitAddress other = (SegwitAddress) o;
        return this.params == other.params && witnessVersion == other.witnessVersion
                && Arrays.equals(this.witnessProgram, other.witnessProgram);
    }

    /**
     * Returns the textual form of the address.
     *
     * @return textual form encoded in bech32
     */
    public String toBech32() {
        Bech32.Encoding encoding = (witnessVersion == 0) ?  Bech32.Encoding.BECH32 : Bech32.Encoding.BECH32M;
        return Bech32.encode(encoding, params.getSegwitHrp(), Bech32.Bech32Bytes.ofSegwit(witnessVersion, witnessProgram));
    }
}
