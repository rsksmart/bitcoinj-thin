package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;
import org.junit.Test;

public class NonStandardErpRedeemScriptParserHardcodedTest {

    private final RedeemScriptParser nonStandardErpRedeemScriptParserHardcoded = new NonStandardErpRedeemScriptParserHardcoded();

    @Test (expected = ScriptException.class)
    public void extractStandardRedeemScriptChunks_shouldThrowScriptException() {
        // Act
        nonStandardErpRedeemScriptParserHardcoded.extractStandardRedeemScriptChunks();
    }
}
