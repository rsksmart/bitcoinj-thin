/*
 * Copyright 2012 The Bitcoin Developers
 * Copyright 2012 Matt Corallo
 * Copyright 2015 Andreas Schildbach
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

package co.rsk.bitcoinj.deprecated.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Objects;

/**
 * <p>A data structure that contains proofs of block inclusion for one or more transactions, in an efficient manner.</p>
 *
 * <p>The encoding works as follows: we traverse the tree in depth-first order, storing a bit for each traversed node,
 * signifying whether the node is the parent of at least one matched leaf txid (or a matched txid itself). In case we
 * are at the leaf level, or this bit is 0, its merkle node hash is stored, and its children are not explored further.
 * Otherwise, no hash is stored, but we recurse into both (or the only) child branch. During decoding, the same
 * depth-first traversal is performed, consuming bits and hashes as they were written during encoding.</p>
 *
 * <p>The serialization is fixed and provides a hard guarantee about the encoded size,
 * <tt>SIZE &lt;= 10 + ceil(32.25*N)</tt> where N represents the number of leaf nodes of the partial tree. N itself
 * is bounded by:</p>
 *
 * <p>
 * N &lt;= total_transactions<br>
 * N &lt;= 1 + matched_transactions*tree_height
 * </p>
 *
 * <p><pre>The serialization format:
 *  - uint32     total_transactions (4 bytes)
 *  - varint     number of hashes   (1-3 bytes)
 *  - uint256[]  hashes in depth-first order (&lt;= 32*N bytes)
 *  - varint     number of bytes of flag bits (1-3 bytes)
 *  - byte[]     flag bits, packed per 8 in a byte, least significant bit first (&lt;= 2*N-1 bits)
 * The size constraints follow from this.</pre></p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class PartialMerkleTree extends Message {
    // the total number of transactions in the block
    private int transactionCount;

    // node-is-parent-of-matched-txid bits
    private byte[] matchedChildBits;

    // txids and internal hashes
    private List<Sha256Hash> hashes;

    // coinbase to check is part of a valid 64 byte tx 
    private static long COINBASE_CHECKED = Utils.readUint32(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}, 0);
    
    public PartialMerkleTree(NetworkParameters params, byte[] payloadBytes, int offset) throws ProtocolException {
        super(params, payloadBytes, offset);
    }

    /**
     * Constructs a new PMT with the given bit set (little endian) and the raw list of hashes including internal hashes,
     * taking ownership of the list.
     */
    public PartialMerkleTree(NetworkParameters params, byte[] bits, List<Sha256Hash> hashes, int origTxCount) {
        super(params);
        this.matchedChildBits = bits;
        this.hashes = hashes;
        this.transactionCount = origTxCount;
    }

    /**
     * Calculates a PMT given the list of leaf hashes and which leaves need to be included. The relevant interior hashes
     * are calculated and a new PMT returned.
     */
    public static PartialMerkleTree buildFromLeaves(NetworkParameters params, byte[] includeBits, List<Sha256Hash> allLeafHashes) {
        // Calculate height of the tree.
        int height = 0;
        while (getTreeWidth(allLeafHashes.size(), height) > 1)
            height++;
        List<Boolean> bitList = new ArrayList<Boolean>();
        List<Sha256Hash> hashes = new ArrayList<Sha256Hash>();
        traverseAndBuild(height, 0, allLeafHashes, includeBits, bitList, hashes);
        byte[] bits = new byte[(int)Math.ceil(bitList.size() / 8.0)];
        for (int i = 0; i < bitList.size(); i++)
            if (bitList.get(i))
                Utils.setBitLE(bits, i);
        return new PartialMerkleTree(params, bits, hashes, allLeafHashes.size());
    }

    @Override
    public void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        Utils.uint32ToByteStreamLE(transactionCount, stream);

        stream.write(new VarInt(hashes.size()).encode());
        for (Sha256Hash hash : hashes)
            stream.write(hash.getReversedBytes());

        stream.write(new VarInt(matchedChildBits.length).encode());
        stream.write(matchedChildBits);
    }

    @Override
    protected void parse() throws ProtocolException {
        transactionCount = (int) readUint32();

        int nHashes = (int) readVarInt();
        hashes = new ArrayList<Sha256Hash>(Math.min(nHashes, Utils.MAX_INITIAL_ARRAY_LENGTH));
        for (int i = 0; i < nHashes; i++)
            hashes.add(readHash());

        int nFlagBytes = (int) readVarInt();
        matchedChildBits = readBytes(nFlagBytes);

        length = cursor - offset;
    }

    // Based on CPartialMerkleTree::TraverseAndBuild in Bitcoin Core.
    private static void traverseAndBuild(int height, int pos, List<Sha256Hash> allLeafHashes, byte[] includeBits,
                                         List<Boolean> matchedChildBits, List<Sha256Hash> resultHashes) {
        boolean parentOfMatch = false;
        // Is this node a parent of at least one matched hash?
        for (int p = pos << height; p < (pos+1) << height && p < allLeafHashes.size(); p++) {
            if (Utils.checkBitLE(includeBits, p)) {
                parentOfMatch = true;
                break;
            }
        }
        // Store as a flag bit.
        matchedChildBits.add(parentOfMatch);
        if (height == 0 || !parentOfMatch) {
            // If at height 0, or nothing interesting below, store hash and stop.
            resultHashes.add(calcHash(height, pos, allLeafHashes));
        } else {
            // Otherwise descend into the subtrees.
            int h = height - 1;
            int p = pos * 2;
            traverseAndBuild(h, p, allLeafHashes, includeBits, matchedChildBits, resultHashes);
            if (p + 1 < getTreeWidth(allLeafHashes.size(), h))
                traverseAndBuild(h, p + 1, allLeafHashes, includeBits, matchedChildBits, resultHashes);
        }
    }

    private static Sha256Hash calcHash(int height, int pos, List<Sha256Hash> hashes) {
        if (height == 0) {
            // Hash at height 0 is just the regular tx hash itself.
            return hashes.get(pos);
        }
        int h = height - 1;
        int p = pos * 2;
        Sha256Hash left = calcHash(h, p, hashes);
        // Calculate right hash if not beyond the end of the array - copy left hash otherwise.
        Sha256Hash right;
        if (p + 1 < getTreeWidth(hashes.size(), h)) {
            right = calcHash(h, p + 1, hashes);
        } else {
            right = left;
        }
        return combineLeftRight(left.getBytes(), right.getBytes());
    }

    // helper function to efficiently calculate the number of nodes at given height in the merkle tree
    private static int getTreeWidth(int transactionCount, int height) {
        return (transactionCount + (1 << height) - 1) >> height;
    }
    
    private static class ValuesUsed {
        public int bitsUsed = 0, hashesUsed = 0;
    }
    
    // recursive function that traverses tree nodes, consuming the bits and hashes produced by TraverseAndBuild.
    // it returns the hash of the respective node.
    private Sha256Hash recursiveExtractHashes(int height, int pos, ValuesUsed used, List<Sha256Hash> matchedHashes) throws VerificationException {
        if (used.bitsUsed >= matchedChildBits.length*8) {
            // overflowed the bits array - failure
            throw new VerificationException("PartialMerkleTree overflowed its bits array");
        }
        boolean parentOfMatch = Utils.checkBitLE(matchedChildBits, used.bitsUsed++);
        if (height == 0 || !parentOfMatch) {
            // if at height 0, or nothing interesting below, use stored hash and do not descend
            if (used.hashesUsed >= hashes.size()) {
                // overflowed the hash array - failure
                throw new VerificationException("PartialMerkleTree overflowed its hash array");
            }
            Sha256Hash hash = hashes.get(used.hashesUsed++);
            if (height == 0 && parentOfMatch) // in case of height 0, we have a matched txid
                matchedHashes.add(hash);
            return hash;
        } else {
            // otherwise, descend into the subtrees to extract matched txids and hashes
            byte[] left = recursiveExtractHashes(height - 1, pos * 2, used, matchedHashes).getBytes(), right;
            if (pos * 2 + 1 < getTreeWidth(transactionCount, height-1)) {
                right = recursiveExtractHashes(height - 1, pos * 2 + 1, used, matchedHashes).getBytes();
                if (Arrays.equals(right, left))
                    throw new VerificationException("Invalid merkle tree with duplicated left/right branches");
            } else {
                right = left;
            }
            checkNotAValid64ByteTransaction(left, right);
            // and combine them before returning
            return combineLeftRight(left, right);
        }
    }

    private static Sha256Hash combineLeftRight(byte[] left, byte[] right) {
        return Sha256Hash.wrapReversed(Sha256Hash.hashTwice(
            Utils.reverseBytes(left), 0, 32,
            Utils.reverseBytes(right), 0, 32));
    }

    /**
     * Checks supplied bytes DO NOT represent a valid bitcoin transaction.
     * Fixes attack described on https://bitslog.wordpress.com/2018/06/09/leaf-node-weakness-in-bitcoin-merkle-tree-design/
     * @throws VerificationException if bytes DO represent a valid bitcoin transaction.
     */
    private void checkNotAValid64ByteTransaction(byte[] left, byte[] right) throws VerificationException {
        byte[] leftAndRight = new byte[left.length + right.length];
        System.arraycopy(left, 0, leftAndRight, 0, 32);
        System.arraycopy(right, 0, leftAndRight, 32, 32);

        int _offset = 0;

        // Skip version
        _offset += 4;

        // Check inputs count
        byte inputCount = leftAndRight[_offset];
        _offset += 1;
        if (inputCount != 1) {
            // Only 1 input fits in 64 bytes
            return;
        }

        // Skip input 0 outpoint hash
        _offset += 32;

        // Check output 0 index
        long output0Index = Utils.readUint32(leftAndRight, _offset);
        _offset += 4;
        if (output0Index > 1000000 && output0Index != COINBASE_CHECKED){
            // this value is capped by max btc tx size
            // and should check also is not a valid coinbase expressed in a byte array
            return;
        }

        // Check input 0 script length
        byte input0ScriptLength = leftAndRight[_offset];
        _offset += 1;
        if (input0ScriptLength < 0 || input0ScriptLength > 4) {
            // Script length should be 0 to 4 to create a valid 64 byte tx
            return;
        }

        // Skip input 0 script
        _offset += input0ScriptLength;

        // Skip input 0 sequence
        _offset += 4;

        // Check output count
        byte outputCount = leftAndRight[_offset];
        _offset += 1;
        if (outputCount != 1) {
            // Only 1 output fits in 64 bytes
            return;
        }

        // check output 0 value
        long output0Value = Utils.readInt64(leftAndRight, _offset);
        _offset += 8;
        long maxNumberOfSatoshis = 21000000l * 100000000l;
        if (output0Value < 1 || output0Value > maxNumberOfSatoshis) {
            //  0 < value < 21 millions (expressed in satoshis) validation failed
            return;
        }

        // check output 0 script length
        byte output0ScriptLength = leftAndRight[_offset];
        _offset += 1;
        if (output0ScriptLength < 0 || output0ScriptLength > 4) {
            // Script length should be 0 to 4 to create a valid 64 byte tx
            return;
        }

        // check input 0 script length + output 0 script length
        if ((input0ScriptLength + output0ScriptLength) != 4) {
            // Script length should be 0 to 4
            return;
        }

        // Skip output 0 script.
        // Commented out _offset increment because _offset is never read again
        //_offset += output0ScriptLength;

        // Skip lock time
        // Commented out _offset increment because _offset is never read again
        //_offset += 4;

        // If code reaches here, it means "left + right" represent a a valid btc transaction
        throw new VerificationException("Supplied nodes form a valid btc transaction");
    }



    /**
     * Extracts tx hashes that are in this merkle tree
     * and returns the merkle root of this tree.
     * 
     * The returned root should be checked against the
     * merkle root contained in the block header for security.
     * 
     * @param matchedHashesOut A list which will contain the matched txn (will be cleared).
     * @return the merkle root of this merkle tree
     * @throws ProtocolException if this partial merkle tree is invalid
     */
    public Sha256Hash getTxnHashAndMerkleRoot(List<Sha256Hash> matchedHashesOut) throws VerificationException {
        matchedHashesOut.clear();
        
        // An empty set will not work
        if (transactionCount == 0)
            throw new VerificationException("Got a CPartialMerkleTree with 0 transactions");
        // check for excessively high numbers of transactions
        if (transactionCount > BtcBlock.MAX_BLOCK_SIZE / 60) // 60 is the lower bound for the size of a serialized CTransaction
            throw new VerificationException("Got a CPartialMerkleTree with more transactions than is possible");
        // there can never be more hashes provided than one for every txid
        if (hashes.size() > transactionCount)
            throw new VerificationException("Got a CPartialMerkleTree with more hashes than transactions");
        // there must be at least one bit per node in the partial tree, and at least one node per hash
        if (matchedChildBits.length*8 < hashes.size())
            throw new VerificationException("Got a CPartialMerkleTree with fewer matched bits than hashes");
        // calculate height of tree
        int height = 0;
        while (getTreeWidth(transactionCount, height) > 1)
            height++;
        // traverse the partial tree
        ValuesUsed used = new ValuesUsed();
        Sha256Hash merkleRoot = recursiveExtractHashes(height, 0, used, matchedHashesOut);
        // verify that all bits were consumed (except for the padding caused by serializing it as a byte sequence)
        if ((used.bitsUsed+7)/8 != matchedChildBits.length ||
                // verify that all hashes were consumed
                used.hashesUsed != hashes.size())
            throw new VerificationException("Got a CPartialMerkleTree that didn't need all the data it provided");
        
        return merkleRoot;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialMerkleTree other = (PartialMerkleTree) o;
        return transactionCount == other.transactionCount && hashes.equals(other.hashes)
            && Arrays.equals(matchedChildBits, other.matchedChildBits);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(transactionCount, hashes, Arrays.hashCode(matchedChildBits));
    }

    @Override
    public String toString() {
        return "PartialMerkleTree{" +
                "transactionCount=" + transactionCount +
                ", matchedChildBits=" + Arrays.toString(matchedChildBits) +
                ", hashes=" + hashes +
                '}';
    }
}
