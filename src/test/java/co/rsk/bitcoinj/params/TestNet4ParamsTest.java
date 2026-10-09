/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package co.rsk.bitcoinj.params;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import co.rsk.bitcoinj.core.BtcBlock;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.StoredBlock;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.store.BlockStoreException;
import co.rsk.bitcoinj.store.BtcBlockStore;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link TestNet4Params}, the BIP-94 Bitcoin testnet4 network parameters.
 *
 * The difficulty tests replay a full, real difficulty period downloaded from the live testnet4
 * chain (mempool.space), heights 141120..143136. That period ends in a minimum-difficulty block,
 * which is exactly the case where BIP-94 diverges from the old testnet3 rules.
 */
public class TestNet4ParamsTest {

    private static final String FIXTURE = "/co/rsk/bitcoinj/params/testnet4-headers-141120-143136.tsv"; // '|'-separated despite the .tsv extension
    private static final int BOUNDARY_HEIGHT = 143136; // multiple of 2016 -> a difficulty transition point

    private static final String EXPECTED_GENESIS_HASH =
        "00000000da84f2bafbbc53dee25a72ae507ff4914b867c565be350b0da8bf043"; // https://mempool.space/testnet4/block/00000000da84f2bafbbc53dee25a72ae507ff4914b867c565be350b0da8bf043
    private static final String EXPECTED_GENESIS_MERKLE =
        "7aa0a7ae1e223414cb807e40cd57e667b718e42aaf9306db9102fe28912b7b4e"; // https://mempool.space/testnet4/block/00000000da84f2bafbbc53dee25a72ae507ff4914b867c565be350b0da8bf043?showDetails=true&view=actual#details

    private static TestNet4Params params;

    @BeforeClass
    public static void setUp() {
        params = TestNet4Params.get();
    }

    // ---- Identity / genesis ----

    @Test
    public void networkIdentity() {
        assertEquals(NetworkParameters.ID_TESTNET4, params.getId());
        assertEquals(48333, params.getPort());
        assertEquals(0x1c163f28L, params.getPacketMagic());
    }

    @Test
    public void genesisBlockMatchesTestnet4() {
        assertEquals(EXPECTED_GENESIS_HASH, params.getGenesisBlock().getHashAsString());
        assertEquals(EXPECTED_GENESIS_MERKLE, params.getGenesisBlock().getMerkleRoot().toString());
    }

    @Test
    public void fromIdResolvesTestNet4() {
        NetworkParameters resolved = NetworkParameters.fromID(NetworkParameters.ID_TESTNET4);
        assertTrue(resolved instanceof TestNet4Params);
    }

    // ---- Difficulty: replay of a real testnet4 period ----

    @Test
    public void acceptsEveryRealBlockAcrossARealRetargetPeriod() throws Exception {
        TreeMap<Integer, BtcBlock> byHeight = new TreeMap<>();
        MapStore store = loadFixture(byHeight);

        int firstHeight = byHeight.firstKey();
        int lastHeight = byHeight.lastKey();
        boolean sawBoundary = false;
        for (int height = firstHeight + 1; height <= lastHeight; height++) {
            BtcBlock nextBlock = byHeight.get(height);
            StoredBlock previousBlock = store.get(byHeight.get(height - 1).getHash());
            if (height % params.getInterval() == 0) {
                sawBoundary = true;
            }
            try {
                params.checkDifficultyTransitions(previousBlock, nextBlock, store);
            } catch (VerificationException rejection) {
                fail("Real testnet4 block " + height + " was rejected: " + rejection.getMessage());
            }
        }
        assertTrue("fixture must contain at least one retarget boundary", sawBoundary);
    }

    @Test
    public void reconstructedHeadersMatchTheRealChainHashes() throws Exception {
        // Building the header from its fields must reproduce the exact block id from the chain,
        // otherwise the fixture / block construction would be wrong.
        TreeMap<Integer, BtcBlock> byHeight = new TreeMap<>();
        loadFixture(byHeight);
        assertTrue(byHeight.size() > 2000);
    }

    @Test
    public void rejectsTamperedDifficultyAtBoundary() throws Exception {
        TreeMap<Integer, BtcBlock> byHeight = new TreeMap<>();
        MapStore store = loadFixture(byHeight);

        BtcBlock realBlock = byHeight.get(BOUNDARY_HEIGHT);
        BtcBlock tamperedBlock = new BtcBlock(params, realBlock.getVersion(), realBlock.getPrevBlockHash(),
            realBlock.getMerkleRoot(), realBlock.getTimeSeconds(), 0x1d00ffffL /* wrong: minimum difficulty */,
            realBlock.getNonce(), new ArrayList<BtcTransaction>());
        StoredBlock previousBlock = store.get(byHeight.get(BOUNDARY_HEIGHT - 1).getHash());

        try {
            params.checkDifficultyTransitions(previousBlock, tamperedBlock, store);
            fail("A block with tampered difficulty bits should be rejected at the retarget boundary");
        } catch (VerificationException expected) {
            // expected
        }
    }

