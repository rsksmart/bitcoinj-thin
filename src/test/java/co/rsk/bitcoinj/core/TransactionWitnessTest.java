package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.script.ScriptOpCodes;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TransactionWitnessTest {
    private static final Script redeemScript = new Script(
        Hex.decode("5221027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d35610072102d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856210346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e53ae")
    ); // data from tx https://mempool.space/testnet/tx/1744459aeaf7369aadc9fc40de9ab2bf575b14e35029b35a7ee4bbd3de65af7f
    private static final byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());

    private static final Script witnessScript = new ScriptBuilder()
        .number(ScriptOpCodes.OP_0)
        .data(redeemScriptHash)
        .build();
    private static final byte[] witnessScriptHash = Utils.sha256hash160(witnessScript.getProgram());

    private static final byte[] op0 = new byte[] {};

    private List<byte[]> pushes;

    @Test
    public void of_withValidPushes_createsTransactionWitnessWithPushes() {
        // arrange
        pushes = new ArrayList<>();

        pushes.add(op0);
        pushes.add(witnessScriptHash);

        // act
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);

        // assert
        for (int i = 0; i < pushes.size(); i++) {
            assertEquals(pushes.get(i), transactionWitness.getPush(i));
        }
    }

    @Test
    public void of_withOneNullPush_throwsNPE() {
        // arrange
        pushes = new ArrayList<>();

        pushes.add(op0);
        pushes.add(witnessScriptHash);

        pushes.add(null);

        // act & assert
        assertThrows(NullPointerException.class, () -> TransactionWitness.of(pushes));
    }
}
