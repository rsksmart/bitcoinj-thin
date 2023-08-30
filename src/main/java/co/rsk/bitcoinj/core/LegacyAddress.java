/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Giannis Dzegoutanis
 * Copyright 2015 Andreas Schildbach
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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;

import javax.annotation.Nullable;

import co.rsk.bitcoinj.params.Networks;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.Script.ScriptType;
import co.rsk.bitcoinj.script.ScriptPattern;

import com.google.common.base.Objects;

/*
 * <p>A Bitcoin address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an elliptic curve public key
 * plus a set of network parameters. Not to be confused with a {@link PeerAddress} or {@link AddressMessage}
 * which are about network (TCP) addresses.</p>
 *
 * <p>A standard address is built by taking the RIPE-MD160 hash of the public key bytes, with a version prefix and a
 * checksum suffix, then encoding it textually as base58. The version prefix is used to both denote the network for
 * which the address is valid (see {@link NetworkParameters}, and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, another (currently unsupported
 * type) can contain a hash of a script instead.</p>
 */
public class LegacyAddress extends Address {
    /**
     * An address is a RIPEMD160 hash of a public key, therefore is always 160 bits or 20 bytes.
     */
    public static final int LENGTH = 20;

    /** True if P2SH, false if P2PKH. */
    public final boolean p2sh;

    /**
     * Private constructor. Use {@link #fromBase58(NetworkParameters, String)},
     * {@link #fromPubKeyHash(NetworkParameters, byte[])}, {@link #fromP2SHHash(NetworkParameters, byte[])} or
     * {@link #fromKey(NetworkParameters, BtcECKey)}.
     *
     * @param params
     *            network this address is valid for
     * @param p2sh
     *            true if hash160 is hash of a script, false if it is hash of a pubkey
     * @param hash160
     *            20-byte hash of pubkey or script
     */
    public LegacyAddress(NetworkParameters params, boolean p2sh, byte[] hash160) throws AddressFormatException {
        super(params, hash160);
        if (hash160.length != 20)
            throw new AddressFormatException.InvalidDataLength(
                "Legacy addresses are 20 byte (160 bit) hashes, but got: " + hash160.length);
        this.p2sh = p2sh;
    }

    /**
     * Construct a {@link LegacyAddress} that represents the given pubkey hash. The resulting address will be a P2PKH type of
     * address.
     *
     * @param params
     *            network this address is valid for
     * @param hash160
     *            20-byte pubkey hash
     * @return constructed address
     */
    public static LegacyAddress fromPubKeyHash(NetworkParameters params, byte[] hash160) throws AddressFormatException {
        return new LegacyAddress(params, false, hash160);
    }

    /*
     * Construct a {@link LegacyAddress} that represents the public part of the given {@link ECKey}. Note that an address is
     * derived from a hash of the public key and is not the public key itself.
     *
     * @param params
     *            network this address is valid for
     * @param key
     *            only the public part is used
     * @return constructed address
     */
    public static LegacyAddress fromKey(NetworkParameters params, BtcECKey key) {
        return fromPubKeyHash(params, key.getPubKeyHash());
    }

    /**
     * Construct a {@link LegacyAddress} that represents the given P2SH script hash.
     *
     * @param params
     *            network this address is valid for
     * @param hash160
     *            P2SH script hash
     * @return constructed address
     */
    public static LegacyAddress fromScriptHash(NetworkParameters params, byte[] hash160) throws AddressFormatException {
        return new LegacyAddress(params, true, hash160);
    }

    /** @deprecated use {@link #fromScriptHash(NetworkParameters, byte[])} */
    @Deprecated
    public static LegacyAddress fromP2SHHash(NetworkParameters params, byte[] hash160) {
        return fromScriptHash(params, hash160);
    }

