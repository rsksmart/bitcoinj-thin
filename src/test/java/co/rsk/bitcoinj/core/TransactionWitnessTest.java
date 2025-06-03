package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.script.RedeemScriptUtils;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.script.ScriptOpCodes;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TransactionWitnessTest {
    private static final int FIRST_INPUT_INDEX = 0;
    private static final NetworkParameters MAINNET_PARAMS = MainNetParams.get();
    private static final List<BtcECKey> FEDERATION_KEYS = getNDefaultRedeemScriptKeys(20);
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
    private static final Sha256Hash redeemScriptSerialized = Sha256Hash.of(redeemScript.getProgram());
    private static final Script p2shP2wshOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
    private static final int sigsPrefixCount = p2shP2wshOutputScript.getSigsPrefixCount();
    private static final Script witnessScript = new ScriptBuilder()
        .number(ScriptOpCodes.OP_0)
        .data(redeemScriptSerialized.getBytes())
        .build();
    private static final byte[] witnessScriptHash = Utils.sha256hash160(witnessScript.getProgram());
    private static final byte[] op0 = new byte[] {};
    private static final Coin fundingValue = Coin.FIFTY_COINS;
    private static final BtcTransaction fundingTx = getFundingBtcTransaction();
    private List<byte[]> pushes;
    private BtcTransaction btcTx;
    private Sha256Hash btcTxSigHashForWitness;


    @Before
    public void setUp() {
        pushes = new ArrayList<>();
        btcTx = new BtcTransaction(MAINNET_PARAMS);
        btcTx.addInput(fundingTx.getOutput(0));
        btcTx.addInput(fundingTx.getOutput(1));
        TransactionWitness witnessWithRedeemScript = createBaseWitnessThatSpendsFromErpRedeemScript(redeemScript);
        btcTx.setWitness(FIRST_INPUT_INDEX, witnessWithRedeemScript);
        btcTxSigHashForWitness = btcTx.hashForWitnessSignature(FIRST_INPUT_INDEX, redeemScript, fundingValue,
            BtcTransaction.SigHash.ALL, false);
    }

    @Test
    public void of_withValidPushes_createsTransactionWitnessWithPushes() {
        // arrange
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
        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        pushes = new ArrayList<>();
        TransactionWitness transactionWitness = TransactionWitness.of(pushes);

        // act & assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> transactionWitness.getSigInsertionIndex(hashForSignature, fedKey1));
    }

    @Test
    public void getSigInsertionIndex_whenMalformedRedeemScript_shouldThrowException() {
        // arrange
        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
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
    public void getSigInsertionIndex_withWitnessWithoutSignatures_shouldReturnZeroForAllKeys() {
        // arrange
        TransactionWitness witness = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        for (BtcECKey key: FEDERATION_KEYS) {
            int sigInsertionIndex = witness.getSigInsertionIndex(btcTxSigHashForWitness, key);

            // assert
            Assert.assertEquals(0, sigInsertionIndex);
        }
    }

    @Test
    public void getSigInsertionIndex_withAGreaterPubKeySignatureInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int sigIndexForFedKey1BeforeSigning = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        int sigIndexForFedKey2BeforeSigning = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        Assert.assertEquals(0, sigIndexForFedKey1BeforeSigning);
        Assert.assertEquals(0, sigIndexForFedKey2BeforeSigning);

        // sign with fedKey1
        signInput(btcTx, fedKey1, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);
        int sigIndexForFedKey1AfterSigning = witnessWithSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        int sigIndexForFedKey2AfterSigning = witnessWithSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);

        Assert.assertEquals(1, sigIndexForFedKey1AfterSigning);
        Assert.assertEquals(1, sigIndexForFedKey2AfterSigning);
    }

    @Test
    public void getSigInsertionIndex_withALowerPubKeySignatureInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        // sign with fedKey2
        signInput(btcTx, fedKey2, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexForFedKey1 = witnessWithSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);

        // assert
        Assert.assertEquals(0, sigIndexForFedKey1);
    }

    @Test
    public void getSigInsertionIndex_withTheSamePubKeyWhichSignatureAlreadyInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        // sign with fedKey1
        signInput(btcTx, fedKey1, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexAfterInsertingSignature = witnessWithSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);

        // assert
        assertEquals(1, sigIndexAfterInsertingSignature);
    }

    @Test
    public void getSigInsertionIndex_withDifferentSignaturesInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        // sign with fedKey2
        signInput(btcTx, fedKey2, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
        TransactionWitness witnessWithSignature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int sigIndexForFedKey1 = witnessWithSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        int sigIndexForFedKey3 = witnessWithSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);

        Assert.assertEquals(0, sigIndexForFedKey1);
        // fedKey3 should be inserted after fedKey2
        Assert.assertEquals(1, sigIndexForFedKey3);

        // now fedKey1 signs the input and pushes fedKey2's signature one position
        signInput(btcTx, fedKey1, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
        TransactionWitness witnessWithFedKey1Signature = btcTx.getWitness(FIRST_INPUT_INDEX);

        sigIndexForFedKey3 = witnessWithFedKey1Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);
        Assert.assertEquals(2, sigIndexForFedKey3);
    }

    @Test
    public void getSigInsertionIndex_withFedKey3SignatureInTheWitness_shouldReturnIndexCorrectly() {
        // arrange
        // sign with fedKey3
        signInput(btcTx, fedKey3, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
        TransactionWitness witnessWithFedKey3Signature = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act
        int sigIndexForFedKey1 = witnessWithFedKey3Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        int sigIndexForFedKey2 = witnessWithFedKey3Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);

        // assert
        Assert.assertEquals(0, sigIndexForFedKey1);
        Assert.assertEquals(0, sigIndexForFedKey2);
    }

    @Test
    public void getSigInsertionIndex_withWitnessFilledWithSignatures_shouldReturnTheProperIndex() {
        // arrange
        int i = 0;
        while(i < redeemScript.getNumberOfSignaturesRequiredToSpend()) {
            BtcECKey key = FEDERATION_KEYS.get(i);
            signInput(btcTx, key, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
            i++;
        }

        TransactionWitness signedTransactionWitness = btcTx.getWitness(FIRST_INPUT_INDEX);
        BtcECKey key = FEDERATION_KEYS.get(i);

        // act
        int sigInsertionIndex = signedTransactionWitness.getSigInsertionIndex(btcTxSigHashForWitness, key);

        //assert
        assertEquals(i, sigInsertionIndex);
    }

    @Test
    public void updateWitnessWithSignature_withOneSignature_shouldReturnAWitnessWithTheSignaturePlacedCorrectly() {
        // signing order: [fedKey1]
        // arrange
        byte[] signatureEncodeToBitcoin = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);

        TransactionWitness witnessWithoutSignature = btcTx.getWitness(FIRST_INPUT_INDEX);
        int sigIndex = witnessWithoutSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, sigIndex);

        // act
        TransactionWitness witnessWithOneSignature = witnessWithoutSignature.updateWitnessWithSignature(
            p2shP2wshOutputScript, signatureEncodeToBitcoin, sigIndex);

        // assert
        assertSignaturesAreInOrder(witnessWithOneSignature, Lists.newArrayList(signatureEncodeToBitcoin));
    }

    @Test
    public void updateWitnessWithSignature_twoTimesWithTheSameSignature_shouldInsertBoth() {
        // signing order: [fedKey1, fedKey1]
        // expected signatures order: [signatureFed1, signatureFed1]
        // arrange
        byte[] fedKey1TxSignature = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int sigIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, sigIndex);
        TransactionWitness witnessWithOneSignature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1TxSignature, sigIndex);
        assertSignaturesAreInOrder(witnessWithOneSignature, Lists.newArrayList(fedKey1TxSignature));

        int sigIndexAfterSigning = witnessWithOneSignature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertNotEquals(sigIndex, sigIndexAfterSigning);
        assertEquals(1, sigIndexAfterSigning);

        TransactionWitness witnessWithTwoSignatures = witnessWithOneSignature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1TxSignature, sigIndexAfterSigning);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey1TxSignature, fedKey1TxSignature));
    }

    @Test
    public void updateWitnessWithSignature_withWitnessFilledWithSignatures_shouldThrowAnError() {
        // signing order: [fedKey1, .., maxFedKey, fedKey1] signatures
        // expected signatures order: [signatureFed1, .., signatureMaxFed]
        // arrange
        int i = 0;
        int numberOfSignaturesRequiredToSpend = redeemScript.getNumberOfSignaturesRequiredToSpend();
        while(i < numberOfSignaturesRequiredToSpend) {
            BtcECKey key = FEDERATION_KEYS.get(i);
            signInput(btcTx, key, FIRST_INPUT_INDEX, btcTxSigHashForWitness);
            i++;
        }

        // getSuffixCount doesn't consider OP_NOTIF param op code, so the calculation for
        // the number of signatures required in updateWitnessWithSignature is
        // wrong.
        BtcECKey key = FEDERATION_KEYS.get(i++);
        signInput(btcTx, key, FIRST_INPUT_INDEX, btcTxSigHashForWitness);

        byte[] txSig = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        TransactionWitness signedTransactionWitness = btcTx.getWitness(FIRST_INPUT_INDEX);
        key = FEDERATION_KEYS.get(i);
        int sigInsertionIndex = signedTransactionWitness.getSigInsertionIndex(btcTxSigHashForWitness, key);

        // act & assert
        assertTrue(numberOfSignaturesRequiredToSpend < sigInsertionIndex);
        assertEquals(numberOfSignaturesRequiredToSpend + 1, sigInsertionIndex);

        // It fails because the witness is already filled with signatures.
        // Then, the sigIndex is higher than the amount of signatures required.
        assertThrows(IllegalArgumentException.class, () -> signedTransactionWitness.updateWitnessWithSignature(
            p2shP2wshOutputScript, txSig, sigInsertionIndex));
    }

    @Test
    public void updateWitnessWithSignature_withTheLowestSignatureInWitness_shouldInsertTheNewOneAsSecond() {
        // signing order: [fedKey1, fedKey2]
        // expected signatures order: [signatureFed1, signatureFed2]
        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);
        int fed1SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, fed1SigInsertionIndex);

        // act & assert
        TransactionWitness witnessWithFedKey1Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithFedKey1Signature, Lists.newArrayList(fedKey1SignatureEncoded));

        byte[] fedKey2TxSignature = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        int fed2SigInsertionIndex = witnessWithFedKey1Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(1, fed2SigInsertionIndex);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey1Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2TxSignature, fed2SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey1SignatureEncoded, fedKey1SignatureEncoded));
    }

    @Test
    public void updateWitnessWithSignature_withALowerSignatureInWitness_shouldInsertTheNewOneAsFirst() {
        // signing order: [fedKey2, fedKey1]
        // expected signatures order: [signatureFed1, signatureFed2]
        // arrange
        byte[] fedKey2SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey2, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int fed2SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(0, fed2SigInsertionIndex);
        TransactionWitness witnessWithFedKey2Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2SignatureEncoded, fed2SigInsertionIndex);
        assertSignaturesAreInOrder(witnessWithFedKey2Signature, Lists.newArrayList(fedKey2SignatureEncoded));

        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        int fed1SigInsertionIndex = witnessWithFedKey2Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, fed1SigInsertionIndex);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey2Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);

        ArrayList<byte[]> expectedSignatures = Lists.newArrayList(fedKey1SignatureEncoded, fedKey2SignatureEncoded);
        assertSignaturesAreInOrder(witnessWithTwoSignatures, expectedSignatures);
    }

    @Test
    public void updateWitnessWithSignature_withThreeSignaturesInDescendingOrder_shouldBeInsertedRespectingTheOrder() {
        // signing order: [fedKey3, fedKey2, fedKey1]
        // expected signatures order: [signatureFed1, signatureFed2, signatureFed3]
        // arrange
        byte[] fedKey3SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey3, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int fed3SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);
        assertEquals(0, fed3SigInsertionIndex);
        TransactionWitness witnessWithFedKey3Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey3SignatureEncoded, fed3SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithFedKey3Signature, Lists.newArrayList(fedKey3SignatureEncoded));

        byte[] fedKey2SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey2, btcTxSigHashForWitness);
        int fed2SigInsertionIndex = witnessWithFedKey3Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(0, fed2SigInsertionIndex);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey3Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2SignatureEncoded, fed2SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey2SignatureEncoded, fedKey3SignatureEncoded));

        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        int fed1SigInsertionIndex = witnessWithTwoSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, fed1SigInsertionIndex);
        TransactionWitness witnessWithThreeSignatures = witnessWithTwoSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithThreeSignatures, Lists.newArrayList(fedKey1SignatureEncoded, fedKey2SignatureEncoded, fedKey3SignatureEncoded));
    }

    @Test
    public void updateWitnessWithSignature_withThreeUnorderedSignatures_shouldBeInsertedRespectingTheOrder() {
        // signing order: [fedKey1, fedKey3, fedKey2]
        // expected signatures order: [signatureFed1, signatureFed2, signatureFed3]
        // arrange
        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);

        // act & assert
        int fed1SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, fed1SigInsertionIndex);
        TransactionWitness witnessWithFedKey1Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithFedKey1Signature, Lists.newArrayList(fedKey1SignatureEncoded));

        byte[] fedKey3SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey3, btcTxSigHashForWitness);
        int fed3SigInsertionIndex = witnessWithFedKey1Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);
        assertEquals(1, fed3SigInsertionIndex);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey1Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey3SignatureEncoded, fed3SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey3SignatureEncoded));

        byte[] fedKey2SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey2, btcTxSigHashForWitness);
        int fed2SigInsertionIndex = witnessWithTwoSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(1, fed2SigInsertionIndex);
        TransactionWitness witnessWithThreeSignatures = witnessWithTwoSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2SignatureEncoded, fed2SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithThreeSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey2SignatureEncoded, fedKey3SignatureEncoded));
    }

    @Test
    public void updateWitnessWithSignature_withThreeSignaturesInAscendingOrder_shouldBeInsertedRespectingTheOrder() {
        // signing order: [fedKey1, fedKey2, fedKey3]
        // expected signatures order: [signatureFed1, signatureFed2, signatureFed3]
        // arrange
        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);
        int fed1SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, fed1SigInsertionIndex);
        // act & assert
        TransactionWitness witnessWithFedKey1Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithFedKey1Signature, Lists.newArrayList(fedKey1SignatureEncoded));

        byte[] fedKey2SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey2, btcTxSigHashForWitness);
        int fed2SigInsertionIndex = witnessWithFedKey1Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(1, fed2SigInsertionIndex);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey1Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2SignatureEncoded, fed2SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey2SignatureEncoded));

        byte[] fedKey3SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey3, btcTxSigHashForWitness);
        int fed3SigInsertionIndex = witnessWithTwoSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);
        assertEquals(2, fed3SigInsertionIndex);
        TransactionWitness witnessWithThreeSignatures = witnessWithTwoSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey3SignatureEncoded, fed3SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithThreeSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey2SignatureEncoded, fedKey3SignatureEncoded));
    }

    @Test
    public void updateWitnessWithSignature_withThreeSignaturesInADifferentOrder_shouldBeInsertedRespectingTheOrder() {
        // signing order: [fedKey2, fedKey1, fedKey3]
        // expected signatures order: [signatureFed1, signatureFed2, signatureFed3]
        // arrange
        byte[] fedKey2SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey2, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);
        int fed2SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(0, fed2SigInsertionIndex);
        // act & assert
        TransactionWitness witnessWithFedKey2Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2SignatureEncoded, fed2SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithFedKey2Signature, Lists.newArrayList(fedKey2SignatureEncoded));

        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        int fed1SigInsertionIndex = witnessWithFedKey2Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey2Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);
        assertEquals(0, fed1SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey2SignatureEncoded));

        byte[] fedKey3SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey3, btcTxSigHashForWitness);
        int fed3SigInsertionIndex = witnessWithTwoSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);
        assertEquals(2, fed3SigInsertionIndex);
        TransactionWitness witnessWithThreeSignatures = witnessWithTwoSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey3SignatureEncoded, fed3SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithThreeSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey2SignatureEncoded, fedKey3SignatureEncoded));
    }

    @Test
    public void updateWitnessWithSignature_withThreeSignaturesInASecondDifferentOrder_shouldBeInsertedRespectingTheOrder() {
        // signing order: [fedKey3, fedKey1, fedKey2]
        // expected signatures order: [signatureFed1, signatureFed2, signatureFed3]
        // arrange
        byte[] fedKey3SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey3, btcTxSigHashForWitness);
        TransactionWitness witnessWithoutSignatures = btcTx.getWitness(FIRST_INPUT_INDEX);
        int fed3SigInsertionIndex = witnessWithoutSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey3);
        assertEquals(0, fed3SigInsertionIndex);
        // act & assert
        TransactionWitness witnessWithFedKey3Signature = witnessWithoutSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey3SignatureEncoded, fed3SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithFedKey3Signature, Lists.newArrayList(fedKey3SignatureEncoded));

        byte[] fedKey1SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey1, btcTxSigHashForWitness);
        int fed1SigInsertionIndex = witnessWithFedKey3Signature.getSigInsertionIndex(btcTxSigHashForWitness, fedKey1);
        assertEquals(0, fed1SigInsertionIndex);
        TransactionWitness witnessWithTwoSignatures = witnessWithFedKey3Signature.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey1SignatureEncoded, fed1SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithTwoSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey3SignatureEncoded));

        byte[] fedKey2SignatureEncoded = getTransactionSignatureEncodedToBtc(fedKey2, btcTxSigHashForWitness);
        int fed2SigInsertionIndex = witnessWithTwoSignatures.getSigInsertionIndex(btcTxSigHashForWitness, fedKey2);
        assertEquals(1, fed2SigInsertionIndex);
        TransactionWitness witnessWithThreeSignatures = witnessWithTwoSignatures.updateWitnessWithSignature(
            p2shP2wshOutputScript, fedKey2SignatureEncoded, fed2SigInsertionIndex);

        assertSignaturesAreInOrder(witnessWithThreeSignatures, Lists.newArrayList(fedKey1SignatureEncoded,
            fedKey2SignatureEncoded, fedKey3SignatureEncoded));
    }

    private void assertSignaturesAreInOrder(TransactionWitness witness, List<byte[]> expectedSignatures) {
        int index = 0;
        for (byte[] expectedSignature : expectedSignatures) {
            int signaturePosition = sigsPrefixCount + index;
            byte[] actualSignature = witness.getPush(signaturePosition);
            assertArrayEquals(expectedSignature, actualSignature);
            index++;
        }
    }

    private byte[] getTransactionSignatureEncodedToBtc(BtcECKey key, Sha256Hash sigHash) {
        byte[] federatorSig = key.sign(sigHash).encodeToDER();
        BtcECKey.ECDSASignature signature = BtcECKey.ECDSASignature.decodeFromDER(federatorSig);
        TransactionSignature txSig = new TransactionSignature(signature, BtcTransaction.SigHash.ALL, false);
        return txSig.encodeToBitcoin();
    }

    private static BtcTransaction getFundingBtcTransaction() {
        BtcTransaction btcTx = new BtcTransaction(MAINNET_PARAMS);
        final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(901)).toAddress(MAINNET_PARAMS);
        btcTx.addOutput(fundingValue, userAddress);
        btcTx.addOutput(fundingValue, userAddress);
        return btcTx;
    }

    private void signInput(BtcTransaction btcTx, BtcECKey key, int inputIndex, Sha256Hash sigHash) {
        byte[] txSigEncodedForBitcoin = getTransactionSignatureEncodedToBtc(key, sigHash);

        TransactionWitness transactionWitness = btcTx.getWitness(inputIndex);
        int sigIndex = transactionWitness.getSigInsertionIndex(sigHash, key);
        TransactionWitness witnessWithSignature = transactionWitness.updateWitnessWithSignature(p2shP2wshOutputScript,
            txSigEncodedForBitcoin, sigIndex);
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

        byte[] opNotIf = {};
        pushes.add(opNotIf);
        pushes.add(redeemScript.getProgram());
        return TransactionWitness.of(pushes);
    }

    public static List<BtcECKey> getNDefaultRedeemScriptKeys(int n) {
        ArrayList<BtcECKey> keys = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            long seed = i * 100;
            BtcECKey btcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(seed));
            keys.add(btcECKey);
        }
        keys.sort(BtcECKey.PUBKEY_COMPARATOR);

        return keys;
    }
}
