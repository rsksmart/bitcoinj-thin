/*
 * Copyright 2011 Google Inc.
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

import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.params.Networks;
import co.rsk.bitcoinj.params.TestNet3Params;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static co.rsk.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class AddressTest {
    static final NetworkParameters testParams = TestNet3Params.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    /** A pub key hash and the base58 it renders as on testnet. */
    private static final String PUB_KEY_HASH_HEX = "fda79a24e50ff70ff42f7d89585da5bd19d9e5cc";
    private static final String TESTNET_BASE58 = "n4eA2nbYqErp7H6jebchxAN59DmNpksexv";
    /** A second pub key hash and the base58 it renders as on mainnet. */
    private static final String OTHER_PUB_KEY_HASH_HEX = "4a22c3c4cbb31e4d03b15550636762bda0baf85a";
    private static final String MAINNET_BASE58 = "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL";

    private static final int PUB_KEY_HASH_LENGTH = 20;

    @Test
    public void testJavaSerialization() throws Exception {
        LegacyAddress testAddress = LegacyAddress.fromBase58(testParams, TESTNET_BASE58);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testAddress);
        VersionedChecksummedBytes testAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(testAddress, testAddressCopy);

        LegacyAddress mainAddress = LegacyAddress.fromBase58(mainParams, MAINNET_BASE58);
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainAddress);
        VersionedChecksummedBytes mainAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(mainAddress, mainAddressCopy);
    }

    @Test
    public void stringification() throws Exception {
        // Test a testnet address.
        LegacyAddress a = new LegacyAddress(testParams, HEX.decode(PUB_KEY_HASH_HEX));
        assertEquals(TESTNET_BASE58, a.toString());
        assertFalse(a.isP2SHAddress());

        LegacyAddress b = new LegacyAddress(mainParams, HEX.decode(OTHER_PUB_KEY_HASH_HEX));
        assertEquals(MAINNET_BASE58, b.toString());
        assertFalse(b.isP2SHAddress());
    }
    
    @Test
    public void decoding() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(testParams, TESTNET_BASE58);
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.HEX.encode(a.getHash160()));

        LegacyAddress b = LegacyAddress.fromBase58(mainParams, MAINNET_BASE58);
        assertEquals("4a22c3c4cbb31e4d03b15550636762bda0baf85a", Utils.HEX.encode(b.getHash160()));
    }
    
    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            LegacyAddress.fromBase58(testParams, "this is not a valid address!");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            LegacyAddress.fromBase58(testParams, "");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            LegacyAddress.fromBase58(testParams, MAINNET_BASE58);
            fail();
        } catch (WrongNetworkException e) {
            // Success.
            assertEquals(e.verCode, MainNetParams.get().getAddressHeader());
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.get().getAcceptableAddressCodes()));
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() throws Exception {
        NetworkParameters params = LegacyAddress.getParametersFromAddress(MAINNET_BASE58);
        assertEquals(MainNetParams.get().getId(), params.getId());
        params = LegacyAddress.getParametersFromAddress(TESTNET_BASE58);
        assertEquals(TestNet3Params.get().getId(), params.getId());
    }

    @Test
    public void getAltNetwork() throws Exception {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
                acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        NetworkParameters params = LegacyAddress.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = LegacyAddress.getParametersFromAddress(MAINNET_BASE58);
        assertEquals(MainNetParams.get().getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            LegacyAddress.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) { }
    }
    
    @Test
    public void p2shAddress() throws Exception {
        // Test that we can construct P2SH addresses
        LegacyAddress mainNetP2SHAddress = LegacyAddress.fromBase58(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(mainNetP2SHAddress.version, MainNetParams.get().p2shHeader);
        assertTrue(mainNetP2SHAddress.isP2SHAddress());
        LegacyAddress testNetP2SHAddress = LegacyAddress.fromBase58(TestNet3Params.get(), "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(testNetP2SHAddress.version, TestNet3Params.get().p2shHeader);
        assertTrue(testNetP2SHAddress.isP2SHAddress());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = LegacyAddress.getParametersFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(MainNetParams.get().getId(), mainNetParams.getId());
        NetworkParameters testNetParams = LegacyAddress.getParametersFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(TestNet3Params.get().getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("2ac4b0b501117cc8119c5797b519538d4942e90e");
        LegacyAddress a = LegacyAddress.fromP2SHHash(mainParams, hex);
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", a.toString());
        LegacyAddress b = LegacyAddress.fromP2SHHash(testParams, HEX.decode("18a0e827269b5211eb51a4af1b2fa69333efa722"));
        assertEquals("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe", b.toString());
        LegacyAddress c = LegacyAddress.fromP2SHScript(mainParams, ScriptBuilder.createP2SHOutputScript(hex));
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", c.toString());
    }

    @Test
    public void cloning() throws Exception {
        LegacyAddress a = new LegacyAddress(testParams, HEX.decode(PUB_KEY_HASH_HEX));
        LegacyAddress b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() throws Exception {
        assertEquals(MAINNET_BASE58, LegacyAddress.fromBase58(null, MAINNET_BASE58).toBase58());
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        LegacyAddress b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonEqualTo() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        LegacyAddress b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonLessThan() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        LegacyAddress b = LegacyAddress.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");

        int result = a.compareTo(b);
        assertTrue(result < 0);
    }

    @Test
    public void comparisonGreaterThan() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");
        LegacyAddress b = LegacyAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");

        int result = a.compareTo(b);
        assertTrue(result > 0);
    }

    @Test
    public void comparisonBytesVsString() throws Exception {
        // TODO: To properly test this we need a much larger data set
        LegacyAddress a = LegacyAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        LegacyAddress b = LegacyAddress.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");

        int resultBytes = a.compareTo(b);
        int resultsString = a.toString().compareTo(b.toString());
        assertTrue( resultBytes < 0 );
        assertTrue( resultsString < 0 );
    }

    @Test
    public void equals_withDifferentClass_onMainnet_shouldReturnFalse() {
        assertNotEqualToPlainVersionedBytes(mainParams);
    }

    @Test
    public void equals_withDifferentClass_onTestnet_shouldReturnFalse() {
        assertNotEqualToPlainVersionedBytes(testParams);
    }

    private void assertNotEqualToPlainVersionedBytes(NetworkParameters params) {
        byte[] hash160 = HEX.decode(PUB_KEY_HASH_HEX);

        LegacyAddress address = new LegacyAddress(params, hash160);
        VersionedChecksummedBytes sameVersionAndBytes =
            new VersionedChecksummedBytes(params.getAddressHeader(), hash160);

        assertNotEquals(address, sameVersionAndBytes);
    }

    @Test
    public void equals_withDifferentVersion_shouldReturnFalse() {
        byte[] hash160 = HEX.decode(PUB_KEY_HASH_HEX);

        LegacyAddress mainNetAddress = new LegacyAddress(mainParams, hash160);
        LegacyAddress testNetAddress = new LegacyAddress(testParams, hash160);

        assertNotEquals(mainNetAddress, testNetAddress);
    }

    @Test
    public void equals_withDifferentHash_onMainnet_shouldReturnFalse() {
        assertDifferentHashesAreNotEqual(mainParams);
    }

    @Test
    public void equals_withDifferentHash_onTestnet_shouldReturnFalse() {
        assertDifferentHashesAreNotEqual(testParams);
    }

    private void assertDifferentHashesAreNotEqual(NetworkParameters params) {
        LegacyAddress address = new LegacyAddress(params, HEX.decode(PUB_KEY_HASH_HEX));
        LegacyAddress anotherAddress = new LegacyAddress(params, HEX.decode(OTHER_PUB_KEY_HASH_HEX));

        assertNotEquals(address, anotherAddress);
    }

    @Test
    public void hashCode_withEqualAddresses_shouldMatch() {
        assertHashCodeMatches(testParams, PUB_KEY_HASH_HEX, TESTNET_BASE58);
        assertHashCodeMatches(mainParams, OTHER_PUB_KEY_HASH_HEX, MAINNET_BASE58);
    }

    private void assertHashCodeMatches(NetworkParameters params, String hash160Hex, String base58) {
        LegacyAddress fromHash = new LegacyAddress(params, HEX.decode(hash160Hex));
        LegacyAddress fromBase58 = LegacyAddress.fromBase58(params, base58);

        assertEquals(fromHash, fromBase58);
        assertEquals(fromHash.hashCode(), fromBase58.hashCode());
    }

    @Test
    public void constructor_withHashShorterThanTwentyBytes_onMainnet_shouldThrow() {
        assertRejectsLengthsBelowTwenty(mainParams);
    }

    @Test
    public void constructor_withHashShorterThanTwentyBytes_onTestnet_shouldThrow() {
        assertRejectsLengthsBelowTwenty(testParams);
    }

    private void assertRejectsLengthsBelowTwenty(NetworkParameters params) {
        for (int length = 0; length < PUB_KEY_HASH_LENGTH; length++) {
            final int hashLength = length;

            assertThrows(IllegalArgumentException.class, () -> new LegacyAddress(params, new byte[hashLength]));
        }
    }

    @Test
    public void constructor_withHashLongerThanTwentyBytes_onMainnet_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyAddress(mainParams, new byte[PUB_KEY_HASH_LENGTH + 1]));
    }

    @Test
    public void constructor_withHashLongerThanTwentyBytes_onTestnet_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyAddress(testParams, new byte[PUB_KEY_HASH_LENGTH + 1]));
    }

    @Test
    public void constructor_withTwentyByteHash_shouldNotThrow() {
        LegacyAddress mainNetAddress = new LegacyAddress(mainParams, new byte[PUB_KEY_HASH_LENGTH]);
        LegacyAddress testNetAddress = new LegacyAddress(testParams, new byte[PUB_KEY_HASH_LENGTH]);

        assertEquals(PUB_KEY_HASH_LENGTH, mainNetAddress.getHash160().length);
        assertEquals(PUB_KEY_HASH_LENGTH, testNetAddress.getHash160().length);
    }
}
