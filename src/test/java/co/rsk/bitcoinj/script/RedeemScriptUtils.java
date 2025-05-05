package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.removeOpCheckMultisig;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Utils;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public final class RedeemScriptUtils {

    private RedeemScriptUtils() {
    }

    public static Script createStandardRedeemScript(List<BtcECKey> publicKeys) {
        return ScriptBuilder.createRedeemScript(publicKeys.size() / 2 + 1, publicKeys);
    }

    public static Script createCustomRedeemScript(List<BtcECKey> publicKeys) {
        Script redeem = ScriptBuilder.createRedeemScript(
            publicKeys.size() / 2 + 1,
            publicKeys
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(redeem.getChunks())
            .build();
    }

    public static Script createNonStandardErpRedeemScript(
        List<BtcECKey> defaultRedeemScriptKeys,
        List<BtcECKey> emergencyRedeemScriptKeys,
        Long csvValue
    ) {
        Script defaultFedRedeemScript = ScriptBuilder.createRedeemScript(
            defaultRedeemScriptKeys.size() / 2 + 1,
            defaultRedeemScriptKeys
        );

        Script erpFedRedeemScript = ScriptBuilder.createRedeemScript(
            emergencyRedeemScriptKeys.size() / 2 + 1,
            emergencyRedeemScriptKeys
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
        List<BtcECKey> defaultRedeemScriptKeys,
        List<BtcECKey> emergencyRedeemScriptKeys,
        Long csvValue
    ) {
        Script defaultFedRedeemScript = ScriptBuilder.createRedeemScript(
            defaultRedeemScriptKeys.size() / 2 + 1,
            defaultRedeemScriptKeys
        );

        Script erpFedRedeemScript = ScriptBuilder.createRedeemScript(
            emergencyRedeemScriptKeys.size() / 2 + 1,
            emergencyRedeemScriptKeys
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

    public static Script createFlyoverRedeemScript(byte[] derivationArgumentsHashBytes, Script internalRedeemScript) {
        List<ScriptChunk> internalRedeemScriptChunks = internalRedeemScript.getChunks();
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .data(derivationArgumentsHashBytes)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(internalRedeemScriptChunks)
            .build();
    }

    public static List<BtcECKey> getDefaultRedeemScriptKeys() {
        List<BtcECKey> keys = Arrays.asList(
            BtcECKey.fromPrivate(BigInteger.valueOf(100)),
            BtcECKey.fromPrivate(BigInteger.valueOf(200)),
            BtcECKey.fromPrivate(BigInteger.valueOf(300)),
            BtcECKey.fromPrivate(BigInteger.valueOf(400)),
            BtcECKey.fromPrivate(BigInteger.valueOf(500)),
            BtcECKey.fromPrivate(BigInteger.valueOf(600)),
            BtcECKey.fromPrivate(BigInteger.valueOf(700)),
            BtcECKey.fromPrivate(BigInteger.valueOf(800)),
            BtcECKey.fromPrivate(BigInteger.valueOf(900))
        );
        keys.sort(BtcECKey.PUBKEY_COMPARATOR);

        return keys;
    }

    public static List<BtcECKey> getEmergencyRedeemScriptKeys() {
        List<BtcECKey> keys = Arrays.asList(
            BtcECKey.fromPrivate(BigInteger.valueOf(101)),
            BtcECKey.fromPrivate(BigInteger.valueOf(202)),
            BtcECKey.fromPrivate(BigInteger.valueOf(303)),
            BtcECKey.fromPrivate(BigInteger.valueOf(404))
        );
        keys.sort(BtcECKey.PUBKEY_COMPARATOR);

        return keys;
    }

    public static Script createP2shP2wshErpCustomRedeemScript(List<BtcECKey> defaultRedeemScriptKeys, List<BtcECKey> emergencyRedeemScriptKeys, long csvValue) {
        Script defaultCustomRedeemScript = ScriptBuilder.createCustomRedeemScript(
            defaultRedeemScriptKeys.size() / 2 + 1,
            defaultRedeemScriptKeys
        );

        Script erpFedRedeemScript = ScriptBuilder.createRedeemScript(
            emergencyRedeemScriptKeys.size() / 2 + 1,
            emergencyRedeemScriptKeys
        );

        byte[] serializedCsvValue = Utils.reverseBytes(BigInteger.valueOf(csvValue).toByteArray());

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(defaultCustomRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpFedRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ENDIF)
            .build();


    }
}