    /**
     * @deprecated use {@link #fromScriptHash(NetworkParameters, byte[])} in combination with
     *             {@link ScriptPattern#extractHashFromP2SH(Script)}
     */
    @Deprecated
    public static LegacyAddress fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
        checkArgument(ScriptPattern.isP2SH(scriptPubKey), "Not a P2SH script");
        return fromScriptHash(params, ScriptPattern.extractHashFromP2SH(scriptPubKey));
    }

    /**
     * Construct a {@link LegacyAddress} from its base58 form.
     *
     * @param params
     *            expected network this address is valid for, or null if if the network should be derived from the
     *            base58
     * @param base58
     *            base58-encoded textual form of the address
     * @throws AddressFormatException
     *             if the given base58 doesn't parse or the checksum is invalid
     * @throws AddressFormatException.WrongNetwork
     *             if the given address is valid but for a different chain (eg testnet vs mainnet)
     */
    public static LegacyAddress fromBase58(@Nullable NetworkParameters params, String base58)
        throws AddressFormatException, AddressFormatException.WrongNetwork {
        byte[] versionAndDataBytes = Base58.decodeChecked(base58);
        int version = versionAndDataBytes[0] & 0xFF;
        byte[] bytes = Arrays.copyOfRange(versionAndDataBytes, 1, versionAndDataBytes.length);
        if (params == null) {
            for (NetworkParameters p : Networks.get()) {
                if (version == p.getAddressHeader())
                    return new LegacyAddress(p, false, bytes);
                else if (version == p.getP2SHHeader())
                    return new LegacyAddress(p, true, bytes);
            }
            throw new AddressFormatException.InvalidPrefix("No network found for " + base58);
        } else {
            if (version == params.getAddressHeader())
                return new LegacyAddress(params, false, bytes);
            else if (version == params.getP2SHHeader())
                return new LegacyAddress(params, true, bytes);
            throw new AddressFormatException.WrongNetwork(version);
        }
    }

    /** @deprecated use {@link #fromPubKeyHash(NetworkParameters, byte[])} */
    @Deprecated
    public LegacyAddress(NetworkParameters params, byte[] hash160) throws AddressFormatException {
        this(params, false, hash160);
    }

    /**
     * Get the version header of an address. This is the first byte of a base58 encoded address.
     *
     * @return version header as one byte
     */
    public int getVersion() {
        return p2sh ? params.getP2SHHeader() : params.getAddressHeader();
    }

    /**
     * Returns the base58-encoded textual form, including version and checksum bytes.
     *
     * @return textual form
     */
    public String toBase58() {
        return Base58.encodeChecked(getVersion(), bytes);
    }

    /** @deprecated use {@link #getHash()} */
    @Deprecated
    public byte[] getHash160() {
        return getHash();
    }

    /** The (big endian) 20 byte hash that is the core of a Bitcoin address. */
    @Override
    public byte[] getHash() {
        return bytes;
    }

    /**
     * Get the type of output script that will be used for sending to the address. This is either
     * {@link ScriptType#P2PKH} or {@link ScriptType#P2SH}.
     *
     * @return type of output script
     */
    @Override
    public ScriptType getOutputScriptType() {
        return p2sh ? ScriptType.P2SH : ScriptType.P2PKH;
    }

    /** @deprecated Use {@link #getOutputScriptType()} */
    @Deprecated
    public boolean isP2SHAddress() {
        return p2sh;
    }

    /**
     * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
     * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
     * compatible with the current wallet.
     *
     * @return network the address is valid for
     * @throws AddressFormatException if the given base58 doesn't parse or the checksum is invalid
     */
    public static NetworkParameters getParametersFromAddress(String address) throws AddressFormatException {
        return LegacyAddress.fromBase58(null, address).getParameters();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LegacyAddress other = (LegacyAddress) o;
        return super.equals(other) && this.p2sh == other.p2sh;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), p2sh);
    }

    @Override
    public String toString() {
        return toBase58();
    }

    @Override
    public LegacyAddress clone() throws CloneNotSupportedException {
        return (LegacyAddress) super.clone();
    }
}




/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Giannis Dzegoutanis
 * Copyright 2015 Andreas Schildbach
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
 *//*


package co.rsk.bitcoinj.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import co.rsk.bitcoinj.params.Networks;
import co.rsk.bitcoinj.script.Script;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

*/
/**
 * <p>A Bitcoin address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an elliptic curve public key
 * plus a set of network parameters. Not to be confused with a {@link PeerAddress} or {@link AddressMessage}
 * which are about network (TCP) addresses.</p>
 *
 * <p>A standard address is built by taking the RIPE-MD160 hash of the public key bytes, with a version prefix and a
 * checksum suffix, then encoding it textually as base58. The version prefix is used to both denote the network for
 * which the address is valid (see {@link NetworkParameters}, and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, another (currently unsupported
 * type) can contain a hash of a script instead.</p>
 *//*

public class Address extends VersionedChecksummedBytes {
    */
