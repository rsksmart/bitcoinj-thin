package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FlyoverRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);
    protected final byte[] derivationHash;

    public FlyoverRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks,
        List<ScriptChunk> rawChunks
    ) {
        super(
            scriptType,
            redeemScriptChunks,
            rawChunks
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE;
        this.derivationHash = redeemScriptChunks.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public Script extractStandardRedeemScript(List<ScriptChunk> chunks) {

        List<ScriptChunk> chunksWithoutFlyover = chunks.subList(2, chunks.size());

        if (isMultiSig(chunksWithoutFlyover)) {
            return StandardRedeemScriptParser
                .extractStandardRedeemScript();
        }
        if (isErpFed(chunksWithoutFlyover)) {
            return ErpFederationRedeemScriptParser.
                extractStandardRedeemScript(chunksWithoutFlyover);
        }
        if (isP2shErpFed(chunksWithoutFlyover)) {
            return P2shErpFederationRedeemScriptParser.
                extractStandardRedeemScript(chunksWithoutFlyover);
        }

        String message = "Provided redeem script has unknown structure";
        logger.debug("[FastBridgeParser] {}", message);
        throw new VerificationException(message);
    }

    public static Script createFastBridgeRedeemScript(
        Script redeemScript,
        Sha256Hash derivationArgumentsHash
    ) {

        List<ScriptChunk> chunks = redeemScript.getChunks();

        if (RedeemScriptValidator.hasFastBridgePrefix(chunks)) {
            String message = "Provided redeem script is already a fast bridge redeem script";
            logger.debug("[createFastBridgeRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        if (derivationArgumentsHash == null || derivationArgumentsHash.equals(Sha256Hash.ZERO_HASH)) {
            String message = "Derivation arguments are not valid";
            logger.debug("[createFastBridgeRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationArgumentsHash.getBytes())
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(chunks)
            .build();
    }

    public boolean isMultiSig(List<ScriptChunk> chunksWithoutFlyover) {
        return RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksWithoutFlyover);
    }
    public boolean isErpFed(List<ScriptChunk> chunksWithoutFlyover) {
        return RedeemScriptValidator.hasErpRedeemScriptStructure(chunksWithoutFlyover);
    }
    public boolean isP2shErpFed(List<ScriptChunk> chunksWithoutFlyover) {
        return RedeemScriptValidator.hasP2shErpRedeemScriptStructure(chunksWithoutFlyover);
    }

    public static boolean isFastBridgeMultiSig(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasStandardRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }

    public static boolean isFastBridgeErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }

    public static boolean isFastBridgeP2shErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasP2shErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
