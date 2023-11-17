package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
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
        defaultRedeemScriptKeys = RedeemScriptTestUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptTestUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScript_fromP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptTestUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100L
        );
        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        Script obtainedRedeemScript = new Script(P2shErpFederationRedeemScriptParser.extractStandardRedeemScript(
            erpRedeemScript.getChunks()
        ));

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        P2shErpFederationRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void isP2shErpFed() {
        Script erpRedeemScript = RedeemScriptTestUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertTrue(P2shErpFederationRedeemScriptParser.isP2shErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isErpFed_falseWithP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptTestUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L
        );

        Assert.assertFalse(ErpFederationRedeemScriptParser.isErpFed(erpRedeemScript.getChunks()));
    }

    @Test
    public void isP2shErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptTestUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(P2shErpFederationRedeemScriptParser.isP2shErpFed(customRedeemScript.getChunks()));
    }
}
