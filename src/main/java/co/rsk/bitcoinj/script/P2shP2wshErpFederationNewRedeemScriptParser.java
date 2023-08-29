package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Utils;

public class P2shP2wshErpFederationNewRedeemScriptParser {
    public static Script createErpP2shP2wshNewRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue) {

        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpRedeemScript = scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(defaultFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ENDIF)
            .build();

        return erpRedeemScript;
    }
}
