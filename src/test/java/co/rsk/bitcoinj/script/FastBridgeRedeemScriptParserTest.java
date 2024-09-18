package co.rsk.bitcoinj.script;

import static org.junit.Assert.assertEquals;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FastBridgeRedeemScriptParserTest {

    private List<BtcECKey> publicKeys;

    @Before
    public void setUp() {
        publicKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenGetFromMultiSigFastBridgeRedeemScript_shouldReturnScripChunks() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);
        List<ScriptChunk> standardRedeemScriptChunks = standardRedeemScript.getChunks();

        List<ScriptChunk> obtainedRedeemScriptChunks = FastBridgeRedeemScriptParser.extractStandardRedeemScriptChunks(fastBridgeRedeemScript);

        Assert.assertEquals(standardRedeemScriptChunks, obtainedRedeemScriptChunks);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenGetFromMultiSigStandardRedeemScript_shouldReturnScripChunks() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);
        List<ScriptChunk> redeemScriptChunks = redeemScript.getChunks();
        List<ScriptChunk> obtainedRedeemScriptChunks = FastBridgeRedeemScriptParser.extractStandardRedeemScriptChunks(redeemScript);

        Assert.assertEquals(redeemScriptChunks, obtainedRedeemScriptChunks);
    }

    @Test
    public void createMultiSigFastBridgeRedeemScript_valid_parameters() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});

        Script expectedFastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            derivationArgumentsHash.getBytes(),
            publicKeys
        );

        Script obtainedRedeemScript = FastBridgeRedeemScriptParser.createMultiSigFastBridgeRedeemScript(
            redeemScript,
            derivationArgumentsHash
        );
        Assert.assertEquals(expectedFastBridgeRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void createMultiSigFastBridgeRedeemScript_fb_redeem_script() {
        Sha256Hash data = Sha256Hash.of(new byte[]{1});
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data.getBytes(),
            publicKeys
        );

        FastBridgeRedeemScriptParser.createMultiSigFastBridgeRedeemScript(fastBridgeRedeemScript, data);
    }

    @Test(expected = VerificationException.class)
    public void createMultiSigFastBridgeRedeemScript_null_derivation_arguments_hash() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);
        FastBridgeRedeemScriptParser.createMultiSigFastBridgeRedeemScript(redeemScript, null);
    }

    @Test(expected = VerificationException.class)
    public void createMultiSigFastBridgeRedeemScript_zero_hash_as_derivation_arguments_hash() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);
        FastBridgeRedeemScriptParser.createMultiSigFastBridgeRedeemScript(redeemScript, Sha256Hash.ZERO_HASH);
    }

    @Test
    public void getDerivationArgumentsHash_from_fast_bridge_multiSig() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        FastBridgeRedeemScriptParser fastBridgeRedeemScriptParser = (FastBridgeRedeemScriptParser) parser;

        Assert.assertArrayEquals(fastBridgeRedeemScriptParser.getDerivationHash(), data);
    }

    @Test
    public void isFastBridgeMultisig() {
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            derivationArgumentsHash.getBytes(),
            publicKeys
        );

        Assert.assertTrue(FastBridgeRedeemScriptParser.isFastBridgeMultiSig(fastBridgeRedeemScript.getChunks()));
    }

    @Test
    public void isFastBridgeMultisig_falseWithCustomRedeemScrip() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(publicKeys);

        Assert.assertFalse(FastBridgeRedeemScriptParser.isFastBridgeMultiSig(customRedeemScript.getChunks()));
    }

    @Test
    public void findKeyInRedeem_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        List<BtcECKey> actualPubKeys = parser.getPubKeys();

        List<BtcECKey> expectedPubKeys = publicKeys.stream()
            .map(btcECKey -> BtcECKey.fromPublicOnly(btcECKey.getPubKey())).collect(
                Collectors.toList());
        assertEquals(expectedPubKeys, actualPubKeys);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_fast_bridge_redeem_script_no_matching_key() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        BtcECKey unmatchingBtcECKey = BtcECKey.fromPrivate(BigInteger.valueOf(1400));
        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());

        parser.findKeyInRedeem(unmatchingBtcECKey);
    }

    @Test
    public void getPubKeys_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        List<BtcECKey> obtainedList = parser.getPubKeys();

        List<String> expectedKeysList = new ArrayList<>();
        for (BtcECKey key : publicKeys) {
            expectedKeysList.add(key.getPublicKeyAsHex());
        }

        List<String> obtainedKeysList = new ArrayList<>();
        for (BtcECKey key : obtainedList) {
            obtainedKeysList.add(key.getPublicKeyAsHex());
        }

        Collections.sort(expectedKeysList);
        Collections.sort(obtainedKeysList);

        assertEquals(expectedKeysList, obtainedKeysList);
    }

    @Test
    public void getM_from_multiSig_fast_bridge_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());

        int m = publicKeys.size() / 2 + 1;
        assertEquals(m, parser.getM());
    }
}
