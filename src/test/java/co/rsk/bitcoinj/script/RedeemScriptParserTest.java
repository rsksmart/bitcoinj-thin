package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.RedeemScriptUtil;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.script.RedeemScriptParser.ScriptType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedeemScriptParserTest {
    private final List<BtcECKey> btcECKeyList = new ArrayList<>();
    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(100));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(200));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(300));

    @Before
    public void setUp() {
        btcECKeyList.add(ecKey1);
        btcECKeyList.add(ecKey2);
        btcECKeyList.add(ecKey3);
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_multiSig_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        RedeemScriptParser ms = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertTrue(ms.isFastBridgeMultiSig());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, ms.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_standard_multiSig_chunk() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());
        Assert.assertTrue(ms.isStandardMultiSig());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, ms.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_P2SH_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        Script spk = ScriptBuilder.createP2SHOutputScript(2,
            Arrays.asList(ecKey1, ecKey2, ecKey3));

        Script inputScript = spk.createEmptyInputScript(null, fastBridgeRedeemScript);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertTrue(ms.isFastBridgeMultiSig());
        Assert.assertEquals(ScriptType.P2SH, ms.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_standard_P2SH_chunk() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);

        Script spk = ScriptBuilder.createP2SHOutputScript(2,
            Arrays.asList(ecKey1, ecKey2, ecKey3));

        Script inputScript = spk.createEmptyInputScript(null, redeemScript);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertTrue(ms.isStandardMultiSig());
        Assert.assertEquals(ScriptType.P2SH, ms.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_custom_redeem_script_no_multiSig() {
        Script redeemScript = RedeemScriptUtil.createCustomRedeemScript(btcECKeyList);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());
        Assert.assertTrue(ms.isNotMultiSig());
    }

    @Test
    public void create_RedeemScriptParser_object_from_custom_redeem_script_insufficient_chunks() {
        Script redeemScript = new Script(new byte[2]);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());
        Assert.assertTrue(ms.isNotMultiSig());
        Assert.assertEquals(ScriptType.UNDEFINED, ms.getScriptType());
    }

    @Test
    public void getDerivationArgumentsHash_from_fast_bridge_multiSig() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        FastBridgeRedeemScriptParser ms =
            (FastBridgeRedeemScriptParser) RedeemScriptParserFactory.get(
                fastBridgeRedeemScript.getChunks()
            );
        Assert.assertArrayEquals(ms.getDerivationHash(), data);
    }

    @Test
    public void getSigInsertionIndex_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);

        BtcTransaction fundTx = new BtcTransaction(networkParameters);
        fundTx.addOutput(Coin.FIFTY_COINS, ecKey1.toAddress(networkParameters));

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTx.getOutput(0));

        Script spk = ScriptBuilder.createP2SHOutputScript(2,
            Arrays.asList(ecKey1, ecKey2, ecKey3));

        Script inputScript = spk.createEmptyInputScript(null, fastBridgeRedeemScript);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(inputScript.getChunks());

        Sha256Hash sigHash = spendTx.hashForSignature(0, fastBridgeRedeemScript,
            BtcTransaction.SigHash.ALL, false);

        BtcECKey.ECDSASignature sign1 = ecKey1.sign(sigHash);
        TransactionSignature txSig = new TransactionSignature(sign1,
            BtcTransaction.SigHash.ALL, false);

        byte[] txSigEncoded = txSig.encodeToBitcoin();

        int sigIndex = ms.getSigInsertionIndex(sigHash, ecKey1);
        Assert.assertEquals(0, sigIndex);

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, txSigEncoded,
            sigIndex, 1, 1);

        RedeemScriptParser ms2 = RedeemScriptParserFactory.get(inputScript.getChunks());

        sigIndex = ms2.getSigInsertionIndex(sigHash, ecKey2);
        Assert.assertEquals(1, sigIndex);
    }

    @Test
    public void getSigInsertionIndex_no_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);

        BtcTransaction fundTx = new BtcTransaction(networkParameters);
        fundTx.addOutput(Coin.FIFTY_COINS, ecKey1.toAddress(networkParameters));

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTx.getOutput(0));

        Script spk = ScriptBuilder.createP2SHOutputScript(2,
            Arrays.asList(ecKey1, ecKey2, ecKey3));

        Script inputScript = spk.createEmptyInputScript(redeemScript.getPubKeys().get(0),
            redeemScript);

        RedeemScriptParser ms = RedeemScriptParserFactory.get(inputScript.getChunks());

        Sha256Hash sigHash = spendTx.hashForSignature(0, redeemScript,
            BtcTransaction.SigHash.ALL, false);

        BtcECKey.ECDSASignature sign1 = ecKey1.sign(sigHash);
        TransactionSignature txSig = new TransactionSignature(sign1, BtcTransaction.SigHash.ALL, false);
        byte[] txSigEncoded = txSig.encodeToBitcoin();

        int sigIndex = ms.getSigInsertionIndex(sigHash, ecKey1);
        Assert.assertEquals(0, sigIndex);

        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, txSigEncoded,
            sigIndex, 1, 1);

        RedeemScriptParser ms2 = RedeemScriptParserFactory.get(inputScript.getChunks());

        sigIndex = ms2.getSigInsertionIndex(sigHash, ecKey2);
        Assert.assertEquals(1, sigIndex);
    }

    @Test
    public void findKeyInRedeem_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        RedeemScriptParser ms = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertTrue(ms.findKeyInRedeem(ecKey1) >= 0);
        Assert.assertTrue(ms.findKeyInRedeem(ecKey2) >= 0);
        Assert.assertTrue(ms.findKeyInRedeem(ecKey3) >= 0);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_fast_bridge_redeem_script_no_matching_key() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(400));
        RedeemScriptParser ms = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());

        ms.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_standard_redeem_script_no_matching_key() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(400));
        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());

        ms.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test
    public void getPubKeys_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        RedeemScriptParser ms = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        List<BtcECKey> obtainedList = ms.getPubKeys();

        List<String> expectedKeysList = new ArrayList<>();
        for (BtcECKey key : btcECKeyList) {
            expectedKeysList.add(key.getPublicKeyAsHex());
        }

        List<String> obtainedKeysList = new ArrayList<>();
        for (BtcECKey key : obtainedList) {
            obtainedKeysList.add(key.getPublicKeyAsHex());
        }

        Collections.sort(expectedKeysList);
        Collections.sort(obtainedKeysList);

        Assert.assertEquals(expectedKeysList, obtainedKeysList);
    }

    @Test
    public void getPubKeys_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);

        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());
        List<BtcECKey> obtainedList = ms.getPubKeys();

        List<String> expectedKeysList = new ArrayList<>();
        for (BtcECKey key : btcECKeyList) {
            expectedKeysList.add(key.getPublicKeyAsHex());
        }

        List<String> obtainedKeysList = new ArrayList<>();
        for (BtcECKey key : obtainedList) {
            obtainedKeysList.add(key.getPublicKeyAsHex());
        }

        Collections.sort(expectedKeysList);
        Collections.sort(obtainedKeysList);

        Assert.assertEquals(expectedKeysList, obtainedKeysList);
    }

    @Test(expected = ScriptException.class)
    public void getPubKeys_invalid_redeem_script() {
        Script script = ScriptBuilder.createP2SHOutputScript(new byte[20]);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(script.getChunks());
        ms.getPubKeys();
    }

    @Test
    public void extractRedeemScriptFromMultiSigFastBridgeRedeemScript_fb_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        Script standardRedeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);

        Script obtainedRedeemScript = FastBridgeRedeemScriptParser.extractRedeemScriptFromMultiSigFastBridgeRedeemScript(
            fastBridgeRedeemScript);

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test
    public void extractRedeemScriptFromMultiSigFastBridgeRedeemScript_std_redeem_script() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        Script obtainedRedeemScript =
            FastBridgeRedeemScriptParser.extractRedeemScriptFromMultiSigFastBridgeRedeemScript(redeemScript);

        Assert.assertEquals(redeemScript, obtainedRedeemScript);
    }

    @Test
    public void createMultiSigFastBridgeRedeemScript_valid_parameters() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});

        Script expectedFastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            derivationArgumentsHash.getBytes(), btcECKeyList);

        Assert.assertEquals(expectedFastBridgeRedeemScript,
            FastBridgeRedeemScriptParser
                .createMultiSigFastBridgeRedeemScript(redeemScript, derivationArgumentsHash));
    }

    @Test(expected = VerificationException.class)
    public void createMultiSigFastBridgeRedeemScript_fb_redeem_script() {
        Sha256Hash data = Sha256Hash.of(new byte[]{1});
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data.getBytes(), btcECKeyList);

        FastBridgeRedeemScriptParser
            .createMultiSigFastBridgeRedeemScript(fastBridgeRedeemScript, data);
    }

    @Test(expected = VerificationException.class)
    public void createMultiSigFastBridgeRedeemScript_null_derivation_arguments_hash() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        FastBridgeRedeemScriptParser.createMultiSigFastBridgeRedeemScript(redeemScript, null);
    }

    @Test(expected = VerificationException.class)
    public void createMultiSigFastBridgeRedeemScript_zero_hash_as_derivation_arguments_hash() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        FastBridgeRedeemScriptParser.createMultiSigFastBridgeRedeemScript(redeemScript, Sha256Hash.ZERO_HASH);
    }

    @Test
    public void getM_from_no_multiSig() {
        Script redeemScript = new Script(new byte[2]);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());
        Assert.assertEquals(-1, ms.getM());
    }

    @Test
    public void getM_from_multiSig_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtil.createFastBridgeRedeemScript(
            data, btcECKeyList);

        RedeemScriptParser ms = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertEquals(2, ms.getM());
    }

    @Test
    public void getM_from_multiSig_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtil.createStandardRedeemScript(btcECKeyList);
        RedeemScriptParser ms = RedeemScriptParserFactory.get(redeemScript.getChunks());
        Assert.assertEquals(2, ms.getM());
    }
}
