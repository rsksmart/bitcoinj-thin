package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.util.List;

public class NonStandardErpRedeemScriptParserHardcoded implements RedeemScriptParser {

    NonStandardErpRedeemScriptParserHardcoded() { }

    @Override
    public int getM() {
        return -1;
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        return -1;
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        throw new ScriptException("Only usable for multisig scripts.");
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        return 0;
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        throw new ScriptException("Only usable for multisig scripts.");
    }

    @Override
    public boolean hasErpFormat() {
        // This parser is exclusive for an invalid redeem script.
        // Therefore, it is not considered as a valid erp format.
        return false;
    }
}
