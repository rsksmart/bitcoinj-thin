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

import static co.rsk.bitcoinj.script.RedeemScriptUtils.createNonStandardErpRedeemScript;
import static co.rsk.bitcoinj.script.RedeemScriptUtils.createP2shErpRedeemScript;
import static co.rsk.bitcoinj.script.ScriptOpCodes.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.BtcTransaction.SigHash;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.MessageSerializer;
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
import co.rsk.bitcoinj.script.Script.ScriptType;
import co.rsk.bitcoinj.script.Script.VerifyFlag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class ScriptTest {

    private static final byte[] NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");

    private static final byte[] FLYOVER_DERIVATION_HASH = Sha256Hash.of(new byte[]{1}).getBytes();
    private static final long CSV_VALUE = 52_560L;
    private static final NetworkParameters MAINNET_PARAMS = MainNetParams.get();

    private static final List<BtcECKey> FEDERATION_KEYS = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private static final List<BtcECKey> ERP_FEDERATION_KEYS = RedeemScriptUtils.getEmergencyRedeemScriptKeys();

    private static final int REQUIRED_SIGNATURES = FEDERATION_KEYS.size() / 2 + 1;

    private static final int STANDARD_MULTISIG_SCRIPT_SIG_CHUNKS = 1 + REQUIRED_SIGNATURES + 1; // One for OP_0 at the beginning and one for the redeem script at the end
    private static final int P2SH_ERP_MULTISIG_SCRIPT_SIG_CHUNKS = 1 + REQUIRED_SIGNATURES + 2; // One for OP_0 at the beginning plus one for for the flow op code and one for the redeem script at the end

    @Test
    public void testScriptSig() {
        // Arrange
        // From tx 144c51489015966bd7dccf62d3f289b5fc326d7fb8b4766287209130601d6f94 on mainnet.
        final String expectedAddress = "13jVTrDQrmob6Bc3MAcVhiJ15V22aypzB9";
        final String scriptSig = "473044022056eb4d70a5e060b2fff471833a27369d7f58f37272db069ff343aa8cea49d5ec0220018655699f56628e985ffa96777edde790ee830c8d2575ccbc720292e507f0650121025256789f7facfb367ae3afb232e9e50bb4d887ba06bdaa8c406a011fd5a3c89d";

        byte[] scriptSigInBytes = Hex.decode(scriptSig);
        Script p2pkh = new Script(scriptSigInBytes);

        // Test we can extract the from address.
        // Act
        byte[] pubKeyInBytes = p2pkh.getPubKey();

        // Assert
        byte[] deriveAddressInBytes = Utils.sha256hash160(pubKeyInBytes);
        Address addressFromP2pkhScriptSig = new Address(MAINNET_PARAMS, deriveAddressInBytes);
        assertEquals(expectedAddress, addressFromP2pkhScriptSig.toString());
    }

    @Test
    public void getPubKey_whenScriptSig_shouldThrowException(){
        // arrange
        // From tx f464e7c6200af3afaaac0b38ea38adcd7521df0d522dad95f991bab48439cf3e on mainnet.
        final String scriptSig = "00473044022033fb52c62b3354e9b6af80506141f38443994678fdc83e5638069a9738d8e5060220351f4f385f4cb2700e4befe0e7e2782f573291edd9e6485e4f46abcada39df8501483045022100f69ef21fc9baadb1e15aeb69376b2b3595aab64894a89fcb6bbc42947c3ad2910220181577ada1b37822645faae8d935d9d4ad49338dbcbf59a218bc2a82d5c2559201483045022100caecafd376515ead5f84ff933a118e0bf7360f3f28cc977347cdb46fa8adefcc02202736a5ae2c756e0cb968082853f06e3353265d609ebd196eea89914c2423ced201483045022100bfae4595d53450fe569611f51cb56de78dee248c6813235a246796ec6ef72f1e02200b2afef75892d0fcaba2eaabc4889cca09ad456aae88d0d05c2f4259f43903d001483045022100d96d0707f783866e9ecc013f2c1a648af5ca5e1a3b600f593f40c2d7a68175c1022066095b9d688281314f1c4271b664cd9ff1939f8f48cd046f61a0cb0cdff2346301004dc901645521020ace50bab1230f8002a0bfe619482af74b338cc9e4c956add228df47e6adae1c21025093f439fb8006fd29ab56605ffec9cdc840d16d2361004e1337a2f86d8bd2db210275d473555de2733c47125f9702b0f870df1d817379f5587f09b6c40ed2c6c9492102a95f095d0ce8cb3b9bf70cc837e3ebe1d107959b1fa3f9b2d8f33446f9c8cbdb2103250c11be0561b1d7ae168b1f59e39cbc1fd1ba3cf4d2140c1a365b2723a2bf9321034851379ec6b8a701bd3eef8a0e2b119abb4bdde7532a3d6bcbff291b0daf3f25210350179f143a632ce4e6ac9a755b82f7f4266cfebb116a42cadb104c2c2a3350f92103b04fbd87ef5e2c0946a684c8c93950301a45943bbe56d979602038698facf9032103b58a5da144f5abab2e03e414ad044b732300de52fa25c672a7f7b3588877190659ae670350cd00b275532102370a9838e4d15708ad14a104ee5606b36caaaaf739d833e67770ce9fd9b3ec80210257c293086c4d4fe8943deda5f890a37d11bebd140e220faa76258a41d077b4d42103c2660a46aa73078ee6016dee953488566426cf55fc8011edd0085634d75395f92103cd3e383ec6e12719a6c69515e5559bcbe037d0aa24c187e1e26ce932e22ad7b354ae68";

        byte[] scriptSigInBytes = Hex.decode(scriptSig);
        Script script = new Script(scriptSigInBytes);

        // act
        assertThrows(ScriptException.class, script::getPubKey);
    }

    @Test
    public void testScriptPubKey() {
        // From tx f464e7c6200af3afaaac0b38ea38adcd7521df0d522dad95f991bab48439cf3e on mainnet.
        final String pubKeyInHex = "76a914742e7c6115d8ffa7a241ac3fff1d7fd9e149bd6588ac";
        final String expectedAddressFromScriptSig = "1BbK59V1D4V5xgHjfenSVbYoWXkf67eDND";
        byte[] pubKeyBytes = Hex.decode(pubKeyInHex);

        Script pubKey = new Script(pubKeyBytes);

        String expectedPubKey = "DUP HASH160 PUSHDATA(20)[742e7c6115d8ffa7a241ac3fff1d7fd9e149bd65] EQUALVERIFY CHECKSIG";
        assertEquals(expectedPubKey, pubKey.toString());

        Address actualAddress = new Address(MAINNET_PARAMS, pubKey.getPubKeyHash());
        assertEquals(expectedAddressFromScriptSig, actualAddress.toString());
    }

    @Test
    public void testMultiSig() {
        List<BtcECKey> signers = Lists.newArrayList(new BtcECKey(), new BtcECKey(), new BtcECKey());
        assertTrue(ScriptBuilder.createMultiSigOutputScript(2, signers).isSentToMultiSig());
        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(3, signers);
        assertTrue(multiSigScript.isSentToMultiSig());
        List<BtcECKey> pubKeys = new ArrayList<>(3);
        for (BtcECKey signer : signers) {
            BtcECKey pubKey = BtcECKey.fromPublicOnly(signer.getPubKeyPoint());
            pubKeys.add(pubKey);
        }
        assertEquals(multiSigScript.getPubKeys(), pubKeys);

        Script signerScript = ScriptBuilder.createOutputScript(signers.get(0));
        assertFalse(signerScript.isSentToMultiSig());

        // Fail if we ask for more signatures than signers.
        assertThrows(IllegalArgumentException.class, () -> Script.createMultiSigOutputScript(4, signers));

        // Fail if we ask for no signatures
        assertThrows(IllegalArgumentException.class, () -> Script.createMultiSigOutputScript(0, signers));

        // Fail if we ask for negative number of signatures
        assertThrows(IllegalArgumentException.class, () -> Script.createMultiSigOutputScript(-1, signers));
    }

    @Test
    public void testP2SHOutputScript() {
        final String addressFromP2shScript = "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU";
        Address p2shAddress = Address.fromBase58(MainNetParams.get(), addressFromP2shScript);
        assertTrue(ScriptBuilder.createOutputScript(p2shAddress).isPayToScriptHash());
    }

    @Test
    public void testIp() {
        final String pubKeyScriptInHex = "41043e96222332ea7848323c08116dddafbfa917b8e37f0bdf63841628267148588a09a43540942d58d49717ad3fabfe14978cf4f0a8b84d2435dad16e9aa4d7f935ac";
        byte[] pubKeyScriptInBytes = Hex.decode(pubKeyScriptInHex);
        Script pubKeyScript = new Script(pubKeyScriptInBytes);
        assertTrue(pubKeyScript.isSentToRawPubKey());
    }
    
    @Test
    public void testCreateMultiSigInputScript() {
        // Setup transaction and signatures
        Script multisigScript = ScriptBuilder.createMultiSigOutputScript(
            REQUIRED_SIGNATURES,
            FEDERATION_KEYS);

        BtcTransaction fundingTx = new BtcTransaction(MAINNET_PARAMS);
        fundingTx.addInput(Sha256Hash.ZERO_HASH, 0, new Script(new byte[]{}));
        fundingTx.addOutput(Coin.COIN, multisigScript);
        TransactionOutput fundingTxOutput = fundingTx.getOutput(0);

        BtcTransaction spendTx = new BtcTransaction(MAINNET_PARAMS);
        spendTx.addInput(fundingTxOutput);

        final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(900)).toAddress(
            MAINNET_PARAMS);
        Script recipientScript = ScriptBuilder.createOutputScript(userAddress);
        Coin amountToSend = fundingTxOutput.getValue();
        spendTx.addOutput(amountToSend, recipientScript);

        Sha256Hash sigHash = spendTx.hashForSignature(0, multisigScript, SigHash.ALL, false);

        List<TransactionSignature> signatures = new ArrayList<>();
        for (int i = 0; i < REQUIRED_SIGNATURES; i++) {
            BtcECKey.ECDSASignature ecdsaSignature = FEDERATION_KEYS.get(i).sign(sigHash);
            TransactionSignature txSignature = new TransactionSignature(ecdsaSignature, SigHash.ALL, false);
            signatures.add(txSignature);
        }

        // Create p2sh multiSig input script
        Script p2shScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, multisigScript);

        // Assert that the input script contains standard multiSig op codes + expected number of required signatures

        assertEquals(STANDARD_MULTISIG_SCRIPT_SIG_CHUNKS, p2shScript.getChunks().size());

        // Assert that the input script created contains the original multiSig
        // script as the last chunk
        ScriptChunk redeemScriptChunk = p2shScript.getChunks().get(p2shScript.getChunks().size() - 1);
        Assert.assertArrayEquals(redeemScriptChunk.data, multisigScript.getProgram());

        // Create regular multiSig input script
        Script multiSigInputScript = ScriptBuilder.createMultiSigInputScript(signatures);

        // Assert that the input script only contains the OP_CODE zero and the required number of signatures.
        final int expectedOpZeroNumberOfChunks = 1;
        int expectedNumberOfChunks = expectedOpZeroNumberOfChunks + REQUIRED_SIGNATURES;
        assertEquals(expectedNumberOfChunks, multiSigInputScript.getChunks().size());

        // Assert that the input script created does not end with the original
        // multiSig script
        redeemScriptChunk = multiSigInputScript.getChunks().get(multiSigInputScript.getChunks().size() - 1);
        Assert.assertThat(redeemScriptChunk.data, IsNot.not(equalTo(multisigScript.getProgram())));
    }

    @Test
    public void createAndUpdateEmptyInputScript() {
        TransactionSignature transactionSignature = TransactionSignature.dummy();
        BtcECKey signer = new BtcECKey();

        // pay-to-pubKey
        Script p2pkScript = ScriptBuilder.createInputScript(transactionSignature);
        assertThat(p2pkScript.getChunks().get(0).data, equalTo(transactionSignature.encodeToBitcoin()));
        p2pkScript = ScriptBuilder.createInputScript(null);
        assertThat(p2pkScript.getChunks().get(0).opcode, equalTo(OP_0));

        // pay-to-address
        p2pkScript = ScriptBuilder.createInputScript(transactionSignature, signer);
        assertThat(p2pkScript.getChunks().get(0).data, equalTo(transactionSignature.encodeToBitcoin()));
        p2pkScript = ScriptBuilder.createInputScript(null, signer);
        assertThat(p2pkScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(1).data, equalTo(signer.getPubKey()));

        // pay-to-script-hash
        BtcECKey signer2 = new BtcECKey();
        Script multisigScript = ScriptBuilder.createMultiSigOutputScript(2, Arrays.asList(signer, signer2));
        p2pkScript = ScriptBuilder.createP2SHMultiSigInputScript(Arrays.asList(transactionSignature, transactionSignature), multisigScript);
        assertThat(p2pkScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(1).data, equalTo(transactionSignature.encodeToBitcoin()));
        assertThat(p2pkScript.getChunks().get(2).data, equalTo(transactionSignature.encodeToBitcoin()));
        assertThat(p2pkScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        p2pkScript = ScriptBuilder.createP2SHMultiSigInputScript(null, multisigScript);
        assertThat(p2pkScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(1).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(2).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        p2pkScript = ScriptBuilder.updateScriptWithSignature(p2pkScript, transactionSignature.encodeToBitcoin(), 0, 1, 1);
        assertThat(p2pkScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(1).data, equalTo(transactionSignature.encodeToBitcoin()));
        assertThat(p2pkScript.getChunks().get(2).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        p2pkScript = ScriptBuilder.updateScriptWithSignature(p2pkScript, transactionSignature.encodeToBitcoin(), 1, 1, 1);
        assertThat(p2pkScript.getChunks().get(0).opcode, equalTo(OP_0));
        assertThat(p2pkScript.getChunks().get(1).data, equalTo(transactionSignature.encodeToBitcoin()));
        assertThat(p2pkScript.getChunks().get(2).data, equalTo(transactionSignature.encodeToBitcoin()));
        assertThat(p2pkScript.getChunks().get(3).data, equalTo(multisigScript.getProgram()));

        // updating scriptSig with no missing signatures
        final Script finalP2pkScript = p2pkScript;
        assertThrows(IllegalArgumentException.class, () -> ScriptBuilder.updateScriptWithSignature(
            finalP2pkScript, transactionSignature.encodeToBitcoin(), 1, 1, 1));
    }

    @Test
    public void testOp0() {
        // Check that OP_0 doesn't NPE and pushes an empty stack frame.
        BtcTransaction btcTransaction = new BtcTransaction(MAINNET_PARAMS);
        TransactionInput transactionInput = new TransactionInput(MAINNET_PARAMS, btcTransaction, new byte[]{});
        btcTransaction.addInput(transactionInput);
        Script script = new ScriptBuilder().smallNum(0).build();

        LinkedList<byte[]> stack = new LinkedList<>();
        Script.executeScript(btcTransaction, 0, script, stack, Script.ALL_VERIFY_FLAGS);
        assertEquals("OP_0 push length", 0, stack.get(0).length);
    }
    
    @Test
    public void dataDrivenValidScripts() throws Exception {
        InputStreamReader scriptValidJsonInStream = new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream(
                "script_valid.json")), Charsets.UTF_8);
        JsonNode validScripsInJsonArray = new ObjectMapper().readTree(scriptValidJsonInStream);
        for (JsonNode validScriptInJson : validScripsInJsonArray) {
            try {
                Script scriptSig = parseScriptString(validScriptInJson.get(0).asText());
                Script scriptPubKey = parseScriptString(validScriptInJson.get(1).asText());
                Set<VerifyFlag> verifyFlags = parseVerifyFlags(validScriptInJson.get(2).asText());
                scriptSig.correctlySpends(new BtcTransaction(MAINNET_PARAMS), 0, scriptPubKey, verifyFlags);
            } catch (Throwable actualException){
                fail();
            }
        }
    }
    
    @Test
    public void dataDrivenInvalidScripts() throws Exception {
        InputStream invalidScriptsInBytes = Objects.requireNonNull(
            getClass().getResourceAsStream("script_invalid.json"));
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(invalidScriptsInBytes
            , Charsets.UTF_8));
        for (JsonNode test : json) {
            assertThrows(ScriptException.class, () -> {
                Script scriptSig = parseScriptString(test.get(0).asText());
                Script scriptPubKey = parseScriptString(test.get(1).asText());
                Set<VerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());
                scriptSig.correctlySpends(new BtcTransaction(MAINNET_PARAMS), 0, scriptPubKey, verifyFlags);
            });
        }
    }

    @Test
    public void dataDrivenValidTransactions() throws Exception {
        MessageSerializer defaultSerializer = MAINNET_PARAMS.getDefaultSerializer();
        InputStreamReader validTransactionsJsonArrayInBytes = new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream("tx_valid.json")), Charsets.UTF_8);
        JsonNode validTxsInJsonArray = new ObjectMapper().readTree(validTransactionsJsonArrayInBytes);
        for (JsonNode validTxInJson : validTxsInJsonArray) {
            if (validTxInJson.isArray() && validTxInJson.size() == 1 && validTxInJson.get(0).isTextual()) {
                continue; // This is a comment.
            }

            Map<TransactionOutPoint, Script> scriptPubKeys = parseScriptPubKeys(validTxInJson.get(0));
            BtcTransaction transaction = defaultSerializer.makeTransaction(
                Hex.decode(validTxInJson.get(1).asText().toLowerCase()));
            transaction.verify();
            Set<VerifyFlag> verifyFlags = parseVerifyFlags(validTxInJson.get(2).asText());

            for (int i = 0; i < transaction.getInputs().size(); i++) {
                TransactionInput input = transaction.getInputs().get(i);
                if (input.getOutpoint().getIndex() == 0xffffffffL) {
                    input.getOutpoint().setIndex(-1);
                }
                input.getScriptSig()
                    .correctlySpends(transaction, i, scriptPubKeys.get(input.getOutpoint()),
                        verifyFlags);
                assertTrue(scriptPubKeys.containsKey(input.getOutpoint()));
            }
        }
    }

    @Test
    public void dataDrivenInvalidTransactions() throws Exception {
        InputStreamReader invalidTxsJsonInStream = new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream(
                "tx_invalid.json")), Charsets.UTF_8);
        JsonNode invalidTxsInJsonArray = new ObjectMapper().readTree(invalidTxsJsonInStream);
        for (JsonNode test : invalidTxsInJsonArray) {
            if (test.isArray() && test.size() == 1 && test.get(0).isTextual())
                continue; // This is a comment.
            Map<TransactionOutPoint, Script> scriptPubKeys = parseScriptPubKeys(test.get(0));
            BtcTransaction transaction = MAINNET_PARAMS.getDefaultSerializer().makeTransaction(Hex.decode(test.get(1).asText().toLowerCase()));
            Set<VerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());

            boolean valid = true;
            try {
                transaction.verify();
            } catch (VerificationException e) {
                valid = false;
            }

            // Bitcoin Core checks this case in CheckTransaction, but we leave it to
            // later where we will see an attempt to double-spend, so we explicitly check here
            HashSet<TransactionOutPoint> set = new HashSet<>();
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
        // pay to pubKey
        BtcECKey signer = new BtcECKey();
        Address expectedAddress = signer.toAddress(MAINNET_PARAMS);
        Address actualAddress = ScriptBuilder.createOutputScript(signer).getToAddress(
            MAINNET_PARAMS, true);
        assertEquals(expectedAddress, actualAddress);

        // pay to pubKey hash
        assertEquals(expectedAddress, ScriptBuilder.createOutputScript(expectedAddress).getToAddress(
            MAINNET_PARAMS, true));
        // pay to script hash
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(new byte[20]);
        Address scriptAddress = Address.fromP2SHScript(MAINNET_PARAMS, p2shScript);
        assertEquals(scriptAddress, p2shScript.getToAddress(MAINNET_PARAMS, true));
    }

    @Test
    public void createP2shP2wshOutputScript_createsExpectedOutputScript() {
        // data from tx https://mempool.space/testnet/tx/1744459aeaf7369aadc9fc40de9ab2bf575b14e35029b35a7ee4bbd3de65af7f
        Script redeemScript = new Script(
            Hex.decode("5221027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d35610072102d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856210346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e53ae")
        );

        // act
        Script outputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);

        // assert
        // output script structure is hash160, hashed redeem script, equal opcode
        int expectedOutputScriptSize = 3;
        assertEquals(expectedOutputScriptSize, outputScript.getChunks().size());

        int hash160opcode = outputScript.getChunks().get(0).opcode;
        assertEquals(OP_HASH160, hash160opcode);

        int equalOpcode = outputScript.getChunks().get(2).opcode;
        assertEquals(OP_EQUAL, equalOpcode);

        byte[] redeemScriptData = outputScript.getChunks().get(1).data;
        assertNotNull(redeemScriptData);

        // check expected p2sh-p2wsh redeem script hash matches with our redeem script data
        byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
        Script p2shP2wshRedeemScript = new ScriptBuilder()
            .number(ScriptOpCodes.OP_0)
            .data(redeemScriptHash)
            .build();
        byte[] p2shP2wshRedeemScriptHash = Utils.sha256hash160(p2shP2wshRedeemScript.getProgram());
        assertArrayEquals(p2shP2wshRedeemScriptHash, redeemScriptData);

        // check redeem script hash from tx matches with our redeem script data
        String redeemScriptHashFromTx = "d220e5b2484931d0fad089dedd87e17022683a51";
        assertEquals(redeemScriptHashFromTx, Hex.toHexString(redeemScriptData));
    }

    @Test(expected = ScriptException.class)
    public void getToAddressNoPubKey() {
        ScriptBuilder.createOutputScript(new BtcECKey()).getToAddress(MAINNET_PARAMS, false);
    }

    /** Test encoding of zero, which should result in an opcode */
    @Test
    public void numberBuilderZero() {
        final ScriptBuilder builder = new ScriptBuilder();

        // 0 should encode directly to 0
        builder.number(0);
        byte[] actualScriptBytes = builder.build().getProgram();

        byte[] expectedScriptBytes = {
            0x00         // Pushed data
        };
        assertArrayEquals(expectedScriptBytes, actualScriptBytes);
    }

    @Test
    public void numberBuilderPositiveOpCode() {
        final ScriptBuilder builder = new ScriptBuilder();

        builder.number(5);
        byte[] actualScriptBytes = builder.build().getProgram();

        byte[] expectedScriptBytes = {
            0x55         // Pushed data
        };
        assertArrayEquals(expectedScriptBytes, actualScriptBytes);
        builder.number(5);
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
        byte[] actualScriptBytes = builder.build().getProgram();

        byte[] expectedScriptBytes = {
            0x03,             // Length of the pushed data
            0x00, (byte) 0x80, 0x00  // Pushed data
        };
        assertArrayEquals(expectedScriptBytes, actualScriptBytes);
    }

    @Test
    public void numberBuilderNegative() {
        // Check encoding of a negative value
        final ScriptBuilder builder = new ScriptBuilder();
        builder.number(-5);
        byte[] actualScriptBytes = builder.build().getProgram();

        byte[] expectedScriptBytes = {
            0x01,        // Length of the pushed data
            ((byte) 133) // Pushed data
        };
        assertArrayEquals(expectedScriptBytes, actualScriptBytes);
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_whenFlyoverRedeemScript_shouldReturnNumberOfRequiredSignatures() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(
            FEDERATION_KEYS);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH, redeemScript);
        int actualNumberOfSignaturesRequiredToSpend = flyoverRedeemScript.getNumberOfSignaturesRequiredToSpend();

        Assert.assertEquals(REQUIRED_SIGNATURES, actualNumberOfSignaturesRequiredToSpend);
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_whenNonStandardErpRedeemScript_shouldReturnNumberOfRequiredSignatures() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            500L
        );
        int actualNumberOfSignaturesRequiredToSpend = nonStandardErpRedeemScript.getNumberOfSignaturesRequiredToSpend();

        Assert.assertEquals(REQUIRED_SIGNATURES, actualNumberOfSignaturesRequiredToSpend);
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_whenFlyoverNonStandardRedeemScript_shouldReturnNumberOfRequiredSignatures() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );
        int actualNumberOfSignaturesRequiredToSpend = flyoverRedeemScript.getNumberOfSignaturesRequiredToSpend();

        Assert.assertEquals(REQUIRED_SIGNATURES, actualNumberOfSignaturesRequiredToSpend);
    }

    @Test
    public void getNumberOfSignaturesRequiredToSpend_whenStandardRedeemScript_shouldReturnNumberOfRequiredSignatures() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        int actualNumberOfSignaturesRequiredToSpend = redeemScript.getNumberOfSignaturesRequiredToSpend();

        Assert.assertEquals(REQUIRED_SIGNATURES, actualNumberOfSignaturesRequiredToSpend);
    }

    @Test
    public void getSigInsertionIndex_whenScriptSigWithFlyoverRedeemScript_shouldReturnIndex() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH, redeemScript);

        testGetSigInsertionIndex(flyoverRedeemScript);
    }

    @Test
    public void getSigInsertionIndex_whenScriptSigWithNonStandardErpRedeemScript_shouldReturnIndex() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        testGetSigInsertionIndex(nonStandardErpRedeemScript);
    }

    @Test
    public void getSigInsertionIndex_whenScriptSigWithP2shRedeemScript_shouldReturnIndex() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        testGetSigInsertionIndex(redeemScript);
    }

    @Test
    public void getSigInsertionIndex_whenScriptSigFlyoverNonStandardErpRedeemScript_shouldReturnIndex() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );


        testGetSigInsertionIndex(flyoverRedeemScript);
    }

    @Test
    public void getSigInsertionIndex_whenFlyoverP2shRedeemScript_shouldReturnIndex() {
        Script p2shRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            p2shRedeemScript
        );

        testGetSigInsertionIndex(flyoverRedeemScript);
    }

    @Test
    public void getSigInsertionIndex_whenStandardRedeemScript_shouldReturnSigInsertionIndex() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        testGetSigInsertionIndex(redeemScript);
    }

    @Test
    public void getSigInsertionIndex_whenEmptyScript_shouldThrownArrayIndexOutOfBoundsException() {
        Script redeemScript = new ScriptBuilder().build();
        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> redeemScript.getSigInsertionIndex(hashForSignature, signingKey));
    }

    @Test
    public void getSigInsertionIndex_whenP2shScript_shouldThrowNullPointerException() {
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(
            REQUIRED_SIGNATURES,
            FEDERATION_KEYS
        );

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        assertThrows(NullPointerException.class, () -> p2shOutputScript.getSigInsertionIndex(hashForSignature, signingKey));
    }

    @Test
    public void getSigInsertionIndex_whenTestnetHardcodeRedeemScript_shouldThrowNullPointerException() {
        final byte[] erpTestnetRedeemScriptBytes = Hex.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
        Script erpTestnetRedeemScript = new Script(erpTestnetRedeemScriptBytes);

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        assertThrows(NullPointerException.class, () -> erpTestnetRedeemScript.getSigInsertionIndex(hashForSignature, signingKey));
    }

    @Test
    public void getSigInsertionIndex_whenScriptSigWithTestnetHardcodeRedeemScript_shouldReturnIndex() {
        final byte[] erpTestnetRedeemScriptBytes = Hex.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
        Script erpTestnetRedeemScript = new Script(erpTestnetRedeemScriptBytes);

        ScriptBuilder erpTestnetScriptSigBuilder = new ScriptBuilder();
        erpTestnetScriptSigBuilder.number(OP_0);
        erpTestnetScriptSigBuilder.data(new byte[]{}).data(TransactionSignature.dummy().encodeToBitcoin());
        erpTestnetScriptSigBuilder.data(new byte[]{}).data(TransactionSignature.dummy().encodeToBitcoin());
        erpTestnetScriptSigBuilder.data(new byte[]{}).data(TransactionSignature.dummy().encodeToBitcoin());
        erpTestnetScriptSigBuilder.number(OP_0);
        erpTestnetScriptSigBuilder.data(erpTestnetRedeemScript.getProgram());

        Script erpTestnetScriptSig = erpTestnetScriptSigBuilder.build();

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        int actualSigInsertionIndex = erpTestnetScriptSig.getSigInsertionIndex(hashForSignature, signingKey);
        Assert.assertEquals(0, actualSigInsertionIndex);
    }

    @Test(expected = ScriptException.class)
    public void getSigInsertionIndex_whenMalformedRedeemScript_shouldThrowException() {
        Script customRedeemScript = new Script(new byte[2]);

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = BtcECKey.fromPrivate(BigInteger.valueOf(800));

        customRedeemScript.getSigInsertionIndex(hashForSignature, signingKey);
    }

    @Test
    public void getSigInsertionIndex_whenScriptWithOnlyOP0AndRedeemScript_shouldReturnZero() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(FEDERATION_KEYS, ERP_FEDERATION_KEYS, CSV_VALUE);
        Script customRedeemScript = new ScriptBuilder().number(OP_0)
            .data(nonStandardErpRedeemScript.getProgram()).build();

        assertEquals(2, customRedeemScript.getChunks().size());
        ScriptType actualScriptType = customRedeemScript.getScriptType();
        assertEquals(ScriptType.NO_TYPE, actualScriptType);

        Sha256Hash hashForSignature = Sha256Hash.of(new byte[]{1});
        BtcECKey signingKey = FEDERATION_KEYS.get(1);

        int sigInsertionIndex = customRedeemScript.getSigInsertionIndex(hashForSignature, signingKey);
        Assert.assertEquals(0, sigInsertionIndex);
    }

    @Test
    public void isSentToMultiSig_whenNonStandardErpRedeemScriptHardcoded_shouldReturnFalse() {
        Script nonStandardErpTestnetRedeemScript = new Script(NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);
        assertScriptIsNotMultiSig(nonStandardErpTestnetRedeemScript);
    }

    private static void assertScriptIsNotMultiSig(Script script) {
        Assert.assertFalse(script.isSentToMultiSig());
    }

    @Test
    public void isSentToMultiSig_whenFlyoverNonStandardErpRedeemScriptHardcoded_shouldReturnFalse() {
        Script nonStandardErpTestnetRedeemScript = new Script(NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH, nonStandardErpTestnetRedeemScript);
        assertScriptIsNotMultiSig(flyoverRedeemScript);
    }

    @Test
    public void isSentToMultiSig_whenEmptyScript_shouldReturnFalse() {
        Script emptyScript = new ScriptBuilder().build();
        assertScriptIsNotMultiSig(emptyScript);
    }

    @Test
    public void isSentToMultiSig_whenInvalidScript_shouldReturnFalse() {
        Script invalidScript = new Script(new byte[5]);
        assertScriptIsNotMultiSig(invalidScript);
    }

    @Test
    public void isSentToMultiSig_whenScriptSig_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(
            redeemScript.getNumberOfSignaturesRequiredToSpend(),
            FEDERATION_KEYS
        );

        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, redeemScript);

        assertScriptIsNotMultiSig(scriptSig);
    }

    @Test
    public void isSentToMultiSig_whenP2shOutputScript_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(redeemScript);

        assertScriptIsNotMultiSig(p2SHOutputScript);
    }

    @Test
    public void isSentToMultiSig_whenStandardRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        assertScriptIsMultiSig(redeemScript);
    }

    private static void assertScriptIsMultiSig(Script script) {
        Assert.assertTrue(script.isSentToMultiSig());
    }

    @Test
    public void isSentToMultiSig_whenNonStandardErpRedeemScript_shouldReturnTrue() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        assertScriptIsMultiSig(nonStandardErpRedeemScript);
    }

    @Test
    public void getSigsPrefixCount_whenP2PKOutputScript_shouldReturnZero() {
        BtcECKey signer = FEDERATION_KEYS.get(0);
        Script p2pkScript = ScriptBuilder.createOutputScript(signer);
        assertEquals(0, p2pkScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenP2PKOutputScript_shouldReturnZero() {
        BtcECKey signer = FEDERATION_KEYS.get(0);
        Script p2pkScript = ScriptBuilder.createOutputScript(signer);
        assertEquals(0, p2pkScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenMultiSigOutputScript_shouldReturnOne() {
        BtcECKey signer = FEDERATION_KEYS.get(0);
        BtcECKey signer2 = FEDERATION_KEYS.get(1);
        Script multisigOutputScript = ScriptBuilder.createMultiSigOutputScript(2, Arrays.asList(signer, signer2));
        assertEquals(1, multisigOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenMultiSigOutputScript_shouldReturnZero() {
        BtcECKey signer = FEDERATION_KEYS.get(0);
        BtcECKey signer2 = FEDERATION_KEYS.get(1);
        Script multisigOutputScript = ScriptBuilder.createMultiSigOutputScript(2, Arrays.asList(signer, signer2));
        assertEquals(0, multisigOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenP2shMultisigOutputScript_shouldReturnOne() {
        BtcECKey signer = FEDERATION_KEYS.get(0);
        BtcECKey signer2 = FEDERATION_KEYS.get(1);
        Script p2shMultisigOutputScript = ScriptBuilder.createP2SHOutputScript(2, Arrays.asList(signer, signer2));
        assertEquals(1, p2shMultisigOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenP2shMultisigOutputScript_shouldReturnOne() {
        BtcECKey signer = FEDERATION_KEYS.get(0);
        BtcECKey signer2 = FEDERATION_KEYS.get(1);
        Script p2shMultisigOutputScript = ScriptBuilder.createP2SHOutputScript(2, Arrays.asList(signer, signer2));
        assertEquals(1, p2shMultisigOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenP2shErpRedeemScript_withP2SHOutputScript_shouldBothReturnOne() {
        Script p2shErpRedeemScript = createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(p2shErpRedeemScript);

        assertEquals(1, p2shErpRedeemScript.getSigsPrefixCount());
        assertEquals(1, p2shOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenP2shErpRedeemScript_withP2SHOutputScript_shouldReturnZeroAndOne() {
        Script p2shErpRedeemScript = createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        // Actually it should be 2 (redeemScript + OP_NOTIF)
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(p2shErpRedeemScript);

        assertEquals(0, p2shErpRedeemScript.getSigsSuffixCount());
        assertEquals(1, p2shOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenFlyoverStandardRedeemScript_withP2SHOutputScript_shouldBothReturnOne() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        assertEquals(1, flyoverRedeemScript.getSigsPrefixCount());
        assertEquals(1, p2shOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenFlyoverStandardRedeemScript_withP2SHOutputScript_shouldReturnZeroAndOne() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        assertEquals(0, flyoverRedeemScript.getSigsSuffixCount());
        assertEquals(1, p2shOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenFlyoverNonStandardErpRedeemScript_withP2SHOutputScript_shouldBothReturnOne() {
        Script nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            nonStandardErpRedeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        assertEquals(1, flyoverRedeemScript.getSigsPrefixCount());
        assertEquals(1, p2shOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenFlyoverNonStandardErpRedeemScript_withP2SHOutputScript_shouldReturnZeroAndOne() {
        Script nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            nonStandardErpRedeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        assertEquals(0, flyoverRedeemScript.getSigsSuffixCount());
        assertEquals(1, p2shOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenFlyoverP2shErpRedeemScript_withP2SHOutputScript_shouldBothReturnOne() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        assertEquals(1, flyoverRedeemScript.getSigsPrefixCount());
        assertEquals(1, p2shOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenFlyoverP2shErpRedeemScript_withP2SHOutputScript_shouldReturnZeroAndOne() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        assertEquals(0, flyoverRedeemScript.getSigsSuffixCount());
        assertEquals(1, p2shOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenStandardRedeemScript_withP2SHOutputScript_shouldBothReturnOne() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        Script standardP2SHOutputScript = ScriptBuilder.createP2SHOutputScript(standardRedeemScript);

        assertEquals(1, standardRedeemScript.getSigsPrefixCount());
        assertEquals(1, standardP2SHOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenStandardRedeemScript_withP2SHOutputScript_shouldReturnZeroAndOne() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        Script standardP2SHOutputScript = ScriptBuilder.createP2SHOutputScript(standardRedeemScript);

        assertEquals(0, standardRedeemScript.getSigsSuffixCount());
        assertEquals(1, standardP2SHOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenNonStandardRedeemScript_withP2SHOutputScript_shouldBothReturnOne() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script standardP2SHOutputScript = ScriptBuilder.createP2SHOutputScript(nonStandardErpRedeemScript);

        assertEquals(1, nonStandardErpRedeemScript.getSigsPrefixCount());
        assertEquals(1, standardP2SHOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenNonStandardRedeemScript_withP2SHOutputScript_shouldReturnZeroAndOne() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script standardP2SHOutputScript = ScriptBuilder.createP2SHOutputScript(nonStandardErpRedeemScript);

        assertEquals(0, nonStandardErpRedeemScript.getSigsSuffixCount());
        assertEquals(1, standardP2SHOutputScript.getSigsSuffixCount());
    }

    @Test
    public void getSigsPrefixCount_whenStandardRedeemScript_withP2SHP2WSHOutputScript_shouldReturnOne() {
        Script standardErpRedeemScript = createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script standardP2SHP2WSHOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(standardErpRedeemScript);

        assertEquals(1, standardErpRedeemScript.getSigsPrefixCount());
        assertEquals(1, standardP2SHP2WSHOutputScript.getSigsPrefixCount());
    }

    @Test
    public void getSigsSuffixCount_whenStandardRedeemScript_withP2SHP2WSHOutputScript_shouldReturnOne() {
        Script standardErpRedeemScript = createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script standardP2SHP2WSHOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(standardErpRedeemScript);

        assertEquals(0, standardErpRedeemScript.getSigsSuffixCount());
        assertEquals(1, standardP2SHP2WSHOutputScript.getSigsSuffixCount());
    }

    @Test
    public void isSentToMultiSig_whenP2shErpRedeemScript_shouldReturnTrue() {
        Script p2shErpRedeemScript = createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        assertScriptIsMultiSig(p2shErpRedeemScript);
    }

    @Test
    public void isSentToMultiSig_whenFlyoverStandardRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        assertScriptIsMultiSig(flyoverRedeemScript);
    }

    @Test
    public void isSentToMultiSig_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        assertScriptIsMultiSig(flyoverRedeemScript);
    }

    @Test
    public void isSentToMultiSig_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        assertScriptIsMultiSig(flyoverRedeemScript);
    }

    /**
     * This test just prove that the validation when parsing a flyover redeem script
     * is only checking the structure but not the content of the chunks. To check the
     * flyover prefix is using {@link RedeemScriptValidator#hasFlyoverPrefix(List<ScriptChunk>) hasFlyoverPrefix}
     */
    @Test
    public void isSentToMultiSig_whenZeroHashFlyoverPrefix_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Sha256Hash invalidFlyoverHash = Sha256Hash.ZERO_HASH;
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            invalidFlyoverHash.getBytes(),
            redeemScript
        );

        assertScriptIsMultiSig(flyoverRedeemScript);
    }

    @Test
    public void createEmptyInputScript_whenStandardRedeemScript() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        Script standardP2SHOutputScript = ScriptBuilder.createP2SHOutputScript(standardRedeemScript);
        Script inputScript = standardP2SHOutputScript.createEmptyInputScript(null, standardRedeemScript);

        int expectedOpZeroes = 1 + REQUIRED_SIGNATURES;

        assertScriptSigStructure(
            inputScript.getChunks(),
            STANDARD_MULTISIG_SCRIPT_SIG_CHUNKS,
            expectedOpZeroes,
            standardRedeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_whenFlyoverStandardMultisig_shouldCreateValidScriptSig() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(FEDERATION_KEYS);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            redeemScript
        );

        Script standardP2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);
        Script scriptSig = standardP2shOutputScript.createEmptyInputScript(null, flyoverRedeemScript);

        int expectedOpZeroes = 1 + REQUIRED_SIGNATURES;

        assertScriptSigStructure(
            scriptSig.getChunks(),
            STANDARD_MULTISIG_SCRIPT_SIG_CHUNKS,
            expectedOpZeroes,
            flyoverRedeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_whenNonStandardErpRedeemScript() {
        Script nonStandardErpRedeemScript = createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(nonStandardErpRedeemScript);
        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, nonStandardErpRedeemScript);

        // The expected Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedOpZeroes = 2 + REQUIRED_SIGNATURES;

        assertScriptSigStructure(
            scriptSig.getChunks(),
            P2SH_ERP_MULTISIG_SCRIPT_SIG_CHUNKS,
            expectedOpZeroes,
            nonStandardErpRedeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_whenFlyoverNonStandardErpRedeemScript_shouldCreateValidNonStandardErpRedeemScript() {
        Script nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            nonStandardErpRedeemScript
        );

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);
        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, flyoverRedeemScript);

        // The expected Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedOpZeroes = 2 + REQUIRED_SIGNATURES;

        assertScriptSigStructure(
            scriptSig.getChunks(),
            P2SH_ERP_MULTISIG_SCRIPT_SIG_CHUNKS,
            expectedOpZeroes,
            flyoverRedeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_whenP2shErpRedeemScript_shouldCreateValidScriptSig() {
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(p2shErpRedeemScript);
        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, p2shErpRedeemScript);

        // The expected P2sh Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedOpZeroes = 2 + REQUIRED_SIGNATURES;

        assertScriptSigStructure(
            scriptSig.getChunks(),
            P2SH_ERP_MULTISIG_SCRIPT_SIG_CHUNKS,
            expectedOpZeroes,
            p2shErpRedeemScript.getProgram()
        );
    }

    @Test
    public void createEmptyInputScript_whenFlyoverP2shErpRedeemScript_shouldAddOpZeroFlow() {
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            FEDERATION_KEYS,
            ERP_FEDERATION_KEYS,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH,
            p2shErpRedeemScript
        );

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);
        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, flyoverRedeemScript);

        // The expected P2sh Erp input script structure is:
        // First element: OP_0 - Belonging to the standard of BTC
        // M elements OP_0 - Belonging to M/N amount of signatures
        // OP_0 - Belonging to ERP
        // Last element: Program of redeem script
        int expectedOpZeroes = 2 + REQUIRED_SIGNATURES;

        assertScriptSigStructure(
            scriptSig.getChunks(),
            P2SH_ERP_MULTISIG_SCRIPT_SIG_CHUNKS,
            expectedOpZeroes,
            flyoverRedeemScript.getProgram()
        );
    }

    private void assertScriptSigStructure(
        List<ScriptChunk> chunks,
        int expectedChunksSize,
        int expectedOpZeroes,
        byte[] redeemScriptProgram
    ) {
        // Validate input scripts chunks
        Assert.assertEquals(expectedChunksSize, chunks.size());

        int actualOpZeroes = 0;
        for (ScriptChunk chunk : chunks) {
            if (chunk.equalsOpCode(OP_0)) {
                actualOpZeroes++;
            }
        }

        Assert.assertEquals(expectedOpZeroes, actualOpZeroes);
        Assert.assertArrayEquals(redeemScriptProgram, chunks.get(chunks.size() - 1).data);
    }

    private void testGetSigInsertionIndex(Script redeemScript) {
        final BtcECKey fedKey1 = FEDERATION_KEYS.get(0);
        final BtcECKey fedKey2 = FEDERATION_KEYS.get(1);

        BtcTransaction fundTx = new BtcTransaction(MAINNET_PARAMS);

        final Address userAddress = BtcECKey.fromPrivate(BigInteger.valueOf(900)).toAddress(
            MAINNET_PARAMS);
        fundTx.addOutput(Coin.FIFTY_COINS, userAddress);

        BtcTransaction spendTx = new BtcTransaction(MAINNET_PARAMS);
        spendTx.addInput(fundTx.getOutput(0));

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(redeemScript);

        Script inputScript = p2SHOutputScript.createEmptyInputScript(null, redeemScript);

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
            if (w.isEmpty())
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
                flags.add(VerifyFlag.valueOf(flag));
            }
        }
        return flags;
    }

    private Map<TransactionOutPoint, Script> parseScriptPubKeys(JsonNode inputs) throws IOException {
        Map<TransactionOutPoint, Script> scriptPubKeys = new HashMap<>();
        for (JsonNode input : inputs) {
            String hash = input.get(0).asText();
            int index = input.get(1).asInt();
            String script = input.get(2).asText();
            Sha256Hash sha256Hash = Sha256Hash.wrap(Hex.decode(hash));
            scriptPubKeys.put(new TransactionOutPoint(MAINNET_PARAMS, index, sha256Hash), parseScriptString(script));
        }
        return scriptPubKeys;
    }

    private <T extends Throwable> T assertThrows(Class<T> expectedExceptionType, Executable executable) {
        try {
            executable.execute();
        } catch (Throwable throwable) {
            if (expectedExceptionType.isInstance(throwable)) {
                return (T) throwable;
            }
        }
        throw new AssertionError("Expected exception of type " + expectedExceptionType.getName() + " but no exception was thrown");
    }

    private interface Executable {
        void execute() throws Throwable;
    }
}
