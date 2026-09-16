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

/**
 * <p>A Bitcoin address identifies the destination of an output. Not to be confused with a
 * {@link PeerAddress} or {@link AddressMessage}, which are about network (TCP) addresses.</p>
 *
 * <p>Implementations differ in how they encode that destination. {@link LegacyAddress} carries a
 * 20 byte hash behind a base58check version byte. A segwit address carries a witness program of
 * its own length behind a bech32 human readable part, and is therefore not a
 * {@link VersionedChecksummedBytes} at all, which is why this type is an interface.</p>
 *
 * <p>Keep it narrow. Anything that only makes sense for one encoding, such as base58 rendering,
 * belongs on the implementation and not here.</p>
 */
public interface Address {

    /**
     * The bytes that identify the destination. For a legacy address this is the 20 byte hash; for
     * a segwit address it is the witness program.
     */
    byte[] getHash();

    /** The network this address was built for. */
    NetworkParameters getParameters();
}
