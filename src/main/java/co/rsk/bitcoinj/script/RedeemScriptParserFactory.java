package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.script.RedeemScriptParser.ScriptType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedeemScriptParserFactory {
    private static final byte[] ERP_TESTNET_REDEEM_SCRIPT_BYTES = Utils.HEX.decode("6453210208f40073a9e43b3e9103acec79767a6de9b0409749884e989960fee578012fce210225e892391625854128c5c4ea4340de0c2a70570f33db53426fc9c746597a03f42102afc230c2d355b1a577682b07bc2646041b5d0177af0f98395a46018da699b6da210344a3c38cd59afcba3edcebe143e025574594b001700dec41e59409bdbd0f2a0921039a060badbeb24bee49eb2063f616c0f0f0765d4ca646b20a88ce828f259fcdb955670300cd50b27552210216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3210275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f1421034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f5368ae");
    private static final Logger logger = LoggerFactory.getLogger(RedeemScriptParserFactory.class);

    public static RedeemScriptParser get(List<ScriptChunk> chunks) {
        // Due to a validation error, during the time this federation existed in testnet
        // bitcoinj-thin would not detect it correctly as an ERP fed
        // We need to keep this behaviour for the given redeem script to keep the consensus in testnet
        ScriptParserResult scriptParserResult = ScriptParser.parseScriptProgram(ERP_TESTNET_REDEEM_SCRIPT_BYTES);
        if (scriptParserResult.getChunks().equals(chunks)) {
            logger.debug("[get] Received redeem script matches the testnet federation hardcoded one. Return NoRedeemScriptParser");
            return new NoRedeemScriptParser();
        }

        if (chunks.size() < 4) {
            // A multisig redeem script must have at least 4 chunks (OP_N [PUB1 ...] OP_N CHECK_MULTISIG)
            logger.trace("[get] Less than 4 chunks, return NoRedeemScriptParser");
            return new NoRedeemScriptParser();
        }

        ParseResult result = extractRedeemScriptFromChunks(chunks);

        if (result == null) {
            return new NoRedeemScriptParser();
        }

        if (FastBridgeParser.isFastBridgeMultiSig(result.internalScript)) {
            logger.debug("[get] Return FastBridgeRedeemScriptParser");
            return new FastBridgeParser(
                result.scriptType,
                RedeemScriptParser.MultiSigType.FAST_BRIDGE_MULTISIG,
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
        if (P2shErpFederationRedeemScriptParser.isP2shErpFed(result.internalScript)) {
            logger.debug("[get] Return P2shErpFederationRedeemScriptParser");
            return new P2shErpFederationRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        }
        if (FastBridgeParser.isFastBridgeP2shErpFed(result.internalScript)) {
            logger.debug("[get] Return FastBridgeP2shErpRedeemScriptParser");
            return new FastBridgeParser(
                result.scriptType,
                RedeemScriptParser.MultiSigType.FAST_BRIDGE_P2SH_ERP_FED,
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
        if (FastBridgeParser.isFastBridgeErpFed(result.internalScript)) {
            logger.debug("[get] Return FastBridgeErpRedeemScriptParser");
            return new FastBridgeParser(
                result.scriptType,
                RedeemScriptParser.MultiSigType.FAST_BRIDGE_ERP_FED,
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
            // ERP and standard (+fastBridge) finish with OP_CHECKMULTISIG and P2SHERP (+fastBridge) finish with OP_ENDIF
            if (
                    lastByte == ScriptOpCodes.OP_CHECKMULTISIG ||
                    lastByte == ScriptOpCodes.OP_CHECKMULTISIGVERIFY ||
                    lastByte == ScriptOpCodes.OP_ENDIF
            ) {
                ScriptParserResult result = ScriptParser.parseScriptProgram(lastChunk.data);
                if (result.getException().isPresent()) {
                    String message = String.format("Error trying to parse inner script. %s", result.getException().get());
                    logger.debug("[extractRedeemScriptFromChunks] {}", message);
                    throw new ScriptException(message);
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
