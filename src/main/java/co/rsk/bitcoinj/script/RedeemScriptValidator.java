package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;

public class RedeemScriptValidator {

    private RedeemScriptValidator() {}

    /***
     * Expected buggy structure:
     * OP_NOTIF
     *  OP_M
     *  PUBKEYS...N
     *  OP_N
     * OP_ELSE
     *  OP_PUSHBYTES
     *  CSV_VALUE
     *  OP_CHECKSEQUENCEVERIFY
     *  OP_DROP
     *  OP_M
     *  PUBKEYS...N
     *  OP_N
     *  OP_ENDIF
     * OP_CHECKMULTISIG
     */

    protected static boolean hasLegacyErpRedeemScriptStructure(List<ScriptChunk> chunks) {
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

                hasErpStructure = pushBytesOpcode.isPushData() &&
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

        return defaultFedRedeemScript.isSentToMultiSig() && erpFedRedeemScript.isSentToMultiSig();
    }


    /***
     * Expected structure:
     * OP_NOTIF
     *  OP_M
     *  PUBKEYS...N
     *  OP_N
     *  OP_CHECKMULTISIG
     * OP_ELSE
     *  OP_PUSHBYTES
     *  CSV_VALUE
     *  OP_CHECKSEQUENCEVERIFY
     *  OP_DROP
     *  OP_M
     *  PUBKEYS...N
     *  OP_N
     *  OP_CHECKMULTISIG
     * OP_ENDIF
     */

    protected static boolean hasP2shErpRedeemScriptStructure(List<ScriptChunk> chunks) {
        ScriptChunk firstChunk = chunks.get(0);

        boolean hasErpPrefix = firstChunk.opcode == ScriptOpCodes.OP_NOTIF;
        boolean hasEndIfOpcode = chunks.get(chunks.size() - 1).equalsOpCode(ScriptOpCodes.OP_ENDIF);

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

                hasErpStructure = pushBytesOpcode.isPushData() &&
                        csvOpcode.equalsOpCode(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY) &&
                        opDrop.equalsOpCode(ScriptOpCodes.OP_DROP);

                break;
            }
        }

        if (!hasErpStructure) {
            return false;
        }

        // Validate both default and erp federations redeem scripts.
        // Extract the default PowPeg and the emergency multisig redeemscript chunks
        List<ScriptChunk> defaultFedRedeemScriptChunks = chunks.subList(1, elseOpcodeIndex);
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        Script defaultFedRedeemScript = scriptBuilder
            .addChunks(defaultFedRedeemScriptChunks)
            .build();

        List<ScriptChunk> erpFedRedeemScriptChunks = chunks.subList(elseOpcodeIndex + 4, chunks.size() - 1);
        scriptBuilder = new ScriptBuilder();
        Script erpFedRedeemScript = scriptBuilder
            .addChunks(erpFedRedeemScriptChunks)
            .build();

        // Both should be standard multisig redeem scripts
        return defaultFedRedeemScript.isSentToMultiSig() && erpFedRedeemScript.isSentToMultiSig();
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
        if (!redeemScript.isSentToMultiSig()) {
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
