package co.rsk.bitcoinj.script;

import static org.junit.Assert.assertEquals;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StandardRedeemScriptParserTest {

    private static final long CSV_VALUE = 52_560L;

    private List<BtcECKey> defaultRedeemScriptKeys;
    private StandardRedeemScriptParser standardRedeemScriptParser;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);
        standardRedeemScriptParser = new StandardRedeemScriptParser(
            standardRedeemScript.getChunks());
    }

    @Test
    public void findKeyInRedeem_ok() {
        BtcECKey federatorBtcKey = defaultRedeemScriptKeys.get(0);
        standardRedeemScriptParser.findKeyInRedeem(federatorBtcKey);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenUnknownKey_shouldFail() {
        BtcECKey unknownKey = BtcECKey.fromPrivate(BigInteger.valueOf(1400));
        standardRedeemScriptParser.findKeyInRedeem(unknownKey);
    }

    @Test
    public void getPubKeys_ok() {
        List<BtcECKey> actualPubKeys = standardRedeemScriptParser.getPubKeys();
        List<BtcECKey> expectedPubKeys = defaultRedeemScriptKeys.stream()
            .map(btcECKey -> BtcECKey.fromPublicOnly(btcECKey.getPubKey())).collect(
                Collectors.toList());

        assertEquals(expectedPubKeys, actualPubKeys);
    }

    @Test
    public void getM_ok() {
        int expectedM = defaultRedeemScriptKeys.size() / 2 + 1;
        assertEquals(expectedM, standardRedeemScriptParser.getM());
    }

    @Test
    public void extractStandardRedeemScriptChunks_ok() {
        // Arrange
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> expectedRedeemScriptChunks = redeemScript.getChunks();

        // Act
        List<ScriptChunk> actualRedeemScripChunks = standardRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScripChunks);
    }

    @Test
    public void getMultiSigType_ok() {
        Assert.assertEquals(RedeemScriptParser.MultiSigType.STANDARD_MULTISIG, standardRedeemScriptParser.getMultiSigType());
    }

    @Test
    public void isStandardMultiSig_ok() {
        Assert.assertTrue(StandardRedeemScriptParser.isStandardMultiSig(standardRedeemScriptParser.extractStandardRedeemScriptChunks()));
    }

    @Test
    public void isStandardMultiSig_whenEmptyScript_shouldReturnFalse() {
        Assert.assertFalse(StandardRedeemScriptParser.isStandardMultiSig(new Script(new byte[]{}).getChunks()));
    }

    @Test
    public void isStandardMultiSig_whenNonStandardErpRedeemScriptChunks_shouldReturnFalse() {
        List<ScriptChunk> nonStandardRedeemScriptChunks = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            RedeemScriptUtils.getEmergencyRedeemScriptKeys(),
            CSV_VALUE
        ).getChunks();
        Assert.assertFalse(StandardRedeemScriptParser.isStandardMultiSig(nonStandardRedeemScriptChunks));
    }

    @Test
    public void isStandardMultiSig_whenP2shRedeemScriptChunks_shouldReturnFalse() {
        List<ScriptChunk> p2shErpRedeemScriptChunks = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            RedeemScriptUtils.getEmergencyRedeemScriptKeys(),
            CSV_VALUE
        ).getChunks();
        Assert.assertFalse(StandardRedeemScriptParser.isStandardMultiSig(p2shErpRedeemScriptChunks));
    }

    @Test
    public void isStandardMultiSig_whenFlyoverRedeemScriptChunks_shouldReturnFalse() {
        Script p2shRedeemScriptChunks = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            RedeemScriptUtils.getEmergencyRedeemScriptKeys(),
            CSV_VALUE
        );

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            derivationArgumentsHash.getBytes(), p2shRedeemScriptChunks
        ).getChunks();

        Assert.assertFalse(StandardRedeemScriptParser.isStandardMultiSig(flyoverRedeemScriptChunks));
    }
}
