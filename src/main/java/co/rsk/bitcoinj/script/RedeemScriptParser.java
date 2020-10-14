package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIG;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIGVERIFY;
import static com.google.common.base.Preconditions.checkArgument;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedeemScriptParser {

    public enum MultiSigType {
        NO_MULTISIG_TYPE,
        STANDARD_MULTISIG,
        FAST_BRIDGE_MULTISIG,
    }

    public enum ScriptType {
        P2SH,
        REDEEM_SCRIPT
    }

    private static final Logger logger = LoggerFactory.getLogger(RedeemScriptParser.class);
    private final MultiSigType multiSigType;
    private ScriptType scriptType;
    private byte[] derivationArgumentsHash;
    protected List<ScriptChunk> chunks;
    protected List<ScriptChunk> internalScript;

    public RedeemScriptParser(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) {
            this.multiSigType = MultiSigType.NO_MULTISIG_TYPE;
            this.scriptType = null;
            this.chunks = null;
            return;
        }

        extractRedeemScriptFromChunks(chunks);

        if (internalScript == null) {
            this.multiSigType = MultiSigType.NO_MULTISIG_TYPE;
            this.chunks = null;
            return;
        }

        if (internalIsFastBridgeMultiSig(internalScript)) {
            this.derivationArgumentsHash = internalScript.get(0).data;
            internalScript = internalScript.subList(2, internalScript.size());
            this.multiSigType = MultiSigType.FAST_BRIDGE_MULTISIG;
        } else if (internalIsStandardMultiSig(internalScript)) {
            this.derivationArgumentsHash = null;
            this.multiSigType = MultiSigType.STANDARD_MULTISIG;
        } else {
            this.multiSigType = MultiSigType.NO_MULTISIG_TYPE;
        }

        this.chunks = Collections.unmodifiableList(new ArrayList<>(chunks));
    }

    private void extractRedeemScriptFromChunks(List<ScriptChunk> chunks) {
        ScriptChunk lastChunk = chunks.get(chunks.size() - 1);
        if (isRedeemLikeScript(chunks)) {
            this.scriptType = ScriptType.REDEEM_SCRIPT;
            this.internalScript = chunks;
        } else {
            if (lastChunk.data != null && lastChunk.data.length > 0) {
                int lastByte = lastChunk.data[lastChunk.data.length - 1] & 0xff;
                if (lastByte == OP_CHECKMULTISIG) {
                    this.scriptType = ScriptType.P2SH;
                    this.internalScript = new Script(lastChunk.data).getChunks();
                    return;
                }
            }
            this.scriptType = null;
            this.internalScript = null;
        }
    }

    public byte[] getDerivationArgumentsHash() {
        return derivationArgumentsHash;
    }

    private boolean internalIsFastBridgeMultiSig(List<ScriptChunk> chunks) {
        if (!isRedeemLikeScript(chunks)) {
            return false;
        }

        ScriptChunk firstChunk = chunks.get(0);

        if (firstChunk.data == null) {
            return false;
        }

        boolean hasFastBridgePrefix = firstChunk.opcode == 32 && firstChunk.data.length == 32 &&
            chunks.get(1).opcode == ScriptOpCodes.OP_DROP;

        if (!hasFastBridgePrefix) {
            return false;
        }
        return hasRedeemScriptFormat(chunks.subList(2, chunks.size()));
    }

    private boolean isRedeemLikeScript(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) return false;
        ScriptChunk chunk = chunks.get(chunks.size() - 1);
        // Must end in OP_CHECKMULTISIG[VERIFY]
        if (!chunk.isOpCode()) return false;
        return chunk.equalsOpCode(OP_CHECKMULTISIG) || chunk.equalsOpCode(OP_CHECKMULTISIGVERIFY);
    }

    private boolean hasRedeemScriptFormat(List<ScriptChunk> chunks) {
        try {
            // Second to last chunk must be an OP_N opcode and there should be
            // that many data chunks (keys).
            ScriptChunk m = chunks.get(chunks.size() - 2);
            if (!m.isOpCode()) return false;
            int numKeys = Script.decodeFromOpN(m.opcode);
            if (numKeys < 1 || chunks.size() != 3 + numKeys) return false;
            for (int i = 1; i < chunks.size() - 2; i++) {
                if (chunks.get(i).isOpCode()) return false;
            }
            // First chunk must be an OP_N opcode too.
            if (Script.decodeFromOpN(chunks.get(0).opcode) < 1) return false;
        } catch (IllegalStateException e) {
            return false;   // Not an OP_N opcode.
        }
        return true;
    }

    private boolean internalIsStandardMultiSig(List<ScriptChunk> chunks) {
        return isRedeemLikeScript(chunks) && hasRedeemScriptFormat(chunks);
    }

    public boolean isStandardMultiSig() {
        return this.multiSigType == MultiSigType.STANDARD_MULTISIG;
    }
    public boolean isFastBridgeMultiSig() {
        return this.multiSigType == MultiSigType.FAST_BRIDGE_MULTISIG;
    }

    public boolean isNotMultiSig() {
        return this.multiSigType == MultiSigType.NO_MULTISIG_TYPE;
    }

    public ScriptType getScriptType() {
        return this.scriptType;
    }

    public int getM() {
        if (isNotMultiSig()) {
            return -1;
        }

        return internalScript.get(0).opcode;
    }

    public int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey) {
        // Iterate over existing signatures, skipping the initial OP_0, the final redeem script
        // and any placeholder OP_0 sigs.
        List<ScriptChunk> existingChunks = chunks.subList(1, chunks.size() - 1);
        Script redeemScript = new Script(internalScript);

        int sigCount = 0;
        int myIndex = redeemScript.findKeyInRedeem(signingKey);
        Iterator var8 = existingChunks.iterator();

        while(var8.hasNext()) {
            ScriptChunk chunk = (ScriptChunk)var8.next();
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

    public int findKeyInRedeem(BtcECKey key) {
        if (internalScript != null) {
            List<ScriptChunk> chunks = internalScript;
            checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
            int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
            for (int i = 0; i < numKeys; i++) {
                if (Arrays.equals(chunks.get(1 + i).data, key.getPubKey())) {
                    return i;
                }
            }

        }
        throw new IllegalStateException(
            "Could not find matching key " + key.toString() + " in script " + this);
    }

    public List<BtcECKey> getPubKeys() {
        if (internalScript != null) {
            List<ScriptChunk> chunks = internalScript;
            if (isStandardMultiSig() || isFastBridgeMultiSig()) {
                ArrayList<BtcECKey> result = Lists.newArrayList();
                int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
                for (int i = 0; i < numKeys; i++)
                    result.add(BtcECKey.fromPublicOnly(chunks.get(1 + i).data));
                return result;
            }

        }
        throw new ScriptException("Only usable for multisig scripts.");
    }

    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        if (internalScript != null) {
            List<ScriptChunk> chunks = internalScript;
            checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
            int numKeys = Script.decodeFromOpN(chunks.get(chunks.size() - 2).opcode);
            TransactionSignature signature = TransactionSignature
                .decodeFromBitcoin(signatureBytes, true);
            for (int i = 0; i < numKeys; i++) {
                if (BtcECKey.fromPublicOnly(chunks.get(i + 1).data).verify(hash, signature)) {
                    return i;
                }
            }
        }
        throw new IllegalStateException(
            "Could not find matching key for signature on " + hash.toString() + " sig "
                + Utils.HEX.encode(signatureBytes));
    }

    public Script extractRedeemScriptFromMultiSigFastBridgeRedeemScript(Script redeemScript) {
        if (internalIsFastBridgeMultiSig(redeemScript.getChunks())) {
            return ScriptBuilder.createRedeemScript(
                redeemScript.getNumberOfSignaturesRequiredToSpend(),
                redeemScript.getPubKeys()
            );
        } else {
            String message = String.format("Provided redeem script is not a fast bridge type: %s",
                redeemScript.toString());

            logger.debug(message);
            throw new VerificationException(message);
        }
    }

    public Script createMultiSigFastBridgeRedeemScript(Script redeemScript,
        Sha256Hash derivationArgumentsHash) {
        if (internalIsFastBridgeMultiSig(redeemScript.getChunks())) {
            String message = "Provided redeem script is already a fast bridge redeem script";
            logger.debug(message);
            throw new VerificationException(message);
        }

        if (derivationArgumentsHash == null || derivationArgumentsHash.equals(Sha256Hash.ZERO_HASH)) {
            String message = "Derivation arguments are not valid";
            logger.debug(message);
            throw new VerificationException(message);
        }

        byte[] program = redeemScript.getProgram();
        byte[] reed = Arrays.copyOf(program, program.length);
        byte[] prefix = new byte[33];

        // Hash length
        prefix[0] = 0x20;
        System.arraycopy(derivationArgumentsHash.getBytes(), 0, prefix, 1,
            derivationArgumentsHash.getBytes().length);

        byte[] c = new byte[prefix.length + 1 + reed.length];
        System.arraycopy(prefix, 0, c, 0, prefix.length);

        // OP_DROP to ignore pushed hash
        c[prefix.length] = 0x75;
        System.arraycopy(reed, 0, c, prefix.length + 1, reed.length);

        return new Script(c);
    }
}
