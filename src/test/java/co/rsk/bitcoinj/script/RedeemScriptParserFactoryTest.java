package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType.NO_MULTISIG_TYPE;
import static co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType.STANDARD_MULTISIG;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedeemScriptParserFactoryTest {

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(110));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(220));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(330));

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    }

    @Test(expected = ScriptException.class)
    public void get_whenEmptyScript_shouldThrowScriptException() {
        Script emptyScript = new Script(new byte[0]);
        RedeemScriptParserFactory.get(emptyScript.getChunks());
    }

    @Test(expected = ScriptException.class)
    public void get_whenMalFormedRedeemScript_shouldThrowScriptException() {
        Script malFormeScript = RedeemScriptUtils.createCustomRedeemScript(defaultRedeemScriptKeys);
        RedeemScriptParserFactory.get(malFormeScript.getChunks());
    }

    @Test(expected = ScriptException.class)
    public void get_whenScriptSig_shouldThrowScriptException() {
        byte[] flyoverDerivationHash = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            flyoverDerivationHash,
            defaultRedeemScriptKeys
        );

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, fastBridgeRedeemScript);
        RedeemScriptParserFactory.get(scriptSig.getChunks());
    }

    @Test(expected = ScriptException.class)
    public void get_whenP2shOutputScript_shouldThrowScriptException() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(redeemScript);

        RedeemScriptParserFactory.get(p2SHOutputScript.getChunks());
    }

    @Test
    public void get_whenFlyoverStandardRedeemScript_shouldReturnRedeemScriptParser() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            defaultRedeemScriptKeys
        );

        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertEquals(MultiSigType.FAST_BRIDGE_MULTISIG, redeemScriptParser.getMultiSigType());
    }

    @Test
    public void get_whenStandardRedeemScript_shouldReturnRedeemScriptParser() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(STANDARD_MULTISIG, redeemScriptParser.getMultiSigType());
    }

    @Test
    public void get_whenErpRedeemScript_shouldReturnRedeemScriptParser() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.ERP_FED, redeemScriptParser.getMultiSigType());
    }

    @Test
    public void get_whenFlyoverErpRedeemScript_shouldReturnRedeemScriptParser() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_ERP_FED, redeemScriptParser.getMultiSigType());
    }

    @Test
    public void get_whenP2shErpRedeemScript_shouldReturnRedeemScriptParser() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.P2SH_ERP_FED, redeemScriptParser.getMultiSigType());
    }

    @Test
    public void get_whenFlyoverP2shErpRedeemScript_shouldReturnRedeemScriptParser() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_P2SH_ERP_FED, redeemScriptParser.getMultiSigType());
    }

    @Test
    public void get_whenHardcodedTestnetRedeemScript_shouldReturnHardcodeTestnetParser() {
        final byte[] erpTestnetRedeemScriptSerialized = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
        Script erpTestnetRedeemScript = new Script(erpTestnetRedeemScriptSerialized);

        RedeemScriptParser redeemScriptParser = RedeemScriptParserFactory.get(erpTestnetRedeemScript.getChunks());

        Assert.assertEquals(NO_MULTISIG_TYPE, redeemScriptParser.getMultiSigType());
    }
}
