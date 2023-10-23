package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FlyoverRedeemScriptParserTest {

    private List<BtcECKey> publicKeys;
    private Script redeemScript;
    Sha256Hash derivationHash;

    @Before
    public void setUp() {
        publicKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        redeemScript = ScriptBuilder.createRedeemScript(publicKeys.size() / 2 + 1, publicKeys);
        derivationHash = Sha256Hash.of(new byte[]{1});
    }

    @Test
    public void extractRedeemScriptFromFlyoverRedeemScript() {
        Script flyoverRedeemScript = FlyoverRedeemScriptParser.createFlyoverRedeemScript(
            redeemScript,
            derivationHash
        );
        Script obtainedRedeemScript = FlyoverRedeemScriptParser.extractRedeemScript(
            flyoverRedeemScript
        );

        Assert.assertEquals(redeemScript, obtainedRedeemScript);
    }
    
    @Test
    public void hasFlyoverPrefix() {
        Script flyoverRedeemScript = FlyoverRedeemScriptParser.createFlyoverRedeemScript(
            redeemScript,
            derivationHash
        );

        Assert.assertTrue(RedeemScriptValidator.hasFastBridgePrefix(flyoverRedeemScript.getChunks()));
    }

    @Test(expected = VerificationException.class)
    public void createFlyoverRedeemScriptFromFlyoverRedeemScriptThrowsError() {
        Script flyoverRedeemScript = FlyoverRedeemScriptParser.createFlyoverRedeemScript(
            redeemScript,
            derivationHash
        );

        FlyoverRedeemScriptParser.createFlyoverRedeemScript(flyoverRedeemScript, derivationHash);
    }

    @Test(expected = VerificationException.class)
    public void createFlyoverRedeemScriptWithNullDerivationHashThrowsError() {
        FlyoverRedeemScriptParser.createFlyoverRedeemScript(redeemScript, null);
    }

    @Test(expected = VerificationException.class)
    public void createFlyoverRedeemScriptWithZeroHashDerivationHashThrowsError() {
        FlyoverRedeemScriptParser.createFlyoverRedeemScript(redeemScript, Sha256Hash.ZERO_HASH);
    }

    @Test
    public void getDerivationArgumentsHash() {
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(
            redeemScript,
            derivationHash
        );
        byte[] obtainedDerivationHash = flyoverRedeemScriptParser.getDerivationHash();

        Assert.assertArrayEquals(obtainedDerivationHash, derivationHash.getBytes());
    }
}
