package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ScriptBuilderTest {
    @Test
    public void createMultiSigOutputScript_withTwentyPubKeys_shouldReturnAValidScript() {
        // Arrange
        int numberOfKeys = 20;

        // Act & Assert
        assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(numberOfKeys);
    }

    @Test
    public void createMultiSigOutputScript_withFifteenPubKeys_shouldReturnAValidScript() {
        // Arrange
        int numberOfKeys = 15;

        // Act & Assert
        assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(numberOfKeys);
    }

    @Test
    public void createMultiSigOutputScript_withSixteenPubKeys_shouldReturnAValidScript() {
        // Arrange
        int numberOfKeys = 16;

        // Act & Assert
        assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(numberOfKeys);
    }

    private static void assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(int numberOfKeys) {
        List<BtcECKey> ecKeys = RedeemScriptUtils.getNKeys(numberOfKeys);
        int expectedThreshold = numberOfKeys / 2 + 1;

        Script multiSigOutputScript = ScriptBuilder.createMultiSigOutputScript(expectedThreshold, ecKeys);

        // threshold (1) + pubkeys (numberOfKeys) + num of pubKeys (1) + OP_CHECKMULTISIG (1)
        int expectedNumberOfChunks = getExpectedNumberOfChunks(numberOfKeys);
        List<ScriptChunk> chunks = multiSigOutputScript.getChunks();
        assertEquals(expectedNumberOfChunks, chunks.size());

        int index = 0;
        int actualThreshold = chunks.get(index++).decodeN();
        assertEquals(expectedThreshold, actualThreshold);

        List<byte[]> pubKeys = ecKeys.stream().map(BtcECKey::getPubKey).collect(Collectors.toList());
        for (int i = 0; i < pubKeys.size(); i++) {
            byte[] actualPubKeyInTheScript = chunks.get(i + 1).data;
            assertArrayEquals(pubKeys.get(i), actualPubKeyInTheScript);
            index++;
        }

        int actualTotalKeysNumber = chunks.get(index++).decodeN();
        assertEquals(pubKeys.size(), actualTotalKeysNumber);

        int actualMultiSigOpCode = chunks.get(index).opcode;
        assertEquals(ScriptOpCodes.OP_CHECKMULTISIG, actualMultiSigOpCode);
    }

    private static int getExpectedNumberOfChunks(int pubKeys) {
        // threshold (1) + pubkeys (N) + num of pubKeys (1) + OP_CHECKMULTISIG (1)
        return pubKeys + 3;
    }

}
