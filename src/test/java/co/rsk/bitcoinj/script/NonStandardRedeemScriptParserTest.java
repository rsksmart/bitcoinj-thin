package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;
import org.junit.Test;

public class NonStandardRedeemScriptParserTest {

    private final RedeemScriptParser nonStandardRedeemScriptParser = new NonStandardRedeemScriptParser();

    @Test (expected = ScriptException.class)
    public void extractStandardRedeemScriptChunks_shouldThrowScriptException() {
        // Act
        nonStandardRedeemScriptParser.extractStandardRedeemScriptChunks();
    }
}
