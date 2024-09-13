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

    private List<ScriptChunk> extractDefaultRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        chunksForRedeem.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Standard redeem script obtained from ERP redeem script has an invalid structure";
            logger.debug("[extractDefaultRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
    }

    public static Script createNonStandardErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

        return createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue,
            serializedCsvValue
        );
    }

    @Deprecated
    // This method encodes the CSV value as unsigned Big Endian which is not correct.
    // It should be encoded as signed LE
    // Keeping this method for backwards compatibility in rskj
    public static Script createNonStandardErpRedeemScriptDeprecated(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        final int CSV_SERIALIZED_LENGTH = 2;
        byte[] serializedCsvValue = Utils.unsignedLongToByteArrayBE(csvValue,
            CSV_SERIALIZED_LENGTH);

        return createNonStandardErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue,
            serializedCsvValue
        );
    }

    public static boolean isNonStandardErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(chunks);
    }

    private static Script createNonStandardErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue,
        byte[] serializedCsvValue) {

        validateNonStandardErpRedeemScriptValues(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script nonStandardErpRedeemScript = scriptBuilder
            .op(ScriptOpCodes.OP_NOTIF)
            .addChunks(removeOpCheckMultisig(defaultFederationRedeemScript))
            .op(ScriptOpCodes.OP_ELSE)
            .data(serializedCsvValue)
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .addChunks(removeOpCheckMultisig(erpFederationRedeemScript))
            .op(ScriptOpCodes.OP_ENDIF)
            .op(ScriptOpCodes.OP_CHECKMULTISIG)
            .build();

        // Validate the created redeem script has a valid structure
        if (!RedeemScriptValidator.hasNonStandardErpRedeemScriptStructure(nonStandardErpRedeemScript.getChunks())) {
            String message = String.format(
                "Created redeem script has an invalid structure, it is not a non standard ERP redeem script. Redeem script created: %s",
                nonStandardErpRedeemScript
            );
            logger.debug("[createNonStandardErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }
        return nonStandardErpRedeemScript;
    }

    private static void validateNonStandardErpRedeemScriptValues(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(defaultFederationRedeemScript.getChunks()) ||
            !RedeemScriptValidator.hasStandardRedeemScriptStructure(erpFederationRedeemScript.getChunks())) {

            String message = "Provided redeem scripts have an invalid structure, not standard";
            logger.debug("[validateErpRedeemScriptValues] {}", message);
            throw new VerificationException(message);
        }

        if (csvValue <= 0 || csvValue > MAX_CSV_VALUE) {
            String message = String.format(
                "Provided csv value %d must be larger than 0 and lower than %d",
                csvValue,
                MAX_CSV_VALUE
            );
            logger.warn("[validateErpRedeemScriptValues] {}", message);
            throw new VerificationException(message);
        }
    }
}
