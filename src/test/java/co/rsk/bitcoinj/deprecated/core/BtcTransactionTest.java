/*
 * Copyright 2014 Google Inc.
 * Copyright 2016 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.rsk.bitcoinj.deprecated.core;

import co.rsk.bitcoinj.deprecated.crypto.TransactionSignature;
import co.rsk.bitcoinj.deprecated.params.UnitTestParams;
import co.rsk.bitcoinj.deprecated.script.Script;
import co.rsk.bitcoinj.deprecated.script.ScriptBuilder;
import co.rsk.bitcoinj.deprecated.testing.FakeTxBuilder;
import org.easymock.*;
import org.junit.*;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import static co.rsk.bitcoinj.deprecated.core.Utils.HEX;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Just check the Transaction.verify() method. Most methods that have complicated logic in Transaction are tested
 * elsewhere, e.g. signing and hashing are well exercised by the wallet tests, the full block chain tests and so on.
 * The verify method is also exercised by the full block chain tests, but it can also be used by API users alone,
 * so we make sure to cover it here as well.
 */
public class BtcTransactionTest {
    private static final NetworkParameters PARAMS = UnitTestParams.get();
    private static final Address ADDRESS = new BtcECKey().toAddress(PARAMS);

    private BtcTransaction tx;

    @Before
    public void setUp() throws Exception {
        Context context = new Context(PARAMS);
        tx = FakeTxBuilder.createFakeTx(PARAMS);
    }

    @Test(expected = VerificationException.EmptyInputsOrOutputs.class)
    public void emptyOutputs() throws Exception {
        tx.clearOutputs();
        tx.verify();
    }

    @Test(expected = VerificationException.EmptyInputsOrOutputs.class)
    public void emptyInputs() throws Exception {
        tx.clearInputs();
        tx.verify();
    }

    @Test(expected = VerificationException.LargerThanMaxBlockSize.class)
    public void tooHuge() throws Exception {
        tx.getInput(0).setScriptBytes(new byte[BtcBlock.MAX_BLOCK_SIZE]);
        tx.verify();
    }

    @Test(expected = VerificationException.DuplicatedOutPoint.class)
    public void duplicateOutPoint() throws Exception {
        TransactionInput input = tx.getInput(0);
        input.setScriptBytes(new byte[1]);
        tx.addInput(input.duplicateDetached());
        tx.verify();
    }

    @Test(expected = VerificationException.NegativeValueOutput.class)
    public void negativeOutput() throws Exception {
        tx.getOutput(0).setValue(Coin.NEGATIVE_SATOSHI);
        tx.verify();
    }

    @Test(expected = VerificationException.ExcessiveValue.class)
    public void exceedsMaxMoney2() throws Exception {
        Coin half = PARAMS.getMaxMoney().divide(2).add(Coin.SATOSHI);
        tx.getOutput(0).setValue(half);
        tx.addOutput(half, ADDRESS);
        tx.verify();
    }

    @Test(expected = VerificationException.UnexpectedCoinbaseInput.class)
    public void coinbaseInputInNonCoinbaseTX() throws Exception {
        tx.addInput(Sha256Hash.ZERO_HASH, 0xFFFFFFFFL, new ScriptBuilder().data(new byte[10]).build());
        tx.verify();
    }

    @Test(expected = VerificationException.CoinbaseScriptSizeOutOfRange.class)
    public void coinbaseScriptSigTooSmall() throws Exception {
        tx.clearInputs();
        tx.addInput(Sha256Hash.ZERO_HASH, 0xFFFFFFFFL, new ScriptBuilder().build());
        tx.verify();
    }

    @Test(expected = VerificationException.CoinbaseScriptSizeOutOfRange.class)
    public void coinbaseScriptSigTooLarge() throws Exception {
        tx.clearInputs();
        TransactionInput input = tx.addInput(Sha256Hash.ZERO_HASH, 0xFFFFFFFFL, new ScriptBuilder().data(new byte[99]).build());
        assertEquals(101, input.getScriptBytes().length);
        tx.verify();
    }

    @Test
    public void testEstimatedLockTime_WhenParameterSignifiesBlockHeight() {
        int TEST_LOCK_TIME = 20;
        Date now = Calendar.getInstance().getTime();

        BtcBlockChain mockBlockChain = createMock(BtcBlockChain.class);
        EasyMock.expect(mockBlockChain.estimateBlockTime(TEST_LOCK_TIME)).andReturn(now);

        BtcTransaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.setLockTime(TEST_LOCK_TIME); // less than five hundred million

        replay(mockBlockChain);

        assertEquals(tx.estimateLockTime(mockBlockChain), now);
    }

