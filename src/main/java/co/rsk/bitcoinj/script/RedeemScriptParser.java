package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import java.util.List;

public interface RedeemScriptParser {
    int getM();

    int findKeyInRedeem(BtcECKey key);

    List<BtcECKey> getPubKeys();

    int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash);

    List<ScriptChunk> extractStandardRedeemScriptChunks();

    boolean hasErpFormat();
}
