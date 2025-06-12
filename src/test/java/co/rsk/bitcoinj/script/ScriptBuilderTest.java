package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import org.junit.Test;

import java.util.ArrayList;
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

    @Test(expected = IllegalArgumentException.class)
    public void createMultiSigOutputScript_withZeroPubKeys_shouldThrowAnException() {
        // Arrange
        ArrayList<BtcECKey> emptyKeys = new ArrayList<>();
        int expectedThreshold = 1;

        // Act & Assert
        ScriptBuilder.createMultiSigOutputScript(expectedThreshold, emptyKeys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMultiSigOutputScript_withLessKeysThanTheThreshold_shouldThrowAnException() {
        // Arrange
        List<BtcECKey> ecKeys = RedeemScriptUtils.getNKeys(1);
        int expectedThreshold = 2;

        // Act & Assert
        ScriptBuilder.createMultiSigOutputScript(expectedThreshold, ecKeys);
    }

    @Test
    public void createMultiSigOutputScript_withOnePubKey_shouldReturnAValidScript() {
        // Arrange
        int numberOfKeys = 1;

        // Act & Assert
        assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(numberOfKeys);
    }

    @Test
    public void createMultiSigOutputScript_withTenPubKeys_shouldReturnAValidScript() {
        // Arrange
        int numberOfKeys = 10;

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
        int expectedNumberOfChunks = numberOfKeys + 3;
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

}
