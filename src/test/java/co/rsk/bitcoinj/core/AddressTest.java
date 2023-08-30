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
 *


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

public class LegacyAddressTest {
    static final NetworkParameters testParams = TestNet3Params.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    @Test
    public void testJavaSerialization() throws Exception {
        LegacyAddress testLegacyAddress = LegacyAddress.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testLegacyAddress);
        VersionedChecksummedBytes testLegacyAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(testLegacyAddress, testLegacyAddressCopy);

        LegacyAddress mainLegacyAddress = LegacyAddress.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainLegacyAddress);
        VersionedChecksummedBytes mainLegacyAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(mainLegacyAddress, mainLegacyAddressCopy);
    }

    @Test
    public void stringification() throws Exception {
        // Test a testnet LegacyAddress.
        LegacyAddress a = new LegacyAddress(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toString());
        assertFalse(a.isP2SHLegacyAddress());

        LegacyAddress b = new LegacyAddress(mainParams, HEX.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));
        assertEquals("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL", b.toString());
        assertFalse(b.isP2SHLegacyAddress());
    }
    
    @Test
    public void decoding() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.HEX.encode(a.getHash160()));

        LegacyAddress b = LegacyAddress.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals("4a22c3c4cbb31e4d03b15550636762bda0baf85a", Utils.HEX.encode(b.getHash160()));
    }
    
    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            LegacyAddress.fromBase58(testParams, "this is not a valid LegacyAddress!");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (LegacyAddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            LegacyAddress.fromBase58(testParams, "");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (LegacyAddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            LegacyAddress.fromBase58(testParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
            fail();
        } catch (WrongNetworkException e) {
            // Success.
            assertEquals(e.verCode, MainNetParams.get().getLegacyAddressHeader());
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.get().getAcceptableLegacyAddressCodes()));
        } catch (LegacyAddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() throws Exception {
        NetworkParameters params = LegacyAddress.getParametersFromLegacyAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals(MainNetParams.get().getId(), params.getId());
        params = LegacyAddress.getParametersFromLegacyAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals(TestNet3Params.get().getId(), params.getId());
    }

    @Test
    public void getAltNetwork() throws Exception {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                LegacyAddressHeader = 48;
                p2shHeader = 5;
                acceptableLegacyAddressCodes = new int[] { LegacyAddressHeader, p2shHeader };
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse LegacyAddress
        NetworkParameters params = LegacyAddress.getParametersFromLegacyAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = LegacyAddress.getParametersFromLegacyAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals(MainNetParams.get().getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            LegacyAddress.getParametersFromLegacyAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (LegacyAddressFormatException e) { }
    }
    
    @Test
    public void p2shLegacyAddress() throws Exception {
        // Test that we can construct P2SH LegacyAddresses
        LegacyAddress mainNetP2SHLegacyAddress = LegacyAddress.fromBase58(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(mainNetP2SHLegacyAddress.version, MainNetParams.get().p2shHeader);
        assertTrue(mainNetP2SHLegacyAddress.isP2SHLegacyAddress());
        LegacyAddress testNetP2SHLegacyAddress = LegacyAddress.fromBase58(TestNet3Params.get(), "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(testNetP2SHLegacyAddress.version, TestNet3Params.get().p2shHeader);
        assertTrue(testNetP2SHLegacyAddress.isP2SHLegacyAddress());

        // Test that we can determine what network a P2SH LegacyAddress belongs to
        NetworkParameters mainNetParams = LegacyAddress.getParametersFromLegacyAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(MainNetParams.get().getId(), mainNetParams.getId());
        NetworkParameters testNetParams = LegacyAddress.getParametersFromLegacyAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
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
        LegacyAddress a = new LegacyAddress(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        LegacyAddress b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() throws Exception {
        String base58 = "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL";
        assertEquals(base58, LegacyAddress.fromBase58(null, base58).toBase58());
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
}
*/