/**
     * An address is a RIPEMD160 hash of a public key, therefore is always 160 bits or 20 bytes.
     *//*

    public static final int LENGTH = 20;

    private transient NetworkParameters params;

    */
/**
     * Construct an address from parameters, the address version, and the hash160 form. Example:<p>
     *
     * <pre>new Address(MainNetParams.get(), NetworkParameters.getAddressHeader(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     *//*

    public Address(NetworkParameters params, int version, byte[] hash160) throws WrongNetworkException {
        super(version, hash160);
        checkNotNull(params);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");
        if (!isAcceptableVersion(params, version))
            throw new WrongNetworkException(version, params.getAcceptableAddressCodes());
        this.params = params;
    }

    */
/** Returns an Address that represents the given P2SH script hash. *//*

    public static Address fromP2SHHash(NetworkParameters params, byte[] hash160) {
        try {
            return new Address(params, params.getP2SHHeader(), hash160);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    */
/** Returns an Address that represents the script hash extracted from the given scriptPubKey *//*

    public static Address fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
        checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
        return fromP2SHHash(params, scriptPubKey.getPubKeyHash());
    }

    */
/**
     * Construct an address from its Base58 representation.
     * @param params
     *            The expected NetworkParameters or null if you don't want validation.
     * @param base58
     *            The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL".
     * @throws AddressFormatException
     *             if the given base58 doesn't parse or the checksum is invalid
     * @throws WrongNetworkException
     *             if the given address is valid but for a different chain (eg testnet vs mainnet)
     *//*

    public static Address fromBase58(@Nullable NetworkParameters params, String base58) throws AddressFormatException {
        return new Address(params, base58);
    }

    */
/**
     * Construct an address from parameters and the hash160 form. Example:<p>
     *
     * <pre>new Address(MainNetParams.get(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     *//*

    public Address(NetworkParameters params, byte[] hash160) {
        super(params.getAddressHeader(), hash160);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");
        this.params = params;
    }

    */
/** @deprecated Use {@link #fromBase58(NetworkParameters, String)} *//*

    @Deprecated
    public Address(@Nullable NetworkParameters params, String address) throws AddressFormatException {
        super(address);
        if (params != null) {
            if (!isAcceptableVersion(params, version)) {
                throw new WrongNetworkException(version, params.getAcceptableAddressCodes());
            }
            this.params = params;
        } else {
            NetworkParameters paramsFound = null;
            for (NetworkParameters p : Networks.get()) {
                if (isAcceptableVersion(p, version)) {
                    paramsFound = p;
                    break;
                }
            }
            if (paramsFound == null)
                throw new AddressFormatException("No network found for " + address);

            this.params = paramsFound;
        }
    }

    */
/** The (big endian) 20 byte hash that is the core of a Bitcoin address. *//*

    public byte[] getHash160() {
        return bytes;
    }

    */
/**
     * Returns true if this address is a Pay-To-Script-Hash (P2SH) address.
     * See also https://github.com/bitcoin/bips/blob/master/bip-0013.mediawiki: Address Format for pay-to-script-hash
     *//*

    public boolean isP2SHAddress() {
        final NetworkParameters parameters = getParameters();
        return parameters != null && this.version == parameters.p2shHeader;
    }

    */
/**
     * Examines the version byte of the address and attempts to find a matching NetworkParameters. If you aren't sure
     * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
     * compatible with the current wallet. You should be able to handle a null response from this method. Note that the
     * parameters returned is not necessarily the same as the one the Address was created with.
     *
     * @return a NetworkParameters representing the network the address is intended for.
     *//*

    public NetworkParameters getParameters() {
        return params;
    }

    */
/**
     * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
     * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
     * compatible with the current wallet.
     * @return a NetworkParameters of the address
     * @throws AddressFormatException if the string wasn't of a known version
     *//*

    public static NetworkParameters getParametersFromAddress(String address) throws AddressFormatException {
        try {
            return Address.fromBase58(null, address).getParameters();
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    */
/**
     * Check if a given address version is valid given the NetworkParameters.
     *//*

    private static boolean isAcceptableVersion(NetworkParameters params, int version) {
        for (int v : params.getAcceptableAddressCodes()) {
            if (version == v) {
                return true;
            }
        }
        return false;
    }

    */
/**
     * This implementation narrows the return type to <code>Address</code>.
     *//*

    @Override
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }

    // Java serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(params.id);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        params = NetworkParameters.fromID(in.readUTF());
    }
}
*/
