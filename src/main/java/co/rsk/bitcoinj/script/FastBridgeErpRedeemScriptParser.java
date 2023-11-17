package co.rsk.bitcoinj.script;

import java.util.List;

public class FastBridgeErpRedeemScriptParser extends StandardRedeemScriptParser {

    public FastBridgeErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            extractStandardRedeemScript(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_ERP_FED;
    }

    public static List<ScriptChunk> extractStandardRedeemScript(List<ScriptChunk> chunks) {
        return (ErpFederationRedeemScriptParser.
            extractStandardRedeemScript(chunks.subList(2, chunks.size())));
    }

    public static boolean isFastBridgeErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