    @Test
    public void testOptimalEncodingMessageSize() {
        BtcTransaction tx = new BtcTransaction(PARAMS);

        int length = tx.length;

        // add basic transaction input, check the length
        tx.addOutput(new TransactionOutput(PARAMS, null, Coin.COIN, ADDRESS));
        length += getCombinedLength(tx.getOutputs());

        // add basic output, check the length
        length += getCombinedLength(tx.getInputs());

        // optimal encoding size should equal the length we just calculated
        assertEquals(tx.getOptimalEncodingMessageSize(), length);
    }

    private int getCombinedLength(List<? extends Message> list) {
        int sumOfAllMsgSizes = 0;
        for (Message m: list) { sumOfAllMsgSizes += m.getMessageSize() + 1; }
        return sumOfAllMsgSizes;
    }

    @Test
    public void testCLTVPaymentChannelTransactionSpending() {
        BigInteger time = BigInteger.valueOf(20);

        BtcECKey from = new BtcECKey(), to = new BtcECKey(), incorrect = new BtcECKey();
        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);

        BtcTransaction tx = new BtcTransaction(PARAMS);
        tx.addInput(new TransactionInput(PARAMS, tx, new byte[] {}));
        tx.getInput(0).setSequenceNumber(0);
        tx.setLockTime(time.subtract(BigInteger.ONE).longValue());
        TransactionSignature fromSig =
                tx.calculateSignature(0, from, outputScript, BtcTransaction.SigHash.SINGLE, false);
        TransactionSignature toSig =
                tx.calculateSignature(0, to, outputScript, BtcTransaction.SigHash.SINGLE, false);
        TransactionSignature incorrectSig =
                tx.calculateSignature(0, incorrect, outputScript, BtcTransaction.SigHash.SINGLE, false);
        Script scriptSig =
                ScriptBuilder.createCLTVPaymentChannelInput(fromSig, toSig);
        Script refundSig =
                ScriptBuilder.createCLTVPaymentChannelRefund(fromSig);
        Script invalidScriptSig1 =
                ScriptBuilder.createCLTVPaymentChannelInput(fromSig, incorrectSig);
        Script invalidScriptSig2 =
                ScriptBuilder.createCLTVPaymentChannelInput(incorrectSig, toSig);

        try {
            scriptSig.correctlySpends(tx, 0, outputScript, Script.ALL_VERIFY_FLAGS);
        } catch (ScriptException e) {
            e.printStackTrace();
            fail("Settle transaction failed to correctly spend the payment channel");
        }

