package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FastBridgeP2shErpRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeP2shErpRedeemScriptParser.class);

    public FastBridgeP2shErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            extractStandardRedeemScript(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_P2SH_ERP_FED;
    }

    public static List<ScriptChunk> extractStandardRedeemScript(List<ScriptChunk> chunks) {
        return P2shErpFederationRedeemScriptParser.
            extractStandardRedeemScript(chunks.subList(2, chunks.size()));
    }

    public static Script createFastBridgeP2shErpRedeemScript(
        Script p2shErpRedeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        if (!RedeemScriptValidator.hasP2shErpRedeemScriptStructure(p2shErpRedeemScript.getChunks())) {
            String message = "Provided redeem script has not p2sh ERP structure";
            logger.debug("[createFastBridgeP2shErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        List<ScriptChunk> chunks = p2shErpRedeemScript.getChunks();
        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationArgumentsHash.getBytes())
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(chunks)
            .build();
    }

    public static boolean isFastBridgeP2shErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasP2shErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
