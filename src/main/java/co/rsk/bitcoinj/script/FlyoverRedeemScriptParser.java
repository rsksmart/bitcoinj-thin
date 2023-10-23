package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class FlyoverRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);
    protected final byte[] derivationHash;
    protected MultiSigType multiSigType;

    public FlyoverRedeemScriptParser(
        Script redeemScript,
        byte[] derivationHash
    ) {
        this.derivationHash = derivationHash;
        this.multiSigType = MultiSigType.FLYOVER;
    }
    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static Script createFlyoverRedeemScript(
        Script redeemScript,
        byte[] derivationHash
    ) {

        List<ScriptChunk> chunks = redeemScript.getChunks();

        if (RedeemScriptValidator.hasFastBridgePrefix(chunks)) {
            String message = "Provided redeem script is already a fast bridge redeem script";
            logger.debug("[createFlyoverRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        if (derivationHash == null || Arrays.equals((derivationHash), (Sha256Hash.ZERO_HASH).getBytes())) {
            String message = "Derivation arguments are not valid";
            logger.debug("[createFlyoverRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationHash)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(chunks)
            .build();
    }

    public static Script extractRedeemScript(
        Script redeemScript
    ) {
        List<ScriptChunk> chunks = redeemScript.getChunks();

        if (!RedeemScriptValidator.hasFastBridgePrefix(chunks)) {
            String message = "Provided redeem script is not a flyover redeem script";
            logger.debug("[createFastBridgeRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        List<ScriptChunk> chunksWithoutFlyover = chunks.subList(2, chunks.size());
        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .addChunks(chunksWithoutFlyover)
            .build();
    }
}