        try {
            refundSig.correctlySpends(tx, 0, outputScript, Script.ALL_VERIFY_FLAGS);
            fail("Refund passed before expiry");
        } catch (ScriptException e) { }
        try {
            invalidScriptSig1.correctlySpends(tx, 0, outputScript, Script.ALL_VERIFY_FLAGS);
            fail("Invalid sig 1 passed");
        } catch (ScriptException e) { }
        try {
            invalidScriptSig2.correctlySpends(tx, 0, outputScript, Script.ALL_VERIFY_FLAGS);
            fail("Invalid sig 2 passed");
        } catch (ScriptException e) { }
    }

    @Test
    public void testCLTVPaymentChannelTransactionRefund() {
        BigInteger time = BigInteger.valueOf(20);

        BtcECKey from = new BtcECKey(), to = new BtcECKey(), incorrect = new BtcECKey();
        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);

        BtcTransaction tx = new BtcTransaction(PARAMS);
        tx.addInput(new TransactionInput(PARAMS, tx, new byte[] {}));
        tx.getInput(0).setSequenceNumber(0);
        tx.setLockTime(time.add(BigInteger.ONE).longValue());
        TransactionSignature fromSig =
                tx.calculateSignature(0, from, outputScript, BtcTransaction.SigHash.SINGLE, false);
        TransactionSignature incorrectSig =
                tx.calculateSignature(0, incorrect, outputScript, BtcTransaction.SigHash.SINGLE, false);
        Script scriptSig =
                ScriptBuilder.createCLTVPaymentChannelRefund(fromSig);
        Script invalidScriptSig =
                ScriptBuilder.createCLTVPaymentChannelRefund(incorrectSig);

        try {
            scriptSig.correctlySpends(tx, 0, outputScript, Script.ALL_VERIFY_FLAGS);
        } catch (ScriptException e) {
            e.printStackTrace();
            fail("Refund failed to correctly spend the payment channel");
        }

        try {
            invalidScriptSig.correctlySpends(tx, 0, outputScript, Script.ALL_VERIFY_FLAGS);
            fail("Invalid sig passed");
        } catch (ScriptException e) { }
    }

    @Test
    public void testToStringWhenLockTimeIsSpecifiedInBlockHeight() {
        BtcTransaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        TransactionInput input = tx.getInput(0);
        input.setSequenceNumber(42);

        int TEST_LOCK_TIME = 20;
        tx.setLockTime(TEST_LOCK_TIME);

        Calendar cal = Calendar.getInstance();
        cal.set(2085, 10, 4, 17, 53, 21);
        cal.set(Calendar.MILLISECOND, 0);

        BtcBlockChain mockBlockChain = createMock(BtcBlockChain.class);
        EasyMock.expect(mockBlockChain.estimateBlockTime(TEST_LOCK_TIME)).andReturn(cal.getTime());

        replay(mockBlockChain);

        String str = tx.toString(mockBlockChain);

        assertEquals(str.contains("block " + TEST_LOCK_TIME), true);
        assertEquals(str.contains("estimated to be reached at"), true);
    }

    @Test
    public void testToStringWhenIteratingOverAnInputCatchesAnException() {
        BtcTransaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        TransactionInput ti = new TransactionInput(PARAMS, tx, new byte[0]) {
            @Override
            public Script getScriptSig() throws ScriptException {
                throw new ScriptException("");
            }
        };

        tx.addInput(ti);
        assertEquals(tx.toString().contains("[exception: "), true);
    }

    @Test
    public void testToStringWhenThereAreZeroInputs() {
        BtcTransaction tx = new BtcTransaction(PARAMS);
        assertEquals(tx.toString().contains("No inputs!"), true);
    }

    @Test(expected = ScriptException.class)
    public void testAddSignedInputThrowsExceptionWhenScriptIsNotToRawPubKeyAndIsNotToAddress() {
        BtcECKey key = new BtcECKey();
        Address addr = key.toAddress(PARAMS);
        BtcTransaction fakeTx = FakeTxBuilder.createFakeTx(PARAMS, Coin.COIN, addr);

        BtcTransaction tx = new BtcTransaction(PARAMS);
        tx.addOutput(fakeTx.getOutput(0));

        Script script = ScriptBuilder.createOpReturnScript(new byte[0]);

        tx.addSignedInput(fakeTx.getOutput(0).getOutPointFor(), script, key);
    }

    @Test
    public void testPrioSizeCalc() throws Exception {
        BtcTransaction tx1 = FakeTxBuilder.createFakeTx(PARAMS, Coin.COIN, ADDRESS);
        int size1 = tx1.getMessageSize();
        int size2 = tx1.getMessageSizeForPriorityCalc();
        assertEquals(113, size1 - size2);
        tx1.getInput(0).setScriptSig(new Script(new byte[109]));
        assertEquals(78, tx1.getMessageSizeForPriorityCalc());
        tx1.getInput(0).setScriptSig(new Script(new byte[110]));
        assertEquals(78, tx1.getMessageSizeForPriorityCalc());
        tx1.getInput(0).setScriptSig(new Script(new byte[111]));
        assertEquals(79, tx1.getMessageSizeForPriorityCalc());
    }

    @Test
    public void testCoinbaseHeightCheck() throws VerificationException {
        // Coinbase transaction from block 300,000
        final byte[] transactionBytes = HEX.decode("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4803e09304062f503253482f0403c86d53087ceca141295a00002e522cfabe6d6d7561cf262313da1144026c8f7a43e3899c44f6145f39a36507d36679a8b7006104000000000000000000000001c8704095000000001976a91480ad90d403581fa3bf46086a91b2d9d4125db6c188ac00000000");
        final int height = 300000;
        final BtcTransaction transaction = PARAMS.getDefaultSerializer().makeTransaction(transactionBytes);
        transaction.checkCoinBaseHeight(height);
    }

    /**
     * Test a coinbase transaction whose script has nonsense after the block height.
     * See https://github.com/bitcoinj/bitcoinj/issues/1097
     */
    @Test
    public void testCoinbaseHeightCheckWithDamagedScript() throws VerificationException {
        // Coinbase transaction from block 224,430
        final byte[] transactionBytes = HEX.decode(
            "010000000100000000000000000000000000000000000000000000000000000000"
            + "00000000ffffffff3b03ae6c0300044bd7031a0400000000522cfabe6d6d0000"
            + "0000000000b7b8bf0100000068692066726f6d20706f6f6c7365727665726aac"
            + "1eeeed88ffffffff01e0587597000000001976a91421c0d001728b3feaf11551"
            + "5b7c135e779e9f442f88ac00000000");
        final int height = 224430;
        final BtcTransaction transaction = PARAMS.getDefaultSerializer().makeTransaction(transactionBytes);
        transaction.checkCoinBaseHeight(height);
    }

    @Test
    public void optInFullRBF() {
        // a standard transaction as wallets would create
        BtcTransaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        assertFalse(tx.isOptInFullRBF());

        tx.getInputs().get(0).setSequenceNumber(TransactionInput.NO_SEQUENCE - 2);
        assertTrue(tx.isOptInFullRBF());
    }

    /**
     * Ensure that hashForSignature() doesn't modify a transaction's data, which could wreak multithreading havoc.
     */
    @Test
    public void testHashForSignatureThreadSafety() {
        BtcBlock genesis = UnitTestParams.get().getGenesisBlock();
        BtcBlock block1 = genesis.createNextBlock(new BtcECKey().toAddress(UnitTestParams.get()),
                    genesis.getTransactions().get(0).getOutput(0).getOutPointFor());

        final BtcTransaction tx = block1.getTransactions().get(1);
        final String txHash = tx.getHashAsString();
        final String txNormalizedHash = tx.hashForSignature(0, new byte[0], BtcTransaction.SigHash.ALL.byteValue()).toString();

        for (int i = 0; i < 100; i++) {
            // ensure the transaction object itself was not modified; if it was, the hash will change
            assertEquals(txHash, tx.getHashAsString());
            new Thread(){
                public void run() {
                    assertEquals(txNormalizedHash, tx.hashForSignature(0, new byte[0], BtcTransaction.SigHash.ALL.byteValue()).toString());
                }
            };
        }
    }

    @Test
    public void testParseTxtWithWitness()  {

        // The hash in this test case was created using an address p2sh-segwit.
        byte[] rawTx = Hex.decode("02000000000101447711046c3c1f4db72f8b3157c49680ee760aa61b75659d9be8747d73ddb6710000000017160014b360da23a791dea490161fbb944b728a21d036c2ffffffff02b8d3f505000000001976a914193a62891c7a8061f12082d469ea137859fa951b88ac00e1f505000000001976a9147d8928e43014111434c5d985736de102721019cc88ac024730440220078458be99e3e262ea60883c16c7bc8fcdb40eda6420f33e7b03c4ecca8fe3180220734a09a4afa9874e1bc4050995e16ba71397e807a19cfce156a184e764a594cd0121038f505a331ea2cc00fbf4793975ef440904ba795356e839c07583f8bec4c07ace00000000");//02000000000101447711046c3c1f4db72f8b3157c49680ee760aa61b75659d9be8747d73ddb6710000000017160014b360da23a791dea490161fbb944b728a21d036c2ffffffff02b8d3f505000000001976a914193a62891c7a8061f12082d469ea137859fa951b88ac00e1f505000000001976a9147d8928e43014111434c5d985736de102721019cc88ac024730440220078458be99e3e262ea60883c16c7bc8fcdb40eda6420f33e7b03c4ecca8fe3180220734a09a4afa9874e1bc4050995e16ba71397e807a19cfce156a184e764a594cd0121038f505a331ea2cc00fbf4793975ef440904ba795356e839c07583f8bec4c07ace00000000
        BtcTransaction tx = new BtcTransaction(PARAMS, rawTx);

        assertTrue(tx.hasWitness());
        assertEquals(tx.countWitnesses(),1);

        //signers
        assertArrayEquals(tx.getWitness(0).getPush(0), Hex.decode("30440220078458be99e3e262ea60883c16c7bc8fcdb40eda6420f33e7b03c4ecca8fe3180220734a09a4afa9874e1bc4050995e16ba71397e807a19cfce156a184e764a594cd01"));
        //pubkey
        assertArrayEquals(tx.getWitness(0).getPush(1), Hex.decode("038f505a331ea2cc00fbf4793975ef440904ba795356e839c07583f8bec4c07ace"));

    }

    @Test
    public void testSerializerTxtWithWitness() {

        // The hash in this test case was created using an address p2sh-segwit.
        byte[] rawTx = Hex.decode("02000000000101447711046c3c1f4db72f8b3157c49680ee760aa61b75659d9be8747d73ddb6710000000017160014b360da23a791dea490161fbb944b728a21d036c2ffffffff02b8d3f505000000001976a914193a62891c7a8061f12082d469ea137859fa951b88ac00e1f505000000001976a9147d8928e43014111434c5d985736de102721019cc88ac024730440220078458be99e3e262ea60883c16c7bc8fcdb40eda6420f33e7b03c4ecca8fe3180220734a09a4afa9874e1bc4050995e16ba71397e807a19cfce156a184e764a594cd0121038f505a331ea2cc00fbf4793975ef440904ba795356e839c07583f8bec4c07ace00000000");//02000000000101447711046c3c1f4db72f8b3157c49680ee760aa61b75659d9be8747d73ddb6710000000017160014b360da23a791dea490161fbb944b728a21d036c2ffffffff02b8d3f505000000001976a914193a62891c7a8061f12082d469ea137859fa951b88ac00e1f505000000001976a9147d8928e43014111434c5d985736de102721019cc88ac024730440220078458be99e3e262ea60883c16c7bc8fcdb40eda6420f33e7b03c4ecca8fe3180220734a09a4afa9874e1bc4050995e16ba71397e807a19cfce156a184e764a594cd0121038f505a331ea2cc00fbf4793975ef440904ba795356e839c07583f8bec4c07ace00000000
        BtcTransaction tx = new BtcTransaction(PARAMS, rawTx);

        assertTrue(tx.hasWitness());

        assertTrue(Arrays.equals(rawTx, tx.bitcoinSerialize()));

    }

    @Test
    public void testFindWitnessCommitment() {
        // Coinbase tx for a block that has no witnesses.
        // Nevertheless, there is a witness commitment (but no witness reserved).
        byte[] rawTx = Hex.decode("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff2e03175a070004d31c9e5904d700bf0f08c2e1c3c90001017a152f426974436c7562204e6574776f726b2f4e59412fffffffff024167305e000000001976a9142cc2b87a28c8a097f48fcc1d468ced6e7d39958d88ac0000000000000000266a24aa21a9ed3d03076733467c45b08ec503a0c5d406647b073e1914d35b5111960ed625f3b700000000");
        BtcTransaction coinbase = new BtcTransaction(PARAMS, rawTx);
        assertEquals("919a0df2253172a55bebcb9002dbe775b8511f84955b282ca6dae826fdd94f90", coinbase.getHash(false).toString());
        assertEquals("919a0df2253172a55bebcb9002dbe775b8511f84955b282ca6dae826fdd94f90", coinbase.getHash(true).toString());
        Sha256Hash witnessCommitment = coinbase.findWitnessCommitment();
        assertEquals("3d03076733467c45b08ec503a0c5d406647b073e1914d35b5111960ed625f3b7", witnessCommitment.toString());
    }

    @Test
    public void testFindWitnessCommitment2() throws Exception {
        // Coinbase tx for a block that has witnesses.
        byte[] rawTx = Hex.decode("010000000001010000000000000000000000000000000000000000000000000000000000000000ffffffff5e03255a070459489e592f426978696e2f426974636f696e456e74657270726973652f4e59412ffabe6d6d0e2fee583706b08b63225903bb6b6d9accbc75fb61a14d3c075285aefa4031c0010000000000000018f0148762b9db6c00000000ffffffff02a8994968000000001976a914cef3550ff9e637ddd120717d43fc21f8a563caf888ac0000000000000000266a24aa21a9edc3c1145d8070a57e433238e42e4c022c1e51ca2a958094af243ae1ee252ca1060120000000000000000000000000000000000000000000000000000000000000000000000000");
        BtcTransaction coinbase = new BtcTransaction(PARAMS, rawTx);
        assertEquals("9c1ab453283035800c43eb6461eb46682b81be110a0cb89ee923882a5fd9daa4", coinbase.getHash(false).toString());
        assertEquals("2bbda73aa4e561e7f849703994cc5e563e4bcf103fb0f6fef5ae44c95c7b83a6", coinbase.getHash(true).toString());
        Sha256Hash witnessCommitment = coinbase.findWitnessCommitment();
        assertEquals("c3c1145d8070a57e433238e42e4c022c1e51ca2a958094af243ae1ee252ca106", witnessCommitment.toString());
        byte[] witnessReserved = coinbase.getWitness(0).getPush(0);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", HEX.encode(witnessReserved));
    }
}
