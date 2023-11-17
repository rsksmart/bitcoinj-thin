package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class FastBridgeP2shErpRedeemScriptParserTest {

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptTestUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptTestUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScript_fromFastBridgeP2shErpRedeemScript() {
        Long csvValue = 100L;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script fastBridgeP2shErpRedeemScript = RedeemScriptTestUtils.createFastBridgeP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            csvValue,
            derivationArgumentsHash.getBytes()
        );

        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        Script obtainedRedeemScript = new Script (FastBridgeP2shErpRedeemScriptParser.extractStandardRedeemScript(
            fastBridgeP2shErpRedeemScript.getChunks()
        ));

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys
        );

        FastBridgeP2shErpRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void isFastBridgeP2shErpFed() {
        Script fastBridgeP2shErpRedeemScript = RedeemScriptTestUtils.createFastBridgeP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(FastBridgeP2shErpRedeemScriptParser.isFastBridgeP2shErpFed(
            fastBridgeP2shErpRedeemScript.getChunks())
        );
    }

    @Test
    public void isFastBridgeP2shErpFed_falseWithFastBridgeErpFed() {
        Script fastBridgeErpRedeemScript = RedeemScriptTestUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            200L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertFalse(FastBridgeP2shErpRedeemScriptParser.isFastBridgeP2shErpFed(
            fastBridgeErpRedeemScript.getChunks())
        );
    }

    @Test
    public void isFastBridgeP2shErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptTestUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        Assert.assertFalse(FastBridgeP2shErpRedeemScriptParser.isFastBridgeP2shErpFed(
            customRedeemScript.getChunks())
        );
    }

    private static class RawGeneratedRedeemScript {
        Script powpegScript;
        Sha256Hash derivationHash;
        Script expectedScript;

        @JsonCreator
        public RawGeneratedRedeemScript(
            @JsonProperty("powpegScript") String powpegScript,
            @JsonProperty("derivationHash") String derivationHash,
            @JsonProperty("script") String expectedScript
        ) {
            this.powpegScript = new Script(Hex.decode(powpegScript));
            this.derivationHash = Sha256Hash.wrap(Hex.decode(derivationHash));
            this.expectedScript = new Script(Hex.decode(expectedScript));
        }
    }
}
