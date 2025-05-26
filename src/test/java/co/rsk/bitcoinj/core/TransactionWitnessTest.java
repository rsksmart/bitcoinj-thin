package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.script.RedeemScriptUtils;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.script.ScriptOpCodes;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TransactionWitnessTest {
    private static final NetworkParameters MAINNET_PARAMS = MainNetParams.get();
    private static final List<BtcECKey> FEDERATION_KEYS = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private static final BtcECKey fedKey1 = FEDERATION_KEYS.get(0);
    private static final BtcECKey fedKey2 = FEDERATION_KEYS.get(1);
    private static final List<BtcECKey> ERP_FEDERATION_KEYS = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    private static final long CSV_VALUE = 52_560L;
    private static final Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
        FEDERATION_KEYS,
        ERP_FEDERATION_KEYS,
        CSV_VALUE
    );
    private static final byte[] redeemScriptSerialized = redeemScript.getProgram();
    private static final Script witnessScript = new ScriptBuilder()
        .number(ScriptOpCodes.OP_0)
        .data(redeemScriptSerialized)
        .build();
    private static final byte[] witnessScriptHash = Utils.sha256hash160(witnessScript.getProgram());
    private static final byte[] op0 = new byte[] {};
    private static final Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
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
    public void hashCode_withNoPush_shouldBeOne() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);

        // act
        int hashCode = transactionWitness1.hashCode();

        // assert
        int hashCodeExpected = 1;
        assertEquals(hashCodeExpected, hashCode);
    }

    @Test
    public void equals_withTwoTransactionWitness_withZeroPushCount_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(0);
        TransactionWitness transactionWitness2 = new TransactionWitness(0);
        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void equals_withTwoTransactionWitness_withSamePushes_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(1);
        TransactionWitness transactionWitness2 = new TransactionWitness(1);
        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void equals_withTwoTransactionWitness_withDifferentEmptyPushCount_shouldBeTrue() {
        // arrange
        TransactionWitness transactionWitness1 = new TransactionWitness(1);
        TransactionWitness transactionWitness2 = new TransactionWitness(2);
        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
        assertEquals(hashCode1, hashCode2);
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
        byte[] push = {0x1};
        TransactionWitness transactionWitness1 = new TransactionWitness(0);
        TransactionWitness transactionWitness2 = new TransactionWitness(1);
        transactionWitness2.setPush(0, push);
        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertNotEquals(transactionWitness1, transactionWitness2);
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    public void equals_withTwoTransactionWitnessesWithTheSameElementsPushed_shouldBeTrue() {
        // arrange
        byte[] samePush = {0x1};

        TransactionWitness transactionWitness1 = new TransactionWitness(1);
        transactionWitness1.setPush(0, samePush);

        TransactionWitness transactionWitness2 = new TransactionWitness(1);
        transactionWitness2.setPush(0, samePush);

        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertEquals(transactionWitness1, transactionWitness2);
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void equals_withTwoTransactionWitnessesWithOneDifferentPush_shouldBeFalse() {
        // arrange
        byte[] samePush = {0x1};

        TransactionWitness transactionWitness1 = new TransactionWitness(2);
        byte[] differentPush = {0x2};
        transactionWitness1.setPush(0, samePush);
        transactionWitness1.setPush(1, differentPush);

        TransactionWitness transactionWitness2 = new TransactionWitness(2);
        byte[] anotherDifferentPush = {0x3};
        transactionWitness1.setPush(0, samePush);
        transactionWitness2.setPush(0, anotherDifferentPush);

        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertNotEquals(transactionWitness1, transactionWitness2);
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    public void getSigInsertionIndex_whenEmptyWitness_shouldThrownArrayIndexOutOfBoundsException() {
        pushes = new ArrayList<>();
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> transactionWitness.getSigInsertionIndex(hashForSignature, fedKey1));
    }

    @Test
    public void getSigInsertionIndex_whenMalformedRedeemScript_shouldThrowException() {
        Script customRedeemScript = new Script(new byte[2]);
        byte[] emptyByte = {};
        pushes = new ArrayList<>();
        pushes.add(emptyByte);
        pushes.add(customRedeemScript.getProgram());
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);
        assertThrows(ScriptException.class, () -> transactionWitness.getSigInsertionIndex(hashForSignature, fedKey1));
    }

    @Test
    public void getSigInsertionIndex_whenScriptWithOnlyEmptyArrayAndRedeemScript_shouldReturnZero() {
        pushes = new ArrayList<>();
        byte[] emptyByte = {};
        pushes.add(emptyByte); // OP_0
        pushes.add(emptyByte); // OP_NOTIF
        pushes.add(redeemScriptSerialized);
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);

        int sigInsertionIndex = transactionWitness.getSigInsertionIndex(hashForSignature, fedKey1);
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void getSigInsertionIndex_whenSegwitWithP2shRedeemScript_withOneSignature_shouldReturnIndexZero() {
        BtcTransaction prevTx = new BtcTransaction(MAINNET_PARAMS);
        final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(900)).toAddress(
            MAINNET_PARAMS);
        Coin prevValue = Coin.FIFTY_COINS;
        prevTx.addOutput(prevValue, userAddress);

        BtcTransaction btcTx = new BtcTransaction(MAINNET_PARAMS);
        btcTx.addInput(prevTx.getOutput(0));
        int inputIndex = 0;
        TransactionWitness witnessScript = createBaseWitnessThatSpendsFromErpRedeemScript(redeemScript);
        btcTx.setWitness(inputIndex, witnessScript);

        Sha256Hash sigHash = btcTx.hashForWitnessSignature(inputIndex, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        TransactionWitness transactionWitness = btcTx.getWitness(inputIndex);
        int sigIndex = transactionWitness.getSigInsertionIndex(sigHash, fedKey1);
        Assert.assertEquals(0, sigIndex);
    }

    @Test
    public void getSigInsertionIndex_whenSegwitWithP2shRedeemScript_withTwoSignatures_shouldReturnIndexCorrectly() {
        BtcTransaction prevTx = new BtcTransaction(MAINNET_PARAMS);
        final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(900)).toAddress(
            MAINNET_PARAMS);
        Coin prevValue = Coin.FIFTY_COINS;
        prevTx.addOutput(prevValue, userAddress);

        BtcTransaction btcTx = new BtcTransaction(MAINNET_PARAMS);
        btcTx.addInput(prevTx.getOutput(0));
        int inputIndex = 0;
        TransactionWitness witnessScript = createBaseWitnessThatSpendsFromErpRedeemScript(redeemScript);
        btcTx.setWitness(inputIndex, witnessScript);

        Sha256Hash sigHash = btcTx.hashForWitnessSignature(inputIndex, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        TransactionWitness transactionWitness = btcTx.getWitness(inputIndex);

        // fedKey1 signature should go to sigIndex 0
        int sigIndex = transactionWitness.getSigInsertionIndex(sigHash, fedKey1);
        Assert.assertEquals(0, sigIndex);

        // fedKey1 signature should go to sigIndex 0 because the signatures are empty yet
        int sigIndex1 = transactionWitness.getSigInsertionIndex(sigHash, fedKey2);
        Assert.assertEquals(0, sigIndex1);

        // sign with fedKey1
        byte[] federatorSig = fedKey1.sign(sigHash).encodeToDER();
        TransactionSignature txSig = new TransactionSignature(BtcECKey.ECDSASignature.decodeFromDER(federatorSig), BtcTransaction.SigHash.ALL, false);
        Script p2SHOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
        TransactionWitness inputWitnessWithSignature = transactionWitness.updateWitnessWithSignature(p2SHOutputScript, txSig.encodeToBitcoin(), sigIndex);
        btcTx.setWitness(inputIndex, inputWitnessWithSignature);

        TransactionWitness transactionWitnessWithSignature = btcTx.getWitness(inputIndex);
        int sigIndex2 = transactionWitnessWithSignature.getSigInsertionIndex(sigHash, fedKey2);
        Assert.assertEquals(1, sigIndex2);

    }

    public static TransactionWitness createBaseWitnessThatSpendsFromErpRedeemScript(Script redeemScript) {
        int pushForEmptyByte = 1;
        int pushForOpNotif = 1;
        int pushForRedeemScript = 1;
        int numberOfSignaturesRequiredToSpend = redeemScript.getNumberOfSignaturesRequiredToSpend();
        int witnessSize = pushForRedeemScript + pushForOpNotif + numberOfSignaturesRequiredToSpend + pushForEmptyByte;

        List<byte[]> pushes = new ArrayList<>(witnessSize);
        byte[] emptyByte = {};
        pushes.add(emptyByte); // OP_0

        for (int i = 0; i < numberOfSignaturesRequiredToSpend; i++) {
            pushes.add(emptyByte);
        }

        pushes.add(emptyByte); // OP_NOTIF
        pushes.add(redeemScript.getProgram());
        return TransactionWitness.of(pushes);
    }
}
