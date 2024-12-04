package co.rsk.bitcoinj.core;

import static co.rsk.bitcoinj.core.CheckpointManager.BASE64;
import static co.rsk.bitcoinj.core.StoredBlock.COMPACT_SERIALIZED_SIZE_LEGACY;
import static co.rsk.bitcoinj.core.StoredBlock.COMPACT_SERIALIZED_SIZE_V2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class CheckpointManagerTest {

    private static final NetworkParameters MAINNET = NetworkParameters.fromID(
        NetworkParameters.ID_MAINNET);
    private static final NetworkParameters TESTNET = NetworkParameters.fromID(
        NetworkParameters.ID_TESTNET);
    private static final BigInteger MAX_WORK_V1 = new BigInteger("ffffffffffffffffffffffff", 16);

    private static final String BINARY_FORMAT_PREFIX = "CHECKPOINTS 1";

    private static final List<String> CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED = Arrays.asList(
        "AAAAAAAAB+EH4QfhAAAH4AEAAAApmwX6UCEnJcYIKTa7HO3pFkqqNhAzJVBMdEuGAAAAAPSAvVCBUypCbBW/OqU0oIF7ISF84h2spOqHrFCWN9Zw6r6/T///AB0E5oOO",
        "AAAAAAAAD8QPxA/EAAAPwAEAAADHtJ8Nq3z30grJ9lTH6bLhKSHX+MxmkZn8z5wuAAAAAK0gXcQFtYSj/IB2KZ38+itS1Da0Dn/3XosOFJntz7A8OsC/T8D/Pxwf0no+",
        "AAAAAAAALUAtQC1AAAAXoAEAAABwvpBfmfp76xvcOzhdR+OPnJ2aLD5znGpD8LkJAAAAALkv0fxOJYZ1dMLCyDV+3AB0y+BW8lP5/8xBMMqLbX7u+gPDT/D/DxwDvhrh"
    );

    private static final List<String> CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED = Arrays.asList(
        // 13 bytes TOO_LARGE_WORK_V1 = ffffffffffffffffffffffffff
        "AAAAAAAAAAAAAAAAAAAAAAAAAP////////////////8AAB+AAQAAANW+iGqrr/fsekjWfL7yhyKCGSieKwRG8nmcnAoAAAAAFEW4aog6zdt5sMVmp3UMo/H/JkXiG/u3vmsfyYvo5ThKBcNPwP8/HBEzbVs",
        "AAAAAAAAAAAAAAAAAAAAAAAAAP////////////////8AACdgAQAAAAnfZFAmRFbc2clq5XzNV2/UbKPLCAB7JOECcDoAAAAAeCpL87HF9/JFao8VX1rqRU/pMsv8F08X8ieq464NqECaBsNP//8AHRvpMAo",
        "AAAAAAAAAAAAAAAAAAAAAAAAAP////////////////8AAC9AAQAAAMipH0cUa3D2Ea/T7sCMt0G4Tuqq5/b/KugBHgYAAAAAIROhXYS8rkGyrLjTJvp2iWRfTDOcu/Rkkf9Az5xpTLjrB8NPwP8/HGbjgbo",
        // 32 bytes MAX_WORK_V2 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        "//////////////////////////////////////////8AAB+AAQAAANW+iGqrr/fsekjWfL7yhyKCGSieKwRG8nmcnAoAAAAAFEW4aog6zdt5sMVmp3UMo/H/JkXiG/u3vmsfyYvo5ThKBcNPwP8/HBEzbVs",
        "//////////////////////////////////////////8AACdgAQAAAAnfZFAmRFbc2clq5XzNV2/UbKPLCAB7JOECcDoAAAAAeCpL87HF9/JFao8VX1rqRU/pMsv8F08X8ieq464NqECaBsNP//8AHRvpMAo",
        "//////////////////////////////////////////8AAC9AAQAAAMipH0cUa3D2Ea/T7sCMt0G4Tuqq5/b/KugBHgYAAAAAIROhXYS8rkGyrLjTJvp2iWRfTDOcu/Rkkf9Az5xpTLjrB8NPwP8/HGbjgbo"
    );

    @Test
    public void readBinaryCheckpoint_whenTestnet_ok() throws IOException {
        readBinaryCheckpoint(TESTNET,
            "/co/rsk/bitcoinj/core/checkpointmanagertest/org.bitcoin.test.checkpoints");
    }

    private void readBinaryCheckpoint(NetworkParameters networkParameters,
        String checkpointPath) throws IOException {
        InputStream checkpointStream = getClass().getResourceAsStream(checkpointPath);
        new CheckpointManager(networkParameters, checkpointStream);
    }

    @Test
    public void readBinaryCheckpoint_whenMainnet_ok() throws IOException {
        readBinaryCheckpoint(MAINNET,
            "/co/rsk/bitcoinj/core/checkpointmanagertest/org.bitcoin.production.checkpoints");
    }

    @Test
    public void readBinaryCheckpoints_whenCheckpointChainWorkIs12Bytes() throws IOException {
        List<StoredBlock> checkpoints = getCheckpoints(CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED,
            COMPACT_SERIALIZED_SIZE_LEGACY);
        try (InputStream binaryCheckpoint = generateBinaryCheckpoints(checkpoints)) {
            CheckpointManager checkpointManager = new CheckpointManager(MAINNET, binaryCheckpoint);

            List<StoredBlock> actualCheckpoints = new ArrayList<>(checkpointManager.checkpoints.values());
            Assert.assertEquals(checkpoints, actualCheckpoints);
        }
    }

    private List<StoredBlock> getCheckpoints(List<String> checkpoints, int blockFormatSize) {
        ByteBuffer buffer = ByteBuffer.allocate(blockFormatSize);
        return checkpoints.stream()
            .map(BASE64::decode)
            .map(bytes -> {
                buffer.clear();
                buffer.put(bytes);
                buffer.flip();

                return blockFormatSize == COMPACT_SERIALIZED_SIZE_LEGACY
                    ? StoredBlock.deserializeCompactLegacy(MAINNET, buffer)
                    : StoredBlock.deserializeCompactV2(MAINNET, buffer);
            }).collect(Collectors.toList());
    }

    private void serializeBlock(ByteBuffer buffer, StoredBlock block, boolean isV1)
        throws IOException {
        buffer.rewind();
        if (isV1) {
            block.serializeCompactLegacy(buffer);
        } else {
            block.serializeCompactV2(buffer);
        }
    }

    private InputStream generateBinaryCheckpoints(List<StoredBlock> checkpoints) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DigestOutputStream digestStream = new DigestOutputStream(outputStream,
                Sha256Hash.newDigest());
            DataOutputStream dataStream = new DataOutputStream(digestStream)) {

            digestStream.on(false);
            dataStream.writeBytes(BINARY_FORMAT_PREFIX);
            dataStream.writeInt(0);
            digestStream.on(true);
            dataStream.writeInt(checkpoints.size());

            ByteBuffer bufferV1 = ByteBuffer.allocate(COMPACT_SERIALIZED_SIZE_LEGACY);
            ByteBuffer bufferV2 = ByteBuffer.allocate(COMPACT_SERIALIZED_SIZE_V2);

            for (StoredBlock block : checkpoints) {
                boolean isV1 = block.getChainWork().compareTo(MAX_WORK_V1) <= 0;
                ByteBuffer buffer = isV1 ? bufferV1 : bufferV2;
                serializeBlock(buffer, block, isV1);
                int limit = isV1 ? COMPACT_SERIALIZED_SIZE_LEGACY
                    : COMPACT_SERIALIZED_SIZE_V2;
                dataStream.write(buffer.array(), 0, limit);
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = IOException.class)
    public void readBinaryCheckpoints_whenV2Format_shouldFail() throws IOException {
        List<StoredBlock> checkpointsV2Format = getCheckpoints(CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED,
            COMPACT_SERIALIZED_SIZE_V2);
        try (InputStream binaryCheckpoint = generateBinaryCheckpoints(checkpointsV2Format)) {
            CheckpointManager checkpointManager = new CheckpointManager(MAINNET, binaryCheckpoint);

            List<StoredBlock> actualCheckpoints = new ArrayList<>(checkpointManager.checkpoints.values());

            Assert.assertNotEquals(checkpointsV2Format, actualCheckpoints);
        }
    }

    @Test(expected = IOException.class)
    public void readBinaryCheckpoints_whenMixFormats_shouldFail()
        throws IOException {
        List<StoredBlock> checkpointsV1Format = getCheckpoints(CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED,
            StoredBlock.COMPACT_SERIALIZED_SIZE_LEGACY);
        List<StoredBlock> checkpointsV2Format = getCheckpoints(CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED,
            COMPACT_SERIALIZED_SIZE_V2);

        List<StoredBlock> checkpoints = new ArrayList<>();
        checkpoints.addAll(checkpointsV1Format);
        checkpoints.addAll(checkpointsV2Format);
        try (InputStream binaryCheckpoint = generateBinaryCheckpoints(checkpoints)) {
            CheckpointManager checkpointManager = new CheckpointManager(MAINNET, binaryCheckpoint);

            List<StoredBlock> actualCheckpoints = new ArrayList<>(checkpointManager.checkpoints.values());

            Assert.assertNotEquals(checkpointsV2Format, actualCheckpoints);
        }
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenCheckpointsNotFound() throws IOException {
        InputStream checkpointStream = getClass().getResourceAsStream("/co/rsk/bitcoinj/core/checkpointmanagertest/notFound.checkpoints.txt");
        new CheckpointManager(MAINNET, checkpointStream);
    }

    @Test(expected = IOException.class)
    public void shouldThrowNullPointerExceptionWhenCheckpointsInUnknownFormat() throws IOException {
        InputStream checkpointStream = getClass().getResourceAsStream("/co/rsk/bitcoinj/core/checkpointmanagertest/unsupportedFormat.checkpoints.txt");
        new CheckpointManager(MAINNET, checkpointStream);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWithNoCheckpoints() throws IOException {
        InputStream checkpointStream = getClass().getResourceAsStream("/co/rsk/bitcoinj/core/checkpointmanagertest/noCheckpoints.checkpoints.txt");
        new CheckpointManager(MAINNET, checkpointStream);
    }

    @Test
    public void canReadTextualFormat() throws IOException {
        InputStream checkpointStream = getClass().getResourceAsStream("/co/rsk/bitcoinj/core/checkpointmanagertest/validTextualFormat.checkpoints.txt");
        CheckpointManager checkpointManager = new CheckpointManager(MAINNET, checkpointStream);

        List<StoredBlock> actualCheckpoints = new ArrayList<>(checkpointManager.checkpoints.values());
        Assert.assertEquals(6, actualCheckpoints.size());
    }

    @Test
    public void canReadTextualMixFormats() throws IOException {
        InputStream checkpointStream = getClass().getResourceAsStream("/co/rsk/bitcoinj/core/checkpointmanagertest/mixFormats.checkpoints.txt");
        CheckpointManager checkpointManager = new CheckpointManager(MAINNET, checkpointStream);

        List<StoredBlock> actualCheckpoints = new ArrayList<>(checkpointManager.checkpoints.values());
        Assert.assertEquals(6, actualCheckpoints.size());
    }
}