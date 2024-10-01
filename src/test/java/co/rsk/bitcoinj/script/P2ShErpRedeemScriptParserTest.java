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

public class P2ShErpRedeemScriptParserTest {

    private static final long CSV_VALUE = 52_560L;

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;
    private P2shErpRedeemScriptParser p2ShErpRedeemScriptParser;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        List<ScriptChunk> p2shErpRedeemScriptChunks = p2shErpRedeemScript.getChunks();
        p2ShErpRedeemScriptParser = new P2shErpRedeemScriptParser(
            p2shErpRedeemScriptChunks
        );
    }

    @Test
    public void extractStandardRedeemScriptChunks_fromP2ShErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100L
        );
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> expectedStandardRedeemScriptChunks = standardRedeemScript.getChunks();

        P2shErpRedeemScriptParser actualP2ShErpRedeemScriptParser = new P2shErpRedeemScriptParser(erpRedeemScript.getChunks());
        List<ScriptChunk> actualStandardRedeemScriptChunks = actualP2ShErpRedeemScriptParser.extractStandardRedeemScriptChunks();

        Assert.assertEquals(expectedStandardRedeemScriptChunks, actualStandardRedeemScriptChunks);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScriptChunks_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        new P2shErpRedeemScriptParser(standardRedeemScript.getChunks());
    }

    @Test
    public void getMultiSigType_shouldReturnP2shErpFedType() {
        Assert.assertEquals(MultiSigType.P2SH_ERP_FED, p2ShErpRedeemScriptParser.getMultiSigType());
    }

    @Test
    public void getM_shouldReturnM() {
        int expectedM = defaultRedeemScriptKeys.size() / 2 + 1;
        Assert.assertEquals(expectedM, p2ShErpRedeemScriptParser.getM());
    }

    @Test
    public void findKeyInRedeem_whenKeyExists_shouldReturnIndex() {
        for (int i = 0; i < defaultRedeemScriptKeys.size(); i++) {
            BtcECKey expectedKey = defaultRedeemScriptKeys.get(i);
            Assert.assertEquals(i, p2ShErpRedeemScriptParser.findKeyInRedeem(expectedKey));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyDoesNotExists_shouldThrowIllegalStateException() {
        BtcECKey unknownKey = BtcECKey.fromPrivate(BigInteger.valueOf(1234567890L));
        p2ShErpRedeemScriptParser.findKeyInRedeem(unknownKey);
    }

    @Test
    public void getPubKeys_shouldReturnPubKeys() {
        List<BtcECKey> actualPubKeys = p2ShErpRedeemScriptParser.getPubKeys();
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
        Assert.assertEquals(expectedStandardRedeemScriptChunks, p2ShErpRedeemScriptParser.extractStandardRedeemScriptChunks());
    }

    @Test
    public void hasErpFormat_shouldReturnTrue() {
        Assert.assertTrue(p2ShErpRedeemScriptParser.hasErpFormat());
    }
}
