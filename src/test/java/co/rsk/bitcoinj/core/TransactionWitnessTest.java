package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.script.ScriptOpCodes;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

    @Test
    public void equals_withAnyObject_shouldBeFalse() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);

        // assert
        assertNotEquals(new Object(), transactionWitness1);
    }

    @Test
    public void equals_withADifferentClass_shouldBeFalse() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);

        // assert
        assertNotEquals("test", transactionWitness1);
    }

    @Test
    public void equals_withTwoTransactionWitness_withZeroPushCount_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);
        TransactionWitness transactionWitness2 = new TransactionWitness(0);

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
    }

    @Test
    public void equals_withTwoTransactionWitness_withOnePushCount_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(1);
        TransactionWitness transactionWitness2 = new TransactionWitness(1);

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
    }

    @Test
    public void equals_withTwoTransactionWitness_withDifferentEmptyPushCount_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(1);
        TransactionWitness transactionWitness2 = new TransactionWitness(2);

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
    }

    @Test
    public void equals_withNullAndAnEmptyTransactionWitness_shouldBeFalse() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);

        // assert
        assertNotEquals(transactionWitness1, null);
    }

    @Test
    public void equals_withTwoTransactionWitness_withDifferentPushCountAndPushes_shouldBeFalse() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);
        TransactionWitness transactionWitness2 = new TransactionWitness(1);
        byte[] push = {0x1};
        transactionWitness2.setPush(0, push);

        // assert
        assertNotEquals(transactionWitness1, transactionWitness2);
    }

    @Test
    public void equals_withTwoTransactionWitnessesWithTheSameElementsPushed_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(1);
        byte[] push = {0x1};
        transactionWitness1.setPush(0, push);

        TransactionWitness transactionWitness2 = new TransactionWitness(1);
        transactionWitness2.setPush(0, push);

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
    }

    @Test
    public void equals_withTwoTransactionWitnessesWithOneDifferentPush_shouldBeFalse() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(2);
        byte[] samePush = {0x1};
        byte[] differentPush = {0x2};
        transactionWitness1.setPush(0, samePush);
        transactionWitness1.setPush(1, differentPush);

        TransactionWitness transactionWitness2 = new TransactionWitness(2);
        byte[] anotherDifferentPush = {0x3};

        transactionWitness1.setPush(0, samePush);
        transactionWitness2.setPush(0, anotherDifferentPush);

        // assert
        assertNotEquals(transactionWitness1, transactionWitness2);
    }
}
