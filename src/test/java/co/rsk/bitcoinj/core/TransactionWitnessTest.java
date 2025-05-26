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
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TransactionWitnessTest {
    public static final int FIRST_INPUT_INDEX = 0;
    private static final NetworkParameters MAINNET_PARAMS = MainNetParams.get();
    private static final List<BtcECKey> FEDERATION_KEYS = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private static final BtcECKey fedKey1 = FEDERATION_KEYS.get(0);
    private static final BtcECKey fedKey2 = FEDERATION_KEYS.get(1);
    private static final BtcECKey fedKey3 = FEDERATION_KEYS.get(2);
    private static final List<BtcECKey> ERP_FEDERATION_KEYS = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    private static final long CSV_VALUE = 52_560L;
    private static final Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
        FEDERATION_KEYS,
        ERP_FEDERATION_KEYS,
        CSV_VALUE
    );
    private static final byte[] redeemScriptSerialized = redeemScript.getProgram();
    private static final Script p2shOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
    private static final Script witnessScript = new ScriptBuilder()
        .number(ScriptOpCodes.OP_0)
        .data(redeemScriptSerialized)
        .build();
    private static final byte[] witnessScriptHash = Utils.sha256hash160(witnessScript.getProgram());
    private static final byte[] op0 = new byte[] {};
    private static final Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
    private static final Coin prevValue = Coin.FIFTY_COINS;
    private static final BtcTransaction prevTx = getPreviousBtcTransaction();
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

        // act
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

        // act
        int hashCode1 = transactionWitness1.hashCode();
        int hashCode2 = transactionWitness2.hashCode();

        // assert
        assertNotEquals(transactionWitness1, transactionWitness2);
        assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    public void getSigInsertionIndex_whenEmptyWitness_shouldThrownArrayIndexOutOfBoundsException() {
        // arrange
        pushes = new ArrayList<>();
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);

        // act & assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> transactionWitness.getSigInsertionIndex(hashForSignature, fedKey1));
    }

    @Test
    public void getSigInsertionIndex_whenMalformedRedeemScript_shouldThrowException() {
        // arrange
        Script customRedeemScript = new Script(new byte[2]);
        byte[] emptyByte = {};
        pushes = new ArrayList<>();
        pushes.add(emptyByte);
        pushes.add(customRedeemScript.getProgram());
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);

        // act & assert
        assertThrows(ScriptException.class, () -> transactionWitness.getSigInsertionIndex(hashForSignature, fedKey1));
    }

    @Test
    public void getSigInsertionIndex_withWitnessWithoutSignatures_shouldReturnZero() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);
        TransactionWitness transactionWitness = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigInsertionIndex = transactionWitness.getSigInsertionIndex(btcTxSigHash, fedKey1);

        // assert
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void getSigInsertionIndex_withTwoDifferentKeys_withNoSignaturesInTheWitness_shouldReturnInBothZero() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);
        TransactionWitness transactionWitness = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        // fedKey1 signature should go to sigIndex 0
        int sigIndexForFedKey1 = transactionWitness.getSigInsertionIndex(btcTxSigHash, fedKey1);
        Assert.assertEquals(0, sigIndexForFedKey1);

        // fedKey2 signature should go to sigIndex 0 because the signatures are empty yet
        int sigIndexForFedKey2 = transactionWitness.getSigInsertionIndex(btcTxSigHash, fedKey2);
        Assert.assertEquals(0, sigIndexForFedKey2);
    }

    @Test
    public void getSigInsertionIndex_withAGreaterPubKeySignatureInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        // sign with fedKey1
        signInput(btcTx, fedKey1, FIRST_INPUT_INDEX, btcTxSigHash);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexForFedKey2 = witnessWithSignature.getSigInsertionIndex(btcTxSigHash, fedKey2);

        // assert
        Assert.assertEquals(1, sigIndexForFedKey2);
    }

    @Test
    public void getSigInsertionIndex_withALowerPubKeySignatureInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        // sign with fedKey1
        signInput(btcTx, fedKey2, FIRST_INPUT_INDEX, btcTxSigHash);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexForFedKey2 = witnessWithSignature.getSigInsertionIndex(btcTxSigHash, fedKey1);

        // assert
        Assert.assertEquals(0, sigIndexForFedKey2);
    }

    @Test
    public void getSigInsertionIndex_withTheSamePubKeyWhichSignatureAlreadyInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        // sign with fedKey1
        signInput(btcTx, fedKey1, FIRST_INPUT_INDEX, btcTxSigHash);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexAfterInsertingSignature = witnessWithSignature.getSigInsertionIndex(btcTxSigHash, fedKey1);

        // assert
        assertEquals(1, sigIndexAfterInsertingSignature);
    }

    @Test
    public void getSigInsertionIndex_withDifferentSignaturesInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        // sign with fedKey2
        signInput(btcTx, fedKey2, FIRST_INPUT_INDEX, btcTxSigHash);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int sigIndexForFedKey1 = witnessWithSignature.getSigInsertionIndex(btcTxSigHash, fedKey1);
        int sigIndexForFedKey3 = witnessWithSignature.getSigInsertionIndex(btcTxSigHash, fedKey3);

        Assert.assertEquals(0, sigIndexForFedKey1);
        // fedKey3 should be inserted after fedKey2
        Assert.assertEquals(1, sigIndexForFedKey3);

        // now fedKey1 signs the input and pushes fedKey2's signature one position
        signInput(btcTx, fedKey1, FIRST_INPUT_INDEX, btcTxSigHash);
        TransactionWitness witnessWithFedKey1Signature = btcTx.getWitness(FIRST_INPUT_INDEX);

        sigIndexForFedKey3 = witnessWithFedKey1Signature.getSigInsertionIndex(btcTxSigHash, fedKey3);
        Assert.assertEquals(2, sigIndexForFedKey3);
    }

    @Test
    public void getSigInsertionIndex_withFedKey3SignatureInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);

        // sign with fedKey3
        signInput(btcTx, fedKey3, FIRST_INPUT_INDEX, btcTxSigHash);
        TransactionWitness witnessWithFedKey3Signature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexForFedKey1 = witnessWithFedKey3Signature.getSigInsertionIndex(btcTxSigHash, fedKey1);
        int sigIndexForFedKey2 = witnessWithFedKey3Signature.getSigInsertionIndex(btcTxSigHash, fedKey2);

        // assert
        Assert.assertEquals(0, sigIndexForFedKey1);
        Assert.assertEquals(0, sigIndexForFedKey2);
    }

    @Test
    public void getSigInsertionIndex_withWitnessFilledWithSignatures_shouldReturnTheProperIndex() {
        BtcTransaction btcTx = getBtcTransactionWithBaseWitnessInInput();
        Sha256Hash btcTxSigHash = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, prevValue,
            BtcTransaction.SigHash.ALL, false);
        TransactionWitness transactionWitness = btcTx.getWitness(FIRST_INPUT_INDEX);

        int witnessPushCount = transactionWitness.getPushCount();
        int sigsSuffixCount = p2shOutputScript.getSigsSuffixCount();

        // the pushes that should have the signatures
        // are between first one (empty byte for checkmultisig bug)
        // and second to last one (op_notif + redeem script)
        byte[] secondToLastPush = transactionWitness.getPush(witnessPushCount - sigsSuffixCount - 1);
        byte[] emptyByte = new byte[]{};

        int i = 0;
        while(Arrays.equals(secondToLastPush, emptyByte)) {
            BtcECKey key = FEDERATION_KEYS.get(i);
            signInput(btcTx, key, FIRST_INPUT_INDEX, btcTxSigHash);
            TransactionWitness witnessWithSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);
            secondToLastPush = witnessWithSignatures.getPush(witnessPushCount - sigsSuffixCount - 1);
            i++;
        }

        TransactionWitness signedTransactionWitness = btcTx.getWitness(FIRST_INPUT_INDEX);
        BtcECKey key = FEDERATION_KEYS.get(i);
        int sigInsertionIndex = signedTransactionWitness.getSigInsertionIndex(btcTxSigHash, key);
        assertEquals(i, sigInsertionIndex);
    }

    private static BtcTransaction getPreviousBtcTransaction() {
        BtcTransaction btcTx = new BtcTransaction(MAINNET_PARAMS);
        final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(901)).toAddress(MAINNET_PARAMS);
        btcTx.addOutput(prevValue, userAddress);
        return btcTx;
    }

    private static BtcTransaction getBtcTransactionWithBaseWitnessInInput() {
        BtcTransaction btcTx = new BtcTransaction(MAINNET_PARAMS);
        btcTx.addInput(prevTx.getOutput(0));
        TransactionWitness witnessWithRedeemScript = createBaseWitnessThatSpendsFromErpRedeemScript(redeemScript);
        btcTx.setWitness(FIRST_INPUT_INDEX, witnessWithRedeemScript);
        return btcTx;
    }

    private void signInput(BtcTransaction btcTx, BtcECKey key, int inputIndex, Sha256Hash sigHash) {
        TransactionWitness transactionWitness = btcTx.getWitness(inputIndex);
        int sigIndex = transactionWitness.getSigInsertionIndex(sigHash, key);
        byte[] federatorSig = key.sign(sigHash).encodeToDER();
        BtcECKey.ECDSASignature signature = BtcECKey.ECDSASignature.decodeFromDER(federatorSig);
        TransactionSignature txSig = new TransactionSignature(signature, BtcTransaction.SigHash.ALL, false);
        TransactionWitness witnessWithSignature = transactionWitness.updateWitnessWithSignature(p2shOutputScript,
            txSig.encodeToBitcoin(), sigIndex);
        btcTx.setWitness(inputIndex, witnessWithSignature);
    }

    private static TransactionWitness createBaseWitnessThatSpendsFromErpRedeemScript(Script redeemScript) {
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
