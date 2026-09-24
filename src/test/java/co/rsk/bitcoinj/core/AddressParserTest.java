package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.params.RegTestParams;
import co.rsk.bitcoinj.params.TestNet3Params;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AddressParserTest {

    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final NetworkParameters REGTEST = RegTestParams.get();

    /** The four types derived from one regtest public key, each confirmed against Bitcoin Core 31. */
    private static final String P2PKH = "n47u2xVMrzaVpgGDK5TJjZHKggM7r8CdAm";
    private static final String P2SH_P2WPKH = "2NAbe5uhy5x3de7CuT2ckaif2aF5BcsxJLf";
    private static final String P2WPKH = "bcrt1q7lhf4defwy62pnx8du74p62daut53revy0wmk2";
    private static final String P2TR =
        "bcrt1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnsdgtqq8";

    /** The bytes behind those four, which are the same on every network. */
    private static final String PUB_KEY_HASH = "f7ee9ab7297134a0ccc76f3d50e94def17488f2c";
    private static final String SCRIPT_HASH = "be56929d90f9eec61155469953f2e2e7ef400c6e";
    private static final String TAPROOT_PROGRAM =
        "4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7";

    @Test
    public void parseAddress_withEachSupportedType_shouldReturnTheRightImplementation() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        assertTrue(parser.parseAddress(P2PKH) instanceof LegacyAddress);
        assertTrue(parser.parseAddress(P2SH_P2WPKH) instanceof LegacyAddress);
        assertTrue(parser.parseAddress(P2WPKH) instanceof SegwitAddress);
        assertTrue(parser.parseAddress(P2TR) instanceof SegwitAddress);
    }

    @Test
    public void parseAddress_withEachSupportedType_shouldRoundTrip() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        for (String address : new String[] { P2PKH, P2SH_P2WPKH, P2WPKH, P2TR }) {
            assertEquals(address, address, parser.parseAddress(address).toString());
        }
    }

    @Test
    public void parseAddress_shouldRecoverTheUnderlyingBytes() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        assertEquals("f7ee9ab7297134a0ccc76f3d50e94def17488f2c",
            Utils.HEX.encode(parser.parseAddress(P2PKH).getHash()));
        assertEquals("f7ee9ab7297134a0ccc76f3d50e94def17488f2c",
            Utils.HEX.encode(parser.parseAddress(P2WPKH).getHash()));
        assertEquals("4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7",
            Utils.HEX.encode(parser.parseAddress(P2TR).getHash()));
    }

    @Test
    public void parseAddress_withLegacyAddressForAnotherNetwork_shouldThrow() {
        AddressParser parser = AddressParser.getDefault(MAINNET);

        assertThrows(AddressFormatException.WrongNetwork.class, () -> parser.parseAddress(P2PKH));
    }

    @Test
    public void parseAddress_withSegwitAddressForAnotherNetwork_shouldThrow() {
        AddressParser parser = AddressParser.getDefault(MAINNET);

        assertThrows(AddressFormatException.WrongNetwork.class, () -> parser.parseAddress(P2WPKH));
        assertThrows(AddressFormatException.WrongNetwork.class, () -> parser.parseAddress(P2TR));
    }

    /**
     * The same four types on mainnet and testnet, derived from the same key as the regtest ones
     * above, so every parser sees all four and the bytes are asserted rather than just the type.
     *
     * <p>Testnet and regtest share their base58 version bytes, so the legacy strings here are the
     * regtest ones. That is existing behaviour and the reason "another network is rejected" cannot
     * hold between those two for base58.</p>
     */
    @Test
    public void parseAddress_withMainnetParser_shouldParseAllFourTypes() {
        assertParsesAllFourTypes(MAINNET,
            "1PbwjuQP3y9F3ZnbbWUvue4zpgkQuSbgD5",
            "3K3S2AmwUVYHSKaMmtzsxmfmMts1s9RsXe",
            "bc1q7lhf4defwy62pnx8du74p62daut53revvqv96s",
            "bc1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnshehf0j");
    }

    @Test
    public void parseAddress_withTestnetParser_shouldParseAllFourTypes() {
        assertParsesAllFourTypes(TESTNET,
            "n47u2xVMrzaVpgGDK5TJjZHKggM7r8CdAm",
            "2NAbe5uhy5x3de7CuT2ckaif2aF5BcsxJLf",
            "tb1q7lhf4defwy62pnx8du74p62daut53revxxhkpr",
            "tb1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnsq3px4a");
    }

    private void assertParsesAllFourTypes(NetworkParameters params,
            String p2pkh, String p2shP2wpkh, String p2wpkh, String p2tr) {
        AddressParser parser = AddressParser.getDefault(params);

        Address legacy = parser.parseAddress(p2pkh);
        assertTrue(p2pkh, legacy instanceof LegacyAddress);
        assertEquals(p2pkh, PUB_KEY_HASH, Utils.HEX.encode(legacy.getHash()));

        Address nested = parser.parseAddress(p2shP2wpkh);
        assertTrue(p2shP2wpkh, nested instanceof LegacyAddress);
        assertTrue(p2shP2wpkh, ((LegacyAddress) nested).isP2SHAddress());
        assertEquals(p2shP2wpkh, SCRIPT_HASH, Utils.HEX.encode(nested.getHash()));

        Address nativeSegwit = parser.parseAddress(p2wpkh);
        assertTrue(p2wpkh, nativeSegwit instanceof SegwitAddress);
        assertEquals(p2wpkh, 0, ((SegwitAddress) nativeSegwit).getWitnessVersion());
        assertEquals(p2wpkh, PUB_KEY_HASH, Utils.HEX.encode(nativeSegwit.getHash()));

        Address taproot = parser.parseAddress(p2tr);
        assertTrue(p2tr, taproot instanceof SegwitAddress);
        assertEquals(p2tr, 1, ((SegwitAddress) taproot).getWitnessVersion());
        assertEquals(p2tr, TAPROOT_PROGRAM, Utils.HEX.encode(taproot.getHash()));
    }

    @Test
    public void parseAddress_withBadChecksum_shouldThrow() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        assertThrows(AddressFormatException.class,
            () -> parser.parseAddress("bcrt1q7lhf4defwy62pnx8du74p62daut53revy0wmk3"));
        assertThrows(AddressFormatException.class,
            () -> parser.parseAddress("n47u2xVMrzaVpgGDK5TJjZHKggM7r8CdAn"));
    }

    @Test
    public void parseAddress_withMixedCase_shouldThrow() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        assertThrows(AddressFormatException.class,
            () -> parser.parseAddress("bcrt1Q7lhf4defwy62pnx8du74p62daut53revy0wmk2"));
    }

    @Test
    public void parseAddress_withEmptyString_shouldThrow() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        assertThrows(AddressFormatException.class, () -> parser.parseAddress(""));
    }

    @Test
    public void parseAddress_withGarbage_shouldThrow() {
        AddressParser parser = AddressParser.getDefault(REGTEST);

        assertThrows(AddressFormatException.class, () -> parser.parseAddress("this is not an address"));
    }
}
