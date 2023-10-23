package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FlyoverRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);
    protected final byte[] derivationHash;

    public FlyoverRedeemScriptParser(
        Script redeemScript,
        Sha256Hash derivationHash
    ) {
        this.derivationHash = derivationHash.getBytes();
    }
    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static Script createFlyoverRedeemScript(
        Script redeemScript,
        Sha256Hash derivationHash
    ) {

        List<ScriptChunk> chunks = redeemScript.getChunks();

        if (RedeemScriptValidator.hasFastBridgePrefix(chunks)) {
            String message = "Provided redeem script is already a fast bridge redeem script";
            logger.debug("[createFlyoverRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        if (derivationHash == null || derivationHash.equals(Sha256Hash.ZERO_HASH)) {
            String message = "Derivation arguments are not valid";
            logger.debug("[createFlyoverRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationHash.getBytes())
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
