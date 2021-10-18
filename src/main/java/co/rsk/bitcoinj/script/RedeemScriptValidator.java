package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIG;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIGVERIFY;

import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.List;

public class RedeemScriptValidator {

    public static Long extractCsvValue(List<ScriptChunk> chunks) {
        for (int i = 1; i < chunks.size(); i++) {
            ScriptChunk chunk = chunks.get(i);
            if (chunk.isOpCode() && chunk.equalsOpCode(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)) {
                ScriptChunk csvValueChunk = chunks.get(i - 1); // CSV value goes right before CSV op code
                return new BigInteger(csvValueChunk.data).longValue();
            }
        }

        throw new VerificationException("No CSV value found");
    }

    protected static boolean isRedeemLikeScript(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) {
            return false;
        }

        ScriptChunk lastChunk = chunks.get(chunks.size() - 1);
        // Must end in OP_CHECKMULTISIG[VERIFY]
        return lastChunk.isOpCode() &&
            (lastChunk.equalsOpCode(OP_CHECKMULTISIG) || lastChunk.equalsOpCode(OP_CHECKMULTISIGVERIFY));
    }

    protected static boolean hasStandardRedeemScriptStructure(List<ScriptChunk> chunks) {
        try {
            if (!isRedeemLikeScript(chunks)) {
                return false;
            }

            // Second to last chunk must be an OP_N opcode and there should be
            // that many data chunks (keys).
            ScriptChunk secondToLastChunk = chunks.get(chunks.size() - 2);
            if (!isOpN(secondToLastChunk)) {
                return false;
            }

            int numKeys = Script.decodeFromOpN(secondToLastChunk.opcode);
            if (numKeys < 1 || chunks.size() != numKeys + 3) { // numKeys + M + N + OP_CHECKMULTISIG
                return false;
            }

            for (int i = 1; i < chunks.size() - 2; i++) {
                if (chunks.get(i).isOpCode()) { // Should be the public keys, not op_codes
                    return false;
                }
            }
            // First chunk must be an OP_N opcode too.
            if (!isOpN(chunks.get(0))) {
                return false;
            }
        } catch (IllegalStateException e) {
            return false;   // Not an OP_N opcode.
        }
        return true;
    }

    protected static boolean hasErpRedeemScriptStructure(List<ScriptChunk> chunks) {
        if (!isRedeemLikeScript(chunks)) {
            return false;
        }

        ScriptChunk firstChunk = chunks.get(0);

        boolean hasErpPrefix = firstChunk.opcode == ScriptOpCodes.OP_NOTIF;
        boolean hasEndIfOpcode = chunks.get(chunks.size() - 2).equalsOpCode(ScriptOpCodes.OP_ENDIF);

        if (!hasErpPrefix || !hasEndIfOpcode) {
            return false;
        }

        boolean hasErpStructure = false;
        int elseOpcodeIndex = 0;

        // Check existence of OP_ELSE opcode, followed by PUSH_BYTES, CSV and OP_DROP and
        // get both default and ERP federations redeem scripts
        for (int i = 1; i < chunks.size(); i++) {
            if (chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE) && chunks.size() >= i + 3) {
                elseOpcodeIndex = i;
                ScriptChunk pushBytesOpcode = chunks.get(elseOpcodeIndex + 1);
                ScriptChunk csvOpcode = chunks.get(elseOpcodeIndex + 2);
                ScriptChunk opDrop = chunks.get(elseOpcodeIndex + 3);

                hasErpStructure = pushBytesOpcode.opcode == ErpFederationRedeemScriptParser.CSV_SERIALIZED_LENGTH &&
                    csvOpcode.equalsOpCode(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY) &&
                    opDrop.equalsOpCode(ScriptOpCodes.OP_DROP);

                break;
            }
        }

        if (!hasErpStructure) {
            return false;
        }

        // Validate both default and erp federations redeem scripts. For this, it is
        // necessary to add opcode OP_CHECKMULTISIG at the end of the redeem scripts
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        List<ScriptChunk> defaultFedRedeemScriptChunks = chunks.subList(1, elseOpcodeIndex);
        Script defaultFedRedeemScript = scriptBuilder
            .addChunks(defaultFedRedeemScriptChunks)
            .addChunk(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null))
            .build();

        scriptBuilder = new ScriptBuilder();
        List<ScriptChunk> erpFedRedeemScriptChunks = chunks.subList(elseOpcodeIndex + 4, chunks.size() - 2);
        Script erpFedRedeemScript = scriptBuilder
            .addChunks(erpFedRedeemScriptChunks)
            .addChunk(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null))
            .build();

        return hasStandardRedeemScriptStructure(defaultFedRedeemScript.getChunks()) &&
            hasStandardRedeemScriptStructure(erpFedRedeemScript.getChunks());
    }

    protected static boolean hasFastBridgePrefix(List<ScriptChunk> chunks) {
        ScriptChunk firstChunk = chunks.get(0);

        if (firstChunk.data == null || chunks.size() < 2) {
            return false;
        }

        return firstChunk.opcode == 32 &&
            firstChunk.data.length == 32 &&
            chunks.get(1).opcode == ScriptOpCodes.OP_DROP;
    }

    protected static List<ScriptChunk> removeOpCheckMultisig(Script redeemScript) {
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(redeemScript.getChunks())) {
            String message = "Redeem script has an invalid structure";
            throw new VerificationException(message);
        }

        // Remove the last chunk, which has CHECKMULTISIG op code
        return redeemScript.getChunks().subList(0, redeemScript.getChunks().size() - 1);
    }

    protected static boolean isOpN(ScriptChunk chunk) {
        return chunk.isOpCode() &&
            chunk.opcode >= ScriptOpCodes.OP_1 && chunk.opcode <= ScriptOpCodes.OP_16;
    }
}
