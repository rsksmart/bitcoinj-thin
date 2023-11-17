package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ErpFederationRedeemScriptParserTest {

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptTestUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptTestUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScript_fromErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptTestUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100L
        );
        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);

        Script obtainedRedeemScript = new Script (ErpFederationRedeemScriptParser.extractStandardRedeemScript(
            erpRedeemScript.getChunks()
        ));

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);

        ErpFederationRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void isErpFed() {
        Script erpRedeemScript = RedeemScriptTestUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertTrue(ErpFederationRedeemScriptParser.isErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptTestUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(ErpFederationRedeemScriptParser.isErpFed(customRedeemScript.getChunks()));
    }

    private void validateErpRedeemScript(
        Script erpRedeemScript,
        Long csvValue,
        boolean hasDeprecatedFormat) {

        /***
         * Expected structure:
         * OP_NOTIF
         *  OP_M
         *  PUBKEYS...N
         *  OP_N
         * OP_ELSE
         *  OP_PUSHBYTES
         *  CSV_VALUE
         *  OP_CHECKSEQUENCEVERIFY
         *  OP_DROP
         *  OP_M
         *  PUBKEYS...N
         *  OP_N
         * OP_ENDIF
         * OP_CHECKMULTISIG
         */

        int expectedCsvValueLength = hasDeprecatedFormat ? 2 : BigInteger.valueOf(csvValue).toByteArray().length;
        byte[] serializedCsvValue = hasDeprecatedFormat ?
            Utils.unsignedLongToByteArrayBE(csvValue, expectedCsvValueLength) :
            Utils.signedLongToByteArrayLE(csvValue);

        byte[] script = erpRedeemScript.getProgram();
        Assert.assertTrue(script.length > 0);

        int index = 0;

        // First byte should equal OP_NOTIF
        Assert.assertEquals(ScriptOpCodes.OP_NOTIF, script[index++]);

        // Next byte should equal M, from an M/N multisig
        int m = defaultRedeemScriptKeys.size() / 2 + 1;
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(m)), script[index++]);

        // Assert public keys
        for (BtcECKey key : defaultRedeemScriptKeys) {
            byte[] pubkey = key.getPubKey();
            Assert.assertEquals(pubkey.length, script[index++]);
            for (int pkIndex = 0; pkIndex < pubkey.length; pkIndex++) {
                Assert.assertEquals(pubkey[pkIndex], script[index++]);
            }
        }

        // Next byte should equal N, from an M/N multisig
        int n = defaultRedeemScriptKeys.size();
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(n)), script[index++]);

        // Next byte should equal OP_ELSE
        Assert.assertEquals(ScriptOpCodes.OP_ELSE, script[index++]);

        // Next byte should equal csv value length
        Assert.assertEquals(expectedCsvValueLength, script[index++]);

        // Next bytes should equal the csv value in bytes
        for (int i = 0; i < expectedCsvValueLength; i++) {
            Assert.assertEquals(serializedCsvValue[i], script[index++]);
        }

        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY).byteValue(), script[index++]);
        Assert.assertEquals(ScriptOpCodes.OP_DROP, script[index++]);

        // Next byte should equal M, from an M/N multisig
        m = emergencyRedeemScriptKeys.size() / 2 + 1;
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(m)), script[index++]);

        for (BtcECKey key: emergencyRedeemScriptKeys) {
            byte[] pubkey = key.getPubKey();
            Assert.assertEquals(Integer.valueOf(pubkey.length).byteValue(), script[index++]);
            for (int pkIndex = 0; pkIndex < pubkey.length; pkIndex++) {
                Assert.assertEquals(pubkey[pkIndex], script[index++]);
            }
        }

        // Next byte should equal N, from an M/N multisig
        n = emergencyRedeemScriptKeys.size();
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(n)), script[index++]);

        Assert.assertEquals(ScriptOpCodes.OP_ENDIF, script[index++]);
        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKMULTISIG).byteValue(), script[index++]);
    }
}
