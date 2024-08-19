/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_0;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_INVALIDOPCODE;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.BtcTransaction.SigHash;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.TransactionInput;
import co.rsk.bitcoinj.core.TransactionOutPoint;
import co.rsk.bitcoinj.core.TransactionOutput;
import co.rsk.bitcoinj.core.UnsafeByteArrayOutputStream;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.script.Script.VerifyFlag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class ScriptTest {
    // From tx 05e04c26c12fe408a3c1b71aa7996403f6acad1045252b1c62e055496f4d2cb1 on the testnet.
    static final String sigProg = "47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c";
    static final String pubkeyProg = "76a91433e81a941e64cda12c6a299ed322ddbdd03f8d0e88ac";

    private static final NetworkParameters mainnetParams = MainNetParams.get();
    private static final Logger log = LoggerFactory.getLogger(ScriptTest.class);
    private final List<BtcECKey> federationKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private final int expectedNumberOfRequiredSignatures = federationKeys.size() / 2 + 1;

    private final List<BtcECKey> erpFederationKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    private final BtcECKey fedKey1 = federationKeys.get(0);
    private final BtcECKey fedKey2 = federationKeys.get(1);
    private final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(900)).toAddress(
        mainnetParams);

    @Test
    public void testScriptSig() {
        byte[] sigProgBytes = Hex.decode(sigProg);
        Script script = new Script(sigProgBytes);
        // Test we can extract the from address.
        byte[] hash160 = Utils.sha256hash160(script.getPubKey());
        Address a = new Address(mainnetParams, hash160);
        assertEquals("15jTWe6r9zqxkjjLFntAWADZosAwiuw4U5", a.toString());
    }

    @Test
    public void testScriptPubKey() {
        // Check we can extract the to address
        byte[] pubkeyBytes = Hex.decode(pubkeyProg);
        Script pubkey = new Script(pubkeyBytes);
        assertEquals("DUP HASH160 PUSHDATA(20)[33e81a941e64cda12c6a299ed322ddbdd03f8d0e] EQUALVERIFY CHECKSIG", pubkey.toString());
        Address toAddr = new Address(mainnetParams, pubkey.getPubKeyHash());
        assertEquals("15jTWe6r9zqxkjjLFntAWADZosAwiuw4U5", toAddr.toString());
    }

    @Test
    public void testMultiSig() {
        List<BtcECKey> keys = Lists.newArrayList(new BtcECKey(), new BtcECKey(), new BtcECKey());
        assertTrue(ScriptBuilder.createMultiSigOutputScript(2, keys).isSentToMultiSig());
        Script script = ScriptBuilder.createMultiSigOutputScript(3, keys);
        assertTrue(script.isSentToMultiSig());
        List<BtcECKey> pubkeys = new ArrayList<>(3);
        for (BtcECKey key : keys) pubkeys.add(BtcECKey.fromPublicOnly(key.getPubKeyPoint()));
        assertEquals(script.getPubKeys(), pubkeys);
        assertFalse(ScriptBuilder.createOutputScript(new BtcECKey()).isSentToMultiSig());
        try {
            // Fail if we ask for more signatures than keys.
            Script.createMultiSigOutputScript(4, keys);
            fail();
        } catch (Throwable e) {
            // Expected.
        }
        try {
            // Must have at least one signature required.
            Script.createMultiSigOutputScript(0, keys);
        } catch (Throwable e) {
            // Expected.
        }
        // Actual execution is tested by the data driven tests.
    }

    @Test
    public void testP2SHOutputScript() {
        Address p2shAddress = Address.fromBase58(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertTrue(ScriptBuilder.createOutputScript(p2shAddress).isPayToScriptHash());
    }

    @Test
    public void testIp() {
        byte[] bytes = Hex.decode("41043e96222332ea7848323c08116dddafbfa917b8e37f0bdf63841628267148588a09a43540942d58d49717ad3fabfe14978cf4f0a8b84d2435dad16e9aa4d7f935ac");
        Script s = new Script(bytes);
        assertTrue(s.isSentToRawPubKey());
    }
    
    @Test
    public void testCreateMultiSigInputScript() {
        // Setup transaction and signatures

        Script multisigScript = ScriptBuilder.createMultiSigOutputScript(expectedNumberOfRequiredSignatures, federationKeys);

        BtcTransaction fundingTx = new BtcTransaction(mainnetParams);
        fundingTx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        fundingTx.addOutput(Coin.COIN, multisigScript);
        TransactionOutput fundingTxOutput = fundingTx.getOutput(0);

        BtcTransaction spendTx = new BtcTransaction(mainnetParams);
        spendTx.addInput(fundingTxOutput);

        Script recipientScript = ScriptBuilder.createOutputScript(userAddress);
        Coin amountToSend = fundingTxOutput.getValue();
        spendTx.addOutput(amountToSend, recipientScript);

        Sha256Hash sighash = spendTx.hashForSignature(0, multisigScript, SigHash.ALL, false);

        List<TransactionSignature> signatures = new ArrayList<>();
        for (int i = 0; i < expectedNumberOfRequiredSignatures; i++) {
            BtcECKey.ECDSASignature partySignature = federationKeys.get(i).sign(sighash);
            TransactionSignature partyTransactionSignature = new TransactionSignature(partySignature, SigHash.ALL, false);
            signatures.add(partyTransactionSignature);
        }

        // Create p2sh multisig input script
        Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, multisigScript);

        // Assert that the input script contains 4 chunks
        int expectedNumberOfChunksForScriptSig = 2 + expectedNumberOfRequiredSignatures;
        assertEquals(expectedNumberOfChunksForScriptSig, inputScript.getChunks().size());

        // Assert that the input script created contains the original multisig
        // script as the last chunk
        ScriptChunk scriptChunk = inputScript.getChunks().get(inputScript.getChunks().size() - 1);
        Assert.assertArrayEquals(scriptChunk.data, multisigScript.getProgram());

        // Create regular multisig input script
        inputScript = ScriptBuilder.createMultiSigInputScript(signatures);

        // Assert that the input script only contains 3 chunks
        int expectedNumberOfChunks = 1 + expectedNumberOfRequiredSignatures;
        assertEquals(expectedNumberOfChunks, inputScript.getChunks().size());

        // Assert that the input script created does not end with the original
        // multisig script
        scriptChunk = inputScript.getChunks().get(inputScript.getChunks().size() - 1);
        Assert.assertThat(scriptChunk.data, IsNot.not(equalTo(multisigScript.getProgram())));
    }

    @Test
    public void createAndUpdateEmptyInputScript() {
        TransactionSignature dummySig = TransactionSignature.dummy();
        BtcECKey key = new BtcECKey();

        // pay-to-pubkey
        Script inputScript = ScriptBuilder.createInputScript(dummySig);
        assertThat(inputScript.getChunks().get(0).data, equalTo(dummySig.encodeToBitcoin()));
        inputScript = ScriptBuilder.createInputScript(null);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));

        // pay-to-address
        inputScript = ScriptBuilder.createInputScript(dummySig, key);
        assertThat(inputScript.getChunks().get(0).data, equalTo(dummySig.encodeToBitcoin()));
        inputScript = ScriptBuilder.createInputScript(null, key);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data, equalTo(key.getPubKey()));

        // pay-to-script-hash
        BtcECKey key2 = new BtcECKey();
        Script multisigScript = ScriptBuilder.createMultiSigOutputScript(2, Arrays.asList(key, key2));
        inputScript = ScriptBuilder.createP2SHMultiSigInputScript(Arrays.asList(dummySig, dummySig), multisigScript);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data, equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(2).data, equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        inputScript = ScriptBuilder.createP2SHMultiSigInputScript(null, multisigScript);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(2).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, dummySig.encodeToBitcoin(), 0, 1, 1);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data, equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(2).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, dummySig.encodeToBitcoin(), 1, 1, 1);
        assertThat(inputScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(inputScript.getChunks().get(1).data, equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(2).data, equalTo(dummySig.encodeToBitcoin()));
        assertThat(inputScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        // updating scriptSig with no missing signatures
        try {
            ScriptBuilder.updateScriptWithSignature(inputScript, dummySig.encodeToBitcoin(), 1, 1, 1);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void testOp0() {
        // Check that OP_0 doesn't NPE and pushes an empty stack frame.
        BtcTransaction tx = new BtcTransaction(mainnetParams);
        tx.addInput(new TransactionInput(mainnetParams, tx, new byte[] {}));
        Script script = new ScriptBuilder().smallNum(0).build();

        LinkedList<byte[]> stack = new LinkedList<byte[]>();
        Script.executeScript(tx, 0, script, stack, Script.ALL_VERIFY_FLAGS);
        assertEquals("OP_0 push length", 0, stack.get(0).length);
    }
    
    @Test
    public void dataDrivenValidScripts() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "script_valid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            Script scriptSig = parseScriptString(test.get(0).asText());
            Script scriptPubKey = parseScriptString(test.get(1).asText());
            Set<VerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());
            try {
                scriptSig.correctlySpends(new BtcTransaction(mainnetParams), 0, scriptPubKey, verifyFlags);
            } catch (ScriptException e) {
                System.err.println(test);
                System.err.flush();
                throw e;
            }
        }
    }
    
    @Test
    public void dataDrivenInvalidScripts() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "script_invalid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            try {
                Script scriptSig = parseScriptString(test.get(0).asText());
                Script scriptPubKey = parseScriptString(test.get(1).asText());
                Set<VerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());
                scriptSig.correctlySpends(new BtcTransaction(mainnetParams), 0, scriptPubKey, verifyFlags);
                System.err.println(test);
                System.err.flush();
                fail();
            } catch (VerificationException e) {
                // Expected.
            }
        }
    }

    @Test
    public void dataDrivenValidTransactions() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "tx_valid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            if (test.isArray() && test.size() == 1 && test.get(0).isTextual())
                continue; // This is a comment.
            BtcTransaction transaction = null;
            try {
                Map<TransactionOutPoint, Script> scriptPubKeys = parseScriptPubKeys(test.get(0));
                transaction = mainnetParams.getDefaultSerializer().makeTransaction(Hex.decode(test.get(1).asText().toLowerCase()));
                transaction.verify();
                Set<VerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());

                for (int i = 0; i < transaction.getInputs().size(); i++) {
                    TransactionInput input = transaction.getInputs().get(i);
                    if (input.getOutpoint().getIndex() == 0xffffffffL)
                        input.getOutpoint().setIndex(-1);
                    assertTrue(scriptPubKeys.containsKey(input.getOutpoint()));
                    input.getScriptSig().correctlySpends(transaction, i, scriptPubKeys.get(input.getOutpoint()),
                            verifyFlags);
                }
            } catch (Exception e) {
                System.err.println(test);
                if (transaction != null)
                    System.err.println(transaction);
                throw e;
            }
        }
    }

    @Test
    public void dataDrivenInvalidTransactions() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream(
                "tx_invalid.json"), Charsets.UTF_8));
        for (JsonNode test : json) {
            if (test.isArray() && test.size() == 1 && test.get(0).isTextual())
                continue; // This is a comment.
            Map<TransactionOutPoint, Script> scriptPubKeys = parseScriptPubKeys(test.get(0));
            BtcTransaction transaction = mainnetParams.getDefaultSerializer().makeTransaction(Hex.decode(test.get(1).asText().toLowerCase()));
            Set<VerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());

            boolean valid = true;
            try {
                transaction.verify();
            } catch (VerificationException e) {
                valid = false;
            }

            // Bitcoin Core checks this case in CheckTransaction, but we leave it to
            // later where we will see an attempt to double-spend, so we explicitly check here
            HashSet<TransactionOutPoint> set = new HashSet<TransactionOutPoint>();
            for (TransactionInput input : transaction.getInputs()) {
                if (set.contains(input.getOutpoint()))
                    valid = false;
                set.add(input.getOutpoint());
            }

            for (int i = 0; i < transaction.getInputs().size() && valid; i++) {
                TransactionInput input = transaction.getInputs().get(i);
                assertTrue(scriptPubKeys.containsKey(input.getOutpoint()));
                try {
                    input.getScriptSig().correctlySpends(transaction, i, scriptPubKeys.get(input.getOutpoint()),
                            verifyFlags);
                } catch (VerificationException e) {
                    valid = false;
                }
            }

            if (valid)
                fail();
        }
    }

    @Test
    public void testCLTVPaymentChannelOutput() {
        Script script = ScriptBuilder.createCLTVPaymentChannelOutput(BigInteger.valueOf(20), new BtcECKey(), new BtcECKey());
        assertTrue("script is locktime-verify", script.isSentToCLTVPaymentChannel());
    }

    @Test
    public void getToAddress() {
        // pay to pubkey
        BtcECKey toKey = new BtcECKey();
        Address toAddress = toKey.toAddress(mainnetParams);
        assertEquals(toAddress, ScriptBuilder.createOutputScript(toKey).getToAddress(
            mainnetParams, true));
        // pay to pubkey hash
        assertEquals(toAddress, ScriptBuilder.createOutputScript(toAddress).getToAddress(
            mainnetParams, true));
        // pay to script hash
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(new byte[20]);
        Address scriptAddress = Address.fromP2SHScript(mainnetParams, p2shScript);
        assertEquals(scriptAddress, p2shScript.getToAddress(mainnetParams, true));
    }

    @Test(expected = ScriptException.class)
    public void getToAddressNoPubKey() {
        ScriptBuilder.createOutputScript(new BtcECKey()).getToAddress(mainnetParams, false);
    }

    /** Test encoding of zero, which should result in an opcode */
    @Test
    public void numberBuilderZero() {
        final ScriptBuilder builder = new ScriptBuilder();

        // 0 should encode directly to 0
        builder.number(0);
        assertArrayEquals(new byte[] {
            0x00         // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilderPositiveOpCode() {
        final ScriptBuilder builder = new ScriptBuilder();

        builder.number(5);
        assertArrayEquals(new byte[] {
            0x55         // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilderBigNum() {
        ScriptBuilder builder = new ScriptBuilder();
        // 21066 should take up three bytes including the length byte
        // at the start

        builder.number(0x524a);
        assertArrayEquals(new byte[] {
            0x02,             // Length of the pushed data
            0x4a, 0x52        // Pushed data
        }, builder.build().getProgram());

        // Test the trimming code ignores zeroes in the middle
        builder = new ScriptBuilder();
        builder.number(0x110011);
        assertEquals(4, builder.build().getProgram().length);

        // Check encoding of a value where signed/unsigned encoding differs
        // because the most significant byte is 0x80, and therefore a
        // sign byte has to be added to the end for the signed encoding.
        builder = new ScriptBuilder();
        builder.number(0x8000);
        assertArrayEquals(new byte[] {
            0x03,             // Length of the pushed data
            0x00, (byte) 0x80, 0x00  // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void numberBuilderNegative() {
        // Check encoding of a negative value
        final ScriptBuilder builder = new ScriptBuilder();
        builder.number(-5);
        assertArrayEquals(new byte[] {
            0x01,        // Length of the pushed data
            ((byte) 133) // Pushed data
        }, builder.build().getProgram());
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data, federationKeys);

        Assert.assertEquals(expectedNumberOfRequiredSignatures, fastBridgeRedeemScript.getNumberOfSignaturesRequiredToSpend());
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_erp_redeem_script() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L
        );

        Assert.assertEquals(expectedNumberOfRequiredSignatures, erpRedeemScript.getNumberOfSignaturesRequiredToSpend());
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_fast_bridge_erp_redeem_script() {
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertEquals(expectedNumberOfRequiredSignatures, fastBridgeErpRedeemScript.getNumberOfSignaturesRequiredToSpend());
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_no_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(federationKeys);
        Assert.assertEquals(expectedNumberOfRequiredSignatures, redeemScript.getNumberOfSignaturesRequiredToSpend());
    }

    @Test
    public void getSigInsertionIndex_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data, federationKeys);

        testGetSigInsertionIndex(fastBridgeRedeemScript);
    }

    @Test
    public void getSigInsertionIndex_erp_redeem_script() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L
        );

        testGetSigInsertionIndex(erpRedeemScript);
    }

    @Test
    public void getSigInsertionIndexWhenP2shRedeemScriptShouldReturnIndex() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            600L
        );

        testGetSigInsertionIndex(redeemScript);
    }

    @Test
    public void getSigInsertionIndex_fast_bridge_erp_redeem_script() {
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        testGetSigInsertionIndex(fastBridgeErpRedeemScript);
    }

    @Test
    public void getSigInsertionIndexWhenFlyoverP2shRedeemScriptShouldReturnIndex() {
        Script flyoverP2shRedeemScript = RedeemScriptUtils.createFastBridgeP2shErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        testGetSigInsertionIndex(flyoverP2shRedeemScript);
    }

    @Test
    public void getSigInsertionIndex_no_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(federationKeys);
        testGetSigInsertionIndex(redeemScript);
    }

    @Test
    public void getSigInsertionIndexWhenEmptyScriptShouldThrowScriptException() {
        Script redeemScript = new ScriptBuilder().build();
        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        int sigInsertionIndex = redeemScript.getSigInsertionIndex(hashForSignature, signingKey);
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void getSigInsertionIndexWhenP2shScriptShouldThrowScriptException() {
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(
            expectedNumberOfRequiredSignatures,
            federationKeys
        );

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        int sigInsertionIndex = p2shOutputScript.getSigInsertionIndex(hashForSignature, signingKey);
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void getSigInsertionIndexWhenTestnetHardcodeRedeemScriptThrowScriptException() {
        final byte[] ERP_TESTNET_REDEEM_SCRIPT_BYTES = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
        Script erpTestnetRedeemScript = new Script(ERP_TESTNET_REDEEM_SCRIPT_BYTES);

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        int sigInsertionIndex = erpTestnetRedeemScript.getSigInsertionIndex(hashForSignature, signingKey);
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void getSigInsertionIndexWhenMalformedRedeemScriptThrowScriptException() {
        Script customRedeemScript = new Script(new byte[2]);

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        int sigInsertionIndex = customRedeemScript.getSigInsertionIndex(hashForSignature, signingKey);
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void isSentToMultiSig_fast_bridge_multiSig() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            federationKeys
        );

        Assert.assertTrue(fastBridgeRedeemScript.isSentToMultiSig());
    }

    @Test
    public void isSentToMultiSig_erp_multiSig() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L
        );

        Assert.assertTrue(erpRedeemScript.isSentToMultiSig());
    }

    @Test
    public void isSentToMultiSig_fast_bridge_erp_multiSig() {
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(fastBridgeErpRedeemScript.isSentToMultiSig());
    }

    @Test
    public void isStandardMultiSig_standard_multiSig() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(federationKeys);
        Assert.assertTrue(redeemScript.isSentToMultiSig());
    }

    @Test
    public void createEmptyInputScript_standard_redeemScript() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(federationKeys);
        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Script inputScript = spk.createEmptyInputScript(null, redeemScript);
        int expectedChunkSize = 2 + expectedNumberOfRequiredSignatures;

        assertInputScriptStructure(
            inputScript.getChunks(),
            expectedChunkSize,
            3,
            redeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_fast_bridge_redeemScript() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            Sha256Hash.of(new byte[]{1}).getBytes(),
            federationKeys
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Script inputScript = spk.createEmptyInputScript(null, redeemScript);
        int expectedChunkSize = 2 + expectedNumberOfRequiredSignatures;

        assertInputScriptStructure(
            inputScript.getChunks(),
            expectedChunkSize,
            3,
            redeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_erp_redeemScript() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Script inputScript = spk.createEmptyInputScript(null, redeemScript);

        // The expected Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedChunkSize = 3 + expectedNumberOfRequiredSignatures;

        assertInputScriptStructure(
            inputScript.getChunks(),
            expectedChunkSize,
            4,
            redeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_fast_bridge_erp_redeemScript() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Script inputScript = spk.createEmptyInputScript(null, redeemScript);

        // The expected Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedChunkSize = 3 + expectedNumberOfRequiredSignatures;

        assertInputScriptStructure(
            inputScript.getChunks(),
            expectedChunkSize,
            4,
            redeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_p2sh_erp_redeemScript() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Script inputScript = spk.createEmptyInputScript(null, redeemScript);

        // The expected P2sh Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedChunkSize = 3 + expectedNumberOfRequiredSignatures;

        assertInputScriptStructure(
            inputScript.getChunks(),
            expectedChunkSize,
            4,
            redeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_fast_bridge_p2sh_erp_redeemScript() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeP2shErpRedeemScript(
            federationKeys,
            erpFederationKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Script inputScript = spk.createEmptyInputScript(null, redeemScript);

        // The expected P2sh Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedChunkSize = 3 + expectedNumberOfRequiredSignatures;

        assertInputScriptStructure(
            inputScript.getChunks(),
            expectedChunkSize,
            4,
            redeemScript.getProgram()
        );
    }

    private void assertInputScriptStructure(
        List<ScriptChunk> chunks,
        int expectedChunksSize,
        int expectedOpZeroes,
        byte[] redeemScriptProgram
    ) {
        // Validate input scripts chunks
        Assert.assertEquals(expectedChunksSize, chunks.size());

        for (int i = 0; i < expectedOpZeroes; i++) {
            Assert.assertTrue(chunks.get(i).equalsOpCode(OP_0));
        }

        Assert.assertArrayEquals(redeemScriptProgram, chunks.get(chunks.size() - 1).data);
    }

    private void testGetSigInsertionIndex(Script redeemScript) {
        BtcTransaction fundTx = new BtcTransaction(mainnetParams);
        fundTx.addOutput(Coin.FIFTY_COINS, userAddress);

        BtcTransaction spendTx = new BtcTransaction(mainnetParams);
        spendTx.addInput(fundTx.getOutput(0));

        Script spk = ScriptBuilder.createP2SHOutputScript(redeemScript);

        Script inputScript = spk.createEmptyInputScript(null, redeemScript);

        Sha256Hash sigHash = spendTx.hashForSignature(0, redeemScript,
            BtcTransaction.SigHash.ALL, false);

        BtcECKey.ECDSASignature sign1 = fedKey1.sign(sigHash);
        TransactionSignature txSig = new TransactionSignature(sign1,
            BtcTransaction.SigHash.ALL, false);

        byte[] txSigEncoded = txSig.encodeToBitcoin();

        int sigIndex = inputScript.getSigInsertionIndex(sigHash, fedKey1);
        Assert.assertEquals(0, sigIndex);

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, txSigEncoded,
            sigIndex, 1, 1);

        Assert.assertFalse(inputScript.getChunks().get(1).equalsOpCode(OP_0));
        Assert.assertArrayEquals(txSigEncoded, inputScript.getChunks().get(1).data);

        sigIndex = inputScript.getSigInsertionIndex(sigHash, fedKey2);
        Assert.assertEquals(1, sigIndex);
    }

    private Script parseScriptString(String string) throws IOException {
        String[] words = string.split("[ \\t\\n]");

        UnsafeByteArrayOutputStream out = new UnsafeByteArrayOutputStream();

        for(String w : words) {
            if (w.equals(""))
                continue;
            if (w.matches("^-?[0-9]*$")) {
                // Number
                long val = Long.parseLong(w);
                if (val >= -1 && val <= 16)
                    out.write(Script.encodeToOpN((int)val));
                else
                    Script.writeBytes(out, Utils.reverseBytes(Utils.encodeMPI(BigInteger.valueOf(val), false)));
            } else if (w.matches("^0x[0-9a-fA-F]*$")) {
                // Raw hex data, inserted NOT pushed onto stack:
                out.write(Hex.decode(w.substring(2).toLowerCase()));
            } else if (w.length() >= 2 && w.startsWith("'") && w.endsWith("'")) {
                // Single-quoted string, pushed as data. NOTE: this is poor-man's
                // parsing, spaces/tabs/newlines in single-quoted strings won't work.
                Script.writeBytes(out, w.substring(1, w.length() - 1).getBytes(
                    StandardCharsets.UTF_8));
            } else if (ScriptOpCodes.getOpCode(w) != OP_INVALIDOPCODE) {
                // opcode, e.g. OP_ADD or OP_1:
                out.write(ScriptOpCodes.getOpCode(w));
            } else if (w.startsWith("OP_") && ScriptOpCodes.getOpCode(w.substring(3)) != OP_INVALIDOPCODE) {
                // opcode, e.g. OP_ADD or OP_1:
                out.write(ScriptOpCodes.getOpCode(w.substring(3)));
            } else {
                throw new RuntimeException("Invalid Data");
            }
        }

        return new Script(out.toByteArray());
    }

    private Set<VerifyFlag> parseVerifyFlags(String str) {
        Set<VerifyFlag> flags = EnumSet.noneOf(VerifyFlag.class);
        if (!"NONE".equals(str)) {
            for (String flag : str.split(",")) {
                try {
                    flags.add(VerifyFlag.valueOf(flag));
                } catch (IllegalArgumentException x) {
                    log.debug("Cannot handle verify flag {} -- ignored.", flag);
                }
            }
        }
        return flags;
    }

    private Map<TransactionOutPoint, Script> parseScriptPubKeys(JsonNode inputs) throws IOException {
        Map<TransactionOutPoint, Script> scriptPubKeys = new HashMap<TransactionOutPoint, Script>();
        for (JsonNode input : inputs) {
            String hash = input.get(0).asText();
            int index = input.get(1).asInt();
            String script = input.get(2).asText();
            Sha256Hash sha256Hash = Sha256Hash.wrap(Hex.decode(hash));
            scriptPubKeys.put(new TransactionOutPoint(mainnetParams, index, sha256Hash), parseScriptString(script));
        }
        return scriptPubKeys;
    }
}
