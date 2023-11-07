package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.hasFastBridgePrefix;
import static co.rsk.bitcoinj.script.RedeemScriptValidator.hasLegacyErpRedeemScriptStructure;

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

    // TODO refactor this

    public static Script extractStandardMultisigRedeemScriptFromCustomRedeemScript(List<ScriptChunk> chunks) {
        if (hasFastBridgePrefix(chunks)) {
            return extractStandardRedeemScriptFromFlyoverRedeemScriptChunks(chunks);
        }
        return extractStandardRedeemScriptFromErpRedeemScriptChunks(chunks);
    }

    public static Script extractScriptWithoutFlyoverOpcodesFromFlyoverRedeemScript(Script redeemScript) {
        List<ScriptChunk> chunks = redeemScript.getChunks();
        List<ScriptChunk> chunksWithoutFlyover = chunks.subList(2, chunks.size());
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .addChunks(chunksWithoutFlyover)
            .build();
    }

    public static Script extractScriptWithoutFlyoverOpcodesFromFlyoverRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksWithoutFlyover = chunks.subList(2, chunks.size());
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .addChunks(chunksWithoutFlyover)
            .build();
    }

    public static Script extractStandardRedeemScriptFromFlyoverRedeemScriptChunks(List<ScriptChunk> chunks) {
        Script insideRedeemScript = extractScriptWithoutFlyoverOpcodesFromFlyoverRedeemScriptChunks(chunks);
        if(!insideRedeemScript.isSentToMultiSig()) {
            insideRedeemScript = extractStandardRedeemScriptFromErpRedeemScript(insideRedeemScript);
        }
        return insideRedeemScript;
    }

    public static Script extractStandardRedeemScriptFromFlyoverRedeemScript(Script redeemScript) {
        Script insideRedeemScript = extractScriptWithoutFlyoverOpcodesFromFlyoverRedeemScript(redeemScript);
        if(!insideRedeemScript.isSentToMultiSig()) {
            insideRedeemScript = extractStandardRedeemScriptFromErpRedeemScript(insideRedeemScript);
        }
        return insideRedeemScript;
    }

    public static Script extractStandardRedeemScriptFromFlyoverErpRedeemScript(Script redeemScript) {
        List<ScriptChunk> chunks = redeemScript.getChunks();
        return extractStandardRedeemScriptFromErpRedeemScriptChunks(chunks.subList(2, chunks.size()));
    }

    public static Script extractStandardRedeemScriptFromErpRedeemScript(Script redeemScript) {
        return extractStandardRedeemScriptFromErpRedeemScriptChunks(redeemScript.getChunks());
    }

    public static Script extractStandardRedeemScriptFromErpRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForStandardRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForStandardRedeem.add(chunks.get(i));
            i++;
        }

        if (hasLegacyErpRedeemScriptStructure(chunks)) {
            chunksForStandardRedeem.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        Script standardRedeemScript = scriptBuilder.addChunks(chunksForStandardRedeem).build();
        // Validate the obtained redeem script has a valid format
        if (!standardRedeemScript.isSentToMultiSig()) {
            String message = "Standard redeem script obtained from ERP redeem script has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunksFromErpRedeemScript] {} {}", message, chunksForStandardRedeem);
            throw new VerificationException(message);
        }

        return new Script(chunksForStandardRedeem);
    }
}
