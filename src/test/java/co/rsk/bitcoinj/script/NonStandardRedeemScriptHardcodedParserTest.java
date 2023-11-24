package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class NonStandardRedeemScriptHardcodedParserTest {
    private final RedeemScriptParser parser = new NonStandardRedeemScriptHardcodedParser();

    @Test
    public void parser_is_NonStandardRedeemScriptHardcodedParser() {
        Assert.assertTrue(parser instanceof NonStandardRedeemScriptHardcodedParser);
    }

    @Test
    public void getMultiSigType_returns_expected_value() {
        MultiSigType multiSigType = parser.getMultiSigType();
        Assert.assertEquals(MultiSigType.NO_MULTISIG_TYPE, multiSigType);
    }

    @Test
    public void getM_returns_expected_value() {
        int m = parser.getM();
        Assert.assertEquals(-1, m);
    }

    @Test
    public void findKeyInRedeem_returns_expected_value() {
        BtcECKey key = BtcECKey.fromPrivate(BigInteger.valueOf(100));
        int keyInRedeem = parser.findKeyInRedeem(key);

        Assert.assertEquals(-1, keyInRedeem);
    }

    @Test (expected = ScriptException.class)
    public void getPubKeys_fails() {
        parser.getPubKeys();
    }

    @Test
    public void findSigInRedeem_returns_expected_value() {
        Sha256Hash dummyHash = Sha256Hash.of(new byte[]{0});
        byte[] dummyData = dummyHash.getBytes();
        int sigInRedeem = parser.findSigInRedeem(dummyData, dummyHash);

        Assert.assertEquals(0, sigInRedeem);
    }

    @Test (expected = ScriptException.class)
    public void extractStandardRedeemScript_fails() {
        parser.extractStandardRedeemScript();
    }
}
