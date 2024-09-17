package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastBridgeErpRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeErpRedeemScriptParser.class);

    public FastBridgeErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            extractStandardRedeemScriptChunks(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_ERP_FED;
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

        chunksForRedeem.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from ERP redeem script has an invalid structure";
            logger.debug("[extractDefaultRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
    }

    public static Script createFastBridgeErpRedeemScript(
        Script erpRedeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        if (!RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(erpRedeemScript.getChunks())) {
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
            RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
