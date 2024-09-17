package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FastBridgeP2shErpRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeP2shErpRedeemScriptParser.class);

    public FastBridgeP2shErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            extractStandardRedeemScriptChunks(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_P2SH_ERP_FED;
    }

    /**
     * @deprecated This method will be removed once FlyoverRedeemScriptParser is implemented.
     *
     */
    @Deprecated
    public static List<ScriptChunk> extractStandardRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> redeemScriptChunks = chunks.subList(2, chunks.size());

        List<ScriptChunk> chunksForRedeem = new ArrayList<>();
        int i = 1;
        while (i < redeemScriptChunks.size() && !redeemScriptChunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(redeemScriptChunks.get(i));
            i++;
        }

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from P2SH ERP redeem script has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
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
