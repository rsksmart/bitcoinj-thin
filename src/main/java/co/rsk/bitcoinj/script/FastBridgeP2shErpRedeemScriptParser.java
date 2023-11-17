package co.rsk.bitcoinj.script;

import java.util.List;

public class FastBridgeP2shErpRedeemScriptParser extends StandardRedeemScriptParser {

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

    public static boolean isFastBridgeP2shErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasP2shErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
