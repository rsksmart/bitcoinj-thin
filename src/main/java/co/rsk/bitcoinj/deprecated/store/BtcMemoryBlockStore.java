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

package co.rsk.bitcoinj.deprecated.store;

import co.rsk.bitcoinj.deprecated.core.BtcBlock;
import co.rsk.bitcoinj.deprecated.core.NetworkParameters;
import co.rsk.bitcoinj.deprecated.core.Sha256Hash;
import co.rsk.bitcoinj.deprecated.core.StoredBlock;
import co.rsk.bitcoinj.deprecated.core.VerificationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps {@link StoredBlock}s in memory. Used primarily for unit testing.
 */
public class BtcMemoryBlockStore implements BtcBlockStore {
    private LinkedHashMap<Sha256Hash, StoredBlock> blockMap = new LinkedHashMap<Sha256Hash, StoredBlock>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Sha256Hash, StoredBlock> eldest) {
            return blockMap.size() > 5000;
        }
    };
    private StoredBlock chainHead;
    private NetworkParameters params;
    private LinkedHashMap<Integer, Sha256Hash> blockHashMap = new LinkedHashMap<Integer, Sha256Hash>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Sha256Hash> eldest) {
            return blockHashMap.size() > 5000;
        }
    };

    public BtcMemoryBlockStore(NetworkParameters params) {
        // Insert the genesis block.
        try {
            BtcBlock genesisHeader = params.getGenesisBlock().cloneAsHeader();
            StoredBlock storedGenesis = new StoredBlock(genesisHeader, genesisHeader.getWork(), 0);
            put(storedGenesis);
            setChainHead(storedGenesis);
            this.params = params;
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);  // Cannot happen.
        } catch (VerificationException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    @Override
    public synchronized final void put(StoredBlock block) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        Sha256Hash hash = block.getHeader().getHash();
        blockMap.put(hash, block);
    }

    @Override
    public synchronized StoredBlock get(Sha256Hash hash) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        return blockMap.get(hash);
    }

    @Override
    public StoredBlock getChainHead() throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        return chainHead;
    }

    @Override
    public final void setChainHead(StoredBlock chainHead) throws BlockStoreException {
        if (blockMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        this.chainHead = chainHead;
        this.setMainChainBlock(chainHead.getHeight(), chainHead.getHeader().getHash());
    }

    @Override
    public Optional<StoredBlock> getInMainchain(int height) {
        if (blockHashMap == null) {
            return Optional.empty();
        }

        Sha256Hash blockHash = blockHashMap.get(height);
        try {
            StoredBlock block = get(blockHash);
            return Optional.of(block);
        } catch (BlockStoreException e) {
            return Optional.empty();
        }
    }

    @Override
    public void setMainChainBlock(int height, Sha256Hash blockHash) throws BlockStoreException {
        if (blockHashMap == null) throw new BlockStoreException("MemoryBlockStore is closed");
        blockHashMap.put(height, blockHash);
    }

    @Override
    public void close() {
        blockMap = null;
        blockHashMap = null;
    }

    @Override
    public NetworkParameters getParams() {
        return params;
    }
}
