package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.RedeemScriptValidator.removeOpCheckMultisig;

import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErpFederationRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(ErpFederationRedeemScriptParser.class);
    public static long MIN_CSV_VALUE = 256L; // 256  is the minimum value that can be represented using 2 bytes without adding extra zeroes
    public static long MAX_CSV_VALUE = 65_535L; // 65_535 is the maximum value that can be represented using 2 bytes
    public static int CSV_SERIALIZED_LENGTH = 2;

    public ErpFederationRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScriptChunks,
        List<ScriptChunk> rawChunks
    ) {
        super(
            scriptType,
            extractStandardRedeemScript(redeemScriptChunks).getChunks(),
            rawChunks
        );
        this.multiSigType = MultiSigType.ERP_FED;
    }

    public static Script extractStandardRedeemScript(List<ScriptChunk> chunks) {
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
            logger.debug("[extractStandardRedeemScriptChunksFromErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }

        return new Script(chunksForRedeem);
    }

    public static Script createErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.unsignedLongToByteArrayLE(csvValue, CSV_SERIALIZED_LENGTH);

        return createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue,
            serializedCsvValue
        );
    }

    @Deprecated
    // This method encodes the CSV value as Big Endian which is not correct. It should be encoded as LE
    // Keeping this method for backwards compatibility in rskj
    public static Script createErpRedeemScriptDeprecated(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.unsignedLongToByteArrayBE(csvValue, CSV_SERIALIZED_LENGTH);

        return createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue,
            serializedCsvValue
        );
    }

    public static boolean isErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasErpRedeemScriptStructure(chunks);
    }

    private static Script createErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue,
        byte[] serializedCsvValue) {

        validateErpRedeemScriptValues(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue,
            serializedCsvValue
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        return scriptBuilder
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
    }

    private static void validateErpRedeemScriptValues(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue,
        byte[] parsedCsvValue
    ) {
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(defaultFederationRedeemScript.getChunks()) ||
            !RedeemScriptValidator.hasStandardRedeemScriptStructure(erpFederationRedeemScript.getChunks())) {

            String message = "Provided redeem scripts have an invalid structure, not standard";
            logger.debug("[validateErpRedeemScriptValues] {}", message);
            throw new VerificationException(message);
        }

        if (csvValue < MIN_CSV_VALUE || csvValue > MAX_CSV_VALUE) {
            String message = String.format(
                "Provided csv value %d must be between %d and %d to ensure the use of %d bytes for serialization",
                csvValue,
                MIN_CSV_VALUE,
                MAX_CSV_VALUE,
                CSV_SERIALIZED_LENGTH
            );
            logger.warn("[validateErpRedeemScriptValues] {}", message);
            throw new VerificationException(message);
        }

        if (parsedCsvValue.length != CSV_SERIALIZED_LENGTH) {
            throw new VerificationException(String.format(
                "Invalid CSV value serialization. Expected %d bytes but got %d",
                CSV_SERIALIZED_LENGTH,
                parsedCsvValue.length
            ));
        }
    }
}
