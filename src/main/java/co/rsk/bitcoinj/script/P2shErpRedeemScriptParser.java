package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class P2shErpRedeemScriptParser implements RedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(P2shErpRedeemScriptParser.class);

    public static long MAX_CSV_VALUE = 65_535L; // 2^16 - 1, since bitcoin will interpret up to 16 bits as the CSV value

    private final RedeemScriptParser defaultRedeemScriptParser;

    public P2shErpRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        List<ScriptChunk> defaultRedeemScriptChunks = extractDefaultRedeemScriptChunks(redeemScriptChunks);
        this.defaultRedeemScriptParser = new StandardRedeemScriptParser(defaultRedeemScriptChunks);
    }

    @Override
    public MultiSigType getMultiSigType() {
        return MultiSigType.P2SH_ERP_FED;
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

    public static Script createP2shErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

        validateP2shErpRedeemScriptValues(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpRedeemScript = scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(defaultFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(erpFederationRedeemScript.getChunks())
            .op(ScriptOpCodes.OP_ENDIF)
            .build();

        // Validate the created redeem script has a valid structure
        if (!RedeemScriptValidator.hasP2shErpRedeemScriptStructure(erpRedeemScript.getChunks())) {
            String message = String.format(
                "Created redeem script has an invalid structure, not P2SH ERP redeem script. Redeem script created: %s",
                erpRedeemScript
            );
            logger.debug("[createErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        return erpRedeemScript;
    }

    public static boolean isP2shErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasP2shErpRedeemScriptStructure(chunks);
    }

    private static void validateP2shErpRedeemScriptValues(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(defaultFederationRedeemScript.getChunks()) ||
            !RedeemScriptValidator.hasStandardRedeemScriptStructure(erpFederationRedeemScript.getChunks())) {

            String message = "Provided redeem scripts has an invalid structure, not standard";
            logger.debug(
                "[validateP2shErpRedeemScriptValues] {}. Default script {}. Emergency script {}",
                message,
                defaultFederationRedeemScript,
                erpFederationRedeemScript
            );
            throw new VerificationException(message);
        }

        if (csvValue <= 0 || csvValue > MAX_CSV_VALUE) {
            String message = String.format(
                "Provided csv value %d must be between 0 and %d",
                csvValue,
                MAX_CSV_VALUE
            );
            logger.warn("[validateP2shErpRedeemScriptValues] {}", message);
            throw new VerificationException(message);
        }
    }
}
