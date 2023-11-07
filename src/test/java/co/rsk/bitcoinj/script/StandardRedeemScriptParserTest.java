package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StandardRedeemScriptParserTest {

    private final List<BtcECKey> btcECKeyList = new ArrayList<>();
    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(100));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(200));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(300));
    private final NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);

    @Before
    public void setUp() {
        btcECKeyList.add(ecKey1);
        btcECKeyList.add(ecKey2);
        btcECKeyList.add(ecKey3);
    }

    @Test
    public void getSigInsertionIndex_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        BtcTransaction fundTx = new BtcTransaction(networkParameters);
        fundTx.addOutput(Coin.FIFTY_COINS, ecKey1.toAddress(networkParameters));

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTx.getOutput(0));

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, fastBridgeRedeemScript);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Sha256Hash sigHash = spendTx.hashForSignature(
            0,
            fastBridgeRedeemScript,
            BtcTransaction.SigHash.ALL,
            false
        );

        BtcECKey.ECDSASignature sign1 = ecKey1.sign(sigHash);
        TransactionSignature txSig = new TransactionSignature(
            sign1,
            BtcTransaction.SigHash.ALL,
            false
        );

        byte[] txSigEncoded = txSig.encodeToBitcoin();

        int sigIndex = inputScript.getSigInsertionIndex(sigHash, ecKey1);
        Assert.assertEquals(0, sigIndex);

        inputScript = ScriptBuilder.updateScriptWithSignature(
            inputScript,
            txSigEncoded,
            sigIndex,
            1,
            1
        );

        parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        sigIndex = inputScript.getSigInsertionIndex(sigHash, ecKey2);
        Assert.assertEquals(1, sigIndex);
    }

    @Test
    public void getSigInsertionIndex_no_fast_bridge_redeem_script() {
        Script redeemScript = RedeemScriptTestUtils.createStandardRedeemScript(btcECKeyList);

        BtcTransaction fundTx = new BtcTransaction(networkParameters);
        fundTx.addOutput(Coin.FIFTY_COINS, ecKey1.toAddress(networkParameters));

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTx.getOutput(0));

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(
            redeemScript.getPubKeys().get(0),
            redeemScript
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Sha256Hash sigHash = spendTx.hashForSignature(
            0,
            redeemScript,
            BtcTransaction.SigHash.ALL,
            false
        );

        BtcECKey.ECDSASignature sign1 = ecKey1.sign(sigHash);
        TransactionSignature txSig = new TransactionSignature(
            sign1,
            BtcTransaction.SigHash.ALL,
            false
        );
        byte[] txSigEncoded = txSig.encodeToBitcoin();

        int sigIndex = inputScript.getSigInsertionIndex(sigHash, ecKey1);
        Assert.assertEquals(0, sigIndex);

        inputScript = ScriptBuilder.updateScriptWithSignature(
            inputScript,
            txSigEncoded,
            sigIndex,
            1,
            1
        );

        parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        sigIndex = inputScript.getSigInsertionIndex(sigHash, ecKey2);
        Assert.assertEquals(1, sigIndex);
    }

    @Test
    public void findKeyInRedeem_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());

        Assert.assertTrue(parser.findKeyInRedeem(ecKey1) >= 0);
        Assert.assertTrue(parser.findKeyInRedeem(ecKey2) >= 0);
        Assert.assertTrue(parser.findKeyInRedeem(ecKey3) >= 0);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_fast_bridge_redeem_script_no_matching_key() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(400));
        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());

        parser.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_standard_redeem_script_no_matching_key() {
        Script redeemScript = RedeemScriptTestUtils.createStandardRedeemScript(btcECKeyList);
        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(400));

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        parser.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test
    public void getPubKeys_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        List<BtcECKey> obtainedList = parser.getPubKeys();

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
        Script redeemScript = RedeemScriptTestUtils.createStandardRedeemScript(btcECKeyList);

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());
        List<BtcECKey> obtainedList = parser.getPubKeys();

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
        RedeemScriptParser parser = RedeemScriptParserFactory.get(script.getChunks());

        parser.getPubKeys();
    }

    @Test
    public void getM_from_no_multiSig() {
        Script redeemScript = new Script(new byte[2]);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(-1, parser.getM());
    }

    @Test
    public void getM_from_multiSig_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());

        Assert.assertEquals(2, parser.getM());
    }

    @Test
    public void getM_from_multiSig_standard_redeem_script() {
        Script redeemScript = RedeemScriptTestUtils.createStandardRedeemScript(btcECKeyList);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(2, parser.getM());
    }
}
