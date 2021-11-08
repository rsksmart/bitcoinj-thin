package co.rsk.bitcoinj.deprecated.script;

import co.rsk.bitcoinj.deprecated.core.BtcECKey;
import co.rsk.bitcoinj.deprecated.core.ScriptException;
import co.rsk.bitcoinj.deprecated.core.Sha256Hash;
import java.util.List;

public class NoRedeemScriptParser implements RedeemScriptParser {

    @Override
    public MultiSigType getMultiSigType() {
        return MultiSigType.NO_MULTISIG_TYPE;
    }

    @Override
    public ScriptType getScriptType() {
        return ScriptType.UNDEFINED;
    }

    @Override
    public int getM() {
        return -1;
    }

    @Override
    public int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey) {
        return 0;
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
    public Script extractStandardRedeemScript() {
        throw new ScriptException("Only usable for multisig scripts.");
    }
}
