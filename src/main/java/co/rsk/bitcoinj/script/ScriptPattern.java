/*
 * Copyright 2017 John L. Jegutanis
 * Copyright 2018 Andreas Schildbach
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

package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.Utils;
import java.util.Arrays;
import java.util.List;


/**
 * This is a Script pattern matcher with some typical script patterns
 */
public class ScriptPattern {
    private static final byte[] SEGWIT_COMMITMENT_HEADER = Utils.HEX.decode("aa21a9ed");

    /**
     * Returns whether this script matches the pattern for a segwit commitment (in an output of the coinbase
     * transaction).
     */
    public static boolean isWitnessCommitment(Script script) {
        List<ScriptChunk> chunks = script.chunks;
        if (chunks.size() < 2)
            return false;
        if (!chunks.get(0).equalsOpCode(ScriptOpCodes.OP_RETURN))
            return false;
        byte[] chunkData = chunks.get(1).data;
        if (chunkData == null || chunkData.length != 36)
            return false;
        if (!Arrays.equals(Arrays.copyOfRange(chunkData, 0, 4), SEGWIT_COMMITMENT_HEADER))
            return false;
        return true;
    }

    /**
     * Retrieves the hash from a segwit commitment (in an output of the coinbase transaction).
     */
    public static Sha256Hash extractWitnessCommitmentHash(Script script) {
        return Sha256Hash.wrap(Arrays.copyOfRange(script.chunks.get(1).data, 4, 36));
    }
}
