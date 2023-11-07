package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.hasFastBridgePrefix;

public class FastBridgeRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeRedeemScriptParser.class);

    protected final byte[] derivationHash;

    public FastBridgeRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        super(
            redeemScriptChunks.subList(2, redeemScriptChunks.size())
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_MULTISIG;
        this.derivationHash = redeemScriptChunks.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static Script extractStandardRedeemScript(Script redeemScript) {
        if(!hasFastBridgePrefix(redeemScript.getChunks())) {
            return redeemScript;
        }
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        List<ScriptChunk> chunksWithoutFlyover = redeemScript.getChunks().subList(2, redeemScript.getChunks().size());
        return scriptBuilder.addChunks(chunksWithoutFlyover).build();
    }

    public static Script createMultiSigFastBridgeRedeemScript(
        Script redeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        if (hasFastBridgePrefix(redeemScript.getChunks())) {
            String message = "Provided redeem script is already a fast bridge redeem script";
            logger.debug("[createMultiSigFastBridgeRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        if (derivationArgumentsHash == null || derivationArgumentsHash.equals(Sha256Hash.ZERO_HASH)) {
            String message = "Derivation arguments are not valid";
            logger.debug("[createMultiSigFastBridgeRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        return scriptBuilder.data(derivationArgumentsHash.getBytes())
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(redeemScript.getChunks())
            .build();
    }

    public static boolean isFastBridgeMultiSig(List<ScriptChunk> chunks) {
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        List<ScriptChunk> chunksWithoutFlyover = chunks.subList(2, chunks.size());
        Script redeemScript = scriptBuilder.addChunks(chunksWithoutFlyover).build();

        return hasFastBridgePrefix(chunks) && redeemScript.isSentToMultiSig();
    }
}
