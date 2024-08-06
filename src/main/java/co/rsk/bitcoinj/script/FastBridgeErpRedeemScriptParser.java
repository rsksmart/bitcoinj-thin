package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastBridgeErpRedeemScriptParser implements RedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeErpRedeemScriptParser.class);

    protected StandardRedeemScriptParser standardRedeemScriptParser;

    public FastBridgeErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        standardRedeemScriptParser = new StandardRedeemScriptParser(
            extractStandardRedeemScript(redeemScriptChunks).getChunks()
        );
    }

    public static Script extractStandardRedeemScript(List<ScriptChunk> chunks) {
        return ErpFederationRedeemScriptParser.
            extractStandardRedeemScript(chunks.subList(2, chunks.size()));
    }

    public static Script createFastBridgeErpRedeemScript(
        Script erpRedeemScript,
        Sha256Hash derivationArgumentsHash
    ) {
        if (!RedeemScriptValidator.hasLegacyErpRedeemScriptStructure(erpRedeemScript.getChunks())) {
            String message = "Provided redeem script has not ERP structure";
            logger.debug("[createFastBridgeErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        List<ScriptChunk> chunks = erpRedeemScript.getChunks();
        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
            .data(derivationArgumentsHash.getBytes())
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(chunks)
            .build();
    }

    public static boolean isFastBridgeErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasFastBridgePrefix(chunks) &&
            RedeemScriptValidator.hasLegacyErpRedeemScriptStructure(chunks.subList(2, chunks.size()));
    }

    @Override
    public MultiSigType getMultiSigType() {
        return standardRedeemScriptParser.getMultiSigType();
    }

    @Override
    public int getM() {
        return standardRedeemScriptParser.getM();
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        return standardRedeemScriptParser.findKeyInRedeem(key);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        return standardRedeemScriptParser.getPubKeys();
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        return standardRedeemScriptParser.findSigInRedeem(signatureBytes, hash);
    }

    @Override
    public Script extractStandardRedeemScript() {
        return null;
    }
}
