package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.script.RedeemScriptParser;
import co.rsk.bitcoinj.script.RedeemScriptParserFactory;
import co.rsk.bitcoinj.script.Script;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.*;
import static com.google.common.base.Preconditions.checkState;

public class TransactionWitness {
    static TransactionWitness empty = new TransactionWitness(0);

    public static TransactionWitness getEmpty() {
        return empty;
    }

    private final List<byte[]> pushes;

    public TransactionWitness(int pushCount) {
        pushes = new ArrayList<>(Math.min(pushCount, Utils.MAX_INITIAL_ARRAY_LENGTH));
    }

    public static TransactionWitness of(List<byte[]> pushes) {
        return new TransactionWitness(pushes);
    }

    private TransactionWitness(List<byte[]> pushes) {
        for (byte[] push : pushes) {
            Objects.requireNonNull(push);
        }
        this.pushes = pushes;
    }

    public byte[] getPush(int i) {
        return pushes.get(i);
    }

    public int getPushCount() {
        return pushes.size();
    }

    public void setPush(int i, byte[] value) {
        while (i >= pushes.size()) {
            pushes.add(new byte[]{});
        }
        pushes.set(i, value);
    }

    /**
     * Create a witness that can redeem a pay-to-witness-pubkey-hash output.
     */
    public static TransactionWitness createWitness(@Nullable final TransactionSignature signature, final BtcECKey pubKey) {
        final byte[] sigBytes = signature != null ? signature.encodeToBitcoin() : new byte[]{};
        final byte[] pubKeyBytes = pubKey.getPubKey();
        final TransactionWitness witness = new TransactionWitness(2);
        witness.setPush(0, sigBytes);
        witness.setPush(1, pubKeyBytes);
        return witness;
    }

    public byte[] getScriptBytes() {
        if (getPushCount() == 0)
            return new byte[0];
        else
            return pushes.get(pushes.size() - 1);
    }

    /**
     replicated logic from {@link Script#getSigInsertionIndex}
     * */
    public int getSigInsertionIndex(Sha256Hash sigHash, BtcECKey signingKey) {
        int witnessSize = getPushCount();
        int redeemScriptIndex = witnessSize - 1;
        byte[] redeemScriptData = getPush(redeemScriptIndex);
        Script redeemScript = new Script(redeemScriptData);
        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        int sigInsertionIndex = 0;
        int keyIndexInRedeem = redeemScriptParser.findKeyInRedeem(signingKey);

        byte[] emptyByte = new byte[]{};
        // the pushes that should have the signatures
        // are between first one (empty byte for checkmultisig bug)
        // and second to last one (op_notif + redeem script)
        for (int i = 1; i < getPushCount() - 1; i ++) {
            byte[] push = getPush(i);
            Preconditions.checkNotNull(push);
            if (!Arrays.equals(push, emptyByte)) {
                if (keyIndexInRedeem < redeemScriptParser.findSigInRedeem(push, sigHash)) {
                    return sigInsertionIndex;
                }

                sigInsertionIndex++;
            }
        }

        return sigInsertionIndex;
    }

    /**
        replicated logic from {@link Script#getScriptSigWithSignature}
    * */
    public TransactionWitness updateWitnessWithSignature(Script outputScript, byte[] signature, int targetIndex) {
        int sigsPrefixCount = outputScript.getSigsPrefixCount();
        int sigsSuffixCount = outputScript.getSigsSuffixCount();
        return updateWitnessWithSignature(signature, targetIndex, sigsPrefixCount, sigsSuffixCount);
    }

    /**
        replicated logic from {@link co.rsk.bitcoinj.script.ScriptBuilder#updateScriptWithSignature}
     * */
    private TransactionWitness updateWitnessWithSignature(byte[] signature, int targetIndex, int sigsPrefixCount, int sigsSuffixCount) {
        int totalPushes = getPushCount();

        byte[] emptyByte = new byte[]{};
        // since we fill the signatures in order, checking
        // the second to last push is enough to know
        // if there's space for new signatures
        byte[] secondToLastPush = getPush(totalPushes - sigsSuffixCount - 1);
        boolean hasMissingSigs = Arrays.equals(secondToLastPush, emptyByte);
        Preconditions.checkArgument(hasMissingSigs, "Witness script is already filled with signatures");

        List<byte[]> updatedPushes = new ArrayList<>();
        // the signatures appear after the prefix
        for (int i = 0; i < sigsPrefixCount; i++) {
            byte[] push = getPush(i);
            updatedPushes.add(push);
        }

        int index = 0;
        boolean inserted = false;
        // copy existing sigs
        for (int i = sigsPrefixCount; i < totalPushes - sigsSuffixCount; i++) {
            if (index == targetIndex) {
                inserted = true;
                updatedPushes.add(signature);
                ++index;
            }

            byte[] push = getPush(i);
            if (!Arrays.equals(push, emptyByte)) {
                updatedPushes.add(push);
                ++index;
            }
        }

        // add zeros for missing signatures
        while (index < totalPushes - sigsPrefixCount - sigsSuffixCount) {
            if (index == targetIndex) {
                inserted = true;
                updatedPushes.add(signature);
            } else {
                updatedPushes.add(emptyByte);
            }
            index++;
        }

        // copy the suffix
        for (int i = totalPushes - sigsSuffixCount; i < totalPushes; i++) {
            byte[] push = getPush(i);
            updatedPushes.add(push);
        }

        checkState(inserted);
        return TransactionWitness.of(updatedPushes);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }

        if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        TransactionWitness otherTxWitness = (TransactionWitness) otherObject;
        if (pushes.size() != otherTxWitness.pushes.size()) {
            return false;
        }

        for (int i = 0; i < pushes.size(); i++) {
            if (!Arrays.equals(pushes.get(i), otherTxWitness.pushes.get(i))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (byte[] push : pushes) {
            hashCode = 31 * hashCode + Arrays.hashCode(push);
        }
        return hashCode;
    }
}
