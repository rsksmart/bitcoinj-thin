package co.rsk.bitcoinj.core;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class StoredBlockTest {

    private static final BigInteger NEGATIVE_CHAIN_WORK = BigInteger.valueOf(-1);
    private static final BigInteger ZERO_CHAIN_WORK = BigInteger.ZERO;
    private static final BigInteger SMALL_CHAIN_WORK = BigInteger.ONE;
    // 8 bytes chain work
    private static final BigInteger EIGHT_BYTES_WORK_V1 = new BigInteger("ffffffffffffffff", 16); // 8 bytes
    // Max chain work to fit in 12 bytes
    private static final BigInteger MAX_WORK_V1 = new BigInteger(/* 12 bytes */ "ffffffffffffffffffffffff", 16);
    // Chain work too large to fit in 12 bytes
    private static final BigInteger TOO_LARGE_WORK_V1 = new BigInteger(/* 13 bytes */ "ffffffffffffffffffffffffff", 16);
    // Max chain work to fit in 32 bytes
    private static final BigInteger MAX_WORK_V2 = new BigInteger(/* 32 bytes */
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    // Chain work too large to fit in 32 bytes
    private static final BigInteger TOO_LARGE_WORK_V2 = new BigInteger(/* 33 bytes */
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    private static final NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    private static final BitcoinSerializer bitcoinSerializer = new BitcoinSerializer(mainnet, false);

    // Just an arbitrary block
    private static final String blockHeader = "00e00820925b77c9ff4d0036aa29f3238cde12e9af9d55c34ed30200000000000000000032a9fa3e12ef87a2327b55db6a16a1227bb381db8b269d90aa3a6e38cf39665f91b47766255d0317c1b1575f";
    private static final int blockHeight = 849137;
    private static final BtcBlock block = bitcoinSerializer.makeBlock(Hex.decode(blockHeader));

    private static final int blockCapacity = StoredBlock.COMPACT_SERIALIZED_SIZE;
    private ByteBuffer blockBuffer;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        blockBuffer = ByteBuffer.allocate(blockCapacity);
    }

    @Test
    public void newStoredBlock_createsExpectedBlock() {
        StoredBlock blockToStore = new StoredBlock(block, EIGHT_BYTES_WORK_V1, blockHeight);

        // assert block was correctly created
        assertEquals(EIGHT_BYTES_WORK_V1, blockToStore.getChainWork());
        assertEquals(block, blockToStore.getHeader());
        assertEquals(blockHeight, blockToStore.getHeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeCompact_forNegativeChainWork_throwsException() {

        StoredBlock blockToStore = new StoredBlock(block, NEGATIVE_CHAIN_WORK, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompact(blockBuffer);
    }

    @Test
    public void serializeAndDeserializeCompact_forZeroChainWork_works() {
        StoredBlock blockToStore = new StoredBlock(block, ZERO_CHAIN_WORK, blockHeight);

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

        StoredBlock blockToStore = new StoredBlock(block, SMALL_CHAIN_WORK, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(block, EIGHT_BYTES_WORK_V1, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(block, MAX_WORK_V1, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(block, TOO_LARGE_WORK_V1, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompact(blockBuffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeCompact_for32bytesChainWork_throwsException() {
        StoredBlock blockToStore = new StoredBlock(block, MAX_WORK_V2, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompact(blockBuffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeCompact_forMoreThan32bytesChainWork_throwsException() {
        StoredBlock blockToStore = new StoredBlock(block, TOO_LARGE_WORK_V2, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompact(blockBuffer);
    }


    @Test(expected = IllegalArgumentException.class)
    public void serializeCompactV2_forNegativeChainWork_throwsException() {
        StoredBlock blockToStore = new StoredBlock(block, NEGATIVE_CHAIN_WORK, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompactV2(blockBuffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeCompactV2_forMoreThan32bytesChainWork_throwsException() {
        StoredBlock blockToStore = new StoredBlock(block, TOO_LARGE_WORK_V2, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompactV2(blockBuffer);
    }

    @Test
    public void serializeCompactV2_forZeroChainWork_works() {
        testSerializeCompactV2(ZERO_CHAIN_WORK);
    }

    @Test
    public void serializeCompactV2_forSmallChainWork_works() {
        testSerializeCompactV2(SMALL_CHAIN_WORK);
    }

    @Test
    public void serializeCompactV2_for8BytesChainWork_works() {
        testSerializeCompactV2(EIGHT_BYTES_WORK_V1);
    }

    @Test
    public void serializeCompactV2_for12ByteChainWork_works() {
        testSerializeCompactV2(MAX_WORK_V1);
    }

    @Test
    public void serializeCompactV2_forTooLargeWorkV1_works() {
        testSerializeCompactV2(TOO_LARGE_WORK_V1);
    }

    @Test
    public void serializeCompactV2_for32BytesChainWork_works() {
        testSerializeCompactV2(MAX_WORK_V2);
    }

    private void testSerializeCompactV2(BigInteger chainWork) {
        StoredBlock blockToStore = new StoredBlock(block, chainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE_V2);
        blockToStore.serializeCompactV2(buf);
        // assert serialized size and that the buffer is full
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE_V2, buf.position());
    }
}
