package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.removeOpCheckMultisig;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import java.util.List;

public class RedeemScriptUtils {

    public static Script createStandardRedeemScript(List<BtcECKey> btcECKeyList) {
        return ScriptBuilder.createRedeemScript(btcECKeyList.size() / 2 + 1, btcECKeyList);
    }

    public static Script createFastBridgeRedeemScript(
        byte[] derivationArgumentsHashBytes,
        List<BtcECKey> btcECKeyList
    ) {
        Script redeem = ScriptBuilder.createRedeemScript(
            btcECKeyList.size() / 2 + 1,
            btcECKeyList
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .data(derivationArgumentsHashBytes)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(redeem.getChunks())
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

    public static Script createFastBridgeP2shErpRedeemScript(
        List<BtcECKey> defaultFedBtcECKeyList,
        List<BtcECKey> erpFedBtcECKeyList,
        Long csvValue,
        byte[] derivationArgumentsHashBytes
    ) {
        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpRedeemScript = createP2shErpRedeemScript(
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
        Script redeem = ScriptBuilder.createRedeemScript(
            btcECKeyList.size() / 2 + 1,
            btcECKeyList
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(redeem.getChunks())
            .build();
    }

    public static Script createErpRedeemScript(
        List<BtcECKey> defaultFedBtcECKeyList,
        List<BtcECKey> erpFedBtcECKeyList,
        Long csvValue
    ) {
        Script defaultFedRedeemScript = ScriptBuilder.createRedeemScript(
            defaultFedBtcECKeyList.size() / 2 + 1,
            defaultFedBtcECKeyList
        );

        Script erpFedRedeemScript = ScriptBuilder.createRedeemScript(
            erpFedBtcECKeyList.size() / 2 + 1,
            erpFedBtcECKeyList
        );

        byte[] serializedCsvValue = Utils.reverseBytes(BigInteger.valueOf(csvValue).toByteArray());

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(removeOpCheckMultisig(defaultFedRedeemScript))
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(removeOpCheckMultisig(erpFedRedeemScript))
            .op(ScriptOpCodes.OP_ENDIF)
            .op(ScriptOpCodes.OP_CHECKMULTISIG)
            .build();
    }

    public static Script createP2shErpRedeemScript(
        List<BtcECKey> defaultFedBtcECKeyList,
        List<BtcECKey> erpFedBtcECKeyList,
        Long csvValue
    ) {
        Script defaultFedRedeemScript = ScriptBuilder.createRedeemScript(
            defaultFedBtcECKeyList.size() / 2 + 1,
            defaultFedBtcECKeyList
        );

        Script erpFedRedeemScript = ScriptBuilder.createRedeemScript(
            erpFedBtcECKeyList.size() / 2 + 1,
            erpFedBtcECKeyList
        );

        byte[] serializedCsvValue = Utils.reverseBytes(BigInteger.valueOf(csvValue).toByteArray());

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(defaultFedRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpFedRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ENDIF)
            .build();
    }
}
