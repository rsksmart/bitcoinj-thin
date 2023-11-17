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
    public void createFastBridgeP2shErpRedeemScript_fromP2shErpRedeemScript() {
        Script erpRedeemScript = RedeemScriptTestUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            5063L
        );

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});

        Script expectedRedeemScript = RedeemScriptTestUtils.createFastBridgeP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            5063L,
            derivationArgumentsHash.getBytes()
        );

        Script obtainedRedeemScript = FastBridgeP2shErpRedeemScriptParser.createFastBridgeP2shErpRedeemScript(
            erpRedeemScript,
            derivationArgumentsHash
        );

        Assert.assertEquals(expectedRedeemScript, obtainedRedeemScript);
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

    @Test
    public void createFastBridgeP2shErpRedeemScript_compareAgainstOtherImplementation() throws IOException {

        byte[] rawRedeemScripts;
        try {
            rawRedeemScripts = Files.readAllBytes(Paths.get("src/test/resources/redeemScripts_fastbridge_p2shERP.json"));
        } catch (IOException e) {
            System.out.println("redeemScripts_p2shERP.json file not found");
            throw(e);
        }

        RawGeneratedRedeemScript[] generatedScripts = new ObjectMapper().readValue(rawRedeemScripts, RawGeneratedRedeemScript[].class);

        for (RawGeneratedRedeemScript generatedScript : generatedScripts) {
            Script bitcoinjScript = FastBridgeP2shErpRedeemScriptParser.createFastBridgeP2shErpRedeemScript(
                generatedScript.powpegScript,
                generatedScript.derivationHash
            );

            Script expectedScript = generatedScript.expectedScript;
            Assert.assertEquals(expectedScript, bitcoinjScript);
        }
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
