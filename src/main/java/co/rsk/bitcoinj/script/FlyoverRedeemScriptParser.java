package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlyoverRedeemScriptParser implements RedeemScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);
    private final RedeemScriptParser redeemScriptParser;

    public FlyoverRedeemScriptParser(List<ScriptChunk> redeemScriptChunks) {
        List<ScriptChunk> internalRedeemScriptChunks = extractInternalRedeemScriptChunks(redeemScriptChunks);
        this.redeemScriptParser = RedeemScriptParserFactory.get(internalRedeemScriptChunks);
    }

    @Override
    public MultiSigType getMultiSigType() {
        return MultiSigType.FLYOVER;
    }

    @Override
    public int getM() {
        return redeemScriptParser.getM();
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        return redeemScriptParser.findKeyInRedeem(key);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        return redeemScriptParser.getPubKeys();
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        return redeemScriptParser.findSigInRedeem(signatureBytes, hash);
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return redeemScriptParser.extractStandardRedeemScriptChunks();
    }

    public static List<ScriptChunk> extractInternalRedeemScriptChunks(List<ScriptChunk> chunks) {
        if (chunks.size() <= 2) {
            String message = "Flyover redeem script obtained has an invalid structure";
            logger.debug("[extractInternalRedeemScriptChunks] {} {}", message, chunks);
            throw new VerificationException(message);
        }

        return chunks.subList(2, chunks.size());
    }
}
