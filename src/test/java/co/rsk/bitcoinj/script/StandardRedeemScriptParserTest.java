package co.rsk.bitcoinj.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import co.rsk.bitcoinj.core.BtcECKey;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class StandardRedeemScriptParserTest {

    private final List<BtcECKey> btcECKeyList = new ArrayList<>();
    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(100));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(200));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(300));
    private Script standardRedeemScript;
    private StandardRedeemScriptParser standardRedeemScriptParser;

    @Before
    public void setUp() {
        btcECKeyList.add(ecKey1);
        btcECKeyList.add(ecKey2);
        btcECKeyList.add(ecKey3);

        standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);
        // Cast object returned by RedeemScriptParserFactory.get() to ensure it is a StandardRedeemScriptParser
        standardRedeemScriptParser = (StandardRedeemScriptParser) RedeemScriptParserFactory.get(standardRedeemScript.getChunks());
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_standard_redeem_script_no_matching_key() {
        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(400));

        standardRedeemScriptParser.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test
    public void getPubKeys_standard_redeem_script() {
        List<BtcECKey> obtainedList = standardRedeemScriptParser.getPubKeys();

        List<String> expectedKeysList = new ArrayList<>();
        for (BtcECKey key : btcECKeyList) {
            expectedKeysList.add(key.getPublicKeyAsHex());
        }

        List<String> obtainedKeysList = new ArrayList<>();
        for (BtcECKey key : obtainedList) {
            obtainedKeysList.add(key.getPublicKeyAsHex());
        }

        Collections.sort(expectedKeysList);
        Collections.sort(obtainedKeysList);

        assertEquals(expectedKeysList, obtainedKeysList);
    }

    @Test
    public void getM_shouldReturnM() {
        assertEquals(2, standardRedeemScriptParser.getM());
    }

    @Test
    public void extractStandardRedeemScriptChunks_shouldReturnScriptChunks() {
        // Act
        List<ScriptChunk> actualRedeemScripChunks = standardRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(standardRedeemScript.getChunks(), actualRedeemScripChunks);
    }

    @Test
    public void hasErpFormat_shouldReturnFalse() {
        assertFalse(standardRedeemScriptParser.hasErpFormat());
    }
}
