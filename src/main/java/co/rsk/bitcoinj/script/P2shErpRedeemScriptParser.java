package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class P2shErpRedeemScriptParser implements RedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(P2shErpRedeemScriptParser.class);

    private final RedeemScriptParser defaultRedeemScriptParser;

    P2shErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        List<ScriptChunk> defaultRedeemScriptChunks = extractDefaultRedeemScriptChunks(redeemScriptChunks);
        this.defaultRedeemScriptParser = new StandardRedeemScriptParser(defaultRedeemScriptChunks);
    }

    @Override
    public int getM() {
        return defaultRedeemScriptParser.getM();
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        return defaultRedeemScriptParser.findKeyInRedeem(key);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        return defaultRedeemScriptParser.getPubKeys();
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        return defaultRedeemScriptParser.findSigInRedeem(signatureBytes, hash);
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return defaultRedeemScriptParser.extractStandardRedeemScriptChunks();
    }

    private List<ScriptChunk> extractDefaultRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from P2SH ERP redeem script has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
    }

    @Override
    public boolean hasErpFormat() {
        return true;
    }
}
