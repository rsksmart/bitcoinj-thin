package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import co.rsk.bitcoinj.script.Script.ScriptType;
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
        defaultRedeemScriptKeys = RedeemScriptTestUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptTestUtils.getEmergencyRedeemScriptKeys();
    }

    @Test
    public void parser_from_hardcoded_script_is_NonStandardRedeemScriptHardcodedParser() {
        byte[] ERP_TESTNET_REDEEM_SCRIPT_BYTES = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
        Script redeemScript = new Script(ERP_TESTNET_REDEEM_SCRIPT_BYTES);
        List<ScriptChunk> chunks = redeemScript.getChunks();
        RedeemScriptParser parser = RedeemScriptParserFactory.get(chunks);
    
        Assert.assertTrue(parser instanceof NonStandardRedeemScriptHardcodedParser);
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_multiSig_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            defaultRedeemScriptKeys
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(fastBridgeRedeemScript.getChunks());
        Assert.assertEquals(MultiSigType.FAST_BRIDGE_MULTISIG, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_standard_multiSig_chunk() {
        Script redeemScript = RedeemScriptTestUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.STANDARD_MULTISIG, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_erp_multiSig_chunk() {
        Script redeemScript = RedeemScriptTestUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.ERP_FED, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_erp_fast_bridge_multiSig_chunk() {
        Script redeemScript = RedeemScriptTestUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_ERP_FED, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_p2sh_erp_multiSig_chunk() {
        Script redeemScript = RedeemScriptTestUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.P2SH_ERP_FED, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_p2sh_erp_multiSig_chunk() {
        Script redeemScript = RedeemScriptTestUtils.createFastBridgeP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_P2SH_ERP_FED, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_P2SH_chunk() {
        byte[] data = Sha256Hash.of(new byte[]{1}).getBytes();
        Script fastBridgeRedeemScript = RedeemScriptTestUtils.createFastBridgeRedeemScript(
            data,
            defaultRedeemScriptKeys
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
        Script redeemScript = RedeemScriptTestUtils.createStandardRedeemScript(defaultRedeemScriptKeys);

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
    public void create_RedeemScriptParser_object_from_erp_P2SH_chunk() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, erpRedeemScript);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertEquals(MultiSigType.ERP_FED, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.P2SH, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_erp_P2SH_chunk() {
        Script fastBridgeErpRedeemScript = RedeemScriptTestUtils.createFastBridgeErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, fastBridgeErpRedeemScript);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_ERP_FED, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.P2SH, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_p2sh_erp_P2SH_chunk() {
        Script p2shErpRedeemScript = RedeemScriptTestUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, p2shErpRedeemScript);
        Script redeemScriptFromInputScript = extractRedeemScriptFromInputScript(inputScript).get();
        RedeemScriptParser parser = RedeemScriptParserFactory.get(redeemScriptFromInputScript.getChunks());

        Assert.assertEquals(MultiSigType.P2SH_ERP_FED, parser.getMultiSigType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_fast_bridge_p2sh_erp_P2SH_chunk() {
        Script fastBridgeP2shErpRedeemScript = RedeemScriptTestUtils.createFastBridgeP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Script spk = ScriptBuilder.createP2SHOutputScript(
            2,
            Arrays.asList(ecKey1, ecKey2, ecKey3)
        );

        Script inputScript = spk.createEmptyInputScript(null, fastBridgeP2shErpRedeemScript);
        RedeemScriptParser parser = RedeemScriptParserFactory.get(inputScript.getChunks());

        Assert.assertEquals(MultiSigType.FAST_BRIDGE_P2SH_ERP_FED, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.P2SH, parser.getScriptType());
    }

    @Test
    public void create_RedeemScriptParser_object_from_custom_redeem_script_no_multiSig() {
        Script redeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultRedeemScriptKeys);
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

    @Test
    public void create_RedeemScriptParser_object_from_hardcoded_testnet_redeem_script() {
        final byte[] ERP_TESTNET_REDEEM_SCRIPT_BYTES = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
        Script erpTestnetRedeemScript = new Script(ERP_TESTNET_REDEEM_SCRIPT_BYTES);

        RedeemScriptParser parser = RedeemScriptParserFactory.get(erpTestnetRedeemScript.getChunks());

        Assert.assertEquals(MultiSigType.NO_MULTISIG_TYPE, parser.getMultiSigType());
        Assert.assertEquals(ScriptType.UNDEFINED, parser.getScriptType());
    }
}
