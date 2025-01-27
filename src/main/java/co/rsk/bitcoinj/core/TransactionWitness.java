package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.crypto.TransactionSignature;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionWitness {
    static TransactionWitness empty = new TransactionWitness(0);

    public static TransactionWitness getEmpty() {
        return empty;
    }

    private final List<byte[]> pushes;

    public TransactionWitness(int pushCount) {
        pushes = new ArrayList<byte[]>(Math.min(pushCount, Utils.MAX_INITIAL_ARRAY_LENGTH));
    }

    public static TransactionWitness of(List<byte[]> pushes) {
        return new TransactionWitness(pushes);
    }

    private TransactionWitness(List<byte[]> pushes) {
        for (byte[] push : pushes)
            Objects.requireNonNull(push);
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
}

