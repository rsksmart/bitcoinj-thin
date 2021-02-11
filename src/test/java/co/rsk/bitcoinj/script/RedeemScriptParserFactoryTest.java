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
    private final List<BtcECKey> defaultFedBtcECKeyList = new ArrayList<>();
    private final List<BtcECKey> erpFedBtcECKeyList = new ArrayList<>();
    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(100));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(200));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(300));
    private final BtcECKey ecKey4 = BtcECKey.fromPrivate(BigInteger.valueOf(400));
    private final BtcECKey ecKey5 = BtcECKey.fromPrivate(BigInteger.valueOf(500));
    private final BtcECKey ecKey6 = BtcECKey.fromPrivate(BigInteger.valueOf(600));

    @Before
    public void setUp() {
        defaultFedBtcECKeyList.add(ecKey1);
        defaultFedBtcECKeyList.add(ecKey2);
        defaultFedBtcECKeyList.add(ecKey3);
        erpFedBtcECKeyList.add(ecKey4);
        erpFedBtcECKeyList.add(ecKey5);
        erpFedBtcECKeyList.add(ecKey6);
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_multiSig_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            defaultFedBtcECKeyList
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertEquals(MultiSigType.FAST_BRIDGE_MULTISIG, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_standard_multiSig_chunk() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.STANDARD_MULTISIG, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_erp_multiSig_chunk() {
        Script redeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            500L
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.ERP_FED, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_erp_fast_bridge_multiSig_chunk() {
        Script redeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_ERP_FED, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.REDEEM_SCRIPT, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_P2SH_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptUtils.createFastBridgeRedeemScript(
            data,
            defaultFedBtcECKeyList
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
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

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
        Script redeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);
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
