package co.rsk.bitcoinj.deprecated.core;

import co.rsk.bitcoinj.deprecated.store.BtcBlockStore;
import co.rsk.bitcoinj.deprecated.store.BtcMemoryBlockStore;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static co.rsk.bitcoinj.deprecated.core.Utils.HEX;
import static org.junit.Assert.assertFalse;

public class BtcBlockChainTest {

    BtcAbstractBlockChain blockchain;

    @Before
    public void setUp() throws Exception {
        final BtcBlockStore blockStore = new BtcMemoryBlockStore(TransactionOutputTest.PARAMS);
        blockchain = new BtcBlockChain(new Context(TransactionOutputTest.PARAMS), blockStore);
    }

    @Test
    public void orphanBlockNotStored() {
        int nonce = 0;
        BtcBlock block = new BtcBlock(
                TransactionOutputTest.PARAMS, 2l, Sha256Hash.of(HEX.decode("0011")),
                Sha256Hash.ZERO_HASH, TransactionOutputTest.PARAMS.genesisBlock.getTime().getTime() + 1, TransactionOutputTest.PARAMS.genesisBlock.getDifficultyTarget(),
                nonce, new ArrayList<BtcTransaction>());
        while (!block.checkProofOfWork(false)) {
            nonce++;
            block = new BtcBlock(
                    TransactionOutputTest.PARAMS, 2l, Sha256Hash.of(HEX.decode("0011")),
                    Sha256Hash.ZERO_HASH, TransactionOutputTest.PARAMS.genesisBlock.getTime().getTime() + 1, TransactionOutputTest.PARAMS.genesisBlock.getDifficultyTarget(),
                    nonce, new ArrayList<BtcTransaction>());
        }
        assertFalse(blockchain.add(block.cloneAsHeader()));
    }

}
