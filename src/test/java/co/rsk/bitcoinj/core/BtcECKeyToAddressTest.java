package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.params.RegTestParams;
import co.rsk.bitcoinj.params.TestNet3Params;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * The four address types the bridge derives from one public key. Every expected value here is
 * confirmed against Bitcoin Core 31 deriveaddresses for pkh(), sh(wpkh()), wpkh() and tr().
 */
public class BtcECKeyToAddressTest {

    private static final NetworkParameters REGTEST = RegTestParams.get();
    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final NetworkParameters TESTNET = TestNet3Params.get();

    private static final BtcECKey KEY = BtcECKey.fromPublicOnly(
        Utils.HEX.decode("030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"));

    @Test
    public void toAddress_withP2pkh_shouldMatchBitcoinCore() {
        assertEquals("n47u2xVMrzaVpgGDK5TJjZHKggM7r8CdAm",
            KEY.toAddress(Script.ScriptType.P2PKH, REGTEST).toString());
    }

    @Test
    public void toAddress_withP2wpkh_shouldMatchBitcoinCore() {
        assertEquals("bcrt1q7lhf4defwy62pnx8du74p62daut53revy0wmk2",
            KEY.toAddress(Script.ScriptType.P2WPKH, REGTEST).toString());
    }

    @Test
    public void toAddress_withP2pkhAndP2wpkh_shouldShareTheSameHash() {
        // BIP141 reuses the P2PKH hash as the witness version 0 program. Not a copy-paste error.
        assertEquals(
            Utils.HEX.encode(KEY.toAddress(Script.ScriptType.P2PKH, REGTEST).getHash()),
            Utils.HEX.encode(KEY.toAddress(Script.ScriptType.P2WPKH, REGTEST).getHash()));
    }

    @Test
    public void toAddress_withAnUnsupportedType_shouldThrow() {
        // Upstream covers P2PKH and P2WPKH and rejects the rest. P2SH-P2WPKH is not here because
        // it is not a distinct output script type; P2TR is not here because upstream has no tweak.
        for (Script.ScriptType type : new Script.ScriptType[] {
                Script.ScriptType.P2SH, Script.ScriptType.P2TR,
                Script.ScriptType.P2WSH, Script.ScriptType.NO_TYPE }) {
            assertThrows(type.toString(), IllegalArgumentException.class,
                () -> KEY.toAddress(type, REGTEST));
        }
    }

    @Test
    public void toAddress_withP2wpkhAndUncompressedKey_shouldThrow() {
        BtcECKey uncompressed = KEY.decompress();

        assertThrows(IllegalArgumentException.class,
            () -> uncompressed.toAddress(Script.ScriptType.P2WPKH, REGTEST));
    }

    @Test
    public void composingNestedSegwit_shouldMatchBitcoinCore() {
        assertEquals("2NAbe5uhy5x3de7CuT2ckaif2aF5BcsxJLf", nestedSegwit(REGTEST, KEY).toString());
    }

    @Test
    public void composingTaproot_shouldMatchBitcoinCore() {
        assertEquals("bcrt1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnsdgtqq8",
            taproot(REGTEST, KEY).toString());
    }

    @Test
    public void composingAllFourTypes_onMainnetAndTestnet_shouldMatchBitcoinCore() {
        assertEquals("1PbwjuQP3y9F3ZnbbWUvue4zpgkQuSbgD5",
            KEY.toAddress(Script.ScriptType.P2PKH, MAINNET).toString());
        assertEquals("3K3S2AmwUVYHSKaMmtzsxmfmMts1s9RsXe", nestedSegwit(MAINNET, KEY).toString());
        assertEquals("bc1q7lhf4defwy62pnx8du74p62daut53revvqv96s",
            KEY.toAddress(Script.ScriptType.P2WPKH, MAINNET).toString());
        assertEquals("bc1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnshehf0j",
            taproot(MAINNET, KEY).toString());

        assertEquals("n47u2xVMrzaVpgGDK5TJjZHKggM7r8CdAm",
            KEY.toAddress(Script.ScriptType.P2PKH, TESTNET).toString());
        assertEquals("2NAbe5uhy5x3de7CuT2ckaif2aF5BcsxJLf", nestedSegwit(TESTNET, KEY).toString());
        assertEquals("tb1q7lhf4defwy62pnx8du74p62daut53revxxhkpr",
            KEY.toAddress(Script.ScriptType.P2WPKH, TESTNET).toString());
        assertEquals("tb1pf3nev47234920c5aa24t4yzx8g40ncvzqynezyfxxnzdtpdnyjnsq3px4a",
            taproot(TESTNET, KEY).toString());
    }

    /**
     * An uncompressed key hashes to 20 bytes like any other, so the byte[] overload would accept
     * it and hand back a P2SH address whose witness cannot be relayed. The key overload is what
     * rejects it, which is why the composition goes through that one.
     */
    @Test
    public void composingNestedSegwit_withUncompressedKey_shouldThrow() {
        BtcECKey uncompressed = KEY.decompress();

        assertEquals(20, uncompressed.getPubKeyHash().length);
        assertThrows(IllegalArgumentException.class, () -> nestedSegwit(REGTEST, uncompressed));
    }

    /**
     * P2SH-P2WPKH is not an output script type, so it composes from the pieces rather than being a
     * case in toAddress. This is the shape a caller uses.
     */
    private static LegacyAddress nestedSegwit(NetworkParameters params, BtcECKey key) {
        Script redeemScript = ScriptBuilder.createP2WPKHOutputScript(key);

        return LegacyAddress.fromP2SHHash(params, Utils.sha256hash160(redeemScript.getProgram()));
    }

    /** Same for taproot: the tweak plus fromProgram. */
    private static SegwitAddress taproot(NetworkParameters params, BtcECKey key) {
        return SegwitAddress.fromProgram(params, 1, Taproot.deriveOutputKey(key));
    }
}
