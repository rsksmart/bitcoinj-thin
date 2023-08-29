package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;

public class P2shP2wshErpFederationRedeemScriptParser {
    public static Script createP2shP2wshErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpP2shP2wshRedeemScript = scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(defaultFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ENDIF)
            .build();
        return erpP2shP2wshRedeemScript;
    }

    public static Script createP2shP2wshErpRedeemScriptWithFlyover(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Sha256Hash derivationPath,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpP2shP2wshRedeemScript = scriptBuilder
            .data(derivationPath.getBytes())
            .op(ScriptOpCodes.OP_DROP)
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(defaultFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ENDIF)
            .build();

        return erpP2shP2wshRedeemScript;
    }
}
