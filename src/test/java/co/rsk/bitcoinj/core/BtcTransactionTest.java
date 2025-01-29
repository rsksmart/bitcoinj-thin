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

package co.rsk.bitcoinj.core;

import static co.rsk.bitcoinj.core.Utils.HEX;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.params.UnitTestParams;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.testing.FakeTxBuilder;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

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

        assertTrue(str.contains("block " + TEST_LOCK_TIME));
        assertTrue(str.contains("estimated to be reached at"));
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
        assertTrue(tx.toString().contains("[exception: "));
    }

    @Test
    public void testToStringWhenThereAreZeroInputs() {
        BtcTransaction tx = new BtcTransaction(PARAMS);
        assertTrue(tx.toString().contains("No inputs!"));
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
        assertEquals(1, tx.countWitnesses());

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
        assertArrayEquals(rawTx, tx.bitcoinSerialize());
    }

    @Test
    public void testWitnessSignatureP2SH_P2WSHSingleAnyoneCanPay() {
        // test vector P2SH-P2WSH from the final example at:
        // https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki#p2sh-p2wsh
        String txHex = "01000000" // version
            + "01" // num txIn
            + "36641869ca081e70f394c6948e8af409e18b619df2ed74aa106c1ca29787b96e" + "01000000" + "00" + "ffffffff" // txIn
            + "02" // num txOut
            + "00e9a43500000000" + "1976a914" + "389ffce9cd9ae88dcc0631e88a821ffdbe9bfe26" + "88ac" // txOut
            + "c0832f0500000000" + "1976a914" + "7480a33f950689af511e6e84c138dbbd3c3ee415" + "88ac" // txOut
            + "00000000"; // nLockTime
        byte[] rawTx = Hex.decode(txHex);

        NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        BtcTransaction tx = new BtcTransaction(mainnet, rawTx);

        BtcECKey pubKey = BtcECKey.fromPublicOnly(
            Hex.decode("02d8b661b0b3302ee2f162b09e07a55ad5dfbe673a9f01d9f0c19617681024306b")
        );
        Script script = new Script(
            Hex.decode("56210307b8ae49ac90a048e9b53357a2354b3334e9c8bee813ecb98e99a7e07e8c3ba32103b28f0c28bfab54554ae8c658ac5c3e0ce6e79ad336331f78c428dd43eea8449b21034b8113d703413d57761b8b9781957b8c0ac1dfe69f492580ca4195f50376ba4a21033400f6afecb833092a9a21cfdf1ed1376e58c5d1f47de74683123987e967a8f42103a6d48b1131e94ba04d9737d61acdaa1322008af9602b3b14862c07a1789aac162102d8b661b0b3302ee2f162b09e07a55ad5dfbe673a9f01d9f0c19617681024306b56ae")
        );
        Sha256Hash hash = tx.hashForWitnessSignature(0, script, Coin.valueOf(987654321L),
            BtcTransaction.SigHash.SINGLE, true);
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(
            Hex.decode("30440220525406a1482936d5a21888260dc165497a90a15669636d8edca6b9fe490d309c022032af0c646a34a44d1f4576bf6a4a74b67940f8faa84c7df9abe12a01a11e2b4783"),
            true,
            true
        );
        assertTrue(pubKey.verify(hash, signature));
    }

    @Test
    public void hashForWitnessSignature_shouldGenerateCorrectSigHash() {
        // test P2SH-P2WSH from real transaction:
        // https://mempool.space/tx/a4d76b6211b078cbc1d2079002437fcf018cc85cd40dd6195bb0f6b42930b96b

        // arrange
        NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        byte[] rawTx = Hex.decode("02000000000101f547ebc11f4cca193a8fbfdf9eaf5728d1c6770da371f5ada0655d68f23f9cca0000000023220020431ef0e7fb92b803aa735278649879a4ffe79f79eca7733a046d1d97698cac4fffffffff01905f0100000000001976a9140eda35d81e2a8537beebbbcdf63e3483be01269288ac0e004730440220375d5ddad1d329105d5bb2453fd4a57f93e8b864b11519cea4c6932d414236d3022056e9567d5e8fea093cab9d85432007add04eed9019790159f3b644c3b3e690930148304502210095201c22ed71453c89288bbb87e98425e59f90523ffbf8669cf6739cb4d98868022017ba3a6903c6aa4d770643717ed74edcddfda54f60f8825f5cd4ed12d265db64014830450221009d8e509f6f9b22e74401f3aa06df9e212af0708e798d9b8ae9badc725a7f3d890220592d4ac99a951408f5d49a76015f9c5c8e54e34ff32bba2bfeb73ea3b4ebd75d01483045022100a960302593ecae2aba3f41bcc4cda98e2fcf54de4c479440abf002c444b98bb0022055440ae8f2b425e7b2f47847794789c769645af002e8a1084dd59e693a5e04c20147304402200d11ffb6808f6b426aff02e603abfc8eac5b3e74e3b2fb4318d47640692c7b0d0220274929b2f6e583c43358adbde3465ea1254de0abd3787db88039d00dd3d3015e014830450221009681ba08b0c826fff6499c86fd0f38216d0ea36b24d440e4aa3f5598c385370602203e9e0dae5141c1fd8598d0cb42bc44a40dda55a0577e458a22d6843e536857b401483045022100da95b59e4aac7451b5fd9efb1fa0df16ef44d8e82070daf28c90a16de50491920220637241a243cf6b7d84b3be0dfe4f08c485dfe193bd97b1290632d190f584311b01483045022100916a09eef76b47165b99e77c9a55592bcdaecd22d4932df2366a92c9304cf80502207cd0b6d85a757952c0fd56d0ee7426a7a14ecddf068707ffab3ec06af87b4a1801483045022100b4b990d471ce70de4be19aa241466b214a27b428333dcccbe885092eec4d71d302200c4e3e50aa4c2b1417ee3f174e7332fc045c304114445e05616e032aa812e9aa01483045022100af9a30d639fc333387ebf77945b4397b85f93ff6a9d8f8aeee8cca22e3383c9e02207460a2adb4264a2ecffa2eb43e59ae78e33b6f9cee44989dbec56281e5e2c1c001483045022100afc850cec037c459bbf2e8b559c863f3fa43f5ae01984d7516051a1995133917022063f04ab8d398825ad9e22a37628cf69af19dda26e6e793a3f2e797350eca6d4b0100fd810520010000000000000000000000000000000000000000000000000000000000000075645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae6800000000");
        BtcTransaction tx = new BtcTransaction(mainnet, rawTx);

        Script script = new Script(
            Hex.decode("20010000000000000000000000000000000000000000000000000000000000000075645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae68")
        );

        // act
        Coin prevOutputValue = Coin.valueOf(100000L);
        Sha256Hash sigHash = tx.hashForWitnessSignature(0, script, prevOutputValue, BtcTransaction.SigHash.ALL, false);

        // assert
        BtcECKey pubKey = BtcECKey.fromPublicOnly(
            Hex.decode("0211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c")
        );
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(
            Hex.decode("30440220375d5ddad1d329105d5bb2453fd4a57f93e8b864b11519cea4c6932d414236d3022056e9567d5e8fea093cab9d85432007add04eed9019790159f3b644c3b3e6909301"),
            true,
            true
        );
        assertTrue(pubKey.verify(sigHash, signature));
    }

    @Test
    public void hashForWitnessSignature_withWrongPrevOutputValue_shouldFail() {
        // test P2SH-P2WSH from real transaction:
        // https://mempool.space/tx/a4d76b6211b078cbc1d2079002437fcf018cc85cd40dd6195bb0f6b42930b96b

        // arrange
        NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        byte[] rawTx = Hex.decode("02000000000101f547ebc11f4cca193a8fbfdf9eaf5728d1c6770da371f5ada0655d68f23f9cca0000000023220020431ef0e7fb92b803aa735278649879a4ffe79f79eca7733a046d1d97698cac4fffffffff01905f0100000000001976a9140eda35d81e2a8537beebbbcdf63e3483be01269288ac0e004730440220375d5ddad1d329105d5bb2453fd4a57f93e8b864b11519cea4c6932d414236d3022056e9567d5e8fea093cab9d85432007add04eed9019790159f3b644c3b3e690930148304502210095201c22ed71453c89288bbb87e98425e59f90523ffbf8669cf6739cb4d98868022017ba3a6903c6aa4d770643717ed74edcddfda54f60f8825f5cd4ed12d265db64014830450221009d8e509f6f9b22e74401f3aa06df9e212af0708e798d9b8ae9badc725a7f3d890220592d4ac99a951408f5d49a76015f9c5c8e54e34ff32bba2bfeb73ea3b4ebd75d01483045022100a960302593ecae2aba3f41bcc4cda98e2fcf54de4c479440abf002c444b98bb0022055440ae8f2b425e7b2f47847794789c769645af002e8a1084dd59e693a5e04c20147304402200d11ffb6808f6b426aff02e603abfc8eac5b3e74e3b2fb4318d47640692c7b0d0220274929b2f6e583c43358adbde3465ea1254de0abd3787db88039d00dd3d3015e014830450221009681ba08b0c826fff6499c86fd0f38216d0ea36b24d440e4aa3f5598c385370602203e9e0dae5141c1fd8598d0cb42bc44a40dda55a0577e458a22d6843e536857b401483045022100da95b59e4aac7451b5fd9efb1fa0df16ef44d8e82070daf28c90a16de50491920220637241a243cf6b7d84b3be0dfe4f08c485dfe193bd97b1290632d190f584311b01483045022100916a09eef76b47165b99e77c9a55592bcdaecd22d4932df2366a92c9304cf80502207cd0b6d85a757952c0fd56d0ee7426a7a14ecddf068707ffab3ec06af87b4a1801483045022100b4b990d471ce70de4be19aa241466b214a27b428333dcccbe885092eec4d71d302200c4e3e50aa4c2b1417ee3f174e7332fc045c304114445e05616e032aa812e9aa01483045022100af9a30d639fc333387ebf77945b4397b85f93ff6a9d8f8aeee8cca22e3383c9e02207460a2adb4264a2ecffa2eb43e59ae78e33b6f9cee44989dbec56281e5e2c1c001483045022100afc850cec037c459bbf2e8b559c863f3fa43f5ae01984d7516051a1995133917022063f04ab8d398825ad9e22a37628cf69af19dda26e6e793a3f2e797350eca6d4b0100fd810520010000000000000000000000000000000000000000000000000000000000000075645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae6800000000");
        BtcTransaction tx = new BtcTransaction(mainnet, rawTx);

        Script script = new Script(
            Hex.decode("20010000000000000000000000000000000000000000000000000000000000000075645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae68")
        );

        // act
        Coin wrongPrevOutputValue = Coin.valueOf(50000L);
        Sha256Hash sigHash = tx.hashForWitnessSignature(0, script, wrongPrevOutputValue, BtcTransaction.SigHash.ALL, false);

        // assert
        BtcECKey pubKey = BtcECKey.fromPublicOnly(
            Hex.decode("0211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c")
        );
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(
            Hex.decode("30440220375d5ddad1d329105d5bb2453fd4a57f93e8b864b11519cea4c6932d414236d3022056e9567d5e8fea093cab9d85432007add04eed9019790159f3b644c3b3e6909301"),
            true,
            true
        );
        assertFalse(pubKey.verify(sigHash, signature));
    }

    @Test
    public void hashForWitnessSignature_withWrongPubKey_shouldFail() {
        // test P2SH-P2WSH from real transaction:
        // https://mempool.space/tx/a4d76b6211b078cbc1d2079002437fcf018cc85cd40dd6195bb0f6b42930b96b

        // arrange
        NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        byte[] rawTx = Hex.decode("02000000000101f547ebc11f4cca193a8fbfdf9eaf5728d1c6770da371f5ada0655d68f23f9cca0000000023220020431ef0e7fb92b803aa735278649879a4ffe79f79eca7733a046d1d97698cac4fffffffff01905f0100000000001976a9140eda35d81e2a8537beebbbcdf63e3483be01269288ac0e004730440220375d5ddad1d329105d5bb2453fd4a57f93e8b864b11519cea4c6932d414236d3022056e9567d5e8fea093cab9d85432007add04eed9019790159f3b644c3b3e690930148304502210095201c22ed71453c89288bbb87e98425e59f90523ffbf8669cf6739cb4d98868022017ba3a6903c6aa4d770643717ed74edcddfda54f60f8825f5cd4ed12d265db64014830450221009d8e509f6f9b22e74401f3aa06df9e212af0708e798d9b8ae9badc725a7f3d890220592d4ac99a951408f5d49a76015f9c5c8e54e34ff32bba2bfeb73ea3b4ebd75d01483045022100a960302593ecae2aba3f41bcc4cda98e2fcf54de4c479440abf002c444b98bb0022055440ae8f2b425e7b2f47847794789c769645af002e8a1084dd59e693a5e04c20147304402200d11ffb6808f6b426aff02e603abfc8eac5b3e74e3b2fb4318d47640692c7b0d0220274929b2f6e583c43358adbde3465ea1254de0abd3787db88039d00dd3d3015e014830450221009681ba08b0c826fff6499c86fd0f38216d0ea36b24d440e4aa3f5598c385370602203e9e0dae5141c1fd8598d0cb42bc44a40dda55a0577e458a22d6843e536857b401483045022100da95b59e4aac7451b5fd9efb1fa0df16ef44d8e82070daf28c90a16de50491920220637241a243cf6b7d84b3be0dfe4f08c485dfe193bd97b1290632d190f584311b01483045022100916a09eef76b47165b99e77c9a55592bcdaecd22d4932df2366a92c9304cf80502207cd0b6d85a757952c0fd56d0ee7426a7a14ecddf068707ffab3ec06af87b4a1801483045022100b4b990d471ce70de4be19aa241466b214a27b428333dcccbe885092eec4d71d302200c4e3e50aa4c2b1417ee3f174e7332fc045c304114445e05616e032aa812e9aa01483045022100af9a30d639fc333387ebf77945b4397b85f93ff6a9d8f8aeee8cca22e3383c9e02207460a2adb4264a2ecffa2eb43e59ae78e33b6f9cee44989dbec56281e5e2c1c001483045022100afc850cec037c459bbf2e8b559c863f3fa43f5ae01984d7516051a1995133917022063f04ab8d398825ad9e22a37628cf69af19dda26e6e793a3f2e797350eca6d4b0100fd810520010000000000000000000000000000000000000000000000000000000000000075645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae6800000000");
        BtcTransaction tx = new BtcTransaction(mainnet, rawTx);

        Script script = new Script(
            Hex.decode("20010000000000000000000000000000000000000000000000000000000000000075645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae68")
        );

        // act
        Coin wrongPrevOutputValue = Coin.valueOf(50000L);
        Sha256Hash sigHash = tx.hashForWitnessSignature(0, script, wrongPrevOutputValue, BtcTransaction.SigHash.ALL, false);

        // assert
        BtcECKey wrongPubKey = BtcECKey.fromPublicOnly(
            Hex.decode("02d8b661b0b3302ee2f162b09e07a55ad5dfbe673a9f01d9f0c19617681024306b")
        );
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(
            Hex.decode("30440220375d5ddad1d329105d5bb2453fd4a57f93e8b864b11519cea4c6932d414236d3022056e9567d5e8fea093cab9d85432007add04eed9019790159f3b644c3b3e6909301"),
            true,
            true
        );
        assertFalse(wrongPubKey.verify(sigHash, signature));
    }
}
