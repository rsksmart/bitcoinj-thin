package co.rsk.bitcoinj.script;

import static com.google.common.base.Preconditions.checkArgument;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlyoverRedeemScriptParser implements RedeemScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(FlyoverRedeemScriptParser.class);

    private final MultiSigType multiSigType;
    private final List<ScriptChunk> redeemScriptChunks;
    private final RedeemScriptParser redeemScriptParser;

    public FlyoverRedeemScriptParser(List<ScriptChunk> redeemScriptChunks) {
        this.redeemScriptChunks = extractStandardRedeemScriptChunks(redeemScriptChunks);
        this.redeemScriptParser = RedeemScriptParserFactory.get(this.redeemScriptChunks.subList(2, this.redeemScriptChunks.size()));
        this.multiSigType = MultiSigType.FLYOVER;
    }

    @Override
    public ScriptType getScriptType() {
        return null;
    }

    @Override
    public int getSigInsertionIndex(Sha256Hash hash, BtcECKey signingKey) {
        return 0;
    }

    @Override
    public MultiSigType getMultiSigType() {
        return multiSigType;
    }

    @Override
    public int getM() {
        checkArgument(redeemScriptChunks.get(0).isOpCode());
        return Script.decodeFromOpN(redeemScriptChunks.get(0).opcode);
    }

    @Override
    public int findKeyInRedeem(BtcECKey key) {
        checkArgument(redeemScriptChunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(redeemScriptChunks.get(redeemScriptChunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            if (Arrays.equals(redeemScriptChunks.get(1 + i).data, key.getPubKey())) {
                return i;
            }
        }

        throw new IllegalStateException("Could not find matching key " + key.toString() + " in script " + this);
    }

    @Override
    public List<BtcECKey> getPubKeys() {
        ArrayList<BtcECKey> result = Lists.newArrayList();
        int numKeys = Script.decodeFromOpN(redeemScriptChunks.get(redeemScriptChunks.size() - 2).opcode);
        for (int i = 0; i < numKeys; i++) {
            result.add(BtcECKey.fromPublicOnly(redeemScriptChunks.get(1 + i).data));
        }

        return result;
    }

    @Override
    public int findSigInRedeem(byte[] signatureBytes, Sha256Hash hash) {
        checkArgument(redeemScriptChunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = Script.decodeFromOpN(redeemScriptChunks.get(redeemScriptChunks.size() - 2).opcode);
        TransactionSignature signature = TransactionSignature.decodeFromBitcoin(signatureBytes, true);
        for (int i = 0; i < numKeys; i++) {
            if (BtcECKey.fromPublicOnly(redeemScriptChunks.get(i + 1).data).verify(hash, signature)) {
                return i;
            }
        }
        throw new IllegalStateException("Could not find matching key for signature on " + hash.toString()
            + " sig " + Utils.HEX.encode(signatureBytes)
        );
    }

    @Override
    public List<ScriptChunk> extractStandardRedeemScriptChunks() {
        return redeemScriptParser.extractStandardRedeemScriptChunks();
    }

    public static List<ScriptChunk> extractStandardRedeemScriptChunks(List<ScriptChunk> chunks) {
        List<ScriptChunk> chunksForRedeem = new ArrayList<>();

        int i = 1;
        while (i < chunks.size() && !chunks.get(i).equalsOpCode(ScriptOpCodes.OP_ELSE)) {
            chunksForRedeem.add(chunks.get(i));
            i++;
        }

        // Validate the obtained redeem script has a valid format
        if (!RedeemScriptValidator.hasStandardRedeemScriptStructure(chunksForRedeem)) {
            String message = "Flyover redeem script obtained has an invalid structure";
            logger.debug("[extractStandardRedeemScriptChunks] {} {}", message, chunksForRedeem);
            throw new VerificationException(message);
        }

        return chunksForRedeem;
    }
}
