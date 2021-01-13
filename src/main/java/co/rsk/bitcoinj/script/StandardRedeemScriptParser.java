package co.rsk.bitcoinj.script;

import static com.google.common.base.Preconditions.checkArgument;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class StandardRedeemScriptParser implements RedeemScriptParser {

    protected MultiSigType multiSigType;
    protected ScriptType scriptType;
    protected List<ScriptChunk> chunks;
    protected List<ScriptChunk> redeemScript;

    public StandardRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScript,
        List<ScriptChunk> chunks
    ) {
        this.multiSigType = MultiSigType.STANDARD_MULTISIG;
        this.scriptType = scriptType;
        this.redeemScript = redeemScript;

        this.chunks = Collections.unmodifiableList(new ArrayList<>(chunks));
    }

    @Override
    public boolean isStandardMultiSig() {
        return this.multiSigType == MultiSigType.STANDARD_MULTISIG;
    }

    @Override
    public boolean isFastBridgeMultiSig() {
        return this.multiSigType == MultiSigType.FAST_BRIDGE_MULTISIG;
    }

    @Override
    public boolean isNotMultiSig() {
        return false;
    }

    @Override
    public ScriptType getScriptType() {
        return this.scriptType;
    }

    @Override
    public int getM() {
        return Script.decodeFromOpN(redeemScript.get(0).opcode);
    }

    @Override
    public int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey) {
        // Iterate over existing signatures, skipping the initial OP_0, the final redeem script
        // and any placeholder OP_0 sigs.
        List<ScriptChunk> existingChunks = chunks.subList(1, chunks.size() - 1);
        Script redeemScript = new Script(this.redeemScript);

        int sigCount = 0;
        int myIndex = redeemScript.findKeyInRedeem(signingKey);
        Iterator chunkIterator = existingChunks.iterator();

        while(chunkIterator.hasNext()) {
            ScriptChunk chunk = (ScriptChunk)chunkIterator.next();
            if (chunk.opcode != 0) {
                Preconditions.checkNotNull(chunk.data);
                if (myIndex < redeemScript.findSigInRedeem(chunk.data, hash)) {
                    return sigCount;
                }

                ++sigCount;
            }
        }

        return sigCount;
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        List<ScriptChunk> chunks = redeemScript;
        checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            if (Arrays.equals(chunks.get(1 + i).data, key.getPubKey())) {
                return i;
            }
        }

        throw new IllegalStateException(
            "Could not find matching key " + key.toString() + " in script " + this);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        List<ScriptChunk> chunks = redeemScript;

        ArrayList<BtcECKey> result = Lists.newArrayList();
        int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            result.add(BtcECKey.fromPublicOnly(chunks.get(1 + i).data));
        }

        return result;
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        List<ScriptChunk> chunks = redeemScript;
        checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
        TransactionSignature signature = TransactionSignature
            .decodeFromBitcoin(signatureBytes, true);
        for (int i = 0; i < numKeys; i++) {
            if (BtcECKey.fromPublicOnly(chunks.get(i + 1).data).verify(hash, signature)) {
                return i;
            }
        }
        throw new IllegalStateException(
            "Could not find matching key for signature on " + hash.toString() + " sig "
                + Utils.HEX.encode(signatureBytes));
    }

}
