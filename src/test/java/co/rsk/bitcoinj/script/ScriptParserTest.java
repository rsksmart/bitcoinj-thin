package co.rsk.bitcoinj.script;

import static org.junit.Assert.assertArrayEquals;

import co.rsk.bitcoinj.core.BtcECKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ScriptParserTest {
    private final List<BtcECKey> fedPublicKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private final long csv = 52_560;
    private final List<BtcECKey> emergencyPublicKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();

    @Test
    public void parseScriptProgram_whenStandardMultiSigRedeemScript_shouldParseOk() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(fedPublicKeys);

        List<ScriptChunk> actualScriptChunks = ScriptParser.parseScriptProgram(
            standardRedeemScript.getProgram());

        List<ScriptChunk> expectedScriptChunks = new ArrayList<>(standardRedeemScript.getChunks());
        assertScriptChunks(expectedScriptChunks, actualScriptChunks);
    }

    @Test
    public void parseScriptProgram_whenP2shErpRedeemScript_shouldParseOk() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(fedPublicKeys,
            emergencyPublicKeys, csv);

        List<ScriptChunk> actualScriptChunks = ScriptParser.parseScriptProgram(
            redeemScript.getProgram());

        List<ScriptChunk> expectedScriptChunks = redeemScript.getChunks();
        assertScriptChunks(expectedScriptChunks, actualScriptChunks);
    }

    @Test
    public void parseScriptProgram_whenErpRedeemScript_shouldParseOk() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(fedPublicKeys,
            emergencyPublicKeys, csv);

        List<ScriptChunk> actualScriptChunks = ScriptParser.parseScriptProgram(
            redeemScript.getProgram());

        List<ScriptChunk> expectedScriptChunks = redeemScript.getChunks();
        assertScriptChunks(expectedScriptChunks, actualScriptChunks);
    }

    private void assertScriptChunks(List<ScriptChunk> expectedScriptChunks, List<ScriptChunk> actualScriptChunks) {
        Assert.assertEquals(expectedScriptChunks.size(), actualScriptChunks.size());
        for (int i = 0; i < expectedScriptChunks.size(); i++) {
            ScriptChunk expectedScriptChunk = expectedScriptChunks.get(i);
            ScriptChunk actualScriptChunk = actualScriptChunks.get(i);
            Assert.assertEquals(expectedScriptChunk.opcode, actualScriptChunk.opcode);
            assertArrayEquals(expectedScriptChunk.data, actualScriptChunk.data);
        }
    }
}
