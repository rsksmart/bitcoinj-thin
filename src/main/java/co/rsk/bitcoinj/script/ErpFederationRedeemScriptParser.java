package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErpFederationRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(ErpFederationRedeemScriptParser.class);

    public ErpFederationRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            extractStandardRedeemScript(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.ERP_FED;
    }

    public static List<ScriptChunk> extractStandardRedeemScript(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        chunksForRedeem.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from ERP redeem script has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunksFromErpRedeemScript] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return (new Script(chunksForRedeem)).getChunks();
    }

    public static boolean isErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasErpRedeemScriptStructure(chunks);
    }
}
