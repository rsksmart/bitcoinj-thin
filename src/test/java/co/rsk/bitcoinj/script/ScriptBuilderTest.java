package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ScriptBuilderTest {
    @Test
    public void createMultiSigOutputScript_withTwentyPubKeys_shouldReturnAValidScript() {
        List<BtcECKey> twentyECKeys = RedeemScriptUtils.getNKeys(20);
        List<byte[]> twentyPubKeys = twentyECKeys.stream().map(BtcECKey::getPubKey).collect(Collectors.toList());
        int threshold = 10;
        Script multiSigOutputScript = ScriptBuilder.createMultiSigOutputScript(threshold, twentyECKeys);
        List<ScriptChunk> chunks = multiSigOutputScript.getChunks();

        // threshold (1) + pubkeys (20) + num of pubKeys (1) + OP_CHECKMULTISIG (1)
        int expectedNumberOfChunks = 23;
        assertEquals(expectedNumberOfChunks, chunks.size());

        int index = 0;
        int actualThreshold = chunks.get(index++).decodeN();
        assertEquals(threshold, actualThreshold);

        for (int i = 0; i < twentyPubKeys.size(); i++) {
            byte[] actualPubKeyInTheScript = chunks.get(index++).data;
            assertArrayEquals(twentyPubKeys.get(i), actualPubKeyInTheScript);
        }

        int actualTotalKeysNumber = chunks.get(index++).decodeN();
        assertEquals(twentyPubKeys.size(), actualTotalKeysNumber);

        int actualMultiSigOpCode = chunks.get(index).opcode;
        assertEquals(ScriptOpCodes.OP_CHECKMULTISIG, actualMultiSigOpCode);
    }
}
