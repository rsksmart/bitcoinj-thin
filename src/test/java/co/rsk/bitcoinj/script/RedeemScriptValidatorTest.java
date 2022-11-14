package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedeemScriptValidatorTest {

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(110));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(220));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(330));

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void isRedeemLikeScript_invalid_redeem_script_missing_checkSig() {
        List<ScriptChunk> chunksWithoutCheckSig = RedeemScriptValidator.removeOpCheckMultisig(
            RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys)
        );

        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(chunksWithoutCheckSig));
    }

    @Test
    public void isRedeemLikeScript_invalid_redeem_script_insufficient_chunks() {
        ScriptBuilder builder = new ScriptBuilder();
        Script redeemScript = builder
            .data(ecKey1.getPubKey())
            .data(ecKey2.getPubKey())
            .data(ecKey3.getPubKey())
            .build();

        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(redeemScript.getChunks()));
    }

    @Test
    public void hasStandardRedeemScriptStructure_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Assert.assertTrue(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasStandardRedeemScriptStructure_non_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        Assert.assertFalse(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Assert.assertFalse(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            Sha256Hash.of(new byte[]{1}).getBytes(),
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_one_byte_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            10L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_two_bytes_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_two_bytes_including_sign_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            130L // Any value above 127 needs an extra byte to indicate the sign
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_three_bytes_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100_000L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_three_bytes_including_sign_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            33_000L // Any value above 32_767 needs an extra byte to indicate the sign
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_four_bytes_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            10_000_000L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_four_bytes_including_sign_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            8_400_000L // Any value above 8_388_607 needs an extra byte to indicate the sign
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_fast_bridge_erp_redeem_script_removing_prefix() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        // Remove fast bridge prefix
        List<ScriptChunk> chunks = redeemScript.getChunks();
        List<ScriptChunk> chunksWithoutFastBridgePrefix = chunks.subList(2, chunks.size());

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(
            chunksWithoutFastBridgePrefix)
        );
    }

    @Test
    public void hasFastBridgePrefix_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            Sha256Hash.of(new byte[]{1}).getBytes(),
            defaultRedeemScriptKeys
        );

        Assert.assertTrue(RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks()));
    }

    @Test
    public void hasFastBridgePrefix_erp_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks()));
    }

    @Test
    public void hasFastBridgePrefix_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Assert.assertFalse(RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks()));
    }

    @Test(expected = VerificationException.class)
    public void removeOpCheckMultiSig_non_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptValidator.removeOpCheckMultisig(redeemScript);
    }

    @Test
    public void removeOpCheckMultiSig_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> chunks = RedeemScriptValidator.removeOpCheckMultisig(redeemScript);

        Assert.assertEquals(defaultRedeemScriptKeys.size() + 2, chunks.size()); // 1 chunk per key + OP_M + OP_N
        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(chunks));
    }

    @Test
    public void isOpN_valid_opcode() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_2, null);
        Assert.assertTrue(RedeemScriptValidator.isOpN(chunk));
    }

    @Test
    public void isOpnN_invalid_opcode() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_DROP, null);
        Assert.assertFalse(RedeemScriptValidator.isOpN(chunk));
    }
}
