package co.rsk.bitcoinj.script;

import java.util.List;

public class FastBridgeRedeemScriptParser extends StandardRedeemScriptParser {

    protected final byte[] derivationHash;

    public FastBridgeRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            redeemScriptChunks.subList(2, redeemScriptChunks.size())
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_MULTISIG;
        this.derivationHash = redeemScriptChunks.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static List<ScriptChunk> extractStandardRedeemScript(Script redeemScript) {
        return (ScriptBuilder.createRedeemScript(
            redeemScript.getNumberOfSignaturesRequiredToSpend(),
            redeemScript.getPubKeys()
        )).getChunks();
    }

    public static boolean isFastBridgeMultiSig(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasStandardRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
