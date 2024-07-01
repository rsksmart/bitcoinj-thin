package co.rsk.bitcoinj.core;

import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;

public class StoredBlockTest {

    // Just an arbitrary block
    private static final byte[] blockBytes = { 0, -128, -71, 44, 36, -15, 35, 19, 10, -30, -98, -119, -97, 12, -85, 114, 101, 55, 34, -27, 76, -33, 59, 48, 68, 82, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, -57, 46, -83, 101, -93, -73, -118, -74, 55, -47, -121, 108, 0, 65, 74, 119, -28, 123, -52, 91, 82, 102, 122, -63, -27, 115, 99, 53, 99, -66, -91, -90, -107, -86, 119, 102, 37, 93, 3, 23, 40, -47, -126, -88 };
    private static final BtcBlock block = new BtcBlock(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), blockBytes);

    @Test
    public void serializeCompact_forZeroChainWork_works() {
        BigInteger zeroChainWork = BigInteger.ZERO;
        StoredBlock blockToStore = new StoredBlock(block, zeroChainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        blockToStore.serializeCompact(buf);
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE, buf.position());
        buf.rewind();
        assertEquals(StoredBlock.deserializeCompact(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), buf), blockToStore);
    }

    @Test
    public void serializeCompact_forSmallChainWork_works() {
        BigInteger smallChainWork = BigInteger.ONE;
        StoredBlock blockToStore = new StoredBlock(block, smallChainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        blockToStore.serializeCompact(buf);
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE, buf.position());
        buf.rewind();
        assertEquals(StoredBlock.deserializeCompact(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), buf), blockToStore);
    }

    @Test
    public void serializeCompact_forLessThan12bytesChainWork_works() {
        long maxLongValue = Long.MAX_VALUE;
        StoredBlock blockToStore = new StoredBlock(block, BigInteger.valueOf(maxLongValue), 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        blockToStore.serializeCompact(buf);
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE, buf.position());
        buf.rewind();
        assertEquals(StoredBlock.deserializeCompact(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), buf), blockToStore);
    }

    @Test
    public void serializeCompact_forMax12bytesChainWork_works() {
        BigInteger maxChainWork = new BigInteger("ffffffffffffffffffffffff", 16); // max chain work to fit in 12 unsigned bytes
        StoredBlock blockToStore = new StoredBlock(block, maxChainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        blockToStore.serializeCompact(buf);
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE, buf.position());
        buf.rewind();
        assertEquals(StoredBlock.deserializeCompact(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), buf), blockToStore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeCompact_for13bytesChainWork_throwsException() {
        BigInteger tooLargeChainWork = new BigInteger("ffffffffffffffffffffffff", 16).add(BigInteger.valueOf(1)); // too large chain work to fit in 12 unsigned bytes
        StoredBlock blockToStore = new StoredBlock(block, tooLargeChainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        blockToStore.serializeCompact(buf);
    }
}
