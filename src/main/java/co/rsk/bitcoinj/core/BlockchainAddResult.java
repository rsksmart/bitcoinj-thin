/**
 * 
 */
package co.rsk.bitcoinj.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mario
 *
 */
public class BlockchainAddResult {

	private Boolean success = Boolean.FALSE;

	private List<Block> orphansBlocksConnected = new ArrayList<Block>();
	private List<FilteredBlock> orphansFilteredBlocksConnected = new ArrayList<FilteredBlock>();



	public void addConnectedOrphan(Block block) {
		orphansBlocksConnected.add(block);
	}

	public void addConnectedOrphans(List<Block> blocks) {
		orphansBlocksConnected.addAll(blocks);
	}

	public void addConnectedFilteredOrphan(FilteredBlock block) {
		orphansFilteredBlocksConnected.add(block);
	}

	public void addFilteredOrphans(List<FilteredBlock> blocks) {
		orphansFilteredBlocksConnected.addAll(blocks);
	}

	public List<Block> getOrphansBlockConnected() {
		return orphansBlocksConnected;
	}
	
	public List<FilteredBlock> getFilteredOrphansConnected() {
		return orphansFilteredBlocksConnected;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public Boolean success() {
		return success;
	}

}
