package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;

public class RedeemScriptValidator {

    protected static boolean isRedeemLikeScript(List<ScriptChunk> chunks) {
        if (chunks.size() < 4) {
            return false;
        }

        ScriptChunk lastChunk = chunks.get(chunks.size() - 1);
        // A standard multisig redeem script must end in OP_CHECKMULTISIG[VERIFY]
        boolean isStandard = lastChunk.isOpCode() &&
                (lastChunk.equalsOpCode(ScriptOpCodes.OP_CHECKMULTISIG) ||
                    lastChunk.equalsOpCode(ScriptOpCodes.OP_CHECKMULTISIGVERIFY));
        if (isStandard) {
            return true;
        }
        // A P2SH ERP like script must finish in OP_ENDIF
        // and the previous element should be OP_CHECKMULTISIG[VERIFY]
        ScriptChunk penultimateChunk = chunks.get(chunks.size() - 2);
        return lastChunk.isOpCode() &&
            lastChunk.equalsOpCode(ScriptOpCodes.OP_ENDIF) &&
            penultimateChunk.isOpCode() &&
            (penultimateChunk.equalsOpCode(ScriptOpCodes.OP_CHECKMULTISIG) ||
                penultimateChunk.equalsOpCode(ScriptOpCodes.OP_CHECKMULTISIGVERIFY));
    }

    protected static boolean hasStandardRedeemScriptStructure(List<ScriptChunk> chunks) {
        try {
            if (!isRedeemLikeScript(chunks)) {
                return false;
            }

            // First chunk must be an OP_N
            if (!isOpN(chunks.get(0))) {
                return false;
            }

            // Second to last chunk must be an OP_N opcode too, and there should be
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

            return true;
        } catch (IllegalStateException e) {
            return false;   // Not an OP_N opcode.
        }
    }

    private static boolean hasCustomRedeemScriptStructure(List<ScriptChunk> chunks) {
        try {
//            if (!isRedeemLikeScript(chunks)) {
//                return false;
//            }

            // First chunk must be an OP_N
            if (!isOpN(chunks.get(0))) {
                return false;
            }

            // Second to last chunk must be an OP_N opcode too, and there should be
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

            return true;
        } catch (IllegalStateException e) {
            return false;   // Not an OP_N opcode.
        }
    }

    protected static boolean hasP2shErpRedeemScriptStructure(List<ScriptChunk> chunks) {
        if (!isRedeemLikeScript(chunks)) {
            return false;
        }

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

        // Validate both default and erp federations redeem scripts.
        // Extract the default PowPeg and the emergency multisig redeemscript chunks
        List<ScriptChunk> defaultFedRedeemScriptChunks = chunks.subList(1, elseOpcodeIndex);
        List<ScriptChunk> erpFedRedeemScriptChunks = chunks.subList(elseOpcodeIndex + 4, chunks.size() - 1);

        // Both should be standard multisig redeemscripts
        return hasStandardRedeemScriptStructure(defaultFedRedeemScriptChunks) &&
                hasStandardRedeemScriptStructure(erpFedRedeemScriptChunks);
    }

    protected static boolean hasNonStandardErpRedeemScriptStructure(List<ScriptChunk> chunks) {
        if (!isRedeemLikeScript(chunks)) {
            return false;
        }

        ScriptChunk firstChunk = chunks.get(0);

        boolean hasErpPrefix = firstChunk.opcode == ScriptOpCodes.OP_NOTIF;
        boolean hasEndIfOpcode = chunks.get(chunks.size() - 2).equalsOpCode(ScriptOpCodes.OP_ENDIF);

        if (!hasErpPrefix || !hasEndIfOpcode) {
            return false;
        }

        boolean hasNonStandardErpStructure = false;
        int elseOpcodeIndex = 0;

        // Check existence of OP_ELSE opcode, followed by PUSH_BYTES, CSV and OP_DROP and
        // get both default and ERP federations redeem scripts
        for (int i = 1; i < chunks.size(); i++) {
            if (chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE) && chunks.size() >= i + 3) {
                elseOpcodeIndex = i;
                ScriptChunk pushBytesOpcode = chunks.get(elseOpcodeIndex + 1);
                ScriptChunk csvOpcode = chunks.get(elseOpcodeIndex + 2);
                ScriptChunk opDrop = chunks.get(elseOpcodeIndex + 3);

                hasNonStandardErpStructure = pushBytesOpcode.isPushData() &&
                    csvOpcode.equalsOpCode(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY) &&
                    opDrop.equalsOpCode(ScriptOpCodes.OP_DROP);

                break;
            }
        }

        if (!hasNonStandardErpStructure) {
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

    protected static boolean hasFlyoverPrefix(List<ScriptChunk> chunks) {
        ScriptChunk firstChunk = chunks.get(0);

        if (firstChunk.data == null || chunks.size() < 2) {
            return false;
        }

        return firstChunk.opcode == 32 &&
            firstChunk.data.length == 32 &&
            chunks.get(1).opcode == ScriptOpCodes.OP_DROP;
    }

    protected static boolean hasFlyoverRedeemScriptStructure(List<ScriptChunk> chunks) {
        if (!hasFlyoverPrefix(chunks)) {
            return false;
        }

        // Validate the obtained redeem script has a valid format
        return isRedeemLikeScript(chunks.subList(2, chunks.size()));
    }

    protected static boolean hasP2shP2wshErpCustomRedeemScriptStructure(List<ScriptChunk> chunks) {
        if (!isRedeemLikeScript(chunks)) {
            return false;
        }

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

        /**
         * Expected structure:
         * OP_NOTIF
         *  <pubkey1>
         *  OP_CHECKSIG
         *  OP_SWAP
         *  <pubkey2>
         *  OP_CHECKSIG
         *  OP_ADD
         *  ...
         *  OP_SWAP
         *  <pubkeyn>
         *  OP_CHECKSIG
         *  OP_ADD
         *  OP_M
         *  OP_NUMEQUAL
         * OP_ELSE
         *  <csv_value>
         *  OP_CHECKSEQUENCEVERIFY
         *  OP_DROP
         *  OP_M
         *  <pubkey1>...<pubkeyN>
         *  OP_N
         *  OP_CHECKMULTISIG
         * OP_ENDIF
         */

        // Validate both default and erp federations redeem scripts.
        // Extract the custom default PowPeg and the emergency multisig redeemscript chunks
        List<ScriptChunk> defaultCustomFedRedeemScriptChunks = chunks.subList(1, elseOpcodeIndex);
        List<ScriptChunk> erpFedRedeemScriptChunks = chunks.subList(elseOpcodeIndex + 4, chunks.size() - 1);

        // First should be the custom redeemscript and the second a multisig redeemscript
        return hasCustomRedeemScriptStructure(defaultCustomFedRedeemScriptChunks) &&
            hasStandardRedeemScriptStructure(erpFedRedeemScriptChunks);
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
