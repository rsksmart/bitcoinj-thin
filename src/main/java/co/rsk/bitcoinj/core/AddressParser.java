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
     * <p>Base58 first, bech32 second, which is upstream's order. A wrong network is reported as
     * such from either encoding, rather than being reduced to a format error.</p>
     *
     * <p>Upstream rethrows the base58 failure when it is bech32 that reports the wrong network,
     * which turns a wrong-network address into an unrelated "illegal character" message. We throw
     * the bech32 failure instead. This is the one behavioural difference from upstream in this
     * class, and it matches what upstream's own javadoc says should happen.</p>
     *
     * @param params the network to parse for
     * @return a parser for that network
     */
    static AddressParser getDefault(NetworkParameters params) {
        return addressString -> {
            try {
                return LegacyAddress.fromBase58(params, addressString);
            } catch (AddressFormatException.WrongNetwork x) {
                throw x;
            } catch (AddressFormatException x) {
                try {
                    return SegwitAddress.fromBech32(params, addressString);
                } catch (AddressFormatException.WrongNetwork x2) {
                    throw x2;
                } catch (AddressFormatException x2) {
                    throw new AddressFormatException(addressString);
                }
            }
        };
    }
}
