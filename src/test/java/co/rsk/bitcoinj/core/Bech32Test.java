package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.core.Bech32.Bech32Data;
import co.rsk.bitcoinj.core.Bech32.Encoding;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class Bech32Test {

    /** BIP173 valid checksums. */
    private static final String[] VALID_BECH32 = {
        "A12UEL5L",
        "a12uel5l",
        "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs",
        "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw",
        "11qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqc8247j",
        "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w",
        "?1ezyfcl"
    };

    /** BIP350 valid checksums. */
    private static final String[] VALID_BECH32M = {
        "A1LQFN3A",
        "a1lqfn3a",
        "an83characterlonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11sg7hg6",
        "abcdef1l7aum6echk45nj3s0wdvt2fg8x9yrzpqzd3ryx",
        "11llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllludsr8",
        "split1checkupstagehandshakeupstreamerranterredcaperredlc445v",
        "?1v759aa"
    };

    /** Invalid under both BIP173 and BIP350. */
    /** BIP173 invalid, with the BIP's own reasons. */
    private static final String[] INVALID_BECH32 = {
        "\u00201nwldj5",                                                                            // HRP character out of range
        "\u007f1axkwrx",                                                                            // HRP character out of range
        "\u00801eym55h",                                                                            // HRP character out of range
        "an84characterslonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1569pvx", // overall max length exceeded
        "pzry9x0s0muk",                                                                             // no separator character
        "1pzry9x0s0muk",                                                                            // empty HRP
        "x1b4n0q5v",                                                                                // invalid data character
        "li1dgmt3",                                                                                 // too short checksum
        "de1lg7wt\u00ff",                                                                           // invalid character in checksum
        "A1G7SGD8",                                                                                 // checksum calculated with uppercase form of HRP
        "10a06t8",                                                                                  // empty HRP
        "1qzzfhee"                                                                                  // empty HRP
    };

    /** BIP350 invalid, with the BIP's own reasons. */
    private static final String[] INVALID_BECH32M = {
        "\u00201xj0phk",                                                                            // HRP character out of range
        "\u007f1g6xzxy",                                                                            // HRP character out of range
        "\u00801vctc34",                                                                            // HRP character out of range
        "an84characterslonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11d6pts4", // overall max length exceeded
        "qyrz8wqd2c9m",                                                                             // no separator character
        "1qyrz8wqd2c9m",                                                                            // empty HRP
        "y1b0jsk6g",                                                                                // invalid data character
        "lt1igcx5c0",                                                                               // invalid data character
        "in1muywd",                                                                                 // too short checksum
        "mm1crxm3i",                                                                                // invalid character in checksum
        "au1s5cgom",                                                                                // invalid character in checksum
        "M1VUXWEZ",                                                                                 // checksum calculated with uppercase form of HRP
        "16plkw9",                                                                                  // empty HRP
        "1p2gdwpf"                                                                                  // empty HRP
    };

    @Test
    public void decode_withValidBech32Vectors_shouldReportBech32() {
        for (String vector : VALID_BECH32) {
            assertEquals(vector, Encoding.BECH32, Bech32.decode(vector).encoding);
        }
    }

    @Test
    public void decode_withValidBech32mVectors_shouldReportBech32m() {
        for (String vector : VALID_BECH32M) {
            assertEquals(vector, Encoding.BECH32M, Bech32.decode(vector).encoding);
        }
    }

    @Test
    public void decode_withInvalidBech32Vectors_shouldThrow() {
        for (String vector : INVALID_BECH32) {
            assertThrows(vector, AddressFormatException.class, () -> Bech32.decode(vector));
        }
    }

    @Test
    public void decode_withInvalidBech32mVectors_shouldThrow() {
        for (String vector : INVALID_BECH32M) {
            assertThrows(vector, AddressFormatException.class, () -> Bech32.decode(vector));
        }
    }

    @Test
    public void decode_withBech32Vector_shouldRoundTrip() {
        for (String vector : VALID_BECH32) {
            Bech32Data decoded = Bech32.decode(vector);
            assertEquals(vector.toLowerCase(),
                Bech32.encode(Encoding.BECH32, decoded.hrp, decoded));
        }
    }

    @Test
    public void decode_withBech32mVector_shouldRoundTrip() {
        for (String vector : VALID_BECH32M) {
            Bech32Data decoded = Bech32.decode(vector);
            assertEquals(vector.toLowerCase(),
                Bech32.encode(Encoding.BECH32M, decoded.hrp, decoded));
        }
    }

    @Test
    public void decode_withMixedCase_shouldThrow() {
        assertThrows(AddressFormatException.class, () -> Bech32.decode("A12Uel5l"));
    }

    @Test
    public void encodeBytes_thenDecodeBytes_shouldReturnTheSameBytes() {
        byte[] payload = Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");

        String encoded = Bech32.encodeBytes(Encoding.BECH32M, "bcrt", payload);

        assertArrayEquals(payload, Bech32.decodeBytes(encoded, "bcrt", Encoding.BECH32M));
    }

    @Test
    public void decodeBytes_withUnexpectedHrp_shouldThrow() {
        String encoded = Bech32.encodeBytes(Encoding.BECH32, "bcrt", new byte[20]);

        assertThrows(AddressFormatException.class,
            () -> Bech32.decodeBytes(encoded, "bc", Encoding.BECH32));
    }

    @Test
    public void decodeBytes_withUnexpectedEncoding_shouldThrow() {
        String encoded = Bech32.encodeBytes(Encoding.BECH32, "bcrt", new byte[20]);

        assertThrows(AddressFormatException.class,
            () -> Bech32.decodeBytes(encoded, "bcrt", Encoding.BECH32M));
    }

    @Test
    public void encode_withEmptyHrp_shouldThrow() {
        assertThrows(AddressFormatException.class,
            () -> Bech32.encodeBytes(Encoding.BECH32, "", new byte[10]));
    }

    @Test
    public void ofSegwit_thenDecode_shouldReturnTheSameVersionAndProgram() {
        byte[] program = Utils.HEX.decode(
            "4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7");

        String encoded = Bech32.encode(Encoding.BECH32M, "bcrt", Bech32.Bech32Bytes.ofSegwit((short) 1, program));
        Bech32Data decoded = Bech32.decode(encoded);

        assertEquals(1, decoded.witnessVersion());
        assertArrayEquals(program, decoded.witnessProgram());
    }

    @Test
    public void ofSegwit_withWitnessVersionZero_shouldRoundTrip() {
        byte[] program = Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");

        String encoded = Bech32.encode(Encoding.BECH32, "bcrt", Bech32.Bech32Bytes.ofSegwit((short) 0, program));
        Bech32Data decoded = Bech32.decode(encoded);

        assertEquals(0, decoded.witnessVersion());
        assertArrayEquals(program, decoded.witnessProgram());
    }
}
