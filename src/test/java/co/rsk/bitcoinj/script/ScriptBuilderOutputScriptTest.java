package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.LegacyAddress;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.SegwitAddress;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.params.RegTestParams;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * The four output scripts the bridge can pay to. Vectors are the four addresses derived from one
 * regtest public key, each confirmed against Bitcoin Core 31 deriveaddresses.
 */
public class ScriptBuilderOutputScriptTest {

    private static final NetworkParameters REGTEST = RegTestParams.get();

    private static final byte[] PUB_KEY_HASH =
        Utils.HEX.decode("f7ee9ab7297134a0ccc76f3d50e94def17488f2c");
    private static final byte[] SCRIPT_HASH =
        Utils.HEX.decode("be56929d90f9eec61155469953f2e2e7ef400c6e");
    private static final byte[] TAPROOT_OUTPUT_KEY =
        Utils.HEX.decode("4c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7");

    @Test
    public void createOutputScript_withP2pkhAddress_shouldReturnExpectedBytes() {
        Address address = new LegacyAddress(REGTEST, PUB_KEY_HASH);

        assertEquals("76a914f7ee9ab7297134a0ccc76f3d50e94def17488f2c88ac",
            Utils.HEX.encode(ScriptBuilder.createOutputScript(address).getProgram()));
    }

    @Test
    public void createOutputScript_withP2shAddress_shouldReturnExpectedBytes() {
        Address address = LegacyAddress.fromP2SHHash(REGTEST, SCRIPT_HASH);

        assertEquals("a914be56929d90f9eec61155469953f2e2e7ef400c6e87",
            Utils.HEX.encode(ScriptBuilder.createOutputScript(address).getProgram()));
    }

    @Test
    public void createOutputScript_withP2wpkhAddress_shouldReturnExpectedBytes() {
        Address address = SegwitAddress.fromProgram(REGTEST, 0, PUB_KEY_HASH);

        assertEquals("0014f7ee9ab7297134a0ccc76f3d50e94def17488f2c",
            Utils.HEX.encode(ScriptBuilder.createOutputScript(address).getProgram()));
    }

    @Test
    public void createOutputScript_withP2trAddress_shouldReturnExpectedBytes() {
        Address address = SegwitAddress.fromProgram(REGTEST, 1, TAPROOT_OUTPUT_KEY);

        assertEquals("51204c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7",
            Utils.HEX.encode(ScriptBuilder.createOutputScript(address).getProgram()));
    }

    @Test
    public void createOutputScript_withP2wshAddress_shouldReturnExpectedBytes() {
        Address address = SegwitAddress.fromProgram(REGTEST, 0, TAPROOT_OUTPUT_KEY);

        assertEquals("00204c679657ca8d4aa7e29deaaaba90463a2af9e182012791112634c4d585b324a7",
            Utils.HEX.encode(ScriptBuilder.createOutputScript(address).getProgram()));
    }

    @Test
    public void createOutputScript_withTaproot_shouldStartWithASingleOpOneByte() {
        // ScriptBuilder.number(OP_1) would push the opcode's value as data and emit 01 51 <program>.
        // smallNum emits OP_1 itself. The P2WPKH path is correct either way, because OP_0 is zero,
        // so this is the assertion that catches the difference.
        byte[] program = ScriptBuilder
            .createOutputScript(SegwitAddress.fromProgram(REGTEST, 1, TAPROOT_OUTPUT_KEY))
            .getProgram();

        assertEquals("first byte must be OP_1", (byte) 0x51, program[0]);
        assertEquals("second byte must be the 32-byte push", (byte) 0x20, program[1]);
        assertEquals("script must be 34 bytes", 34, program.length);
    }

    @Test
    public void createP2WPKHOutputScript_withPubKeyHash_shouldReturnExpectedBytes() {
        assertEquals("0014f7ee9ab7297134a0ccc76f3d50e94def17488f2c",
            Utils.HEX.encode(ScriptBuilder.createP2WPKHOutputScript(PUB_KEY_HASH).getProgram()));
    }

    @Test
    public void createP2WPKHOutputScript_withCompressedKey_shouldHashIt() {
        BtcECKey key = BtcECKey.fromPublicOnly(
            Utils.HEX.decode("030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"));

        assertEquals("0014f7ee9ab7297134a0ccc76f3d50e94def17488f2c",
            Utils.HEX.encode(ScriptBuilder.createP2WPKHOutputScript(key).getProgram()));
    }

    @Test
    public void createP2WPKHOutputScript_withUncompressedKey_shouldThrow() {
        BtcECKey uncompressed = BtcECKey.fromPublicOnly(
            Utils.HEX.decode("030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad"))
            .decompress();

        assertThrows(IllegalArgumentException.class,
            () -> ScriptBuilder.createP2WPKHOutputScript(uncompressed));
    }

    @Test
    public void createP2WPKHOutputScript_withWrongLengthHash_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> ScriptBuilder.createP2WPKHOutputScript(new byte[21]));
    }
}
