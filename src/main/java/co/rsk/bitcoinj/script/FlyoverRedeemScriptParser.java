package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlyoverRedeemScriptParser implements RedeemScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);
    private final MultiSigType multiSigType;
    private final RedeemScriptParser redeemScriptParser;

    public FlyoverRedeemScriptParser(List<ScriptChunk> redeemScriptChunks) {
        List<ScriptChunk> standardRedeemScriptChunks = extractStandardRedeemScriptChunks(redeemScriptChunks);
        this.redeemScriptParser = RedeemScriptParserFactory.get(standardRedeemScriptChunks);
        this.multiSigType = MultiSigType.FLYOVER;
    }

    @Override
    public ScriptType getScriptType() {
        return null;
    }

    @Override
    public int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey) {
        return 0;
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
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return redeemScriptParser.extractStandardRedeemScriptChunks();
    }

    public static List<ScriptChunk> extractStandardRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Flyover redeem script obtained has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
    }
}
