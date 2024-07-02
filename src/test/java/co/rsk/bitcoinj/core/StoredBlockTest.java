package co.rsk.bitcoinj.core;

import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;

public class StoredBlockTest {
    private static final NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    // Just an arbitrary block
    private static final String blockHeader = "00e00820925b77c9ff4d0036aa29f3238cde12e9af9d55c34ed30200000000000000000032a9fa3e12ef87a2327b55db6a16a1227bb381db8b269d90aa3a6e38cf39665f91b47766255d0317c1b1575f";
    private static final int blockHeight = 849137;
    private static final BtcBlock block = new BtcBlock(mainnet, Hex.decode(blockHeader));

    private static final int blockCapacity = StoredBlock.COMPACT_SERIALIZED_SIZE;
    private ByteBuffer blockBuffer;

    @Before
    public void setUp() {
        blockBuffer = ByteBuffer.allocate(blockCapacity);
    }

    @Test
    public void newStoredBlock_createsExpectedBlock() {
        BigInteger chainWork = new BigInteger("ffffffffffffffff", 16); // 8 bytes
        StoredBlock blockToStore = new StoredBlock(block, chainWork, blockHeight);

        // assert block was correctly created
        assertEquals(chainWork, blockToStore.getChainWork());
        assertEquals(block, blockToStore.getHeader());
        assertEquals(blockHeight, blockToStore.getHeight());
    }

    @Test
    public void serializeAndDeserializeCompact_forZeroChainWork_works() {
        BigInteger chainWork = BigInteger.ZERO;
        StoredBlock blockToStore = new StoredBlock(block, chainWork, blockHeight);

        // serialize block
        blockToStore.serializeCompact(blockBuffer);
        assertEquals(blockCapacity, blockBuffer.position());

        // deserialize block
        blockBuffer.rewind();
        StoredBlock blockDeserialized = StoredBlock.deserializeCompact(mainnet, blockBuffer);
        assertEquals(blockDeserialized, blockToStore);
    }

    @Test
    public void serializeAndDeserializeCompact_forSmallChainWork_works() {
        BigInteger chainWork = BigInteger.ONE;
        StoredBlock blockToStore = new StoredBlock(block, chainWork, blockHeight);

        // serialize block
        blockToStore.serializeCompact(blockBuffer);
        assertEquals(blockCapacity, blockBuffer.position());

        // deserialize block
        blockBuffer.rewind();
        StoredBlock blockDeserialized = StoredBlock.deserializeCompact(mainnet, blockBuffer);
        assertEquals(blockDeserialized, blockToStore);
    }

    @Test
    public void serializeAndDeserializeCompact_for8bytesChainWork_works() {
        BigInteger chainWork = new BigInteger("ffffffffffffffff", 16); // 8 bytes
        StoredBlock blockToStore = new StoredBlock(block, chainWork, blockHeight);

        // serialize block
        blockToStore.serializeCompact(blockBuffer);
        assertEquals(blockCapacity, blockBuffer.position());

        // deserialize block
        blockBuffer.rewind();
        StoredBlock blockDeserialized = StoredBlock.deserializeCompact(mainnet, blockBuffer);
        assertEquals(blockDeserialized, blockToStore);
    }

    @Test
    public void serializeAndDeserializeCompact_forMax12bytesChainWork_works() {
        BigInteger chainWork = new BigInteger("ffffffffffffffffffffffff", 16); // max chain work to fit in 12 unsigned bytes
        StoredBlock blockToStore = new StoredBlock(block, chainWork, blockHeight);

        // serialize block
        blockToStore.serializeCompact(blockBuffer);
        assertEquals(blockCapacity, blockBuffer.position());

        // deserialize block
        blockBuffer.rewind();
        StoredBlock blockDeserialized = StoredBlock.deserializeCompact(mainnet, blockBuffer);
        assertEquals(blockDeserialized, blockToStore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeCompact_for13bytesChainWork_throwsException() {
        BigInteger chainWork = new BigInteger("ffffffffffffffffffffffff", 16).add(BigInteger.valueOf(1)); // too large chain work to fit in 12 unsigned bytes
        StoredBlock blockToStore = new StoredBlock(block, chainWork, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompact(blockBuffer);
    }
}
