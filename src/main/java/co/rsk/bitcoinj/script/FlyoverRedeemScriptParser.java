package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlyoverRedeemScriptParser implements RedeemScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);

    private final MultiSigType multiSigType;
    private final List<ScriptChunk> redeemScriptChunks;
    private final byte[] derivationHash;
    private final RedeemScriptParser redeemScriptParser;

    public FlyoverRedeemScriptParser(List<ScriptChunk> redeemScriptChunks) {
        this.multiSigType = MultiSigType.FLYOVER;
        this.redeemScriptChunks = redeemScriptChunks;
        this.derivationHash = extractDerivationHash();
        this.redeemScriptParser = RedeemScriptParserFactory.get(extractRedeemScript());
    }

    public static Script createFlyoverRedeemScript(Script redeemScript, byte[] derivationHash) {
        final String CREATE_FLYOVER_REDEEM_SCRIPT_TAG = "createFlyoverRedeemScript";

        List<ScriptChunk> chunks = redeemScript.getChunks();
        if (RedeemScriptValidator.hasFastBridgePrefix(chunks)) {
            String message = "Provided Redeem Script is already a fast bridge redeem script";
            logger.debug("[{}] {}", CREATE_FLYOVER_REDEEM_SCRIPT_TAG, message);
            throw new VerificationException(message);
        }

        if (derivationHash == null || Arrays.equals((derivationHash), (Sha256Hash.ZERO_HASH).getBytes())) {
            String message = "Derivation arguments are not valid";
            logger.debug("[{}] {}", CREATE_FLYOVER_REDEEM_SCRIPT_TAG, message);
            throw new VerificationException(message);
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationHash)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(chunks)
            .build();
    }

    private byte[] extractDerivationHash() {
        final String EXTRACT_DERIVATION_HASH_TAG = "extractDerivationHash";

        if (!RedeemScriptValidator.hasFastBridgePrefix(redeemScriptChunks)) {
            String message = "Provided redeem script is not a flyover redeem script";
            logger.debug("[{}] {}", EXTRACT_DERIVATION_HASH_TAG, message);
            throw new VerificationException(message);
        }

        return redeemScriptChunks.get(1).data;
    }

    private List<ScriptChunk> extractRedeemScript() {
        final String EXTRACT_REDEEM_SCRIPT_TAG = "extractRedeemScript";
        if (!RedeemScriptValidator.hasFastBridgePrefix(redeemScriptChunks)) {
            String message = "Provided redeem script is not a flyover redeem script";
            logger.debug("[{}] {}", EXTRACT_REDEEM_SCRIPT_TAG, message);
            throw new VerificationException(message);
        }

        return redeemScriptChunks.subList(2, redeemScriptChunks.size());
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    @Override
    public MultiSigType getMultiSigType() {
        return multiSigType;
    }

    @Override
    public int getM() {
        return redeemScriptParser.getM();
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        return redeemScriptParser.findKeyInRedeem(key);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        return redeemScriptParser.getPubKeys();
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        return redeemScriptParser.findSigInRedeem(signatureBytes, hash);
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScript() {
        return redeemScriptParser.extractStandardRedeemScript();
    }
}
