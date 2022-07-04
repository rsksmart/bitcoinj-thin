package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FastBridgeErpRedeemScriptParserTest {

    private final List<BtcECKey> defaultFedBtcECKeyList = new ArrayList<>();
    private final List<BtcECKey> erpFedBtcECKeyList = new ArrayList<>();
    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(100));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(200));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(300));
    private final BtcECKey ecKey4 = BtcECKey.fromPrivate(BigInteger.valueOf(400));
    private final BtcECKey ecKey5 = BtcECKey.fromPrivate(BigInteger.valueOf(500));
    private final BtcECKey ecKey6 = BtcECKey.fromPrivate(BigInteger.valueOf(600));
    private final BtcECKey ecKey7 = BtcECKey.fromPrivate(BigInteger.valueOf(700));
    private final BtcECKey ecKey8 = BtcECKey.fromPrivate(BigInteger.valueOf(800));

    @Before
    public void setUp() {
        defaultFedBtcECKeyList.add(ecKey1);
        defaultFedBtcECKeyList.add(ecKey2);
        defaultFedBtcECKeyList.add(ecKey3);
        erpFedBtcECKeyList.add(ecKey4);
        erpFedBtcECKeyList.add(ecKey5);
        erpFedBtcECKeyList.add(ecKey6);
        erpFedBtcECKeyList.add(ecKey7);
        erpFedBtcECKeyList.add(ecKey8);
    }

    @Test
    public void extractStandardRedeemScript_fromFastBridgeErpRedeemScript() {
        Long csvValue = 100L;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            csvValue,
            derivationArgumentsHash.getBytes()
        );

        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

        Script obtainedRedeemScript = FastBridgeErpRedeemScriptParser.extractStandardRedeemScript(
            fastBridgeErpRedeemScript.getChunks()
        );

        Assert.assertEquals(standardRedeemScript, obtainedRedeemScript);
    }

    @Test(expected = VerificationException.class)
    public void extractStandardRedeemScript_fromStandardRedeemScript_fail() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultFedBtcECKeyList);

        FastBridgeErpRedeemScriptParser.extractStandardRedeemScript(standardRedeemScript.getChunks());
    }

    @Test
    public void createFastBridgeErpRedeemScript_from_Erp_redeem_script() {
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            5063L
        );

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});

        Script expectedRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            5063L,
            derivationArgumentsHash.getBytes()
        );

        Script obtainedRedeemScript = FastBridgeErpRedeemScriptParser.createFastBridgeErpRedeemScript(
            erpRedeemScript,
            derivationArgumentsHash
        );

        Assert.assertEquals(expectedRedeemScript, obtainedRedeemScript);
    }

    @Test
    public void isFastBridgeErpFed() {
        Script fastBridgeErpRedeemScript = RedeemScriptUtils.createFastBridgeErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            200L,
            Sha256Hash.of(new byte[]{1}).getBytes()
        );

        Assert.assertTrue(FastBridgeErpRedeemScriptParser.isFastBridgeErpFed(fastBridgeErpRedeemScript.getChunks()));
    }

    @Test
    public void isFastBridgeErpFed_falseWithCustomRedeemScript() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(defaultFedBtcECKeyList);

        Assert.assertFalse(FastBridgeErpRedeemScriptParser.isFastBridgeErpFed(customRedeemScript.getChunks()));
    }
}
