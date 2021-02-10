package co.rsk.bitcoinj.script;

import java.util.List;

public class FastBridgeErpRedeemScriptParser extends StandardRedeemScriptParser {

    public FastBridgeErpRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScript,
        List<ScriptChunk> chunks
    ) {
        super(
            scriptType,
            extractStandardRedeemScriptFromFastBridgeErpRedeemScript(redeemScript),
            chunks
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_ERP_FED;
    }

    public static List<ScriptChunk> extractStandardRedeemScriptFromFastBridgeErpRedeemScript(
        List<ScriptChunk> chunks
    ) {
        return ErpFederationRedeemScriptParser.
            extractStandardRedeemScriptChunksFromErpRedeemScript(chunks.subList(2, chunks.size()));
    }

    public static boolean isFastBridgeErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
