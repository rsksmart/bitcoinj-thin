package co.rsk.bitcoinj.deprecated.script;

import co.rsk.bitcoinj.deprecated.core.BtcECKey;
import co.rsk.bitcoinj.deprecated.core.VerificationException;
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
        erpFedBtcECKeyList.add(ecKey4);
        erpFedBtcECKeyList.add(ecKey5);
        erpFedBtcECKeyList.add(ecKey6);
        erpFedBtcECKeyList.add(ecKey7);
        erpFedBtcECKeyList.add(ecKey8);
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
    public void createErpRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 200L;

        Script expectedErpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            csvValue
        );

        Script obtainedRedeemScript = ErpFederationRedeemScriptParser.createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        Assert.assertEquals(expectedErpRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void createErpRedeemScript_invalidDefaultFederationRedeemScript() {
        Script defaultFederationRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);
        Script erpFederationRedeemScript = RedeemScriptUtils.createStandardRedeemScript(erpFedBtcECKeyList);
        Long csvValue = 200L;

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
        Long csvValue = 200L;

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
}
