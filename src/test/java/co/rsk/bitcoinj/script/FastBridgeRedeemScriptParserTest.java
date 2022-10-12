package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
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
    public void extractRedeemScriptFromMultiSigFastBridgeRedeemScript_fb_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);

        Script obtainedRedeemScript = FastBridgeRedeemScriptParser.extractStandardRedeemScript(
            fastBridgeRedeemScript
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test
    public void extractRedeemScriptFromMultiSigFastBridgeRedeemScript_std_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(publicKeys);
        Script obtainedRedeemScript = FastBridgeRedeemScriptParser.extractStandardRedeemScript(redeemScript);

        Assert.assertEquals(redeemScript, obtainedRedeemScript);
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
}
