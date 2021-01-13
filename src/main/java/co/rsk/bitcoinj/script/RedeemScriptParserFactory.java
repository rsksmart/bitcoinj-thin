package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIG;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIGVERIFY;

import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.script.RedeemScriptParser.ScriptType;
import java.util.List;

public class RedeemScriptParserFactory {

    public static RedeemScriptParser get(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) {
            // A multisig redeem script must have at least 4 chunks (OP_N [PUB1 ...] OP_N CHECK_MULTISIG)
            return new NoRedeemScriptParser();
        }

        ParseResult result = extractRedeemScriptFromChunks(chunks);

        if (result == null) {
            return new NoRedeemScriptParser();
        }

        if (internalIsFastBridgeMultiSig(result.internalScript)) {
            return new FastBridgeRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        } else if (internalIsStandardMultiSig(result.internalScript)) {
            return new StandardRedeemScriptParser(
                result.scriptType,
                result.internalScript,
                chunks
            );
        }
        return new NoRedeemScriptParser();
    }

    private static ParseResult extractRedeemScriptFromChunks(List<ScriptChunk> chunks) {
        ScriptChunk lastChunk = chunks.get(chunks.size() - 1);
        if (isRedeemLikeScript(chunks)) {
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
        return null;
    }

    private static boolean isRedeemLikeScript(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) return false;
        ScriptChunk chunk = chunks.get(chunks.size() - 1);
        // Must end in OP_CHECKMULTISIG[VERIFY]
        if (!chunk.isOpCode()) return false;
        return chunk.equalsOpCode(OP_CHECKMULTISIG) || chunk.equalsOpCode(OP_CHECKMULTISIGVERIFY);
    }

    private static boolean hasRedeemScriptFormat(List<ScriptChunk> chunks) {
        try {
            // Second to last chunk must be an OP_N opcode and there should be
            // that many data chunks (keys).
            ScriptChunk n = chunks.get(chunks.size() - 2);
            if (!n.isOpCode()) return false;
            int numKeys = Script.decodeFromOpN(n.opcode);
            if (numKeys < 1 || chunks.size() != 3 + numKeys) return false;
            for (int i = 1; i < chunks.size() - 2; i++) {
                if (chunks.get(i).isOpCode()) return false;
            }
            // First chunk must be an OP_N opcode too.
            if (Script.decodeFromOpN(chunks.get(0).opcode) < 1) return false;
        } catch (IllegalStateException e) {
            return false;   // Not an OP_N opcode.
        }
        return true;
    }

    private static boolean internalIsFastBridgeMultiSig(List<ScriptChunk> chunks) {
        if (!isRedeemLikeScript(chunks)) {
            return false;
        }

        ScriptChunk firstChunk = chunks.get(0);

        if (firstChunk.data == null) {
            return false;
        }

        boolean hasFastBridgePrefix =
            firstChunk.opcode == 32 && firstChunk.data.length == 32 &&
                chunks.get(1).opcode == ScriptOpCodes.OP_DROP;

        if (!hasFastBridgePrefix) {
            return false;
        }
        return hasRedeemScriptFormat(chunks.subList(2, chunks.size()));
    }

    private static boolean internalIsStandardMultiSig(List<ScriptChunk> chunks) {
        return isRedeemLikeScript(chunks) && hasRedeemScriptFormat(chunks);
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
