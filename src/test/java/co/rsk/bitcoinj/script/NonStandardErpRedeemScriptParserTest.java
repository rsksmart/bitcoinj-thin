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

    private void validateNonStandardErpRedeemScript(
        Script nonStandardErpRedeemScript,
        Long csvValue,
        boolean hasDeprecatedFormat
    ) {

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

        final int DEPRECATED_FORMAT_LENGTH = 2;
        int expectedCsvValueLength = hasDeprecatedFormat ?
            DEPRECATED_FORMAT_LENGTH:
            BigInteger.valueOf(csvValue).toByteArray().length;
        byte[] serializedCsvValue = hasDeprecatedFormat ?
            Utils.unsignedLongToByteArrayBE(csvValue, expectedCsvValueLength) :
            Utils.signedLongToByteArrayLE(csvValue);

        byte[] nonStandardErpRedeemScriptProgram = nonStandardErpRedeemScript.getProgram();
        Assert.assertTrue(nonStandardErpRedeemScriptProgram.length > 0);


        // First byte should equal OP_NOTIF
        final int OP_NOT_IF_INDEX = 0;
        byte actualOpCodeInOpNotIfPosition = nonStandardErpRedeemScriptProgram[OP_NOT_IF_INDEX];
        Assert.assertEquals(ScriptOpCodes.OP_NOTIF, actualOpCodeInOpNotIfPosition);

        // Next byte should equal M, from an M/N multisig
        int M_STANDARD_VALUE_INDEX = OP_NOT_IF_INDEX + 1;
        int actualOpCodeInMPosition = nonStandardErpRedeemScriptProgram[M_STANDARD_VALUE_INDEX];

        int expectedMStandardFederation = defaultRedeemScriptKeys.size() / 2 + 1;
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(expectedMStandardFederation)), actualOpCodeInMPosition);

        // Assert public keys
        int pubKeysIndex = M_STANDARD_VALUE_INDEX + 1;
        for (int i = 0; i < defaultRedeemScriptKeys.size(); i++) {
            BtcECKey btcFederatorKey = defaultRedeemScriptKeys.get(i);
            byte actualPubKeyLength = nonStandardErpRedeemScriptProgram[pubKeysIndex++];

            byte[] expectedFederatorPubKey = btcFederatorKey.getPubKey();
            Assert.assertEquals(expectedFederatorPubKey.length, actualPubKeyLength);

            for (byte characterPubKey : expectedFederatorPubKey) {
                Assert.assertEquals(characterPubKey, nonStandardErpRedeemScriptProgram[pubKeysIndex++]);
            }
        }

        // Next byte should equal N, from an M/N multisig
        final int N_STANDARD_VALUE_INDEX = pubKeysIndex;
        int nStandardFederation = defaultRedeemScriptKeys.size();
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(nStandardFederation)), nonStandardErpRedeemScriptProgram[N_STANDARD_VALUE_INDEX]);

        // Next byte should equal OP_ELSE
        final int OP_ELSE_INDEX = N_STANDARD_VALUE_INDEX + 1;
        Assert.assertEquals(ScriptOpCodes.OP_ELSE, nonStandardErpRedeemScriptProgram[OP_ELSE_INDEX]);

        // Next byte should equal csv value length
        final int CSV_VALUE_LENGTH_INDEX = OP_ELSE_INDEX + 1;
        Assert.assertEquals(expectedCsvValueLength, nonStandardErpRedeemScriptProgram[CSV_VALUE_LENGTH_INDEX]);

        // Next bytes should equal the csv value in bytes
        final int CSV_VALUE_START_INDEX = CSV_VALUE_LENGTH_INDEX + 1;
        for (int i = 0; i < expectedCsvValueLength; i++) {
            int currentCsvValueIndex = CSV_VALUE_START_INDEX + i;
            Assert.assertEquals(serializedCsvValue[i], nonStandardErpRedeemScriptProgram[currentCsvValueIndex]);
        }

        final int CSV_VALUE_OP_CODE_INDEX = CSV_VALUE_START_INDEX + expectedCsvValueLength;
        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY).byteValue(),
            nonStandardErpRedeemScriptProgram[CSV_VALUE_OP_CODE_INDEX]);

        final int OP_DROP_INDEX = CSV_VALUE_OP_CODE_INDEX + 1;
        Assert.assertEquals(ScriptOpCodes.OP_DROP, nonStandardErpRedeemScriptProgram[OP_DROP_INDEX]);

        // Next byte should equal M, from an M/N multisig
        final int OP_M_ERP_INDEX = OP_DROP_INDEX + 1;

        int expectedMErpFederation = emergencyRedeemScriptKeys.size() / 2 + 1;
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(expectedMErpFederation)), nonStandardErpRedeemScriptProgram[OP_M_ERP_INDEX]);

        int erpPubKeysIndex = OP_M_ERP_INDEX + 1;
        for (BtcECKey btcErpEcKey : emergencyRedeemScriptKeys) {
            byte actualErpKeyLength = nonStandardErpRedeemScriptProgram[erpPubKeysIndex++];

            byte[] erpPubKey = btcErpEcKey.getPubKey();
            byte expectedLength = Integer.valueOf(erpPubKey.length).byteValue();
            Assert.assertEquals(expectedLength, actualErpKeyLength);
            for (byte characterErpPubKey : erpPubKey) {
                Assert.assertEquals(characterErpPubKey, nonStandardErpRedeemScriptProgram[erpPubKeysIndex++]);
            }
        }

        // Next byte should equal N, from an M/N multisig
        final int N_ERP_INDEX = erpPubKeysIndex;
        int actualNErpFederation = nonStandardErpRedeemScriptProgram[N_ERP_INDEX];
        int expectedNErpFederation = emergencyRedeemScriptKeys.size();
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(expectedNErpFederation)), actualNErpFederation);

        // Next byte should equal OP_ENDIF
        final int OP_ENDIF_INDEX = N_ERP_INDEX + 1;
        byte actualOpEndIfValue = nonStandardErpRedeemScriptProgram[OP_ENDIF_INDEX];
        Assert.assertEquals(ScriptOpCodes.OP_ENDIF, actualOpEndIfValue);

        // Next byte should equal OP_CHECKMULTISIG
        final int OP_CHECK_MULTISIG_INDEX = OP_ENDIF_INDEX + 1;
        byte actualOpCheckMultisigValue = nonStandardErpRedeemScriptProgram[OP_CHECK_MULTISIG_INDEX];
        byte expectedOpCheckMultisigValue = Integer.valueOf(ScriptOpCodes.OP_CHECKMULTISIG).byteValue();
        Assert.assertEquals(expectedOpCheckMultisigValue, actualOpCheckMultisigValue);
    }

    @Test
    public void createNonStandardErpRedeemScriptDeprecated() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );

        Script nonStandardErpRedeemScriptParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptParser, CSV_VALUE, true);
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScriptDeprecated_invalidDefaultFederationRedeemScript_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScriptDeprecated_invalidNonStandardNonStandardErpFederationRedeemScript_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            emergencyRedeemScriptKeys
        );

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScriptDeprecated_csvBelowZero_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = -200L;

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScriptDeprecated_csvAboveMaxValue_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = NonStandardErpRedeemScriptParser.MAX_CSV_VALUE + 1;

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createNonStandardErpRedeemScriptDeprecated_csvExactMaxValue_shouldReturnRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = NonStandardErpRedeemScriptParser.MAX_CSV_VALUE;

        Script nonStandardErpRedeemScriptParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptParser, csvValue, true);
    }

    @Test
    public void createNonStandardErpRedeemScriptDeprecated_csvValueOneByteLong_shouldReturnValidRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );

        // For a value that only uses 1 byte it should add leading zeroes to complete 2 bytes
        long oneByteCsvValue = 20L;

        Script erpRedeemScript = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            oneByteCsvValue
        );

        validateNonStandardErpRedeemScript(erpRedeemScript, oneByteCsvValue, true);
    }

    @Test
    public void createNonStandardErpRedeemScript_whenPassingValidRedeemScript_shouldReturnRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );

        Script nonStandardErpRedeemScriptScript = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptScript, CSV_VALUE, false);
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_whenPassingInvalidDefaultFederationRedeemScript_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_whenPassingInvalidNonStandardErpFederationRedeemScript_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            emergencyRedeemScriptKeys
        );

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            CSV_VALUE
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_csvNegativeValue_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = -100L;

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_csvZeroValue_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = 0L;

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_csvAboveMaxValue_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = NonStandardErpRedeemScriptParser.MAX_CSV_VALUE + 1;

        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createNonStandardErpRedeemScript_csvExactMaxValue_shouldReturnValidRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        Long csvValue = NonStandardErpRedeemScriptParser.MAX_CSV_VALUE;

        Script nonStandardErpRedeemScripParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScripParser, csvValue, false);
    }

    @Test
    public void createNonStandardErpRedeemScript_csvValueOneByteLong_shouldReturnValidRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 20L;

        Script nonStandardErpRedeemScriptParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptParser, csvValue, false);
    }

    @Test
    public void createNonStandardErpRedeemScript_csvValueTwoBytesLong_shouldReturnValidRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long twoBytesCsvValue = 500L;

        Script nonStandardErpRedeemScriptParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            twoBytesCsvValue
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptParser, twoBytesCsvValue, false);
    }

    @Test
    public void createNonStandardErpRedeemScript_csvValueTwoBytesLongIncludingSign_shouldReturnValidRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 130; // Any value above 127 needs an extra byte to indicate the sign

        Script nonStandardErpRedeemScriptParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptParser, csvValue, false);
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_csvValueThreeBytesLong_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 100_000L;

        // Should fail since this value is above the max value
        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createNonStandardErpRedeemScript_csvValueThreeBytesLongIncludingSign_shouldCreateValidRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 33_000L; // Any value above 32_767 needs an extra byte to indicate the sign

        Script nonStandardErpRedeemScriptParser = NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateNonStandardErpRedeemScript(nonStandardErpRedeemScriptParser, csvValue, false);
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_csvValueFourBytesLong_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 10_000_000L;

        // Should fail since this value is above the max value
        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createNonStandardErpRedeemScript_whenCsvValueFourBytesLongIncludingSign_shouldFail() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            emergencyRedeemScriptKeys
        );
        long csvValue = 8_400_000L; // Any value above 8_388_607 needs an extra byte to indicate the sign

        // Should fail since this value is above the max value
        NonStandardErpRedeemScriptParser.createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void isNonStandardErpFed_whenNonStandardErpRedeemScript_shouldReturnTrue() {
        Script erpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertTrue(NonStandardErpRedeemScriptParser.isNonStandardErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isNonStandardErpFed_whenCustomRedeemScript_shouldReturnFalse() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(NonStandardErpRedeemScriptParser.isNonStandardErpFed(customRedeemScript.getChunks()));
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
}
