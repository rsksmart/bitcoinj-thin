package co.rsk.bitcoinj.deprecated.script;

import co.rsk.bitcoinj.deprecated.core.Sha256Hash;
import co.rsk.bitcoinj.deprecated.core.VerificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastBridgeRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeRedeemScriptParser.class);

    protected final byte[] derivationHash;

    public FastBridgeRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks,
        List<ScriptChunk> rawChunks
    ) {
        super(
            scriptType,
            redeemScriptChunks.subList(2, redeemScriptChunks.size()),
            rawChunks
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_MULTISIG;
        this.derivationHash = redeemScriptChunks.get(0).data;
    }

    public byte[] getDerivationHash() {
        return derivationHash;
    }

    public static Script extractStandardRedeemScript(Script redeemScript) {
        return ScriptBuilder.createRedeemScript(
            redeemScript.getNumberOfSignaturesRequiredToSpend(),
            redeemScript.getPubKeys()
        );
    }

    public static Script createMultiSigFastBridgeRedeemScript(
        Script redeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        if (RedeemScriptValidator.hasFastBridgePrefix(redeemScript.getChunks())) {
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
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasStandardRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }
}
