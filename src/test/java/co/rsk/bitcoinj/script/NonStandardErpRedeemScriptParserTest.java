package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NonStandardErpRedeemScriptParserTest {

    private static final long CSV_VALUE = 52_560L;

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;
    private NonStandardErpRedeemScriptParser nonStandardErpRedeemScriptParser;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();

        Script erpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        List<ScriptChunk> nonStandardErpRedeemScriptChunks = erpRedeemScript.getChunks();
        nonStandardErpRedeemScriptParser = new NonStandardErpRedeemScriptParser(
            nonStandardErpRedeemScriptChunks
        );
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenNonStandardErpRedeemScript_shouldReturnChunks() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> expectedStandardRedeemScriptChunks = standardRedeemScript.getChunks();

        List<ScriptChunk> actualRedeemScriptChunks = nonStandardErpRedeemScriptParser.extractStandardRedeemScriptChunks();

        Assert.assertEquals(expectedStandardRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScriptChunks_whenStandardRedeemScript_shouldFail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);

        new NonStandardErpRedeemScriptParser(standardRedeemScript.getChunks());
    }

    @Test
    public void isNonStandardErpFed_whenNonStandardErpRedeemScript_shouldReturnTrue() {
        Script erpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(erpRedeemScript.getChunks()));
    }

    @Test
    public void isNonStandardErpFed_whenCustomRedeemScript_shouldReturnFalse() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(customRedeemScript.getChunks()));
    }

    @Test
    public void getMultiSigType_shouldReturnNonStandardErpFedType() {
        Assert.assertEquals(MultiSigType.NON_STANDARD_ERP_FED, nonStandardErpRedeemScriptParser.getMultiSigType());
    }

    @Test
    public void getM_shouldReturnM() {
        int expectedM = defaultRedeemScriptKeys.size() / 2 + 1;
        Assert.assertEquals(expectedM, nonStandardErpRedeemScriptParser.getM());
    }

    @Test
    public void findKeyInRedeem_whenKeyExists_shouldReturnIndex() {
        for (int i = 0; i < defaultRedeemScriptKeys.size(); i++) {
            BtcECKey expectedKey = defaultRedeemScriptKeys.get(i);
            Assert.assertEquals(i, nonStandardErpRedeemScriptParser.findKeyInRedeem(expectedKey));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyDoesNotExists_shouldThrowIllegalStateException() {
        BtcECKey unknownKey = BtcECKey.fromPrivate(BigInteger.valueOf(1234567890L));
        nonStandardErpRedeemScriptParser.findKeyInRedeem(unknownKey);
    }

    @Test
    public void getPubKeys_shouldReturnPubKeys() {
        List<BtcECKey> actualPubKeys = nonStandardErpRedeemScriptParser.getPubKeys();
        List<BtcECKey> expectedPubKeys = defaultRedeemScriptKeys.stream()
            .map(btcECKey -> BtcECKey.fromPublicOnly(btcECKey.getPubKey())).collect(
                Collectors.toList());
        Assert.assertEquals(expectedPubKeys, actualPubKeys);
    }

    @Test
    public void extractStandardRedeemScriptChunks_shouldReturnStandardChunks() {
        List<ScriptChunk> expectedStandardRedeemScriptChunks = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        ).getChunks();
        Assert.assertEquals(expectedStandardRedeemScriptChunks, nonStandardErpRedeemScriptParser.extractStandardRedeemScriptChunks());
    }

    @Test
    public void hasErpFormat_shouldReturnTrue() {
        Assert.assertTrue(nonStandardErpRedeemScriptParser.hasErpFormat());
    }
}
