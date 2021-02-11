package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.removeOpCheckMultisig;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.math.BigInteger;
import java.util.List;
import org.spongycastle.util.encoders.Hex;

public class RedeemScriptUtils {

    public static Script createStandardRedeemScript(List<BtcECKey> btcECKeyList) {
        return ScriptBuilder.createRedeemScript(2, btcECKeyList);
    }

    public static Script createFastBridgeRedeemScript(
        byte[] derivationArgumentsHashBytes,
        List<BtcECKey> btcECKeyList
    ) {
        Script redeem = ScriptBuilder.createRedeemScript(2, btcECKeyList);

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder.data(derivationArgumentsHashBytes)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(redeem.getChunks())
            .build();
    }

    public static Script createErpRedeemScript(
        List<BtcECKey> defaultFedBtcECKeyList,
        List<BtcECKey> erpFedBtcECKeyList,
        Long csvValue
    ) {
        Script defaultFedRedeemScript =
            ScriptBuilder.createRedeemScript(2, defaultFedBtcECKeyList);

        Script erpFedRedeemScript =
            ScriptBuilder.createRedeemScript(2, erpFedBtcECKeyList);

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder.op(ScriptOpCodes.OP_NOTIF)
            .addChunks(removeOpCheckMultisig(defaultFedRedeemScript))
            .op(ScriptOpCodes.OP_ELSE)
            .data(BigInteger.valueOf(csvValue).toByteArray())
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(removeOpCheckMultisig(erpFedRedeemScript))
            .op(ScriptOpCodes.OP_ENDIF)
            .op(ScriptOpCodes.OP_CHECKMULTISIG)
            .build();
    }

    public static Script createFastBridgeErpRedeemScript(
        List<BtcECKey> defaultFedBtcECKeyList,
        List<BtcECKey> erpFedBtcECKeyList,
        Long csvValue,
        byte[] derivationArgumentsHashBytes
    ) {
        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpRedeemScript = createErpRedeemScript(
            defaultFedBtcECKeyList,
            erpFedBtcECKeyList,
            csvValue
        );

        return scriptBuilder
            .data(derivationArgumentsHashBytes)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpRedeemScript.getChunks())
            .build();
    }

    public static Script createCustomRedeemScript(List<BtcECKey> btcECKeyList) {
        Script redeem = ScriptBuilder.createRedeemScript(2, btcECKeyList);

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder.op(ScriptOpCodes.OP_DROP)
            .addChunks(redeem.getChunks())
            .build();
    }
}
