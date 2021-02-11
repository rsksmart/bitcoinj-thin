package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import co.rsk.bitcoinj.script.RedeemScriptParser.ScriptType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedeemScriptParserFactoryTest {
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
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertEquals(MultiSigType.FAST_BRIDGE_MULTISIG, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_standard_multiSig_chunk() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.STANDARD_MULTISIG, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_P2SH_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            btcECKeyList
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, fastBridgeRedeemScript);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_MULTISIG, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.P2SH, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_standard_P2SH_chunk() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(btcECKeyList);

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, redeemScript);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertEquals(MultiSigType.STANDARD_MULTISIG, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.P2SH, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_custom_redeem_script_no_multiSig() {
        Script redeemScript = RedeemScriptUtils.createCustomRedeemScript(btcECKeyList);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.NO_MULTISIG_TYPE, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_custom_redeem_script_insufficient_chunks() {
        Script redeemScript = new Script(new byte[2]);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.NO_MULTISIG_TYPE, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.UNDEFINED, parser.getScriptType());
    }
}
