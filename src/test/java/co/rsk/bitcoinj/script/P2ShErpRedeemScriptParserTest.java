package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
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
    public void createP2shErpRedeemScript_whenValidInputs_shouldCreateRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );

        Script erpRedeemScript = P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );

        validateP2shErpRedeemScript(erpRedeemScript, CSV_VALUE);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenInvalidDefaultFederationRedeemScript_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 300L;

        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenInvalidErpFederationRedeemScript_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 300L;

        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenCsvNegativeValue_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = -100L;

        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenCsvZeroValue_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 0L;

        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenCsvAboveMaxValue_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = P2shErpRedeemScriptParser.MAX_CSV_VALUE + 1;

        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createP2shErpRedeemScript_whenCsvExactMaxValue_shouldCreateRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = P2shErpRedeemScriptParser.MAX_CSV_VALUE;

        Script erpRedeemScript = P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_whenCsvValueOneByteLong_shouldCreateRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long validCsvValue = 20L;

        Script erpRedeemScript = P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            validCsvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, validCsvValue);
    }

    @Test
    public void createP2shErpRedeemScript_whenCsvValueTwoBytesLong_shouldCreateRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long validCsvValue = 500L;

        Script erpRedeemScript = P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            validCsvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, validCsvValue);
    }

    @Test
    public void createP2shErpRedeemScript_whenCsvValueTwoBytesLongIncludingSign_shouldCreateRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 130; // Any value above 127 needs an extra byte to indicate the sign

        Script erpRedeemScript = P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenCsvValueThreeBytesLong_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long invalidCsvValue = 100_000L;

        // Should fail since this value is above the max value
        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            invalidCsvValue
        );
    }

    @Test
    public void createP2shErpRedeemScript_whenCsvValueThreeBytesLongIncludingSign_shouldCreateRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 33_000L; // Any value above 32_767 needs an extra byte to indicate the sign

        Script erpRedeemScript = P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateP2shErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenCsvValueFourBytesLong_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long invalidCsvValue = 10_000_000L;

        // Should fail since this value is above the max value
        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            invalidCsvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_whenCsvValueFourBytesLongIncludingSign_shouldThrowVerificationException() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long invalidCsvValue = 8_400_000L; // Any value above 8_388_607 needs an extra byte to indicate the sign

        // Should fail since this value is above the max value
        P2shErpRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            invalidCsvValue
        );
    }

    @Test
    public void isP2shErpFed() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertTrue(P2shErpRedeemScriptParser.isP2shErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isErpFed_falseWithP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertFalse(NonStandardErpRedeemScriptParser.isNonStandardErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isP2shErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(P2shErpRedeemScriptParser.isP2shErpFed(customRedeemScript.getChunks()));
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
