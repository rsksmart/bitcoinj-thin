package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedeemScriptUtils {
    private static final Logger logger = LoggerFactory.getLogger(RedeemScriptUtils.class);

    private RedeemScriptUtils() { }
    public static Optional<Script> extractRedeemScriptFromInputScript(Script inputScript) {
        List<ScriptChunk> chunks = inputScript.getChunks();

        if (chunks == null || chunks.isEmpty()) {
            return Optional.empty();
        }

        byte[] program = chunks.get(chunks.size() - 1).data;
        if (program == null) {
            return Optional.empty();
        }

        try {
            Script redeemScript = new Script(program);
            return Optional.of(redeemScript);
        } catch (ScriptException e) {
            logger.debug(
                "[extractRedeemScriptFromInput] Failed to extract redeem script from inputScript {}. {}",
                inputScript,
                e.getMessage()
            );
            return Optional.empty();
        }
    }
}
