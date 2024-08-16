package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.ScriptException;
import org.junit.Test;

public class NoRedeemScriptParserTest {

    private final RedeemScriptParser noRedeemScriptParser = new NoRedeemScriptParser();

    @Test (expected = ScriptException.class)
    public void extractStandardRedeemScript_whenNoRedeemScriptIsCalled_shouldThrowScriptException() {
        // Act
        noRedeemScriptParser.extractStandardRedeemScript();
    }
}
