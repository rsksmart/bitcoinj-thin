package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FastBridgeRedeemScriptParserTest {

    private List<BtcECKey> publicKeys;

    @Before
    public void setUp() {
        publicKeys = RedeemScriptTestUtils.getDefaultRedeemScriptKeys();
    }

    @Test
    public void extractRedeemScriptFromMultiSigFastBridgeRedeemScript_fb_redeem_script() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            publicKeys
        );

        Script standardRedeemScript = RedeemScriptTestUtils.createStandardRedeemScript(publicKeys);

        Script obtainedRedeemScript = new Script (FastBridgeRedeemScriptParser.extractStandardRedeemScript(
            fastBridgeRedeemScript
        ));

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test
    public void getDerivationArgumentsHash_from_fast_bridge_multiSig() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
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
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            derivationArgumentsHash.getBytes(),
            publicKeys
        );

        Assert.assertTrue(FastBridgeRedeemScriptParser.isFastBridgeMultiSig(fastBridgeRedeemScript.getChunks()));
    }

    @Test
    public void isFastBridgeMultisig_falseWithCustomRedeemScrip() {
        Script customRedeemScript = RedeemScriptTestUtils.createCustomRedeemScript(publicKeys);

        Assert.assertFalse(FastBridgeRedeemScriptParser.isFastBridgeMultiSig(customRedeemScript.getChunks()));
    }
}
