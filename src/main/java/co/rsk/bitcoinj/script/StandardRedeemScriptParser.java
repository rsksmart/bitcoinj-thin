package co.rsk.bitcoinj.script;

import static com.google.common.base.Preconditions.checkArgument;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.spongycastle.util.encoders.Hex;

public class StandardRedeemScriptParser implements RedeemScriptParser {

    // In case of P2SH represents a scriptSig, where the last chunk is the redeem script (either standard or extended)
    protected List<ScriptChunk> redeemScriptChunks;

    StandardRedeemScriptParser(List<ScriptChunk> redeemScriptChunks) {
        this.redeemScriptChunks = redeemScriptChunks;
    }

    @Override
    public int getM() {
        ScriptChunk firstChunk = redeemScriptChunks.get(0);
        return firstChunk.decodeN();
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        int numKeys = getN();
        for (int i = 0; i < numKeys; i++) {
            if (Arrays.equals(redeemScriptChunks.get(1 + i).data, key.getPubKey())) {
                return i;
            }
        }

        throw new IllegalStateException(String.format(
            "Could not find matching key %s in script", key.getPublicKeyAsHex()
        ));
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        ArrayList<BtcECKey> result = Lists.newArrayList();
        int numKeys = getN();
        for (int i = 0; i < numKeys; i++) {
            result.add(BtcECKey.fromPublicOnly(redeemScriptChunks.get(1 + i).data));
        }

        return result;
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        checkArgument(redeemScriptChunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = getN();
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(signatureBytes, true);
        for (int i = 0; i < numKeys; i++) {
            if (BtcECKey.fromPublicOnly(redeemScriptChunks.get(i + 1).data).verify(hash, signature)) {
                return i;
            }
        }
        throw new IllegalStateException(String.format(
            "Could not find matching key for signature %s on %s", Hex.toHexString(signatureBytes), hash
        ));
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return redeemScriptChunks;
    }

    @Override
    public boolean hasErpFormat() {
        return false;
    }

    private int getN() {
        ScriptChunk secondToLastChunk = redeemScriptChunks.get(redeemScriptChunks.size() - 2); // OP_N, last chunk is OP_CHECKMULTISIG
        return secondToLastChunk.decodeN();
    }
}
