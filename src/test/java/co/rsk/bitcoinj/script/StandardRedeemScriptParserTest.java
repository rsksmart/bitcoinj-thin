package co.rsk.bitcoinj.script;

import static org.junit.Assert.assertEquals;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
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

    @Before
    public void setUp() {
        btcECKeyList.add(ecKey1);
        btcECKeyList.add(ecKey2);
        btcECKeyList.add(ecKey3);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_standard_redeem_script_no_matching_key() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);
        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(400));

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        parser.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test
    public void getPubKeys_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());
        List<BtcECKey> obtainedList = parser.getPubKeys();

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

    @Test(expected = ScriptException.class)
    public void getPubKeys_invalid_redeem_script() {
        Script script = ScriptBuilder.createP2SHOutputScript(new byte[20]);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(script.getChunks());

        parser.getPubKeys();
    }

    @Test
    public void getM_from_multiSig_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        assertEquals(2, parser.getM());
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenGetScriptChunksFromStandardRedeemScriptParser_shouldReturnScriptChunks() {
        // Arrange
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);
        List<ScriptChunk> expectedRedeemScriptChunks = redeemScript.getChunks();
        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScripChunks = redeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScripChunks);
    }
}
