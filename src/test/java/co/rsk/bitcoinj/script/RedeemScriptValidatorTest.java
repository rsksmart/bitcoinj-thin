package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedeemScriptValidatorTest {

    private static final byte[] NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");

    private static final Sha256Hash FLYOVER_DERIVATION_HASH = Sha256Hash.of(new byte[]{1});
    private static final long CSV_VALUE = 52_560L;

    private List<BtcECKey> defaultRedeemScriptKeys;
    private List<BtcECKey> emergencyRedeemScriptKeys;

    private final BtcECKey ecKey1 = BtcECKey.fromPrivate(BigInteger.valueOf(110));
    private final BtcECKey ecKey2 = BtcECKey.fromPrivate(BigInteger.valueOf(220));
    private final BtcECKey ecKey3 = BtcECKey.fromPrivate(BigInteger.valueOf(330));

    private Script standardRedeemScript;
    private Script flyoverStandardRedeemScript;
    private Script nonStandardErpRedeemScript;
    private Script flyoverNonStandardErpRedeemScript;
    private Script p2shErpRedeemScript;
    private Script flyoverP2shErpRedeemScript;
    private Script nonStandardErpTestnetRedeemScript;

    @Before
    public void setUp() {
        defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
        emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();

        standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);
        flyoverStandardRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(), standardRedeemScript);

        nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys, emergencyRedeemScriptKeys, CSV_VALUE);
        flyoverNonStandardErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonStandardErpRedeemScript);

        p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE);
        flyoverP2shErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(), p2shErpRedeemScript);

        nonStandardErpTestnetRedeemScript = new Script(
            NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);
    }

    @Test
    public void isRedeemLikeScript_whenStandardMultisig_shouldReturnTrue() {
        List<ScriptChunk> standardRedeemScriptChunks = standardRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(standardRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenNonStandardErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        List<ScriptChunk> nonStandardErpTestnetRedeemScriptChunks = ScriptParser.parseScriptProgram(
            NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);

        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(nonStandardErpTestnetRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenNonStandardErpRedeemScriptParser_shouldReturnTrue() {
        List<ScriptChunk> nonStandardErpRedeemScriptChunks = nonStandardErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenP2shErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        List<ScriptChunk> p2shErpRedeemScriptChunks = p2shErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(p2shErpRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenFlyoverStandardMultisigRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverStandardRedeemScriptChunks = flyoverStandardRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(flyoverStandardRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverNonStandardErpRedeemScriptChunks = flyoverNonStandardErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(flyoverNonStandardErpRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverP2shErpRedeemScriptChunks = flyoverP2shErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(flyoverP2shErpRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_invalid_redeem_script_missing_checkSig() {
        List<ScriptChunk> chunksWithoutCheckSig = RedeemScriptValidator.removeOpCheckMultisig(standardRedeemScript);
        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(chunksWithoutCheckSig));
    }

    @Test
    public void isRedeemLikeScript_invalid_redeem_script_insufficient_chunks() {
        ScriptBuilder builder = new ScriptBuilder();
        Script redeemScript = builder
            .data(ecKey1.getPubKey())
            .data(ecKey2.getPubKey())
            .data(ecKey3.getPubKey())
            .build();

        List<ScriptChunk> redeemScriptChunks = redeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(redeemScriptChunks));
    }

    @Test
    public void hasStandardRedeemScriptStructure_standard_redeem_script() {
        List<ScriptChunk> standardRedeemScriptChunks = standardRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasStandardRedeemScriptStructure(standardRedeemScriptChunks));
    }

    @Test
    public void hasStandardRedeemScriptStructure_with50defaultRedeemScriptKeys_shouldBeTrue() {
        List<BtcECKey> bigNumberOfDefaultRedeemScriptKeys = RedeemScriptUtils.getNKeys(50);
        Script standardRedeemScriptWithLotsOfKeys = RedeemScriptUtils.createStandardRedeemScript(bigNumberOfDefaultRedeemScriptKeys);
        List<ScriptChunk> standardRedeemScriptWithLotsOfKeysChunks = standardRedeemScriptWithLotsOfKeys.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasStandardRedeemScriptStructure(standardRedeemScriptWithLotsOfKeysChunks));
    }

    @Test
    public void hasStandardRedeemScriptStructure_withLessKeysPushedThanExpected_shouldBeFalse() {
        ScriptBuilder builder = new ScriptBuilder();
        Script redeemScript = builder
            .op(ScriptOpCodes.OP_4)
            .data(ecKey1.getPubKey())
            .op(ScriptOpCodes.OP_4)
            .op(ScriptOpCodes.OP_CHECKMULTISIG)
            .build();
        List<ScriptChunk> redeemScriptChunks = redeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScriptChunks));
    }

    @Test
    public void hasStandardRedeemScriptStructure_withARedeemLikeScript_withoutOpCheckMultisigAsLastOpcode_shouldBeFalse() {
        ScriptBuilder builder = new ScriptBuilder();
        Script redeemScript = builder
            .op(ScriptOpCodes.OP_2)
            .data(ecKey1.getPubKey())
            .data(ecKey2.getPubKey())
            .data(ecKey3.getPubKey())
            .op(ScriptOpCodes.OP_CHECKMULTISIG)
            .op(ScriptOpCodes.OP_ENDIF)
            .build();

        List<ScriptChunk> redeemScriptChunks = redeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScriptChunks));
    }

    @Test
    public void hasStandardRedeemScriptStructure_non_standard_redeem_script() {
        List<ScriptChunk> nonStandardErpRedeemScriptChunks = nonStandardErpRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasStandardRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenCustomRedeemScript_shouldReturnFalse() {
        Script customRedeemScript = RedeemScriptUtils.createCustomRedeemScript(
            defaultRedeemScriptKeys
        );

        List<ScriptChunk> customRedeemScriptChunks = customRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(customRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenStandardRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> standardRedeemScriptChunks = standardRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(standardRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenP2shErpRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> p2shErpRedeemScriptChunks = p2shErpRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(p2shErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenFlyoverStandardRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> flyoverStandardRedeemScriptChunks = flyoverStandardRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(flyoverStandardRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptOneByteCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            10L
        );

        List<ScriptChunk> nonStandardErpRedeemsScriptChunks = redeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemsScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptTwoBytesCsvValue_shouldReturnTrue() {
        List<ScriptChunk> nonStandardErpRedeemScriptChunks = nonStandardErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptTwoBytesIncludingSignCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            130L // Any value above 127 needs an extra byte to indicate the sign
        );

        List<ScriptChunk> nonStandardErpRedeemScriptChunks = redeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptThreeBytesCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100_000L
        );

        List<ScriptChunk> nonStandardErpRedeemScriptChunks = redeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptThreeBytesIncludingSignCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            33_000L // Any value above 32_767 needs an extra byte to indicate the sign
        );

        List<ScriptChunk> nonStandardErpRedeemScriptChunks = redeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptFourBytesCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            10_000_000L
        );

        List<ScriptChunk> nonStandardErpRedeemScriptChunks = redeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptFourBytesIncludingSignCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            8_400_000L // Any value above 8_388_607 needs an extra byte to indicate the sign
        );

        List<ScriptChunk> nonStandardErpRedeemScriptChunks = redeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenFlyoverNonStandardErpRedeemScriptRemovingPrefix_shouldReturnTrue() {
        // Remove fast bridge prefix
        List<ScriptChunk> flyoverNonStandardErpRedeemScriptChunks = flyoverNonStandardErpRedeemScript.getChunks();
        List<ScriptChunk> chunksWithoutFlyoverPrefix = flyoverNonStandardErpRedeemScriptChunks.subList(2, flyoverNonStandardErpRedeemScriptChunks.size());

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(
            chunksWithoutFlyoverPrefix)
        );
    }

    @Test
    public void hasFlyoverPrefix_whenEmptyFlyoverPrefix_shouldReturnFalse() {
        byte[] emptyFlyoverPrefix = {};
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            emptyFlyoverPrefix,
            standardRedeemScript
        );

        List<ScriptChunk> flyoverRedeemScriptChunks = flyoverRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenStandardMultisigRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> standardRedeemScriptChunks = standardRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(standardRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenP2shErpRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> p2shErpRedeemScriptChunks = p2shErpRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(p2shErpRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenScriptSig_shouldReturnFalse() {
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverP2shErpRedeemScript);
        Script scriptSig = p2shOutputScript.createEmptyInputScript(null, flyoverP2shErpRedeemScript);

        List<ScriptChunk> scriptSigChunks = scriptSig.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(scriptSigChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenP2shOutputScript_shouldReturnFalse() {
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverP2shErpRedeemScript);

        List<ScriptChunk> p2shOutputScriptChunks = p2shOutputScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(p2shOutputScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverPrefixAndInvalidScript_shouldReturnTrue() {
        Script invalidScript = new Script(new byte[]{1, 2});

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            invalidScript
        );

        List<ScriptChunk> flyoverRedeemScriptChunks = flyoverRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenZeroFlyoverPrefixAndInvalidScript_shouldReturnTrue() {
        Script invalidScript = new Script(new byte[]{1, 2});

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            Sha256Hash.ZERO_HASH.getBytes(),
            invalidScript
        );

        List<ScriptChunk> flyoverRedeemScriptChunks = flyoverRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverNonStandardErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        Script flyoverNonStandardErpTestnetRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonStandardErpTestnetRedeemScript
        );

        List<ScriptChunk> flyoverNonStandardErpTestnetRedeemScriptChunks = flyoverNonStandardErpTestnetRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverNonStandardErpTestnetRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverStandardMultisigRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverStandardRedeemScriptChunks = flyoverStandardRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverStandardRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverNonStandardErpRedeemScriptChunks = flyoverNonStandardErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverNonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverP2shErpRedeemScriptChunks = flyoverP2shErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverP2shErpRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverNoRedeemScript_shouldReturnFalse() {
        Script emptyScript = new Script(new byte[]{});
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            emptyScript
        ).getChunks();
        Assert.assertFalse(
            RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenEmptyFlyoverDerivationHash_shouldReturnFalse() {
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            new byte[]{},
            standardRedeemScript
        ).getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenScriptSig_shouldReturnFalse() {
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverStandardRedeemScript);
        Script scriptSig = p2shOutputScript.createEmptyInputScript(null, flyoverStandardRedeemScript);

        List<ScriptChunk> scriptSigChunks = scriptSig.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(scriptSigChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenP2shOutputScript_shouldReturnFalse() {
        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverStandardRedeemScript);
        List<ScriptChunk> p2shOutputScriptChunks = p2shOutputScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(p2shOutputScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenZeroFlyoverDerivationHash_shouldReturnTrue() {
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            Sha256Hash.ZERO_HASH.getBytes(),
            standardRedeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverStandardMultisigRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverStandardRedeemScriptChunks = flyoverStandardRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverStandardRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverNonStandardErpRedeemScriptChunks = flyoverNonStandardErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverNonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> flyoverP2shErpRedeemScriptChunks = flyoverP2shErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverP2shErpRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverNonStandardErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        List<ScriptChunk> flyoverNonStandardErpTestnetRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonStandardErpTestnetRedeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverNonStandardErpTestnetRedeemScriptChunks));
    }

    @Test(expected = VerificationException.class)
    public void removeOpCheckMultiSig_whenNonStandardErpRedeemScript_ok() {
        Script nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptValidator.removeOpCheckMultisig(nonStandardErpRedeemScript);
    }

    @Test
    public void removeOpCheckMultiSig_standard_redeem_script() {
        List<ScriptChunk> chunksWithoutOpCheckMultiSig = RedeemScriptValidator.removeOpCheckMultisig(standardRedeemScript);

        Assert.assertEquals(defaultRedeemScriptKeys.size() + 2, chunksWithoutOpCheckMultiSig.size()); // 1 chunk per key + OP_M + OP_N
        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(chunksWithoutOpCheckMultiSig));
    }

    @Test
    public void isOpN_valid_opcode() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_16, null);
        chunk.decodePositiveN();
    }

    @Test
    public void isN_valid_opcode() {
        for (int num = 1; num <= 40; num++) {
            ScriptBuilder builder = new ScriptBuilder();
            builder.number(num);
            Script script = builder.build();
            ScriptChunk chunk = script.getChunks().get(0);
            chunk.decodePositiveN();
        }
    }

    @Test
    public void isOpnN_invalid_opcode() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_DROP, null);
        chunk.decodePositiveN();
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenEmptyScript_shouldReturnFalse() {
        Script emptyScript = new Script(new byte[]{});
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            emptyScript.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenScriptSig_shouldReturnFalse() {
        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(
            p2shErpRedeemScript
        );
        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, p2shErpRedeemScript);
        List<ScriptChunk> scriptSigChunks = scriptSig.getChunks();

        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(scriptSigChunks));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenP2shOutputScript_shouldReturnFalse() {
        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(
            p2shErpRedeemScript
        );
        List<ScriptChunk> p2shOutputScriptChunks = p2SHOutputScript.getChunks();

        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(p2shOutputScriptChunks));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenStandardRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> standardRedeemScriptChunks = standardRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            standardRedeemScriptChunks));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenNonStandardErpRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> nonStandardErpRedeemScriptChunks = nonStandardErpRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            nonStandardErpRedeemScriptChunks));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenNonStandardErpRedeemScriptParserHardcoded_shouldReturnFalse() {
        List<ScriptChunk> nonStandardErpTestnetRedeemScriptChunks = nonStandardErpTestnetRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            nonStandardErpTestnetRedeemScriptChunks));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenFlyoverP2shRedeemScript_shouldReturnFalse() {
        List<ScriptChunk> flyoverP2shErpRedeemScriptChunks = flyoverP2shErpRedeemScript.getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            flyoverP2shErpRedeemScriptChunks));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenP2shRedeemScript_shouldReturnTrue() {
        List<ScriptChunk> p2shErpRedeemScriptChunks = p2shErpRedeemScript.getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            p2shErpRedeemScriptChunks));
    }
}
