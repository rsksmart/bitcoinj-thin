package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.store.BtcBlockStore;
import co.rsk.bitcoinj.store.BtcMemoryBlockStore;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static co.rsk.bitcoinj.core.TransactionOutputTest.PARAMS;
import static co.rsk.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.assertFalse;

public class BtcBlockChainTest {

    BtcAbstractBlockChain blockchain;

    @Before
    public void setUp() throws Exception {
        final BtcBlockStore blockStore = new BtcMemoryBlockStore(PARAMS);
        blockchain = new BtcBlockChain(new Context(PARAMS), blockStore);
    }

    @Test
    public void orphanBlockNotStored() {
        int nonce = 0;
        BtcBlock block = new BtcBlock(
                PARAMS, 2l, Sha256Hash.of(HEX.decode("0011")),
                Sha256Hash.ZERO_HASH, PARAMS.genesisBlock.getTime().getTime() + 1, PARAMS.genesisBlock.getDifficultyTarget(),
                nonce, new ArrayList<BtcTransaction>());
        while (!block.checkProofOfWork(false)) {
            nonce++;
            block = new BtcBlock(
                    PARAMS, 2l, Sha256Hash.of(HEX.decode("0011")),
                    Sha256Hash.ZERO_HASH, PARAMS.genesisBlock.getTime().getTime() + 1, PARAMS.genesisBlock.getDifficultyTarget(),
                    nonce, new ArrayList<BtcTransaction>());
        }
        assertFalse(blockchain.add(block.cloneAsHeader()));
    }

}