    /**
     * The heart of BIP-94: at height 143136 the previous block is a minimum-difficulty block.
     * The old testnet3 rules anchor the retarget to that (minimum) last block and therefore
     * reject the real next block; the testnet4 rules anchor to the first block of the period
     * and accept it.
     */
    @Test
    public void bip94DivergesFromTestnet3AtMinDifficultyBoundary() throws Exception {
        TreeMap<Integer, BtcBlock> byHeight = new TreeMap<>();
        MapStore store = loadFixture(byHeight);

        BtcBlock boundaryBlock = byHeight.get(BOUNDARY_HEIGHT);
        StoredBlock previousBlock = store.get(byHeight.get(BOUNDARY_HEIGHT - 1).getHash());

        // Precondition: the last block of the closing period is indeed a minimum-difficulty block.
        assertEquals(0x1d00ffffL, previousBlock.getHeader().getDifficultyTarget());

        // testnet3 rules reject this real testnet4 block...
        boolean testnet3Rejected = false;
        try {
            TestNet3Params.get().checkDifficultyTransitions(previousBlock, boundaryBlock, store);
        } catch (VerificationException rejection) {
            testnet3Rejected = true;
        }
        assertTrue("testnet3 rules should reject the real testnet4 boundary block", testnet3Rejected);

        // ...but testnet4 (BIP-94) rules accept it.
        try {
            params.checkDifficultyTransitions(previousBlock, boundaryBlock, store);
        } catch (VerificationException rejection) {
            fail("testnet4 rules should accept the real boundary block, but rejected: " + rejection.getMessage());
        }
    }

    /**
     * BIP-94 time-warp mitigation: the first block of a period must not be timestamped more than
     * 600 seconds before the previous block. A back-dated boundary block must be rejected.
     */
    @Test
    public void rejectsBackdatedBoundaryBlockViolatingTimewarpFloor() throws Exception {
        TreeMap<Integer, BtcBlock> byHeight = new TreeMap<>();
        MapStore store = loadFixture(byHeight);

        BtcBlock realBoundary = byHeight.get(BOUNDARY_HEIGHT);
        long previousBlockTime = byHeight.get(BOUNDARY_HEIGHT - 1).getTimeSeconds();
        BtcBlock backdatedBoundary = new BtcBlock(params, realBoundary.getVersion(),
            realBoundary.getPrevBlockHash(), realBoundary.getMerkleRoot(),
            previousBlockTime - 601 /* more than 600s before the previous block */,
            realBoundary.getDifficultyTarget(), realBoundary.getNonce(), new ArrayList<BtcTransaction>());
        StoredBlock previousBlock = store.get(byHeight.get(BOUNDARY_HEIGHT - 1).getHash());

        try {
            params.checkDifficultyTransitions(previousBlock, backdatedBoundary, store);
            fail("A boundary block back-dated more than 600s before the previous block must be rejected");
        } catch (VerificationException expected) {
            // expected: time-warp violation
        }
    }

    // ---- helpers ----

    /** Loads the fixture, verifies each reconstructed header hash equals the chain id, returns the store. */
    private MapStore loadFixture(TreeMap<Integer, BtcBlock> byHeight) throws Exception {
        MapStore store = new MapStore();
        InputStream inputStream = getClass().getResourceAsStream(FIXTURE);
        if (inputStream == null) {
            throw new IllegalStateException("fixture not found on classpath: " + FIXTURE);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\|");
                int height = Integer.parseInt(fields[0]);
                String blockId = fields[1];
                BtcBlock block = new BtcBlock(params, Long.parseLong(fields[2]), Sha256Hash.wrap(fields[3]),
                    Sha256Hash.wrap(fields[4]), Long.parseLong(fields[5]), Long.parseLong(fields[6]),
                    Long.parseLong(fields[7]), new ArrayList<BtcTransaction>());
                assertEquals("reconstructed header hash must match the real chain id at height " + height,
                    blockId, block.getHashAsString());
                byHeight.put(height, block);
                store.put(new StoredBlock(block, BigInteger.valueOf(height + 1L), height));
            }
        }
        return store;
    }

    /** Minimal in-memory block store (hash -> StoredBlock) sufficient for difficulty checks. */
    private static class MapStore implements BtcBlockStore {
        private final Map<Sha256Hash, StoredBlock> blocks = new HashMap<>();

        @Override
        public void put(StoredBlock block) {
            blocks.put(block.getHeader().getHash(), block);
        }

        @Override
        public StoredBlock get(Sha256Hash hash) {
            return blocks.get(hash);
        }

        @Override
        public StoredBlock getChainHead() {
            return null;
        }

        @Override
        public void setChainHead(StoredBlock chainHead) {
        }

        @Override
        public Optional<StoredBlock> getInMainchain(int height) {
            return Optional.empty();
        }

        @Override
        public void setMainChainBlock(int height, Sha256Hash blockHash) {
        }

        @Override
        public void close() throws BlockStoreException {
        }

        @Override
        public NetworkParameters getParams() {
            return params;
        }
    }
}
