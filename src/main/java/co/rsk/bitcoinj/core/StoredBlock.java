/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.rsk.bitcoinj.core;

import co.rsk.bitcoinj.store.BtcBlockStore;
import co.rsk.bitcoinj.store.BlockStoreException;
import com.google.common.base.Objects;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Wraps a {@link BtcBlock} object with extra data that can be derived from the block chain but is slow or inconvenient to
 * calculate. By storing it alongside the block header we reduce the amount of work required significantly.
 * Recalculation is slow because the fields are cumulative - to find the chainWork you have to iterate over every
 * block in the chain back to the genesis block, which involves lots of seeking/loading etc. So we just keep a
 * running total: it's a disk space vs cpu/io tradeoff.<p>
 *
 * StoredBlocks are put inside a {@link BtcBlockStore} which saves them to memory or disk.
 */
public class StoredBlock {
    /* @deprecated Use {@link #CHAIN_WORK_BYTES_V2} instead.
        Size in bytes to represent the total amount of work done so far on this chain. As of June 22, 2024, it takes 12
        unsigned bytes to store this value, so developers should use the V2 format.
     */
    private static final int CHAIN_WORK_BYTES_LEGACY = 12;
    // Size in bytes to represent the total amount of work done so far on this chain.
    private static final int CHAIN_WORK_BYTES_V2 = 32;
    // Size in bytes(int) to represent btc block height
    private static final int HEIGHT_BYTES = 4;

    // Size in bytes of serialized block in legacy format by {@link #serializeCompactLegacy(ByteBuffer)}
    public static final int COMPACT_SERIALIZED_SIZE_LEGACY = BtcBlock.HEADER_SIZE + CHAIN_WORK_BYTES_LEGACY
        + HEIGHT_BYTES;
    // Size in bytes of serialized block in V2 format by {@link #serializeCompactV2(ByteBuffer)}
    public static final int COMPACT_SERIALIZED_SIZE_V2 = BtcBlock.HEADER_SIZE + CHAIN_WORK_BYTES_V2 + HEIGHT_BYTES;

    private BtcBlock header;
    private BigInteger chainWork;
    private int height;

    public StoredBlock(BtcBlock header, BigInteger chainWork, int height) {
        this.header = header;
        this.chainWork = chainWork;
        this.height = height;
    }

    /**
     * The block header this object wraps. The referenced block object must not have any transactions in it.
     */
    public BtcBlock getHeader() {
        return header;
    }

    /**
     * The total sum of work done in this block, and all the blocks below it in the chain. Work is a measure of how
     * many tries are needed to solve a block. If the target is set to cover 10% of the total hash value space,
     * then the work represented by a block is 10.
     */
    public BigInteger getChainWork() {
        return chainWork;
    }

    /**
     * Position in the chain for this block. The genesis block has a height of zero.
     */
    public int getHeight() {
        return height;
    }

    /** Returns true if this objects chainWork is higher than the others. */
    public boolean moreWorkThan(StoredBlock other) {
        return chainWork.compareTo(other.chainWork) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredBlock other = (StoredBlock) o;
        return header.equals(other.header) && chainWork.equals(other.chainWork) && height == other.height;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(header, chainWork, height);
    }

    /**
     * Creates a new StoredBlock, calculating the additional fields by adding to the values in this block.
     */
    public StoredBlock build(BtcBlock block) throws VerificationException {
        // Stored blocks track total work done in this chain, because the canonical chain is the one that represents
        // the largest amount of work done not the tallest.
        BigInteger chainWork = this.chainWork.add(block.getWork());
        int height = this.height + 1;
        return new StoredBlock(block, chainWork, height);
    }

    /**
     * Given a block store, looks up the previous block in this chain. Convenience method for doing
     * <tt>store.get(this.getHeader().getPrevBlockHash())</tt>.
     *
     * @return the previous block in the chain or null if it was not found in the store.
     */
    public StoredBlock getPrev(BtcBlockStore store) throws BlockStoreException {
        return store.get(getHeader().getPrevBlockHash());
    }

    /**
     * @deprecated Use {@link #serializeCompactV2(ByteBuffer)} instead.
     *
     * Serializes the stored block to a custom packed format. Used internally.
     * As of June 22, 2024, it takes 12 unsigned bytes to store the chain work value,
     * so developers should use the V2 format.
     *
     * @param buffer buffer to write to
     */
    @Deprecated
    public void serializeCompactLegacy(ByteBuffer buffer) {
        serializeCompact(buffer, CHAIN_WORK_BYTES_LEGACY);

    }

    /**
     * Serializes the stored block to a custom packed format. Used internally.
     *
     * @param buffer buffer to write to
     */
    public void serializeCompactV2(ByteBuffer buffer) {
        serializeCompact(buffer, CHAIN_WORK_BYTES_V2);
    }

    private void serializeCompact(ByteBuffer buffer, int chainWorkSize) {
        byte[] chainWorkBytes = Utils.bigIntegerToBytes(getChainWork(), chainWorkSize);
        buffer.put(chainWorkBytes);
        buffer.putInt(getHeight());
        // Using unsafeBitcoinSerialize here can give us direct access to the same bytes we read off the wire,
        // avoiding serialization round-trips.
        byte[] bytes = getHeader().unsafeBitcoinSerialize();
        buffer.put(bytes, 0, BtcBlock.HEADER_SIZE);  // Trim the trailing 00 byte (zero transactions).
    }

    /**
     * @deprecated Use {@link #deserializeCompactV2(NetworkParameters, ByteBuffer)} instead.
     *
     * Deserializes the stored block from a custom packed format. Used internally.
     * As of June 22, 2024, it takes 12 unsigned bytes to store the chain work value,
     * so developers should use the V2 format.
     *
     * @param buffer data to deserialize
     * @return deserialized stored block
     */
    @Deprecated
    public static StoredBlock deserializeCompactLegacy(NetworkParameters params, ByteBuffer buffer) throws ProtocolException {
        byte[] chainWorkBytes = new byte[StoredBlock.CHAIN_WORK_BYTES_LEGACY];
        buffer.get(chainWorkBytes);
        BigInteger chainWork = new BigInteger(1, chainWorkBytes);
        int height = buffer.getInt();  // +4 bytes
        byte[] header = new byte[BtcBlock.HEADER_SIZE + 1];    // Extra byte for the 00 transactions length.
        buffer.get(header, 0, BtcBlock.HEADER_SIZE);
        return new StoredBlock(params.getDefaultSerializer().makeBlock(header), chainWork, height);
    }

    /**
     * Deserializes the stored block from a custom packed format. Used internally.
     *
     * @param buffer data to deserialize
     * @return deserialized stored block
     */
    public static StoredBlock deserializeCompactV2(NetworkParameters params, ByteBuffer buffer) throws ProtocolException {
        byte[] chainWorkBytes = new byte[StoredBlock.CHAIN_WORK_BYTES_V2];
        buffer.get(chainWorkBytes);
        BigInteger chainWork = new BigInteger(1, chainWorkBytes);
        int height = buffer.getInt();  // +4 bytes
        byte[] header = new byte[BtcBlock.HEADER_SIZE + 1];    // Extra byte for the 00 transactions length.
        buffer.get(header, 0, BtcBlock.HEADER_SIZE);
        return new StoredBlock(params.getDefaultSerializer().makeBlock(header), chainWork, height);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Block %s at height %d: %s",
                getHeader().getHashAsString(), getHeight(), getHeader().toString());
    }
}
