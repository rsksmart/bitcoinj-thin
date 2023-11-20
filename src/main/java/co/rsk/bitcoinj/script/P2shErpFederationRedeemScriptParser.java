package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class P2shErpFederationRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(P2shErpFederationRedeemScriptParser.class);

    public P2shErpFederationRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            extractStandardRedeemScript(redeemScriptChunks)
        );
        this.multiSigType = MultiSigType.P2SH_ERP_FED;
    }

    public static List<ScriptChunk> extractStandardRedeemScript(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from P2SH ERP redeem script has an invalid structure";
            logger.debug("[extractStandardRedeemScript] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return (new Script(chunksForRedeem)).getChunks();
    }

    public static boolean isP2shErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasP2shErpRedeemScriptStructure(chunks);
    }
}
