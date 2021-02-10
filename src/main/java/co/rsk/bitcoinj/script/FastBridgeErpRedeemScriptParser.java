package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.VerificationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastBridgeErpRedeemScriptParser extends StandardRedeemScriptParser {
    private static final Logger logger = LoggerFactory.getLogger(FastBridgeRedeemScriptParser.class);


    public FastBridgeErpRedeemScriptParser(
        ScriptType scriptType,
        List<ScriptChunk> redeemScript,
        List<ScriptChunk> chunks
    ) {
        super(
            scriptType,
            redeemScript,
            chunks
        );
        this.multiSigType = MultiSigType.FAST_BRIDGE_ERP_FED;
    }

    public static Script extractStandardRedeemScriptFromFastBridgeErpRedeemScript(
        List<ScriptChunk> chunks
    ) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        ScriptChunk firstChunk = chunks.get(0);

        if (firstChunk.data == null) {
            String message = "First chunk data is null";
            logger.debug(message);
            throw new VerificationException(message);
        }

        boolean hasFastBridgeStructure = firstChunk.opcode == 32 && firstChunk.data.length == 32 &&
            chunks.get(1).opcode == ScriptOpCodes.OP_DROP;

        if (!hasFastBridgeStructure) {
            String message = "Fast bridge structure is not correct";
            logger.debug(message);
            throw new VerificationException(message);
        }

        // Add validations (like done in factory)
        boolean hasErpPrefix = chunks.get(3).opcode == ScriptOpCodes.OP_NOTIF;
        boolean hasEndIfOpcode = chunks.get(chunks.size() - 1).equalsOpCode(ScriptOpCodes.OP_ENDIF);

        if (!hasErpPrefix || !hasEndIfOpcode) {
            String message = "Invalid structure for ERP redeem script";
            logger.debug(message);
            throw new VerificationException(message);
        }

        int i = 3;
        while (!chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i ++;
        }

        return new Script(chunksForRedeem);
    }
}
