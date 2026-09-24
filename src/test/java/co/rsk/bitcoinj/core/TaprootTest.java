package co.rsk.bitcoinj.core;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Vectors from BIP86, which gives an internal key and the output key it tweaks to. A compressed
 * public key with an even y has the internal key as its x coordinate, so prefixing it with 02
 * gives a key whose derivation must produce the listed output key.
 */
public class TaprootTest {

    @Test
    public void deriveOutputKey_withBip86FirstReceivingKey_shouldMatchTheVector() {
        assertDerives(
            "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115",
            "a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c");
    }

    @Test
    public void deriveOutputKey_withBip86SecondReceivingKey_shouldMatchTheVector() {
        assertDerives(
            "83dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145",
            "a82f29944d65b86ae6b5e5cc75e294ead6c59391a1edc5e016e3498c67fc7bbb");
    }

    @Test
    public void deriveOutputKey_withBip86ChangeKey_shouldMatchTheVector() {
        assertDerives(
            "399f1b2f4393f29a18c937859c5dd8a77350103157eb880f02e8c08214277cef",
            "882d74e5d0572d5a816cef0041a96b6c1de832f6f9676d9605c44d5e9a97d3dc");
    }

    /**
     * The one BIP341 key-path vector with an empty merkle root, which is the case this class
     * covers. BIP341's other vectors tweak with a script tree, which the Bridge never builds.
     * Asserted under both parities, since an x-only internal key carries none.
     */
    @Test
    public void deriveOutputKey_withBip341EmptyTreeVector_shouldMatchTheVector() {
        String internalKey = "d6889cb081036e0faefa3a35157ad71086b123b2b144b649798b494c300a961d";
        String expected = "53a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343";

        assertDerives(internalKey, expected);
        assertEquals(expected, Utils.HEX.encode(Taproot.deriveOutputKey(
            BtcECKey.fromPublicOnly(Utils.HEX.decode("03" + internalKey)))));
    }

    @Test
    public void deriveOutputKey_withOddYKey_shouldMatchItsEvenCounterpart() {
        // lift_x drops the y parity, so a key and its negation must tweak to the same output key.
        String x = "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";

        byte[] fromEvenY = Taproot.deriveOutputKey(BtcECKey.fromPublicOnly(Utils.HEX.decode("02" + x)));
        byte[] fromOddY = Taproot.deriveOutputKey(BtcECKey.fromPublicOnly(Utils.HEX.decode("03" + x)));

        assertArrayEquals(fromEvenY, fromOddY);
    }

    @Test
    public void deriveOutputKey_withRegtestKey_shouldMatchBitcoinCore() {
        // Confirmed against Bitcoin Core 31 deriveaddresses "tr(...)".
        BtcECKey key = BtcECKey.fromPublicOnly(
            Utils.HEX.decode("030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"));

        assertEquals("4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7",
            Utils.HEX.encode(Taproot.deriveOutputKey(key)));
    }

    @Test
    public void deriveOutputKey_shouldReturnThirtyTwoBytes() {
        BtcECKey key = BtcECKey.fromPublicOnly(
            Utils.HEX.decode("030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"));

        assertEquals(32, Taproot.deriveOutputKey(key).length);
    }

    @Test
    public void deriveOutputKey_withUncompressedKey_shouldThrow() {
        BtcECKey uncompressed = BtcECKey.fromPublicOnly(
            Utils.HEX.decode("030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"))
            .decompress();

        assertThrows(IllegalArgumentException.class, () -> Taproot.deriveOutputKey(uncompressed));
    }

    private static void assertDerives(String internalKeyXOnly, String expectedOutputKey) {
        BtcECKey key = BtcECKey.fromPublicOnly(Utils.HEX.decode("02" + internalKeyXOnly));

        assertEquals(expectedOutputKey, Utils.HEX.encode(Taproot.deriveOutputKey(key)));
    }
}
