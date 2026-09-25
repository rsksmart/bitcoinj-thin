/*
 * Copyright 2023 Sean Gilligan
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
 * <p>Functional interface for parsing the textual form of an address.</p>
 *
 * <p>Ported from bitcoinj 0.17.1, {@code core/src/main/java/org/bitcoinj/base/AddressParser.java}.
 * Upstream reaches the parsing through an {@code AddressParserProvider} and offers variants that
 * guess the network from the string; neither is ported. The bridge always knows its own network,
 * and an address for another one has to be rejected rather than guessed at, since this reads
 * consensus storage.</p>
 */
@FunctionalInterface
public interface AddressParser {

    /**
     * Parse an address for the network this parser was built for.
     *
     * @param addressString string representation of an address
     * @return a validated address object
     * @throws AddressFormatException invalid address string
     */
    Address parseAddress(String addressString) throws AddressFormatException;

    /**
     * <p>The encoding is chosen by the prefix, not by trying one and falling back to the other.
     * Upstream tries base58 first and reports its failure whenever bech32 also fails, so a bech32
     * address for another network comes back as an illegal base58 character. Swapping which
     * failure wins just moves the problem to the other encoding: a malformed base58 address then
     * reports a bech32 error. Only picking the decoder up front lets each encoding report its
     * own reason, which is what a caller reading consensus storage needs.</p>
     *
     * <p>The prefix is matched against the segwit part of any network, not just this one, so that
     * a bech32 address for another network reaches the segwit decoder and is reported as a wrong
     * network rather than as malformed base58.</p>
     *
     * @param params the network to parse for
     * @return a parser for that network
     */
    static AddressParser getDefault(NetworkParameters params) {
        return addressString -> NetworkParameters.hasSegwitHrp(addressString)
            ? SegwitAddress.fromBech32(params, addressString)
            : LegacyAddress.fromBase58(params, addressString);
    }
}
