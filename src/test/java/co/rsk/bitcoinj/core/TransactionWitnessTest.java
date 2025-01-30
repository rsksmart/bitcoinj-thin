package co.rsk.bitcoinj.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TransactionWitnessTest {

    @Test
    public void of_withValidPushes_createsTransactionWitnessWithPushes() {
        // arrange
        byte[] byte1 = new byte[] { 10 };
        byte[] byte2 = new byte[] { 20 };
        byte[] byte3 = new byte[] { 30 };
        byte[] byte4 = new byte[] { 40 };

        List<byte[]> pushes = Arrays.asList(
            byte1, byte2, byte3, byte4
        );

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
        byte[] byte1 = new byte[] { 10 };
        byte[] byte2 = new byte[] { 20 };
        byte[] byte3 = new byte[] { 30 };

        List<byte[]> pushes = Arrays.asList(
            byte1, byte2, byte3, null
        );

        // act & assert
        assertThrows(NullPointerException.class, () -> TransactionWitness.of(pushes));
    }

}
