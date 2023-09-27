package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FastBridgeErpRedeemScriptParserTest {

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScript_fromFastBridgeErpRedeemScript() {
        Long csvValue = 100L;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            csvValue,
            derivationArgumentsHash.getBytes()
        );

        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);

        Script obtainedRedeemScript = FlyoverRedeemScriptParser.extractStandardRedeemScript(
            fastBridgeErpRedeemScript.getChunks()
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);

        FlyoverRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void createFastBridgeErpRedeemScript_from_Erp_redeem_script() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            5063L
        );

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});

        Script expectedRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            5063L,
            derivationArgumentsHash.getBytes()
        );

        Script obtainedRedeemScript = FlyoverRedeemScriptParser.createFastBridgeRedeemScript(
            erpRedeemScript,
            derivationArgumentsHash
        );

        Assert.assertEquals(expectedRedeemScript, obtainedRedeemScript);
    }

    @Test
    public void isFastBridgeErpFed() {
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(FlyoverRedeemScriptParser.isFastBridgeErpFed(fastBridgeErpRedeemScript.getChunks()));
    }

    @Test
    public void isFastBridgeErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys);

        Assert.assertFalse(FlyoverRedeemScriptParser.isFastBridgeErpFed(customRedeemScript.getChunks()));
    }
}
