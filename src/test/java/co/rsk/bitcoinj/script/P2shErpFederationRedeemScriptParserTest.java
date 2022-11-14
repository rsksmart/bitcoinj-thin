package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2shErpFederationRedeemScriptParserTest {

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScript_fromP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100L
        );
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        Script obtainedRedeemScript = P2shErpFederationRedeemScriptParser.extractStandardRedeemScript(
            erpRedeemScript.getChunks()
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        P2shErpFederationRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void createP2shErpRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 300L;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_invalidDefaultFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 300L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_invalidErpFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 300L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_negative_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = -100L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_zero_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 0L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_above_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = P2shErpFederationRedeemScriptParser.MAX_CSV_VALUE + 1;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createP2shErpRedeemScript_csv_exact_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = P2shErpFederationRedeemScriptParser.MAX_CSV_VALUE;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_one_byte_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 20L;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_two_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 500L;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_two_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 130; // Any value above 127 needs an extra byte to indicate the sign

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_value_three_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 100_000L;

        // Should fail since this value is above the max value
        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_three_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 33_000L; // Any value above 32_767 needs an extra byte to indicate the sign

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_value_four_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 10_000_000L;

        // Should fail since this value is above the max value
        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_value_four_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 8_400_000L; // Any value above 8_388_607 needs an extra byte to indicate the sign

        // Should fail since this value is above the max value
        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void isP2shErpFed() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertTrue(P2shErpFederationRedeemScriptParser.isP2shErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isErpFed_falseWithP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertFalse(ErpFederationRedeemScriptParser.isErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isP2shErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(P2shErpFederationRedeemScriptParser.isP2shErpFed(customRedeemScript.getChunks()));
    }

    private void validateP2shErpRedeemScript(
        Script erpRedeemScript,
        Long csvValue) {

        /***
         * Expected structure:
         * OP_NOTIF
         *  OP_M
         *  PUBKEYS...N
         *  OP_N
         *  OP_CHECKMULTISIG
         * OP_ELSE
         *  OP_PUSHBYTES
         *  CSV_VALUE
         *  OP_CHECKSEQUENCEVERIFY
         *  OP_DROP
         *  OP_M
         *  PUBKEYS...N
         *  OP_N
         *  OP_CHECKMULTISIG
         * OP_ENDIF
         */

        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

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

        // Next byte should equal OP_CHECKMULTISIG
        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKMULTISIG).byteValue(), script[index++]);

        // Next byte should equal OP_ELSE
        Assert.assertEquals(ScriptOpCodes.OP_ELSE, script[index++]);

        // Next byte should equal csv value length
        Assert.assertEquals(serializedCsvValue.length, script[index++]);

        // Next bytes should equal the csv value in bytes
        for (int i = 0; i < serializedCsvValue.length; i++) {
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

        // Next byte should equal OP_CHECKMULTISIG
        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKMULTISIG).byteValue(), script[index++]);

        Assert.assertEquals(ScriptOpCodes.OP_ENDIF, script[index++]);
    }
}
