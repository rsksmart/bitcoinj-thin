package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;

import java.util.List;

public class P2shP2wshErpCustomRedeemScriptParser implements RedeemScriptParser {
//    private final StandardCustomRedeemScriptParser defaultCustomRedeemScriptParser;

//    P2shP2wshErpCustomRedeemScriptParser(
//        List<ScriptChunk> redeemScriptChunks
//    ) {
//        List<ScriptChunk> defaultCustomRedeemScriptChunks = extractCustomDefaultRedeemScriptChunks(redeemScriptChunks);
//        this.defaultCustomRedeemScriptParser = new StandardCustomRedeemScriptParser(defaultCustomRedeemScriptChunks);
//    }

    @Override
    public int getM() {
        return 0;
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        return 0;
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        return null;
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        return 0;
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return null;
    }

    @Override
    public boolean hasErpFormat() {
        return false;
    }
}
