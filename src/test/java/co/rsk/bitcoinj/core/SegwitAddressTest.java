package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.params.RegTestParams;
import co.rsk.bitcoinj.params.TestNet3Params;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Vectors ported from bitcoinj 0.17.1 SegwitAddressTest, which takes them from BIP173 and BIP350.
 * Upstream parses through AddressParser and asserts the output script; neither exists here yet, so
 * these parse through {@link SegwitAddress#fromBech32} and assert the witness version and program.
 */
public class SegwitAddressTest {

    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final NetworkParameters REGTEST = RegTestParams.get();

    private static class AddressData {
        final String address;
        final NetworkParameters expectedParams;
        final String expectedWitnessProgram;
        final int expectedWitnessVersion;

        AddressData(String address, NetworkParameters expectedParams, String expectedWitnessProgram,
                int expectedWitnessVersion) {
            this.address = address;
            this.expectedParams = expectedParams;
            this.expectedWitnessProgram = expectedWitnessProgram;
            this.expectedWitnessVersion = expectedWitnessVersion;
        }
    }

    /** From BIP350, which includes the corrected BIP173 vectors. */
    private static final AddressData[] VALID_ADDRESSES = {
        new AddressData("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", MAINNET,
            "751e76e8199196d454941c45d1b3a323f1433bd6", 0),
        new AddressData("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", TESTNET,
            "1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262", 0),
        new AddressData("BC1SW50QGDZ25J", MAINNET, "751e", 16),
        new AddressData("bc1zw508d6qejxtdg4y5r3zarvaryvaxxpcs", MAINNET,
            "751e76e8199196d454941c45d1b3a323", 2),
        new AddressData("tb1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesrxh6hy", TESTNET,
            "000000c4a5cad46221b2a187905e5266362b99d5e91c6ce24d165dab93e86433", 0),
        new AddressData("tb1pqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesf3hn0c", TESTNET,
            "000000c4a5cad46221b2a187905e5266362b99d5e91c6ce24d165dab93e86433", 1),
        new AddressData("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", MAINNET,
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798", 1),
    };

    /** From BIP173 and BIP350. */
    private static final String[] INVALID_ADDRESSES = {
        "tc1qw508d6qejxtdg4y5r3zarvary0c5xw7kg3g4ty",   // invalid human-readable part
        "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5",   // invalid checksum
        "BC13W508D6QEJXTDG4Y5R3ZARVARY0C5XW7KN40WF2",   // invalid witness version
        "bc1rw5uspcuh",                                 // invalid program length
        "bc10w508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kw5rljs90", // invalid program length
        "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P",         // invalid program length for witness version 0
        "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sL5k7", // mixed case
        "bc1zw508d6qejxtdg4y5r3zarvaryvqyzf3du",        // zero padding of more than 4 bits
        "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3pjxtptv", // non-zero padding in 8-to-5 conversion
        "bc1gmk9yu",                                    // empty data section
    };

    @Test
    public void fromBech32_withValidAddresses_shouldReturnExpectedVersionAndProgram() {
        for (AddressData valid : VALID_ADDRESSES) {
            SegwitAddress address = SegwitAddress.fromBech32(valid.expectedParams, valid.address);

            assertEquals(valid.address, valid.expectedWitnessVersion, address.getWitnessVersion());
            assertArrayEquals(valid.address,
                Utils.HEX.decode(valid.expectedWitnessProgram), address.getWitnessProgram());
            assertEquals(valid.address, valid.address.toLowerCase(Locale.ROOT), address.toBech32());
            assertEquals(valid.address, valid.expectedParams, address.getParameters());
        }
    }

    @Test
    /**
     * Each vector is asserted against each network separately. Parsing them in sequence inside one
     * try meant a tb1 vector threw on the mainnet call for network mismatch and the testnet call
     * never ran, so the check the vector exists for was never reached.
     */
    public void fromBech32_withInvalidAddresses_shouldThrow() {
        for (String invalid : INVALID_ADDRESSES) {
            assertThrows(invalid, AddressFormatException.class,
                () -> SegwitAddress.fromBech32(MAINNET, invalid));
            assertThrows(invalid, AddressFormatException.class,
                () -> SegwitAddress.fromBech32(TESTNET, invalid));
        }
    }

    /**
     * Every supported shape on every network, built from a program rather than transcribed, so the
     * combinations missing from the vector list above are covered without adding literals that
     * could be mistyped.
     */
    @Test
    public void toBech32_thenFromBech32_shouldRoundTripOnEveryNetwork() {
        byte[] pubKeyHash = Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");
        byte[] scriptHash =
            Utils.HEX.decode("1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262");

        for (NetworkParameters params : new NetworkParameters[]{MAINNET, TESTNET, REGTEST}) {
            assertRoundTrips(params, 0, pubKeyHash);
            assertRoundTrips(params, 0, scriptHash);
            assertRoundTrips(params, 1, scriptHash);
        }
    }

    private void assertRoundTrips(NetworkParameters params, int witnessVersion, byte[] program) {
        SegwitAddress built = SegwitAddress.fromProgram(params, witnessVersion, program);
        String bech32 = built.toBech32();

        SegwitAddress reparsed = SegwitAddress.fromBech32(params, bech32);

        assertEquals(bech32, built, reparsed);
        assertEquals(bech32, witnessVersion, reparsed.getWitnessVersion());
        assertArrayEquals(bech32, program, reparsed.getWitnessProgram());
        assertTrue(bech32, bech32.startsWith(params.getSegwitHrp() + "1"));
    }

    @Test
    public void fromBech32_withAddressForAnotherNetwork_shouldThrow() {
        assertThrows(AddressFormatException.WrongNetwork.class,
            () -> SegwitAddress.fromBech32(REGTEST, "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"));
    }

    @Test
    public void fromBech32_withWitnessVersionZeroEncodedAsBech32m_shouldThrow() {
        // Same program as the first valid vector, encoded with the bech32m constant.
        String bech32m = Bech32.encode(Bech32.Encoding.BECH32M, "bc",
            Bech32.Bech32Bytes.ofSegwit((short) 0, Utils.HEX.decode("751e76e8199196d454941c45d1b3a323f1433bd6")));

        assertThrows(AddressFormatException.UnexpectedWitnessVersion.class,
            () -> SegwitAddress.fromBech32(MAINNET, bech32m));
    }

    @Test
    public void fromBech32_withWitnessVersionOneEncodedAsBech32_shouldThrow() {
        String taproot = Utils.HEX.encode(
            Utils.HEX.decode("4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7"));
        String bech32 = Bech32.encode(Bech32.Encoding.BECH32, "bcrt",
            Bech32.Bech32Bytes.ofSegwit((short) 1, Utils.HEX.decode(taproot)));

        assertThrows(AddressFormatException.UnexpectedWitnessVersion.class,
            () -> SegwitAddress.fromBech32(REGTEST, bech32));
    }

    @Test
    public void fromBech32_withRegtestTaproot_shouldRoundTrip() {
        String expected = "bcrt1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnsdgtqq8";

        SegwitAddress address = SegwitAddress.fromBech32(REGTEST, expected);

        assertEquals(1, address.getWitnessVersion());
        assertArrayEquals(
            Utils.HEX.decode("4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7"),
            address.getWitnessProgram());
        assertEquals(expected, address.toBech32());
    }

    @Test
    public void fromBech32_withRegtestNativeSegwit_shouldRoundTrip() {
        String expected = "bcrt1q7lhf4defwy62pnx8du74p62daut53revy0wmk2";

        SegwitAddress address = SegwitAddress.fromBech32(REGTEST, expected);

        assertEquals(0, address.getWitnessVersion());
        assertArrayEquals(Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c"),
            address.getWitnessProgram());
        assertEquals(expected, address.toBech32());
    }

    /**
     * Upstream folds this into its valid address loop, extracting the hash from the output script
     * with ScriptPattern. Neither exists here yet, so the hash comes from the vector instead.
     */
    @Test
    public void fromHash_withWitnessVersionZeroVectors_shouldMatchTheParsedAddress() {
        for (AddressData valid : VALID_ADDRESSES) {
            if (valid.expectedWitnessVersion != 0) {
                continue;
            }
            SegwitAddress parsed = SegwitAddress.fromBech32(valid.expectedParams, valid.address);

            SegwitAddress fromHash = SegwitAddress.fromHash(valid.expectedParams,
                Utils.HEX.decode(valid.expectedWitnessProgram));

            assertEquals(valid.address, parsed, fromHash);
            assertEquals(valid.address, valid.address.toLowerCase(Locale.ROOT), fromHash.toBech32());
        }
    }

    @Test
    public void fromHash_withPubKeyHash_shouldBuildAP2wpkhAddress() {
        byte[] hash = Utils.HEX.decode("751e76e8199196d454941c45d1b3a323f1433bd6");

        SegwitAddress address = SegwitAddress.fromHash(MAINNET, hash);

        assertEquals(0, address.getWitnessVersion());
        assertArrayEquals(hash, address.getWitnessProgram());
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", address.toBech32());
    }

    @Test
    public void fromHash_withScriptHash_shouldBuildAP2wshAddress() {
        byte[] hash = Utils.HEX.decode(
            "1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262");

        SegwitAddress address = SegwitAddress.fromHash(TESTNET, hash);

        assertEquals(0, address.getWitnessVersion());
        assertArrayEquals(hash, address.getWitnessProgram());
        assertEquals("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7",
            address.toBech32());
    }

    @Test
    public void fromProgram_withTaproot_shouldBuildABech32mAddress() {
        byte[] program = Utils.HEX.decode(
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");

        SegwitAddress address = SegwitAddress.fromProgram(MAINNET, 1, program);

        assertEquals(1, address.getWitnessVersion());
        assertArrayEquals(program, address.getWitnessProgram());
        assertEquals("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0",
            address.toBech32());
    }

    /**
     * The witness version picks the encoding, so the same program under version 0 and version 1
     * must produce different addresses: bech32 for the first, bech32m for the second.
     */
    @Test
    public void fromProgram_withTheSameProgramUnderTwoVersions_shouldEncodeDifferently() {
        byte[] program = Utils.HEX.decode(
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");

        String version0 = SegwitAddress.fromProgram(MAINNET, 0, program).toBech32();
        String version1 = SegwitAddress.fromProgram(MAINNET, 1, program).toBech32();

        assertEquals("bc1q0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqgp5m2n", version0);
        assertEquals("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", version1);
        assertNotEquals(version0, version1);
    }

    @Test
    public void fromProgram_withWitnessVersionZeroAndInvalidLength_shouldThrow() {
        assertThrows(AddressFormatException.InvalidDataLength.class,
            () -> SegwitAddress.fromProgram(MAINNET, 0, new byte[21]));
    }

    @Test
    public void fromProgram_withTaprootTooShort_shouldThrow() {
        assertThrows(AddressFormatException.InvalidDataLength.class,
            () -> SegwitAddress.fromProgram(MAINNET, 1, new byte[20]));
    }

    @Test
    public void fromProgram_withTaprootTooLong_shouldThrow() {
        assertThrows(AddressFormatException.InvalidDataLength.class,
            () -> SegwitAddress.fromProgram(MAINNET, 1, new byte[33]));
    }

    @Test
    public void fromProgram_withProgramTooShort_shouldThrow() {
        assertThrows(AddressFormatException.InvalidDataLength.class,
            () -> SegwitAddress.fromProgram(MAINNET, 2, new byte[1]));
    }

    @Test
    public void fromProgram_withProgramTooLong_shouldThrow() {
        assertThrows(AddressFormatException.InvalidDataLength.class,
            () -> SegwitAddress.fromProgram(MAINNET, 2, new byte[41]));
    }

    @Test
    public void fromProgram_withWitnessVersionOutOfRange_shouldThrow() {
        assertThrows(AddressFormatException.class,
            () -> SegwitAddress.fromProgram(MAINNET, 17, new byte[20]));
        assertThrows(AddressFormatException.class,
            () -> SegwitAddress.fromProgram(MAINNET, -1, new byte[20]));
    }

    @Test
    public void equals_withEquivalentAddresses_shouldReturnTrue() {
        byte[] program = Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");

        SegwitAddress fromProgram = SegwitAddress.fromProgram(REGTEST, 0, program);
        SegwitAddress fromBech32 = SegwitAddress.fromBech32(REGTEST,
            "bcrt1q7lhf4defwy62pnx8du74p62daut53revy0wmk2");

        assertEquals(fromProgram, fromBech32);
        assertEquals(fromProgram.hashCode(), fromBech32.hashCode());
    }

    /**
     * Upstream compares its Network by reference. NetworkParameters is not an enum, so a second
     * instance of the same network has to still be equal, or a HashMap lookup misses while
     * hashCode says the two agree.
     */
    @Test
    public void equals_withASecondInstanceOfTheSameNetwork_shouldReturnTrue() {
        byte[] program = Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");

        SegwitAddress fromSingleton = SegwitAddress.fromHash(RegTestParams.get(), program);
        SegwitAddress fromNewInstance = SegwitAddress.fromHash(new RegTestParams(), program);

        assertEquals(fromSingleton, fromNewInstance);
        assertEquals(fromSingleton.hashCode(), fromNewInstance.hashCode());

        Map<SegwitAddress, String> byAddress = new HashMap<>();
        byAddress.put(fromSingleton, "found");
        assertEquals("found", byAddress.get(fromNewInstance));
    }

    @Test
    public void equals_withDifferentNetwork_shouldReturnFalse() {
        byte[] program = Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");

        assertNotEquals(SegwitAddress.fromProgram(MAINNET, 0, program),
            SegwitAddress.fromProgram(TESTNET, 0, program));
    }

    @Test
    public void equals_withDifferentWitnessVersion_shouldReturnFalse() {
        byte[] program = Utils.HEX.decode(
            "4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7");

        assertNotEquals(SegwitAddress.fromProgram(MAINNET, 1, program),
            SegwitAddress.fromProgram(MAINNET, 2, program));
    }

    @Test
    public void getSegwitHrp_shouldReturnThePrefixForEachNetwork() {
        assertEquals("bc", MAINNET.getSegwitHrp());
        assertEquals("tb", TESTNET.getSegwitHrp());
        assertEquals("bcrt", REGTEST.getSegwitHrp());
    }

    @Test
    public void getSegwitHrp_withUnknownNetwork_shouldThrow() {
        NetworkParameters unknown = new MainNetParams() {
            @Override
            public String getId() {
                return "some.unknown.network";
            }
        };

        assertThrows(IllegalStateException.class, unknown::getSegwitHrp);
    }
}
