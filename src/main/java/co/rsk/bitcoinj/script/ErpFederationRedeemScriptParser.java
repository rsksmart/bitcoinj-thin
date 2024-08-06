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

public class ErpFederationRedeemScriptParser implements RedeemScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(
        ErpFederationRedeemScriptParser.class);

    public static long MAX_CSV_VALUE = 65_535L; // 2^16 - 1, since bitcoin will interpret up to 16 bits as the CSV value

    protected StandardRedeemScriptParser standardRedeemScriptParser;

    public ErpFederationRedeemScriptParser(
        List<ScriptChunk> redeemScriptChunks
    ) {
        standardRedeemScriptParser = new StandardRedeemScriptParser(
            extractStandardRedeemScript(redeemScriptChunks).getChunks()
        );
    }

    public static Script extractStandardRedeemScript(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        chunksForRedeem.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        Script redeemScript = scriptBuilder.addChunks(chunksForRedeem).build();
        // Validate the obtained redeem script has a valid format
        if (!redeemScript.isSentToMultiSig()) {
            String message = "Standard redeem script obtained from ERP redeem script has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunksFromErpRedeemScript] {} {}", message,
                chunksForRedeem);
            throw new VerificationException(message);
        }

        return new Script(chunksForRedeem);
    }

    public static Script createErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        byte[] serializedCsvValue = Utils.signedLongToByteArrayLE(csvValue);

        return createErpRedeemScript(
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
    public static Script createErpRedeemScriptDeprecated(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        final int CSV_SERIALIZED_LENGTH = 2;
        byte[] serializedCsvValue = Utils.unsignedLongToByteArrayBE(csvValue,
            CSV_SERIALIZED_LENGTH);

        return createErpRedeemScript(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue,
            serializedCsvValue
        );
    }

    public static boolean isErpFed(List<ScriptChunk> chunks) {
        return RedeemScriptValidator.hasLegacyErpRedeemScriptStructure(chunks);
    }

    private static Script createErpRedeemScript(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue,
        byte[] serializedCsvValue) {

        validateErpRedeemScriptValues(
            defaultFederationRedeemScript,
            erpFederationRedeemScript,
            csvValue
        );

        ScriptBuilder scriptBuilder = new ScriptBuilder();

        Script erpRedeemScript = scriptBuilder
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
        if (!RedeemScriptValidator.hasLegacyErpRedeemScriptStructure(erpRedeemScript.getChunks())) {
            String message = String.format(
                "Created redeem script has an invalid structure, not ERP redeem script. Redeem script created: %s",
                erpRedeemScript
            );
            logger.debug("[createErpRedeemScript] {}", message);
            throw new VerificationException(message);
        }
        return erpRedeemScript;
    }

    private static void validateErpRedeemScriptValues(
        Script defaultFederationRedeemScript,
        Script erpFederationRedeemScript,
        Long csvValue
    ) {
        if (!defaultFederationRedeemScript.isSentToMultiSig()
            || !(erpFederationRedeemScript.isSentToMultiSig())) {

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
