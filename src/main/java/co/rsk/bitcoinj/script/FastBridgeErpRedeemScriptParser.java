package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastBridgeErpRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeErpRedeemScriptParser.class);

    public FastBridgeErpRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            scriptType,
            extractStandardRedeemScriptChunks(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_ERP_FED;
    }

    public static List<ScriptChunk> extractStandardRedeemScriptChunks(List<ScriptChunk> chunks) {
        return ErpFederationRedeemScriptParser.extractStandardRedeemScriptChunks(chunks.subList(2, chunks.size()));
    }

    public static Script createFastBridgeErpRedeemScript(
        Script erpRedeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        if (!RedeemScriptValidator.hasErpRedeemScriptStructure(erpRedeemScript.getChunks())) {
            String message = "Provided redeem script has not ERP structure";
            logger.debug("[createFastBridgeErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        List<ScriptChunk> chunks = erpRedeemScript.getChunks();
        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationArgumentsHash.getBytes())
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(chunks)
            .build();
    }

    public static boolean isFastBridgeErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
