package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.ScriptException;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.util.List;

// this class exists to keep backwards compatibility
public class NonStandardRedeemScriptHardcodedParser implements RedeemScriptParser {
    @Override
    public MultiSigType getMultiSigType() {
        return MultiSigType.NO_MULTISIG_TYPE;
    }

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
    public List<ScriptChunk> extractStandardRedeemScript() {
        throw new ScriptException("Only usable for multisig scripts.");
    }
}
