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
    private Script nonstandardErpRedeemScript;
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

        nonstandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys, emergencyRedeemScriptKeys, CSV_VALUE);
        flyoverNonStandardErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonstandardErpRedeemScript);

        p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE);
        flyoverP2shErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(), p2shErpRedeemScript);

        nonStandardErpTestnetRedeemScript = new Script(
            NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);
    }

    @Test
    public void isRedeemLikeScript_whenStandardMultisig_shouldReturnTrue() {
        List<ScriptChunk> chunks = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys).getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(chunks));
    }

    @Test
    public void isRedeemLikeScript_whenNonStandardErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        List<ScriptChunk> nonStandardErpTestnetRedeemScriptChunks = ScriptParser.parseScriptProgram(
            NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);

        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(nonStandardErpTestnetRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenNonStandardErpRedeemScriptParser_shouldReturnTrue() {
        List<ScriptChunk> nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE).getChunks();
        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(nonStandardErpRedeemScript));
    }

    @Test
    public void isRedeemLikeScript_whenP2shErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        List<ScriptChunk> p2shRedeemScriptChunks = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE).getChunks();

        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(p2shRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenFlyoverStandardMultisigRedeemScript_shouldReturnTrue() {
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(
            defaultRedeemScriptKeys);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            standardRedeemScript
        ).getChunks();

        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(flyoverRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        Script nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonStandardErpRedeemScript
        ).getChunks();

        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(flyoverRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE);
        List<ScriptChunk> p2shRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            p2shErpRedeemScript
        ).getChunks();

        Assert.assertTrue(RedeemScriptValidator.isRedeemLikeScript(p2shRedeemScriptChunks));
    }

    @Test
    public void isRedeemLikeScript_invalid_redeem_script_missing_checkSig() {
        List<ScriptChunk> chunksWithoutCheckSig = RedeemScriptValidator.removeOpCheckMultisig(
            RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys)
        );

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

        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(redeemScript.getChunks()));
    }

    @Test
    public void hasStandardRedeemScriptStructure_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Assert.assertTrue(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasStandardRedeemScriptStructure_non_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Assert.assertFalse(RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasNonStandardErpRedeemScriptStructure_whenFlyoverRedeemScript_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Assert.assertFalse(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptOneByteCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            10L
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptTwoBytesCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptTwoBytesIncludingSignCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            130L // Any value above 127 needs an extra byte to indicate the sign
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptThreeBytesCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            100_000L
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptThreeBytesIncludingSignCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            33_000L // Any value above 32_767 needs an extra byte to indicate the sign
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptFourBytesCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            10_000_000L
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenNonStandardErpFedRedeemScriptFourBytesIncludingSignCsvValue_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            8_400_000L // Any value above 8_388_607 needs an extra byte to indicate the sign
        );

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(redeemScript.getChunks()));
    }

    @Test
    public void hasErpRedeemScriptStructure_whenFlyoverNonStandardErpRedeemScriptRemovingPrefix_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        // Remove fast bridge prefix
        List<ScriptChunk> chunks = flyoverRedeemScript.getChunks();
        List<ScriptChunk> chunksWithoutFlyoverPrefix = chunks.subList(2, chunks.size());

        Assert.assertTrue(RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(
            chunksWithoutFlyoverPrefix)
        );
    }

    @Test
    public void hasFlyoverPrefix_whenEmptyFlyoverPrefix_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);

        byte[] emptyFlyoverPrefix = {};
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            emptyFlyoverPrefix,
            redeemScript
        );

        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenStandardMultisigRedeemScript_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(redeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenP2shErpRedeemScript_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(redeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenScriptSig_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        Script scriptSig = p2shOutputScript.createEmptyInputScript(null, flyoverRedeemScript);

        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(scriptSig.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenP2shOutputScript_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        Assert.assertFalse(RedeemScriptValidator.hasFlyoverPrefix(p2shOutputScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverPrefixAndInvalidScript_shouldReturnTrue() {
        Script invalidScript = new Script(new byte[]{1, 2});

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            invalidScript
        );

        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenZeroFlyoverPrefixAndInvalidScript_shouldReturnTrue() {
        Script invalidScript = new Script(new byte[]{1, 2});

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            Sha256Hash.ZERO_HASH.getBytes(),
            invalidScript
        );

        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverNonStandardErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        Script redeemScript = new Script(NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverStandardMultisigRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        Script nonStandardErpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonStandardErpRedeemScript
        );

        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
    }

    @Test
    public void hasFlyoverPrefix_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            CSV_VALUE
        );

        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Assert.assertTrue(RedeemScriptValidator.hasFlyoverPrefix(flyoverRedeemScript.getChunks()));
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
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            new byte[]{},
            redeemScript
        ).getChunks();
        Assert.assertFalse(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenScriptSig_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);
        Script scriptSig = p2shOutputScript.createEmptyInputScript(null, flyoverRedeemScript);

        Assert.assertFalse(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(scriptSig.getChunks()));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenP2shOutputScript_shouldReturnFalse() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        );

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        Assert.assertFalse(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(p2shOutputScript.getChunks()));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenZeroFlyoverDerivationHash_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            Sha256Hash.ZERO_HASH.getBytes(),
            redeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverStandardMultisigRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverNonStandardErpRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverP2shErpRedeemScript_shouldReturnTrue() {
        Script redeemScript = RedeemScriptUtils.createP2shErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys, CSV_VALUE);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            redeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test
    public void hasFlyoverRedeemScriptStructure_whenFlyoverNonStandardErpRedeemScriptParserHardcoded_shouldReturnTrue() {
        Script nonStandardErpTestnetRedeemScript = new Script(NON_STANDARD_ERP_TESTNET_REDEEM_SCRIPT_SERIALIZED);
        List<ScriptChunk> flyoverRedeemScriptChunks = RedeemScriptUtils.createFlyoverRedeemScript(
            FLYOVER_DERIVATION_HASH.getBytes(),
            nonStandardErpTestnetRedeemScript
        ).getChunks();
        Assert.assertTrue(RedeemScriptValidator.hasFlyoverRedeemScriptStructure(flyoverRedeemScriptChunks));
    }

    @Test(expected = VerificationException.class)
    public void removeOpCheckMultiSig_whenNonStandardErpRedeemScript_ok() {
        Script redeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(
            defaultRedeemScriptKeys,
            emergencyRedeemScriptKeys,
            500L
        );

        RedeemScriptValidator.removeOpCheckMultisig(redeemScript);
    }

    @Test
    public void removeOpCheckMultiSig_standard_redeem_script() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        List<ScriptChunk> chunks = RedeemScriptValidator.removeOpCheckMultisig(redeemScript);

        Assert.assertEquals(defaultRedeemScriptKeys.size() + 2, chunks.size()); // 1 chunk per key + OP_M + OP_N
        Assert.assertFalse(RedeemScriptValidator.isRedeemLikeScript(chunks));
    }

    @Test
    public void isOpN_valid_opcode() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_2, null);
        Assert.assertTrue(RedeemScriptValidator.isOpN(chunk));
    }

    @Test
    public void isOpnN_invalid_opcode() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_DROP, null);
        Assert.assertFalse(RedeemScriptValidator.isOpN(chunk));
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

        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            scriptSig.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenP2shOutputScript_shouldReturnFalse() {
        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(
            p2shErpRedeemScript
        );

        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            p2SHOutputScript.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenStandardRedeemScript_shouldReturnFalse() {
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            standardRedeemScript.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenNonStandardErpRedeemScript_shouldReturnFalse() {
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            nonstandardErpRedeemScript.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenNonStandardErpRedeemScriptParserHardcoded_shouldReturnFalse() {
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            nonStandardErpTestnetRedeemScript.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenFlyoverP2shRedeemScript_shouldReturnFalse() {
        Assert.assertFalse(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            flyoverP2shErpRedeemScript.getChunks()));
    }

    @Test
    public void hasP2shErpRedeemScriptStructure_whenP2shRedeemScript_shouldReturnTrue() {
        Assert.assertTrue(RedeemScriptValidator.hasP2shErpRedeemScriptStructure(
            p2shErpRedeemScript.getChunks()));
    }
}
