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
        defaultRedeemScriptKeys = RedeemScriptTestUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptTestUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScript_fromFastBridgeErpRedeemScript() {
        Long csvValue = 100L;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script fastBridgeErpRedeemScript = RedeemScriptTestUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            csvValue,
            derivationArgumentsHash.getBytes()
        );

        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);

        Script obtainedRedeemScript = FastBridgeErpRedeemScriptParser.extractStandardRedeemScript(
            fastBridgeErpRedeemScript.getChunks()
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);

        FastBridgeErpRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void createFastBridgeErpRedeemScript_from_Erp_redeem_script() {
        Script erpRedeemScript = RedeemScriptTestUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            5063L
        );

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});

        Script expectedRedeemScript = RedeemScriptTestUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            5063L,
            derivationArgumentsHash.getBytes()
        );

        Script obtainedRedeemScript = FastBridgeErpRedeemScriptParser.createFastBridgeErpRedeemScript(
            erpRedeemScript,
            derivationArgumentsHash
        );

        Assert.assertEquals(expectedRedeemScript, obtainedRedeemScript);
    }

    @Test
    public void isFastBridgeErpFed() {
        Script fastBridgeErpRedeemScript = RedeemScriptTestUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(FastBridgeErpRedeemScriptParser.isFastBridgeErpFed(fastBridgeErpRedeemScript.getChunks()));
    }

    @Test
    public void isFastBridgeErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptTestUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys);

        Assert.assertFalse(FastBridgeErpRedeemScriptParser.isFastBridgeErpFed(customRedeemScript.getChunks()));
    }
}
