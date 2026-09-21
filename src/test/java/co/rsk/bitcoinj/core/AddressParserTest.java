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

        assertThrows(AddressFormatException.class, () -> parser.parseAddress(P2WPKH));
        assertThrows(AddressFormatException.class, () -> parser.parseAddress(P2TR));
    }

    @Test
    public void parseAddress_withMainnetParser_shouldParseMainnetAddresses() {
        AddressParser parser = AddressParser.getDefault(MAINNET);

        assertTrue(parser.parseAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL") instanceof LegacyAddress);
        assertTrue(parser.parseAddress("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4".toLowerCase())
            instanceof SegwitAddress);
    }

    @Test
    public void parseAddress_withTestnetParser_shouldParseTestnetAddresses() {
        AddressParser parser = AddressParser.getDefault(TESTNET);

        assertTrue(parser.parseAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv") instanceof LegacyAddress);
        assertTrue(parser.parseAddress(
            "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7") instanceof SegwitAddress);
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
