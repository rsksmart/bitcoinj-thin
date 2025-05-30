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
    public void isN_withSmallNumber_returnsTrue() {
        for (int i=0; i<16; i++) {
            ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_1 + i, null);
            assertTrue(chunk.isN());
        }
    }

    @Test
    public void isN_withPushData_returnsTrue() {
        // Need to review the range of values for OP_PUSHDATA1, OP_PUSHDATA2, and OP_PUSHDATA4,
        // above 127 they are serialized as negative numbers and isPushDataNumber() will return false.
        for (int i=1; i<100; i++) {
            ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_PUSHDATA1, new byte[]{(byte) i});
            assertTrue(chunk.isN());

            chunk = new ScriptChunk(ScriptOpCodes.OP_PUSHDATA2, new byte[]{(byte) i});
            assertTrue(chunk.isN());

            chunk = new ScriptChunk(ScriptOpCodes.OP_PUSHDATA4, new byte[]{(byte) i});
            assertTrue(chunk.isN());
        }
    }

    @Test
    public void isN_withNonNumber_returnsFalse() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null);
        assertFalse(chunk.isN());

        chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null);
        assertFalse(chunk.isN());

        chunk = new ScriptChunk(ScriptOpCodes.OP_RETURN, null);
        assertFalse(chunk.isN());

        chunk = new ScriptChunk(ScriptOpCodes.OP_0, null);
        assertFalse(chunk.isN());
    }

    @Test
    public void isN_withNonPushData_returnsFalse() {
        ScriptChunk chunk = new ScriptChunk(OP_PUSHDATA1, null);
        assertFalse(chunk.isN());

        chunk = new ScriptChunk(OP_PUSHDATA2, null);
        assertFalse(chunk.isN());

        chunk = new ScriptChunk(OP_PUSHDATA4, null);
        assertFalse(chunk.isN());
    }

    @Test
    public void decodeN_withSmallNumber_returnsValue() {
        for (int i=0; i<16; i++) {
            ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_1 + i, null);
            assertTrue(chunk.isN());
            assertEquals(i + 1, chunk.decodeN());
        }
    }

    @Test
    public void decodeN_withPushData_returnsValue() {
        // What happens if the value is larger than 1 byte?
        for (int i=1; i<100; i++) {
            ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_PUSHDATA1, new byte[]{(byte) i});
            assertTrue(chunk.isN());
            assertEquals(i, chunk.decodeN());

            chunk = new ScriptChunk(ScriptOpCodes.OP_PUSHDATA2, new byte[]{(byte) i});
            assertTrue(chunk.isN());
            assertEquals(i, chunk.decodeN());

            chunk = new ScriptChunk(ScriptOpCodes.OP_PUSHDATA4, new byte[]{(byte) i});
            assertTrue(chunk.isN());
            assertEquals(i, chunk.decodeN());
        }
    }

    @Test
    public void decodeN_withNonNumber_throwsException() {
        ScriptChunk chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);

        chunk = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);

        chunk = new ScriptChunk(ScriptOpCodes.OP_RETURN, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);

        chunk = new ScriptChunk(ScriptOpCodes.OP_0, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);
    }

    @Test
    public void decodeN_withNonPushData_throwsException() {
        ScriptChunk chunk = new ScriptChunk(OP_PUSHDATA1, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);

        chunk = new ScriptChunk(OP_PUSHDATA2, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);

        chunk = new ScriptChunk(OP_PUSHDATA4, null);
        assertFalse(chunk.isN());
        assertThrows(IllegalArgumentException.class, chunk::decodeN);
    }
}
