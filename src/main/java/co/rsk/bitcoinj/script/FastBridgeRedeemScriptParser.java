package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastBridgeRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeRedeemScriptParser.class);

    protected final byte[] derivationHash;

    public FastBridgeRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScript,
        List<ScriptChunk> chunks
    ) {
        super(
            scriptType,
            redeemScript.subList(2, redeemScript.size()),
            chunks
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_MULTISIG;
        this.derivationHash = redeemScript.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static Script extractRedeemScriptFromMultiSigFastBridgeRedeemScript(Script redeemScript) {
        return ScriptBuilder.createRedeemScript(
            redeemScript.getNumberOfSignaturesRequiredToSpend(),
            redeemScript.getPubKeys()
        );
    }

    public static Script createMultiSigFastBridgeRedeemScript(
        Script redeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        List<ScriptChunk> chunks = redeemScript.getChunks();
        ScriptChunk firstChunk = chunks.get(0);
        boolean hasFastBridgePrefix = false;

        if (firstChunk.data != null) {
            hasFastBridgePrefix = firstChunk.opcode == 32 && firstChunk.data.length == 32 &&
                chunks.get(1).opcode == ScriptOpCodes.OP_DROP;
        }

        if (hasFastBridgePrefix) {
            String message = "Provided redeem script is already a fast bridge redeem script";
            logger.debug(message);
            throw new VerificationException(message);
        }

        if (derivationArgumentsHash == null || derivationArgumentsHash.equals(Sha256Hash.ZERO_HASH)) {
            String message = "Derivation arguments are not valid";
            logger.debug(message);
            throw new VerificationException(message);
        }

        byte[] program = redeemScript.getProgram();
        byte[] reed = Arrays.copyOf(program, program.length);
        byte[] prefix = new byte[33];

        // Hash length
        prefix[0] = 0x20;
        System.arraycopy(derivationArgumentsHash.getBytes(), 0, prefix, 1,
            derivationArgumentsHash.getBytes().length);

        byte[] c = new byte[prefix.length + 1 + reed.length];
        System.arraycopy(prefix, 0, c, 0, prefix.length);

        // OP_DROP to ignore pushed hash
        c[prefix.length] = 0x75;
        System.arraycopy(reed, 0, c, prefix.length + 1, reed.length);

        return new Script(c);
    }
}
