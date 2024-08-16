package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.util.List;

public interface RedeemScriptParser {

    enum MultiSigType {
        NO_MULTISIG_TYPE,
        STANDARD_MULTISIG,
        FAST_BRIDGE_MULTISIG,
        ERP_FED,
        FAST_BRIDGE_ERP_FED,
        P2SH_ERP_FED,
        FAST_BRIDGE_P2SH_ERP_FED
    }

    enum ScriptType {
        P2SH,
        REDEEM_SCRIPT,
        UNDEFINED
    }

    MultiSigType getMultiSigType();

    ScriptType getScriptType();

    int getM();

    int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey);

    int findKeyInRedeem(BtcECKey key);

    List<BtcECKey> getPubKeys();

    int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash);

    List<ScriptChunk> extractStandardRedeemScript();
}
