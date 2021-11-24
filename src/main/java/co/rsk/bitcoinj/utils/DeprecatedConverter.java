package co.rsk.bitcoinj.utils;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.Context;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.UTXO;
import co.rsk.bitcoinj.script.Script;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeprecatedConverter {

    private DeprecatedConverter() {
        throw new IllegalAccessError("Utility class, do not instantiate it");
    }

    public static co.rsk.bitcoinj.deprecated.core.NetworkParameters toDeprecated(NetworkParameters params) {
        return co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(params.getId());
    }

    public static co.rsk.bitcoinj.deprecated.core.Address toDeprecated(NetworkParameters params, Address address) {
        return co.rsk.bitcoinj.deprecated.core.Address.fromBase58(toDeprecated(params), address.toBase58());
    }

    public static co.rsk.bitcoinj.deprecated.core.Coin toDeprecated(Coin coin) {
        return co.rsk.bitcoinj.deprecated.core.Coin.valueOf(coin.getValue());
    }

    public static co.rsk.bitcoinj.deprecated.core.Context toDeprecated(Context context) {
        return new co.rsk.bitcoinj.deprecated.core.Context(
            co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(context.getParams().getId())
        );
    }

    public static co.rsk.bitcoinj.deprecated.core.UTXO toDeprecated(UTXO utxo) throws IOException {
        try (ByteArrayOutputStream ostream = new ByteArrayOutputStream()) {
            utxo.serializeToStream(ostream);
            return new co.rsk.bitcoinj.deprecated.core.UTXO(
                new ByteArrayInputStream(ostream.toByteArray())
            );
        }
    }

    public static List<co.rsk.bitcoinj.deprecated.core.UTXO> toDeprecatedUtxos(List<UTXO> utxos) throws IOException {
        List<co.rsk.bitcoinj.deprecated.core.UTXO> converted = new ArrayList<>();
        for (UTXO utxo : utxos) {
            converted.add(toDeprecated(utxo));
        }

        return converted;
    }

    public static co.rsk.bitcoinj.deprecated.core.BtcTransaction toDeprecated(NetworkParameters params, BtcTransaction btcTransaction) {
        return new co.rsk.bitcoinj.deprecated.core.BtcTransaction(toDeprecated(params), btcTransaction.bitcoinSerialize());
    }

    public static co.rsk.bitcoinj.deprecated.core.BtcECKey toDeprecated(BtcECKey key) {
        return key.isPubKeyOnly() ?
            co.rsk.bitcoinj.deprecated.core.BtcECKey.fromPublicOnly(key.getPubKey()) :
            co.rsk.bitcoinj.deprecated.core.BtcECKey.fromPrivate(key.getPrivKey());
    }

    public static List<co.rsk.bitcoinj.deprecated.core.BtcECKey> toDeprecatedKeys(List<BtcECKey> keys) {
        List<co.rsk.bitcoinj.deprecated.core.BtcECKey> converted = new ArrayList<>();
        for (BtcECKey key : keys) {
            converted.add(toDeprecated(key));
        }

        return converted;
    }

    public static co.rsk.bitcoinj.deprecated.script.Script toDeprecated(Script script) {
        return new co.rsk.bitcoinj.deprecated.script.Script(script.getProgram());
    }

    public static UTXO toStandard(co.rsk.bitcoinj.deprecated.core.UTXO utxo) throws IOException {
        try (ByteArrayOutputStream ostream = new ByteArrayOutputStream()) {
            utxo.serializeToStream(ostream);
            return new UTXO(
                new ByteArrayInputStream(ostream.toByteArray())
            );
        }
    }

    public static List<UTXO> toStandardUtxos(List<co.rsk.bitcoinj.deprecated.core.UTXO> utxos) throws IOException {
        List<UTXO> converted = new ArrayList<>();
        for (co.rsk.bitcoinj.deprecated.core.UTXO utxo : utxos) {
            converted.add(toStandard(utxo));
        }

        return converted;
    }

    public static BtcTransaction toStandard(NetworkParameters params, co.rsk.bitcoinj.deprecated.core.BtcTransaction btcTransaction) {
        return new BtcTransaction(params, btcTransaction.bitcoinSerialize());
    }

    public static BtcECKey toStandard(co.rsk.bitcoinj.deprecated.core.BtcECKey key) {
        return key.isPubKeyOnly() ?
            BtcECKey.fromPublicOnly(key.getPubKey()) :
            BtcECKey.fromPrivate(key.getPrivKey());
    }

    public static List<BtcECKey> toStandardKeys(List<co.rsk.bitcoinj.deprecated.core.BtcECKey> keys) {
        List<BtcECKey> converted = new ArrayList<>();
        for (co.rsk.bitcoinj.deprecated.core.BtcECKey key : keys) {
            converted.add(toStandard(key));
        }

        return converted;
    }

    public static Script toStandard(co.rsk.bitcoinj.deprecated.script.Script script) {
        return new Script(script.getProgram());
    }
}
