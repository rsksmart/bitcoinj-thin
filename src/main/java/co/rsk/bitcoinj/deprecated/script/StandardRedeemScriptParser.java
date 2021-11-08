package co.rsk.bitcoinj.deprecated.script;

import static com.google.common.base.Preconditions.checkArgument;

import co.rsk.bitcoinj.deprecated.core.BtcECKey;
import co.rsk.bitcoinj.deprecated.core.Sha256Hash;
import co.rsk.bitcoinj.deprecated.core.Utils;
import co.rsk.bitcoinj.deprecated.crypto.TransactionSignature;
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
    // In case of P2SH represents a scriptSig, where the last chunk is the redeem script (either standard or extended)
    protected List<ScriptChunk> rawChunks;
    // Standard redeem script
    protected List<ScriptChunk> redeemScriptChunks;

    public StandardRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks,
        List<ScriptChunk> rawChunks
    ) {
        this.multiSigType = MultiSigType.STANDARD_MULTISIG;
        this.scriptType = scriptType;
        this.redeemScriptChunks = redeemScriptChunks;

        this.rawChunks = Collections.unmodifiableList(new ArrayList<>(rawChunks));
    }

    @Override
    public MultiSigType getMultiSigType() {
        return this.multiSigType;
    }

    @Override
    public ScriptType getScriptType() {
        return this.scriptType;
    }

    @Override
    public int getM() {
        checkArgument(redeemScriptChunks.get(0).isOpCode());
        return Script.decodeFromOpN(redeemScriptChunks.get(0).opcode);
    }

    @Override
    public int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey) {
        // Iterate over existing signatures, skipping the initial OP_0, the final redeem script
        // and any placeholder OP_0 sigs.
        List<ScriptChunk> existingChunks = rawChunks.subList(1, rawChunks.size() - 1);
        Script redeemScript = new Script(this.redeemScriptChunks);

        int sigCount = 0;
        int myIndex = redeemScript.findKeyInRedeem(signingKey);
        Iterator chunkIterator = existingChunks.iterator();

        while(chunkIterator.hasNext()) {
            ScriptChunk chunk = (ScriptChunk) chunkIterator.next();
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
        checkArgument(redeemScriptChunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(redeemScriptChunks.get(redeemScriptChunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            if (Arrays.equals(redeemScriptChunks.get(1 + i).data, key.getPubKey())) {
                return i;
            }
        }

        throw new IllegalStateException("Could not find matching key " + key.toString() + " in script " + this);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        ArrayList<BtcECKey> result = Lists.newArrayList();
        int numKeys = Script.decodeFromOpN(redeemScriptChunks.get(redeemScriptChunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            result.add(BtcECKey.fromPublicOnly(redeemScriptChunks.get(1 + i).data));
        }

        return result;
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        checkArgument(redeemScriptChunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(redeemScriptChunks.get(redeemScriptChunks.size() - 2).opcode);
        TransactionSignature signature = TransactionSignature
            .decodeFromBitcoin(signatureBytes, true);
        for (int i = 0; i < numKeys; i++) {
            if (BtcECKey.fromPublicOnly(redeemScriptChunks.get(i + 1).data).verify(hash, signature)) {
                return i;
            }
        }
        throw new IllegalStateException(
            "Could not find matching key for signature on " + hash.toString() + " sig "
                + Utils.HEX.encode(signatureBytes)
        );
    }

    @Override
    public Script extractStandardRedeemScript() {
        return new Script(redeemScriptChunks);
    }

    public static boolean isStandardMultiSig(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasStandardRedeemScriptStructure(chunks);
    }
}
