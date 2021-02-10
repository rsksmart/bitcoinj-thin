package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIG;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIGVERIFY;

import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.script.RedeemScriptParser.ScriptType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedeemScriptParserFactory {

    private static final Logger logger = LoggerFactory.getLogger(RedeemScriptParserFactory.class);

    public static RedeemScriptParser get(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) {
            // A multisig redeem script must have at least 4 chunks (OP_N [PUB1 ...] OP_N CHECK_MULTISIG)
            logger.debug("[get] Less than 4 chunks, return NoRedeemScriptParser");
            return new NoRedeemScriptParser();
        }

        ParseResult result = extractRedeemScriptFromChunks(chunks);

        if (result == null) {
            return new NoRedeemScriptParser();
        }

        if (FastBridgeRedeemScriptParser.isFastBridgeMultiSig(result.internalScript)) {
            logger.debug("[get] Return FastBridgeRedeemScriptParser");
            return new FastBridgeRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        }
        if (StandardRedeemScriptParser.isStandardMultiSig(result.internalScript)) {
            logger.debug("[get] Return StandardRedeemScriptParser");
            return new StandardRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        }
        if (ErpFederationRedeemScriptParser.isErpFed(result.internalScript)) {
            logger.debug("[get] Return ErpFederationRedeemScriptParser");
            return new ErpFederationRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        }
        if (FastBridgeErpRedeemScriptParser.isFastBridgeErpFed(result.internalScript)) {
            logger.debug("[get] Return FastBridgeErpRedeemScriptParser");
            return new FastBridgeErpRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        }

        logger.debug("[get] Return NoRedeemScriptParser");
        return new NoRedeemScriptParser();
    }

    private static ParseResult extractRedeemScriptFromChunks(List<ScriptChunk> chunks) {
        ScriptChunk lastChunk = chunks.get(chunks.size() - 1);
        if (RedeemScriptValidator.isRedeemLikeScript(chunks)) {
            return new ParseResult(chunks, ScriptType.REDEEM_SCRIPT);
        }
        if (lastChunk.data != null && lastChunk.data.length > 0) {
            int lastByte = lastChunk.data[lastChunk.data.length - 1] & 0xff;
            if (lastByte == OP_CHECKMULTISIG || lastByte == OP_CHECKMULTISIGVERIFY) {
                ScriptParserResult result = ScriptParser.parseScriptProgram(lastChunk.data);
                if (result.getException().isPresent()) {
                    throw new ScriptException("Error trying to parse inner script", result.getException().get());
                }
                return new ParseResult(result.getChunks(), ScriptType.P2SH);
            }
        }
        logger.debug("[extractRedeemScriptFromChunks] Could not get redeem script from given chunks");
        return null;
    }

    private static class ParseResult {
        public final List<ScriptChunk> internalScript;
        public final ScriptType scriptType;

        public ParseResult(List<ScriptChunk> internalScript, ScriptType scriptType) {
            this.internalScript = internalScript;
            this.scriptType = scriptType;
        }
    }
}
