package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ErpFederationRedeemScriptParserTest {

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
        defaultFedBtcECKeyList.sort(BtcECKey.PUBKEY_COMPARATOR);

        erpFedBtcECKeyList.add(ecKey4);
        erpFedBtcECKeyList.add(ecKey5);
        erpFedBtcECKeyList.add(ecKey6);
        erpFedBtcECKeyList.add(ecKey7);
        erpFedBtcECKeyList.add(ecKey8);
        erpFedBtcECKeyList.sort(BtcECKey.PUBKEY_COMPARATOR);
    }

    @Test
    public void extractStandardRedeemScript_fromErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            100L
        );
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

        Script obtainedRedeemScript = ErpFederationRedeemScriptParser.extractStandardRedeemScript(
            erpRedeemScript.getChunks()
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

        ErpFederationRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void createErpRedeemScriptDeprecated() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, true);
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScriptDeprecated_invalidDefaultFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 200L;

        ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScriptDeprecated_invalidErpFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 200L;

        ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScriptDeprecated_csv_below_zero() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = -200L;

        ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScriptDeprecated_csv_above_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = ErpFederationRedeemScriptParser.MAX_CSV_VALUE + 1;

        ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createErpRedeemScriptDeprecated_csv_exact_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = ErpFederationRedeemScriptParser.MAX_CSV_VALUE;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, true);
    }

    @Test
    public void createErpRedeemScriptDeprecated_csv_value_one_byte_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);

        // For a value that only uses 1 byte it should add leading zeroes to complete 2 bytes
        long csvValue = 20L;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScriptDeprecated(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, true);
    }

    @Test
    public void createErpRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, false);
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_invalidDefaultFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_invalidErpFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_csv_negative_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = -100L;

        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_csv_zero_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 0L;

        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_csv_above_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = ErpFederationRedeemScriptParser.MAX_CSV_VALUE + 1;

        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createErpRedeemScript_csv_exact_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = ErpFederationRedeemScriptParser.MAX_CSV_VALUE;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, false);
    }

    @Test
    public void createErpRedeemScript_csv_value_one_byte_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 20L;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, false);
    }

    @Test
    public void createErpRedeemScript_csv_value_two_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 500L;

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, false);
    }

    @Test
    public void createErpRedeemScript_csv_value_two_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 130; // Any value above 127 needs an extra byte to indicate the sign

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, false);
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_csv_value_three_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 100_000L;

        // Should fail since this value is above the max value
        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createErpRedeemScript_csv_value_three_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 33_000L; // Any value above 32_767 needs an extra byte to indicate the sign

        Script erpRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue, false);
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_csv_value_four_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 10_000_000L;

        // Should fail since this value is above the max value
        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_csv_value_four_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 8_400_000L; // Any value above 8_388_607 needs an extra byte to indicate the sign

        // Should fail since this value is above the max value
        ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void isErpFed() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            200L
        );

        Assert.assertTrue(ErpFederationRedeemScriptParser.isErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);

        Assert.assertFalse(ErpFederationRedeemScriptParser.isErpFed(customRedeemScript.getChunks()));
    }

    private void validateErpRedeemScript(
        Script erpRedeemScript,
        Long csvValue,
        boolean hasDeprecatedFormat) {

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
        int m = defaultFedBtcECKeyList.size() / 2 + 1;
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(m)), script[index++]);

        // Assert public keys
        for (BtcECKey key : defaultFedBtcECKeyList) {
            byte[] pubkey = key.getPubKey();
            Assert.assertEquals(pubkey.length, script[index++]);
            for (int pkIndex = 0; pkIndex < pubkey.length; pkIndex++) {
                Assert.assertEquals(pubkey[pkIndex], script[index++]);
            }
        }

        // Next byte should equal N, from an M/N multisig
        int n = defaultFedBtcECKeyList.size();
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
        m = erpFedBtcECKeyList.size() / 2 + 1;
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(m)), script[index++]);

        for (BtcECKey key: erpFedBtcECKeyList) {
            byte[] pubkey = key.getPubKey();
            Assert.assertEquals(Integer.valueOf(pubkey.length).byteValue(), script[index++]);
            for (int pkIndex = 0; pkIndex < pubkey.length; pkIndex++) {
                Assert.assertEquals(pubkey[pkIndex], script[index++]);
            }
        }

        // Next byte should equal N, from an M/N multisig
        n = erpFedBtcECKeyList.size();
        Assert.assertEquals(ScriptOpCodes.getOpCode(String.valueOf(n)), script[index++]);

        Assert.assertEquals(ScriptOpCodes.OP_ENDIF, script[index++]);
        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKMULTISIG).byteValue(), script[index++]);
    }
}
