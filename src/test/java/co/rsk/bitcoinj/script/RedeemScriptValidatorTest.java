package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedeemScriptValidatorTest {
    private final List<BtcECKey> defaultFedBtcECKeyList = new ArrayList<>();
    private final List<BtcECKey> erpFedBtcECKeyList = new ArrayList<>();
    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(100));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(200));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(300));
    private final BtcECKey ecKey4 = BtcECKey.fromPrivate(BigInteger.valueOf(400));
    private final BtcECKey ecKey5 = BtcECKey.fromPrivate(BigInteger.valueOf(500));
    private final BtcECKey ecKey6 = BtcECKey.fromPrivate(BigInteger.valueOf(600));
    private final BtcECKey ecKey7 = BtcECKey.fromPrivate(BigInteger.valueOf(700));
    private final BtcECKey ecKey8 = BtcECKey.fromPrivate(BigInteger.valueOf(800));

    @Before
    public void setUp() {
        defaultFedBtcECKeyList.add(ecKey1);
        defaultFedBtcECKeyList.add(ecKey2);
        defaultFedBtcECKeyList.add(ecKey3);
        erpFedBtcECKeyList.add(ecKey4);
        erpFedBtcECKeyList.add(ecKey5);
        erpFedBtcECKeyList.add(ecKey6);
        erpFedBtcECKeyList.add(ecKey7);
        erpFedBtcECKeyList.add(ecKey8);
    }

    @Test
    public void isRedeemLikeScript_invalid_redeem_script_missing_checkSig() {
        List<ScriptChunk> chunksWithoutCheckSig = RedeemScriptValidator.removeOpCheckMultisig(
            RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList)
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
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Assert.assertTrue(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasStandardRedeemScriptStructure_non_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            500L
        );

        Assert.assertFalse(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Assert.assertFalse(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            Sha256Hash.of(new byte[]{1}).getBytes(),
            defaultFedBtcECKeyList
        );

        Assert.assertFalse(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_small_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            10L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_max_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            ErpFederationRedeemScriptParser.MAX_CSV_VALUE
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_invalid_small_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScriptWithoutCsvValueValidation(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            10L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_erp_fed_redeem_script_invalid_large_csv_value() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScriptWithoutCsvValueValidation(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            1_000_000L
        );

        Assert.assertTrue(RedeemScriptValidator.hasErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_fast_bridge_erp_redeem_script_removing_prefix() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
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
            defaultFedBtcECKeyList
        );

        Assert.assertTrue(RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks()));
    }

    @Test
    public void hasFastBridgePrefix_erp_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks()));
    }

    @Test
    public void hasFastBridgePrefix_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Assert.assertFalse(RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks()));
    }

    @Test(expected = VerificationException.class)
    public void removeOpCheckMultiSig_non_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            500L
        );

        RedeemScriptValidator.removeOpCheckMultisig(redeemScript);
    }

    @Test
    public void removeOpCheckMultiSig_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        List<ScriptChunk> chunks = RedeemScriptValidator.removeOpCheckMultisig(redeemScript);

        Assert.assertEquals(5, chunks.size());
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
