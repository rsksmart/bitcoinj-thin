package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FastBridgeParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeParser.class);
    protected final byte[] derivationHash;

    public FastBridgeParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks,
        List<ScriptChunk> rawChunks
    ) {
        super(
            scriptType,
            redeemScriptChunks,
            rawChunks
        );

        // the idea of this was to not have to call the extract function and check the type again
        // so when we call an instance of FastBridgeParser, the multisigType and the redeemScriptChunks are calculated asap
        // and when we call just the extract function from outside, it does the multisig type verification

        List<ScriptChunk> subChunks = redeemScriptChunks.subList(2, redeemScriptChunks.size());
        if (isFastBridgeMultiSig(redeemScriptChunks)) {
            this.multiSigType = MultiSigType.FAST_BRIDGE_MULTISIG;
            this.redeemScriptChunks = StandardRedeemScriptParser
                .extractStandardRedeemScript(subChunks).getChunks();
        }
        if (isFastBridgeErpFed(redeemScriptChunks)) {
            this.multiSigType = MultiSigType.FAST_BRIDGE_ERP_FED;
            this.redeemScriptChunks = ErpFederationRedeemScriptParser.
                extractStandardRedeemScript(subChunks).getChunks();
        }
        if (isFastBridgeP2shErpFed(redeemScriptChunks)) {
            this.multiSigType = MultiSigType.FAST_BRIDGE_P2SH_ERP_FED;
            this.redeemScriptChunks = P2shErpFederationRedeemScriptParser.
                extractStandardRedeemScript(subChunks).getChunks();
        }
        this.derivationHash = redeemScriptChunks.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static Script extractStandardRedeemScript(List<ScriptChunk> chunks) {
        if (isFastBridgeMultiSig(chunks)) {
            return StandardRedeemScriptParser
                .extractStandardRedeemScript(chunks.subList(2, chunks.size()));
        }
        if (isFastBridgeErpFed(chunks)) {
            return ErpFederationRedeemScriptParser.
                extractStandardRedeemScript(chunks.subList(2, chunks.size()));
        }
        if (isFastBridgeP2shErpFed(chunks)) {
            return P2shErpFederationRedeemScriptParser.
                extractStandardRedeemScript(chunks.subList(2, chunks.size()));
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
