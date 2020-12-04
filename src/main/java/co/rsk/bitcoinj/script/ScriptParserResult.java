package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;
import java.util.List;
import java.util.Optional;

public class ScriptParserResult {

    private final List<ScriptChunk> chunks;
    private final Optional<ScriptException> exception;

    public ScriptParserResult(List<ScriptChunk> chunks) {
        this.chunks = chunks;
        this.exception = Optional.empty();
    }

    public ScriptParserResult(List<ScriptChunk> chunks, ScriptException exception) {
        this.chunks = chunks;
        this.exception = Optional.of(exception);
    }

    public Optional<ScriptException> getException() {
        return exception;
    }

    public List<ScriptChunk> getChunks() {
        return chunks;
    }
}
