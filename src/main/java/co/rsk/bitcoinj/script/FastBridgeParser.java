package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FastBridgeParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeParser.class);
    private static MultiSigType type = null;
    protected final byte[] derivationHash;

    public FastBridgeParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks,
        List<ScriptChunk> rawChunks
    ) {
        super(
            scriptType,
            extractStandardRedeemScript(redeemScriptChunks).getChunks(),
            rawChunks
        );
        //this.multiSigType = determineMultiSigType();
        this.derivationHash = redeemScriptChunks.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }
    public static Script extractStandardRedeemScript(List<ScriptChunk> chunks) {
        if (isFastBridgeMultiSig(chunks)) {
            type = MultiSigType.FAST_BRIDGE_MULTISIG;
            return new Script(chunks);
        }
        if (isFastBridgeErpFed(chunks)) {
            type = MultiSigType.FAST_BRIDGE_ERP_FED;
            return ErpFederationRedeemScriptParser.
                extractStandardRedeemScript(chunks.subList(2, chunks.size()));
        }
        if (isFastBridgeP2shErpFed(chunks)) {
            type = MultiSigType.FAST_BRIDGE_P2SH_ERP_FED;
            return P2shErpFederationRedeemScriptParser.
                extractStandardRedeemScript(chunks.subList(2, chunks.size()));
        }

        String message = "Provided redeem script has unknown structure";
        logger.debug("[FastBridgeParser] {}", message);
        throw new VerificationException(message);
    }

    private MultiSigType determineMultiSigType() {
        return type;
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
