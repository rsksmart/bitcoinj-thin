package co.rsk.bitcoinj.core;

import java.nio.Buffer;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StoredBlockTest {

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

    // Just an arbitrary block
    private static final String blockHeader = "00e00820925b77c9ff4d0036aa29f3238cde12e9af9d55c34ed30200000000000000000032a9fa3e12ef87a2327b55db6a16a1227bb381db8b269d90aa3a6e38cf39665f91b47766255d0317c1b1575f";
    private static final int blockHeight = 849137;
    private static final BtcBlock BLOCK = new BtcBlock(mainnet, Hex.decode(blockHeader));

    private static final int blockCapacity = StoredBlock.COMPACT_SERIALIZED_SIZE;
    private ByteBuffer blockBuffer;

    @Before
    public void setUp() {
        blockBuffer = ByteBuffer.allocate(blockCapacity);
    }

    @Test
    public void newStoredBlock_createsExpectedBlock() {
        BigInteger chainWork = new BigInteger("ffffffffffffffff", 16); // 8 bytes
        StoredBlock blockToStore = new StoredBlock(BLOCK, chainWork, blockHeight);

        // assert block was correctly created
        assertEquals(chainWork, blockToStore.getChainWork());
        assertEquals(BLOCK, blockToStore.getHeader());
        assertEquals(blockHeight, blockToStore.getHeight());
    }

    @Test
    public void serializeAndDeserializeCompact_forZeroChainWork_works() {
        BigInteger chainWork = BigInteger.ZERO;
        StoredBlock blockToStore = new StoredBlock(BLOCK, chainWork, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(BLOCK, chainWork, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(BLOCK, chainWork, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(BLOCK, chainWork, blockHeight);

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
        StoredBlock blockToStore = new StoredBlock(BLOCK, chainWork, blockHeight);

        // serialize block should throw illegal argument exception
        blockToStore.serializeCompact(blockBuffer);
    }


    private List<BigInteger> vectors_serializeCompact_pass() {
        return Arrays.asList(
            BigInteger.ZERO, // no work
            BigInteger.ONE, // small work
            BigInteger.valueOf(Long.MAX_VALUE), // a larg-ish work
            MAX_WORK_V1
        );
    }

    @Test
    public void roundtripSerializeCompact_pass() {
        for (BigInteger chainWork : vectors_serializeCompact_pass()) {
            roundtripSerializeCompact(chainWork);
        }
    }

    private List<BigInteger> vectors_serializeCompact_fail() {
        return Arrays.asList(
            TOO_LARGE_WORK_V1,
            MAX_WORK_V2,
            TOO_LARGE_WORK_V2,
            BigInteger.valueOf(-1) // negative
        );
    }

    @Test(expected = RuntimeException.class)
    public void roundtripSerializeCompact_fail() {
        for (BigInteger chainWork : vectors_serializeCompact_fail()) {
            roundtripSerializeCompact(chainWork);
        }
    }

    private void roundtripSerializeCompact(BigInteger chainWork) {
        StoredBlock block = new StoredBlock(BLOCK, chainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        block.serializeCompact(buf);
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE, buf.position());
        buf.rewind();
        assertEquals(StoredBlock.deserializeCompact(mainnet, buf), block);
    }

    private List<BigInteger> vectors_serializeCompactV2_pass() {
        return Arrays.asList(
            BigInteger.ZERO, // no work
            BigInteger.ONE, // small work
            BigInteger.valueOf(Long.MAX_VALUE), // a larg-ish work
            MAX_WORK_V1,
            TOO_LARGE_WORK_V1,
            MAX_WORK_V2
        );
    }

    @Test
    public void roundtripSerializeCompactV2_pass() {
        for (BigInteger chainWork : vectors_serializeCompactV2_pass()) {
            roundtripSerializeCompactV2(chainWork);
        }
    }

    private List<BigInteger> vectors_serializeCompactV2_fail() {
        return Arrays.asList(
            TOO_LARGE_WORK_V2,
            BigInteger.valueOf(-1) // negative
        );
    }

    @Test(expected = RuntimeException.class)
    public void roundtripSerializeCompactV2_fail() {
        for (BigInteger chainWork : vectors_serializeCompactV2_fail()) {
            roundtripSerializeCompactV2(chainWork);
        }
    }

    private void roundtripSerializeCompactV2(BigInteger chainWork) {
        StoredBlock block = new StoredBlock(BLOCK, chainWork, 0);
        ByteBuffer buf = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE_V2);
        block.serializeCompact(buf);
        assertEquals(StoredBlock.COMPACT_SERIALIZED_SIZE_V2, buf.position());
        buf.rewind();
        assertEquals(StoredBlock.deserializeCompact(mainnet, buf), block);
    }

    @Test
    public void moreWorkThan() {
        StoredBlock noWorkBlock = new StoredBlock(BLOCK, BigInteger.ZERO, 0);
        StoredBlock smallWorkBlock = new StoredBlock(BLOCK, BigInteger.ONE, 0);
        StoredBlock maxWorkBlockV1 = new StoredBlock(BLOCK, MAX_WORK_V1, 0);
        StoredBlock maxWorkBlockV2 = new StoredBlock(BLOCK, MAX_WORK_V2, 0);

        assertTrue(smallWorkBlock.moreWorkThan(noWorkBlock));
        assertTrue(maxWorkBlockV1.moreWorkThan(noWorkBlock));
        assertTrue(maxWorkBlockV1.moreWorkThan(smallWorkBlock));
        assertTrue(maxWorkBlockV2.moreWorkThan(maxWorkBlockV1));
    }
}
