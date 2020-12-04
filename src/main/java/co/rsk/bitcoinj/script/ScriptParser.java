package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA1;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA2;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA4;
import static com.google.common.base.Preconditions.checkState;

import co.rsk.bitcoinj.core.ScriptException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScriptParser {
    private static final ScriptChunk[] STANDARD_TRANSACTION_SCRIPT_CHUNKS = {
        new ScriptChunk(ScriptOpCodes.OP_DUP, null, 0),
        new ScriptChunk(ScriptOpCodes.OP_HASH160, null, 1),
        new ScriptChunk(ScriptOpCodes.OP_EQUALVERIFY, null, 23),
        new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null, 24),
    };

    public static ScriptParserResult parseScriptProgram(byte[] scriptProgram) {
        List<ScriptChunk> chunks = new ArrayList<ScriptChunk>(5);   // Common size.
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(scriptProgram);
            int initialSize = bis.available();
            while (bis.available() > 0) {
                int startLocationInProgram = initialSize - bis.available();
                int opcode = bis.read();

                long dataToRead = -1;
                if (opcode >= 0 && opcode < OP_PUSHDATA1) {
                    // Read some bytes of data, where how many is the opcode value itself.
                    dataToRead = opcode;
                } else if (opcode == OP_PUSHDATA1) {
                    if (bis.available() < 1) throw new ScriptException("Unexpected end of script");
                    dataToRead = bis.read();
                } else if (opcode == OP_PUSHDATA2) {
                    // Read a short, then read that many bytes of data.
                    if (bis.available() < 2) throw new ScriptException("Unexpected end of script");
                    dataToRead = bis.read() | (bis.read() << 8);
                } else if (opcode == OP_PUSHDATA4) {
                    // Read a uint32, then read that many bytes of data.
                    // Though this is allowed, because its value cannot be > 520, it should never actually be used
                    if (bis.available() < 4) throw new ScriptException("Unexpected end of script");
                    dataToRead = ((long)bis.read()) | (((long)bis.read()) << 8) | (((long)bis.read()) << 16) | (((long)bis.read()) << 24);
                }

                ScriptChunk chunk;
                if (dataToRead == -1) {
                    chunk = new ScriptChunk(opcode, null, startLocationInProgram);
                } else {
                    if (dataToRead > bis.available())
                        throw new ScriptException("Push of data element that is larger than remaining data");
                    byte[] data = new byte[(int)dataToRead];
                    checkState(dataToRead == 0 || bis.read(data, 0, (int)dataToRead) == dataToRead);
                    chunk = new ScriptChunk(opcode, data, startLocationInProgram);
                }
                // Save some memory by eliminating redundant copies of the same chunk objects.
                for (ScriptChunk c : STANDARD_TRANSACTION_SCRIPT_CHUNKS) {
                    if (c.equals(chunk)) chunk = c;
                }
                chunks.add(chunk);
            }
        } catch (ScriptException exception) {
            return new ScriptParserResult(chunks, exception);
        }
        return new ScriptParserResult(chunks);
    }
}
