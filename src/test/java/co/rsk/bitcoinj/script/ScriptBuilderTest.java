package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Test
    public void createMultiSigOutputScript_with20PubKeys_shouldReturnAValidScript() {
        // Arrange
        int numberOfKeys = 20;

        // Act & Assert
        assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(numberOfKeys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMultiSigOutputScript_withMoreThan20PubKeys_shouldThrowAnException() {
        // Arrange
        List<BtcECKey> ecKeys = RedeemScriptUtils.getNKeys(21);
        int expectedThreshold = 11;

        // Act & Assert
        ScriptBuilder.createMultiSigOutputScript(expectedThreshold, ecKeys);
    }

    @Test
    public void number_whenFifteen_shouldEncodeAsOpcode() {
        // Act
        Script script = new ScriptBuilder().number(15).build();

        // Assert
        assertSingleOpcodeChunk(script, ScriptOpCodes.OP_15);
    }

    @Test
    public void number_when16_shouldEncodeAsOpcode() {
        // Act
        Script script = new ScriptBuilder().number(16).build();

        // Assert
        assertSingleOpcodeChunk(script, ScriptOpCodes.OP_16);
    }

    @Test
    public void number_when17_shouldEncodeAsPushData() {
        // Act
        Script script = new ScriptBuilder().number(17).build();

        // Assert
        List<ScriptChunk> chunks = script.getChunks();
        assertEquals(1, chunks.size());
        byte expectedValue = (byte) 0x11;
        assertSingleBytePushDataChunk(chunks.get(0), expectedValue);
        assertArrayEquals(new byte[]{ 0x01, expectedValue }, script.getProgram());
    }

    @Test
    public void number_withIndex_when16_shouldInsertAsOpcodeAtIndex() {
        // Arrange
        ScriptBuilder builder = new ScriptBuilder()
            .op(ScriptOpCodes.OP_DUP)
            .op(ScriptOpCodes.OP_DROP);

        // Act
        Script script = builder.number(1, 16).build();

        // Assert
        // script should be now OP_DUP OP_16 OP_DROP
        List<ScriptChunk> chunks = script.getChunks();
        assertEquals(3, chunks.size());

        assertEquals(ScriptOpCodes.OP_DUP, chunks.get(0).opcode);
        assertEquals(ScriptOpCodes.OP_16, chunks.get(1).opcode);
        assertNull(chunks.get(1).data);
        assertEquals(ScriptOpCodes.OP_DROP, chunks.get(2).opcode);
        assertArrayEquals(
            new byte[]{ (byte) ScriptOpCodes.OP_DUP, (byte) ScriptOpCodes.OP_16, (byte) ScriptOpCodes.OP_DROP },
            script.getProgram()
        );
    }

    @Test
    public void number_withIndex_when17_shouldInsertAsPushDataAtIndex() {
        // Arrange
        ScriptBuilder builder = new ScriptBuilder()
            .op(ScriptOpCodes.OP_DUP)
            .op(ScriptOpCodes.OP_DROP);

        // Act
        Script script = builder.number(1, 17).build();

        // Assert
        // script should be now OP_DUP 0x01 0x11 OP_DROP
        List<ScriptChunk> chunks = script.getChunks();
        assertEquals(3, chunks.size());

        assertEquals(ScriptOpCodes.OP_DUP, chunks.get(0).opcode);

        byte expectedValue = (byte) 0x11;
        assertSingleBytePushDataChunk(chunks.get(1), expectedValue);

        assertEquals(ScriptOpCodes.OP_DROP, chunks.get(2).opcode);
        assertArrayEquals(
            new byte[]{ (byte) ScriptOpCodes.OP_DUP, 0x01, expectedValue, (byte) ScriptOpCodes.OP_DROP },
            script.getProgram()
        );
    }

    @Test
    public void createMultiSigOutputScript_with16PubKeys_shouldEncodeNumberOfKeysAsOpcode() {
        // Arrange
        List<BtcECKey> keys = RedeemScriptUtils.getNKeys(16);
        int threshold = 9;

        // Act
        Script script = ScriptBuilder.createMultiSigOutputScript(threshold, keys);

        // Assert
        List<ScriptChunk> chunks = script.getChunks();
        ScriptChunk numberOfKeysChunk = chunks.get(chunks.size() - 2);
        assertEquals(ScriptOpCodes.OP_16, numberOfKeysChunk.opcode);
        assertNull(numberOfKeysChunk.data);

        ByteArrayOutputStream expectedProgram = new ByteArrayOutputStream();
        expectedProgram.write(ScriptOpCodes.OP_9);
        for (BtcECKey key : keys) {
            byte[] pubKey = key.getPubKey();
            expectedProgram.write(pubKey.length);
            expectedProgram.write(pubKey, 0, pubKey.length);
        }
        expectedProgram.write(ScriptOpCodes.OP_16);
        expectedProgram.write(ScriptOpCodes.OP_CHECKMULTISIG);
        assertArrayEquals(expectedProgram.toByteArray(), script.getProgram());
    }

    @Test
    public void createMultiSigOutputScript_with17PubKeys_shouldEncodeNumberOfKeysAsPushData() throws IOException {
        // Arrange
        List<BtcECKey> keys = RedeemScriptUtils.getNKeys(17);
        int threshold = 9;

        // Act
        Script script = ScriptBuilder.createMultiSigOutputScript(threshold, keys);

        // Assert
        List<ScriptChunk> chunks = script.getChunks();
        ScriptChunk numberOfKeysChunk = chunks.get(chunks.size() - 2);
        byte expectedValue = (byte) 0x11;
        assertSingleBytePushDataChunk(numberOfKeysChunk, expectedValue);
        byte[] expectedData = new byte[]{ expectedValue };

        ByteArrayOutputStream expectedProgram = new ByteArrayOutputStream();
        expectedProgram.write(ScriptOpCodes.OP_9);
        for (BtcECKey key : keys) {
            byte[] pubKey = key.getPubKey();
            expectedProgram.write(pubKey.length);
            expectedProgram.write(pubKey, 0, pubKey.length);
        }
        expectedProgram.write(expectedData.length);
        expectedProgram.write(expectedData);
        expectedProgram.write(ScriptOpCodes.OP_CHECKMULTISIG);
        assertArrayEquals(expectedProgram.toByteArray(), script.getProgram());
    }

    @Test
    public void createMultiSigOutputScript_withThreshold16_shouldEncodeThresholdAsOpcode() {
        // Arrange
        List<BtcECKey> keys = RedeemScriptUtils.getNKeys(20);
        int threshold = 16;

        // Act
        Script script = ScriptBuilder.createMultiSigOutputScript(threshold, keys);

        // Assert
        List<ScriptChunk> chunks = script.getChunks();
        ScriptChunk thresholdChunk = chunks.get(0);
        assertEquals(ScriptOpCodes.OP_16, thresholdChunk.opcode);
        assertNull(thresholdChunk.data);

        // 20 keys is still above 16, so it must be a push data
        ScriptChunk numberOfKeysChunk = chunks.get(chunks.size() - 2);
        assertSingleBytePushDataChunk(numberOfKeysChunk, (byte) 0x14);
    }

    @Test
    public void createMultiSigOutputScript_withThreshold17_shouldEncodeThresholdAsPushData() {
        // Arrange
        List<BtcECKey> keys = RedeemScriptUtils.getNKeys(20);
        int threshold = 17;

        // Act
        Script script = ScriptBuilder.createMultiSigOutputScript(threshold, keys);

        // Assert
        List<ScriptChunk> chunks = script.getChunks();
        ScriptChunk thresholdChunk = chunks.get(0);
        assertSingleBytePushDataChunk(thresholdChunk, (byte) 0x11);

        // 20 keys is above 16 too
        ScriptChunk numberOfKeysChunk = chunks.get(chunks.size() - 2);
        assertSingleBytePushDataChunk(numberOfKeysChunk, (byte) 0x14);
    }

    private static void assertSingleOpcodeChunk(Script script, int expectedOpcode) {
        List<ScriptChunk> chunks = script.getChunks();
        assertEquals(1, chunks.size());
        assertEquals(expectedOpcode, chunks.get(0).opcode);
        assertNull(chunks.get(0).data);
        assertArrayEquals(new byte[]{ (byte) expectedOpcode }, script.getProgram());
    }

    private static void assertSingleBytePushDataChunk(ScriptChunk chunk, byte expectedValue) {
        assertEquals(1, chunk.opcode);
        assertArrayEquals(new byte[]{ expectedValue }, chunk.data);
    }

    private static void assertGivenNumberOfKeysCreatesAValidMultiSigOutputScript(int numberOfKeys) {
        List<BtcECKey> ecKeys = RedeemScriptUtils.getNKeys(numberOfKeys);
        int expectedThreshold = numberOfKeys / 2 + 1;

        Script multiSigOutputScript = ScriptBuilder.createMultiSigOutputScript(expectedThreshold, ecKeys);
        assertTrue(multiSigOutputScript.isSentToMultiSig());

        // threshold (1) + pubkeys (numberOfKeys) + num of pubKeys (1) + OP_CHECKMULTISIG (1)
        int expectedNumberOfChunks = numberOfKeys + 3;
        List<ScriptChunk> chunks = multiSigOutputScript.getChunks();
        assertEquals(expectedNumberOfChunks, chunks.size());

        int index = 0;
        int actualThreshold = chunks.get(index++).decodePositiveN();
        assertEquals(expectedThreshold, actualThreshold);

        List<byte[]> pubKeys = ecKeys.stream().map(BtcECKey::getPubKey).collect(Collectors.toList());
        for (int i = 0; i < pubKeys.size(); i++) {
            byte[] actualPubKeyInTheScript = chunks.get(i + 1).data;
            assertArrayEquals(pubKeys.get(i), actualPubKeyInTheScript);
            index++;
        }

        int actualTotalKeysNumber = chunks.get(index++).decodePositiveN();
        assertEquals(pubKeys.size(), actualTotalKeysNumber);

        int actualMultiSigOpCode = chunks.get(index).opcode;
        assertEquals(ScriptOpCodes.OP_CHECKMULTISIG, actualMultiSigOpCode);
    }
}
