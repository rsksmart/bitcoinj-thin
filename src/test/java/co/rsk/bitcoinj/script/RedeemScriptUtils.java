package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import java.util.List;

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

    public static Script createCustomRedeemScript(List<BtcECKey> btcECKeyList) {
        Script redeem = ScriptBuilder.createRedeemScript(2, btcECKeyList);

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder.op(ScriptOpCodes.OP_DROP)
            .addChunks(redeem.getChunks())
            .build();
    }
}
