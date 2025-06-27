/*
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

package co.rsk.bitcoinj.script;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA1;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA2;
import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_PUSHDATA4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScriptChunkTest {

    @Test
    public void testShortestPossibleDataPush() {
        assertTrue("empty push", new ScriptBuilder().data(new byte[0]).build().getChunks().get(0)
                .isShortestPossiblePushData());

        for (byte i = -1; i < 127; i++)
            assertTrue("push of single byte " + i, new ScriptBuilder().data(new byte[] { i }).build().getChunks()
                    .get(0).isShortestPossiblePushData());

        for (int len = 2; len < Script.MAX_SCRIPT_ELEMENT_SIZE; len++)
            assertTrue("push of " + len + " bytes", new ScriptBuilder().data(new byte[len]).build().getChunks().get(0)
                    .isShortestPossiblePushData());

        // non-standard chunks
        for (byte i = 1; i <= 16; i++)
            assertFalse("push of smallnum " + i, new ScriptChunk(1, new byte[] { i }).isShortestPossiblePushData());
        assertFalse("push of 75 bytes", new ScriptChunk(OP_PUSHDATA1, new byte[75]).isShortestPossiblePushData());
        assertFalse("push of 255 bytes", new ScriptChunk(OP_PUSHDATA2, new byte[255]).isShortestPossiblePushData());
        assertFalse("push of 65535 bytes", new ScriptChunk(OP_PUSHDATA4, new byte[65535]).isShortestPossiblePushData());
    }

    @Test
    public void isOpCheckMultiSig_withNullData_returnsTrue() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null);
        assertTrue(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIGVERIFY, null);
        assertTrue(chunk.isOpCheckMultiSig());
    }

    @Test
    public void isOpCheckMultiSig_withEmptyData_returnsTrue() {
        byte[] emptyData = new byte[]{};

        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, emptyData);
        assertTrue(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIGVERIFY, emptyData);
        assertTrue(chunk.isOpCheckMultiSig());
    }

    @Test
    public void isOpCheckMultiSig_withData_returnsTrue() {
        byte[] randomData = new byte[]{1, 2, 3};

        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, randomData);
        assertTrue(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIGVERIFY, randomData);
        assertTrue(chunk.isOpCheckMultiSig());
    }

    @Test
    public void isOpCheckMultiSig_withNonOpCheckMultiSig_nullData_returnsFalse() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_RETURN, null);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_0, null);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_1, null);
        assertFalse(chunk.isOpCheckMultiSig());
    }

    @Test
    public void isOpCheckMultiSig_withNonOpCheckMultiSig_emptyData_returnsFalse() {
        byte[] emptyData = new byte[]{};

        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, emptyData);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_RETURN, emptyData);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_0, emptyData);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_1, emptyData);
        assertFalse(chunk.isOpCheckMultiSig());
    }

    @Test
    public void isOpCheckMultiSig_withNonOpCheckMultiSig_withData_returnsFalse() {
        byte[] randomData = new byte[]{1, 2, 3};

        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, randomData);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_RETURN, randomData);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_0, randomData);
        assertFalse(chunk.isOpCheckMultiSig());

        chunk = new ScriptChunk(ScriptOpCodes.OP_1, randomData);
        assertFalse(chunk.isOpCheckMultiSig());
    }

    @Test
    public void isOpCheckMultiSig_withNonOpCode_returnsFalse() {
        ScriptChunk chunk = new ScriptChunk(OP_PUSHDATA1, null);
        assertFalse(chunk.isOpCode());
        assertFalse(chunk.isOpCheckMultiSig());
    }

    @Test
    public void decodePositiveN_withPositiveSmallNumber_returnsExpectedNumber() {
        for (int i=1; i<16; i++) {
            ScriptBuilder builder = new ScriptBuilder();
            builder.number(i);
            Script script = builder.build();

            ScriptChunk chunk = script.chunks.get(0);
            assertEquals(i, chunk.decodePositiveN());
        }
    }

    @Test
    public void decodePositiveN_withPositiveNumber_returnsExpectedNumber() {
        for (int n=0; n<20; n++) { // technically we could go up to Integer.MAX_VALUE, but it's not worth it to go that far
            int i = 1 << n; // i = 2^n
            ScriptBuilder builder = new ScriptBuilder();
            builder.number(i);
            Script script = builder.build();

            ScriptChunk chunk = script.chunks.get(0);
            assertEquals(i, chunk.decodePositiveN());
        }
    }

    @Test
    public void decodePositiveN_forZero_throwsNPE() {
        int zero = 0;
        ScriptBuilder builder = new ScriptBuilder();
        builder.number(zero);
        Script script = builder.build();

        ScriptChunk chunk = script.chunks.get(0);
        assertThrows(NullPointerException.class, chunk::decodePositiveN);
    }

    @Test
    public void decodePositiveN_withNegativeNumber_throwsIAE() {
        for (int n=0; n<20; n++) { // technically we could go down to Integer.MIN_VALUE, but it's not worth it to go that deep
            int i = -(1 << n); // i = -(2^n)
            ScriptBuilder builder = new ScriptBuilder();
            builder.number(i);
            Script script = builder.build();

            ScriptChunk chunk = script.chunks.get(0);
            assertThrows(IllegalArgumentException.class, chunk::decodePositiveN);
        }
    }

    @Test
    public void decodePositiveN_withNullPushData_throwsNPE() {
        ScriptChunk chunk = new ScriptChunk(OP_PUSHDATA1, null);
        assertThrows(NullPointerException.class, chunk::decodePositiveN);

        chunk = new ScriptChunk(OP_PUSHDATA2, null);
        assertThrows(NullPointerException.class, chunk::decodePositiveN);

        chunk = new ScriptChunk(OP_PUSHDATA4, null);
        assertThrows(NullPointerException.class, chunk::decodePositiveN);
    }

    @Test
    public void decodePositiveN_withWrongOpcodes_throwsIAE() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null);
        assertThrows(IllegalArgumentException.class, chunk::decodePositiveN);

        chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null);
        assertThrows(IllegalArgumentException.class, chunk::decodePositiveN);

        chunk = new ScriptChunk(ScriptOpCodes.OP_RETURN, null);
        assertThrows(IllegalArgumentException.class, chunk::decodePositiveN);

        chunk = new ScriptChunk(ScriptOpCodes.OP_DROP, null);
        assertThrows(IllegalArgumentException.class, chunk::decodePositiveN);
    }
}
