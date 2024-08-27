package co.rsk.bitcoinj.script;

import static com.google.common.base.Preconditions.checkArgument;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StandardRedeemScriptParser implements RedeemScriptParser {

    protected MultiSigType multiSigType;
    protected ScriptType scriptType;
    // In case of P2SH represents a scriptSig, where the last chunk is the redeem script (either standard or extended)
    // Standard redeem script
    protected List<ScriptChunk> redeemScriptChunks;

    public StandardRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks
    ) {
        this.multiSigType = MultiSigType.STANDARD_MULTISIG;
        this.scriptType = scriptType;
        this.redeemScriptChunks = redeemScriptChunks;
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
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return redeemScriptChunks;
    }

    public static boolean isStandardMultiSig(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasStandardRedeemScriptStructure(chunks);
    }
}
