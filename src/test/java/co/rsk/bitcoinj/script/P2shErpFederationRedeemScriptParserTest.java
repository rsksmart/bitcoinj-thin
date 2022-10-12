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

public class P2shErpFederationRedeemScriptParserTest {

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
    public void extractStandardRedeemScript_fromP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            100L
        );
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

        Script obtainedRedeemScript = P2shErpFederationRedeemScriptParser.extractStandardRedeemScript(
            erpRedeemScript.getChunks()
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

        P2shErpFederationRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void createP2shErpRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_invalidDefaultFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_invalidErpFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 300L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_negative_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = -100L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_zero_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 0L;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_above_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = P2shErpFederationRedeemScriptParser.MAX_CSV_VALUE + 1;

        P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );
    }

    @Test
    public void createP2shErpRedeemScript_csv_exact_max_value() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = P2shErpFederationRedeemScriptParser.MAX_CSV_VALUE;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_one_byte_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 20L;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_two_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 500L;

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test
    public void createP2shErpRedeemScript_csv_value_two_bytes_long_including_sign() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 130; // Any value above 127 needs an extra byte to indicate the sign

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_value_three_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
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
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        long csvValue = 33_000L; // Any value above 32_767 needs an extra byte to indicate the sign

        Script erpRedeemScript = P2shErpFederationRedeemScriptParser.createP2shErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        validateErpRedeemScript(erpRedeemScript, csvValue);
    }

    @Test(expected = VerificationException.class)
    public void createP2shErpRedeemScript_csv_value_four_bytes_long() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
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
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
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
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            200L
        );

        Assert.assertTrue(P2shErpFederationRedeemScriptParser.isP2shErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isErpFed_falseWithP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            200L
        );

        Assert.assertFalse(ErpFederationRedeemScriptParser.isErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isP2shErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);

        Assert.assertFalse(P2shErpFederationRedeemScriptParser.isP2shErpFed(customRedeemScript.getChunks()));
    }

    private void validateErpRedeemScript(
        Script erpRedeemScript,
        Long csvValue) {

        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

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

        // Next byte should equal OP_CHECKMULTISIG
        Assert.assertEquals(Integer.valueOf(ScriptOpCodes.OP_CHECKMULTISIG).byteValue(), script[index++]);

        Assert.assertEquals(ScriptOpCodes.OP_ENDIF, script[index++]);
    }
}
