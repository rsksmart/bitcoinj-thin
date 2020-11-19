package co.rsk.bitcoinj;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import java.util.Arrays;
import java.util.List;

public class RedeemScriptUtil {

    public static Script createStandardRedeemScript(List<BtcECKey> btcECKeyList) {
        return ScriptBuilder.createRedeemScript(2, btcECKeyList);
    }

    public static Script createFastBridgeRedeemScript(byte[] derivationArgumentsHashBytes,
        List<BtcECKey> btcECKeyList) {
        Script redeem = ScriptBuilder.createRedeemScript(2, btcECKeyList);
        byte[] program = redeem.getProgram();
        byte[] reed = Arrays.copyOf(program, program.length);
        byte[] prefix = new byte[33];

        // Hash length
        prefix[0] = 0x20;
        System.arraycopy(derivationArgumentsHashBytes, 0, prefix, 1,
            derivationArgumentsHashBytes.length);

        byte[] c = new byte[prefix.length + 1 + reed.length];
        System.arraycopy(prefix, 0, c, 0, prefix.length);

        // OP_DROP to ignore pushed hash
        c[prefix.length] = 0x75;
        System.arraycopy(reed, 0, c, prefix.length + 1, reed.length);

        return new Script(c);
    }

    public static Script createCustomRedeemScript(List<BtcECKey> btcECKeyList) {
            Script redeem = ScriptBuilder.createRedeemScript(2, btcECKeyList);
            byte[] program = redeem.getProgram();
            byte[] reed = Arrays.copyOf(program, program.length);
            byte[] c = new byte[1 + reed.length];
            System.arraycopy(new byte[0], 0, c, 0, 0);

            // Add OP_DROP
            c[0] = 0x75;
            System.arraycopy(reed, 0, c, 1, reed.length);

            return new Script(c);
    }
}
