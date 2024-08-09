package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlyoverRedeemScriptParser implements RedeemScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);

    private MultiSigType multiSigType;
    private List<ScriptChunk> redeemScriptChunks;
    private RedeemScriptParser redeemScriptParser;

    public FlyoverRedeemScriptParser(List<ScriptChunk> redeemScriptChunks) {
        this.multiSigType = MultiSigType.FLYOVER;
        this.redeemScriptChunks = redeemScriptChunks;
        this.redeemScriptParser = RedeemScriptParserFactory.get(extractRedeemScript());
    }

    private List<ScriptChunk> extractRedeemScript() {
        if (!RedeemScriptValidator.hasFastBridgePrefix(redeemScriptChunks)) {
            String message = "Provided redeem script is not a flyover redeem script";
            logger.debug("[createFlyoverRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        return redeemScriptChunks.subList(2, redeemScriptChunks.size());
    }

    @Override
    public MultiSigType getMultiSigType() {
        return multiSigType;
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
    public List<ScriptChunk> extractStandardRedeemScript() {
        return redeemScriptParser.extractStandardRedeemScript();
    }
}
