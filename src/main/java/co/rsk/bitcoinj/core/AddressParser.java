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
     * @param params the network to parse for
     * @return a parser for that network
     */
    static AddressParser getDefault(NetworkParameters params) {
        return addressString -> parse(addressString, params);
    }

    /**
     * Base58 first, bech32 second, which is upstream's order. A wrong network on the first attempt
     * is rethrown rather than retried, so that a legacy address for another network reports that
     * rather than failing later as malformed bech32.
     */
    static Address parse(String addressString, NetworkParameters params) throws AddressFormatException {
        try {
            return LegacyAddress.fromBase58(params, addressString);
        } catch (AddressFormatException.WrongNetwork x) {
            throw x;
        } catch (AddressFormatException x) {
            try {
                return SegwitAddress.fromBech32(params, addressString);
            } catch (AddressFormatException.WrongNetwork x2) {
                throw x;
            } catch (AddressFormatException x2) {
                throw new AddressFormatException(addressString);
            }
        }
    }
}
