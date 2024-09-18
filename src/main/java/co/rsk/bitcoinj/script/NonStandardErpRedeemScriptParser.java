package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.removeOpCheckMultisig;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonStandardErpRedeemScriptParser implements RedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(NonStandardErpRedeemScriptParser.class);

    public static long MAX_CSV_VALUE = 65_535L; // 2^16 - 1, since bitcoin will interpret up to 16 bits as the CSV value

    private final RedeemScriptParser defaultRedeemScriptParser;

    public NonStandardErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        List<ScriptChunk> defaultRedeemScriptChunks = extractDefaultRedeemScriptChunks(redeemScriptChunks);
        this.defaultRedeemScriptParser = new StandardRedeemScriptParser(defaultRedeemScriptChunks);
    }

    @Override
    public MultiSigType getMultiSigType() {
        return MultiSigType.NON_STANDARD_ERP_FED;
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

    private static List<ScriptChunk> extractDefaultRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        chunksForRedeem.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from NonStandardErpRedeemScript has an invalid structure";
            logger.debug("[extractDefaultRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
    }

    public static boolean isNonStandardErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(chunks);
    }
}
