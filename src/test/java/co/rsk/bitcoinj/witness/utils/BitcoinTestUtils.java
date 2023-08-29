package co.rsk.bitcoinj.witness.utils;

import co.rsk.bitcoinj.core.BtcECKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import co.rsk.bitcoinj.core.Sha256Hash;

public class BitcoinTestUtils {

    public static List<BtcECKey> getBtcEcKeysFromSeeds(String[] seeds, boolean sorted) {
        List<BtcECKey> keys = Arrays
            .stream(seeds)
            .map(seed -> BtcECKey.fromPrivate(Sha256Hash.hash(seed.getBytes(StandardCharsets.UTF_8))))
            .collect(Collectors.toList());

        if (sorted) {
            keys.sort(BtcECKey.PUBKEY_COMPARATOR);
        }

        return keys;
    }
}
