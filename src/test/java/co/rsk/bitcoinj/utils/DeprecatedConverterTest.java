package co.rsk.bitcoinj.utils;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.Context;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.UTXO;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class DeprecatedConverterTest {

    @Test
    public void toDeprecated_networkParameters_testnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = DeprecatedConverter.toDeprecated(networkParameters);

        Assert.assertEquals(networkParameters.getId(), networkParametersDeprecated.getId());
        Assert.assertEquals(networkParameters.hashCode(), networkParametersDeprecated.hashCode());
    }

    @Test
    public void toDeprecated_networkParameters_mainnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = DeprecatedConverter.toDeprecated(networkParameters);

        Assert.assertEquals(networkParameters.getId(), networkParametersDeprecated.getId());
        Assert.assertEquals(networkParameters.hashCode(), networkParametersDeprecated.hashCode());
    }

    @Test
    public void toDeprecated_address_testnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);

        String addressBase58 = "mweMRzr7EvTCkYLvrHLTwjuRMcpZexjBaL";
        Address address = Address.fromBase58(networkParameters, addressBase58);
        co.rsk.bitcoinj.deprecated.core.Address addressDeprecated = DeprecatedConverter.toDeprecated(networkParameters, address);

        Assert.assertEquals(addressBase58, address.toBase58());
        Assert.assertEquals(addressBase58, addressDeprecated.toBase58());
        Assert.assertArrayEquals(address.getHash160(), addressDeprecated.getHash160());
    }

    @Test
    public void toDeprecated_address_mainnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

        String addressBase58 = "38VeCQjDENgfnipCfQMDbkWPHsQVRAPnmt";
        Address address = Address.fromBase58(networkParameters, addressBase58);
        co.rsk.bitcoinj.deprecated.core.Address addressDeprecated = DeprecatedConverter.toDeprecated(networkParameters, address);

        Assert.assertEquals(addressBase58, address.toBase58());
        Assert.assertEquals(addressBase58, addressDeprecated.toBase58());
        Assert.assertArrayEquals(address.getHash160(), addressDeprecated.getHash160());
    }

    @Test
    public void toDeprecated_coin() {
        long coinValue = 100_000L;
        Coin coin = Coin.valueOf(100_000L);
        co.rsk.bitcoinj.deprecated.core.Coin coinDeprecated = DeprecatedConverter.toDeprecated(coin);

        Assert.assertEquals(coinValue, coin.value);
        Assert.assertEquals(coinValue, coinDeprecated.value);
        Assert.assertEquals(coin.value, coinDeprecated.value);
        Assert.assertEquals(coin.toFriendlyString(), coinDeprecated.toFriendlyString());
    }

    @Test
    public void toDeprecated_context_testnet() {
        String networkParametersId = NetworkParameters.ID_TESTNET;
        NetworkParameters networkParameters = NetworkParameters.fromID(networkParametersId);
        Context context = new Context(networkParameters);
        co.rsk.bitcoinj.deprecated.core.Context contextDeprecated = DeprecatedConverter.toDeprecated(context);

        Assert.assertEquals(networkParametersId, context.getParams().getId());
        Assert.assertEquals(networkParametersId, contextDeprecated.getParams().getId());
        Assert.assertEquals(context.getParams().getId(), contextDeprecated.getParams().getId());
    }

    @Test
    public void toDeprecated_context_mainnet() {
        String networkParametersId = NetworkParameters.ID_MAINNET;
        NetworkParameters networkParameters = NetworkParameters.fromID(networkParametersId);
        Context context = new Context(networkParameters);
        co.rsk.bitcoinj.deprecated.core.Context contextDeprecated = DeprecatedConverter.toDeprecated(context);

        Assert.assertEquals(networkParametersId, context.getParams().getId());
        Assert.assertEquals(networkParametersId, contextDeprecated.getParams().getId());
        Assert.assertEquals(context.getParams().getId(), contextDeprecated.getParams().getId());
    }

    @Test
    public void toDeprecated_utxo() throws IOException {
        UTXO utxo = new UTXO(
            Sha256Hash.of("abc".getBytes(StandardCharsets.UTF_8)),
            0,
            Coin.FIFTY_COINS,
            1000,
            false,
            ScriptBuilder.createOutputScript(new BtcECKey())
        );

        co.rsk.bitcoinj.deprecated.core.UTXO utxoDeprecated = DeprecatedConverter.toDeprecated(utxo);

        Assert.assertArrayEquals(utxo.getHash().getBytes(), utxoDeprecated.getHash().getBytes());
        Assert.assertEquals(utxo.getIndex(), utxoDeprecated.getIndex());
        Assert.assertEquals(utxo.getValue().value, utxoDeprecated.getValue().value);
        Assert.assertEquals(utxo.getHeight(), utxoDeprecated.getHeight());
        Assert.assertEquals(utxo.isCoinbase(), utxoDeprecated.isCoinbase());
        Assert.assertArrayEquals(utxo.getScript().getProgram(), utxoDeprecated.getScript().getProgram());
    }

    @Test
    public void toDeprecatedUtxos() throws IOException {
        UTXO utxo1 = new UTXO(
            Sha256Hash.of("abc".getBytes(StandardCharsets.UTF_8)),
            0,
            Coin.FIFTY_COINS,
            1000,
            false,
            ScriptBuilder.createOutputScript(new BtcECKey())
        );

        UTXO utxo2 = new UTXO(
            Sha256Hash.of("def".getBytes(StandardCharsets.UTF_8)),
            1,
            Coin.COIN,
            500,
            false,
            ScriptBuilder.createOutputScript(new BtcECKey())
        );

        List<UTXO> utxos = Arrays.asList(utxo1, utxo2);
        List<co.rsk.bitcoinj.deprecated.core.UTXO> utxosDeprecated = DeprecatedConverter.toDeprecatedUtxos(utxos);

        co.rsk.bitcoinj.deprecated.core.UTXO utxoDeprecated = utxosDeprecated.get(0);
        Assert.assertArrayEquals(utxo1.getHash().getBytes(), utxoDeprecated.getHash().getBytes());
        Assert.assertEquals(utxo1.getIndex(), utxoDeprecated.getIndex());
        Assert.assertEquals(utxo1.getValue().value, utxoDeprecated.getValue().value);
        Assert.assertEquals(utxo1.getHeight(), utxoDeprecated.getHeight());
        Assert.assertEquals(utxo1.isCoinbase(), utxoDeprecated.isCoinbase());
        Assert.assertArrayEquals(utxo1.getScript().getProgram(), utxoDeprecated.getScript().getProgram());

        utxoDeprecated = utxosDeprecated.get(1);
        Assert.assertArrayEquals(utxo2.getHash().getBytes(), utxoDeprecated.getHash().getBytes());
        Assert.assertEquals(utxo2.getIndex(), utxoDeprecated.getIndex());
        Assert.assertEquals(utxo2.getValue().value, utxoDeprecated.getValue().value);
        Assert.assertEquals(utxo2.getHeight(), utxoDeprecated.getHeight());
        Assert.assertEquals(utxo2.isCoinbase(), utxoDeprecated.isCoinbase());
        Assert.assertArrayEquals(utxo2.getScript().getProgram(), utxoDeprecated.getScript().getProgram());
    }

    @Test
    public void toDeprecated_btcTransaction_testnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);

        BtcTransaction btcTransaction = new BtcTransaction(networkParameters);
        btcTransaction.addInput(
            Sha256Hash.of("a".getBytes(StandardCharsets.UTF_8)),
            0,
            ScriptBuilder.createRedeemScript(2, Arrays.asList(new BtcECKey(), new BtcECKey(), new BtcECKey()))
        );
        btcTransaction.addInput(
            Sha256Hash.of("b".getBytes(StandardCharsets.UTF_8)),
            2,
            ScriptBuilder.createRedeemScript(2, Arrays.asList(new BtcECKey(), new BtcECKey(), new BtcECKey()))
        );
        btcTransaction.addOutput(Coin.COIN, Address.fromBase58(networkParameters, "mweMRzr7EvTCkYLvrHLTwjuRMcpZexjBaL"));

        co.rsk.bitcoinj.deprecated.core.BtcTransaction btcTransactionDeprecated = DeprecatedConverter.toDeprecated(networkParameters, btcTransaction);

        Assert.assertArrayEquals(btcTransaction.getHash().getBytes(), btcTransactionDeprecated.getHash().getBytes());
        Assert.assertEquals(btcTransaction.toString(), btcTransactionDeprecated.toString());
    }

    @Test
    public void toDeprecated_btcTransaction_mainnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

        BtcTransaction btcTransaction = new BtcTransaction(networkParameters);
        btcTransaction.addInput(
            Sha256Hash.of("a".getBytes(StandardCharsets.UTF_8)),
            0,
            ScriptBuilder.createRedeemScript(2, Arrays.asList(new BtcECKey(), new BtcECKey(), new BtcECKey()))
        );
        btcTransaction.addInput(
            Sha256Hash.of("b".getBytes(StandardCharsets.UTF_8)),
            2,
            ScriptBuilder.createRedeemScript(2, Arrays.asList(new BtcECKey(), new BtcECKey(), new BtcECKey()))
        );
        btcTransaction.addOutput(Coin.COIN, Address.fromBase58(networkParameters, "38VeCQjDENgfnipCfQMDbkWPHsQVRAPnmt"));

        co.rsk.bitcoinj.deprecated.core.BtcTransaction btcTransactionDeprecated = DeprecatedConverter.toDeprecated(networkParameters, btcTransaction);

        Assert.assertArrayEquals(btcTransaction.getHash().getBytes(), btcTransactionDeprecated.getHash().getBytes());
        Assert.assertEquals(btcTransaction.toString(), btcTransactionDeprecated.toString());
    }

    @Test
    public void toDeprecated_btcEcKey_withPrivateKey() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        BtcECKey key = new BtcECKey();
        Assert.assertFalse(key.isPubKeyOnly());

        co.rsk.bitcoinj.deprecated.core.BtcECKey keyDeprecated = DeprecatedConverter.toDeprecated(key);

        Assert.assertFalse(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(key.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(key.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(key.getPrivKeyBytes(), keyDeprecated.getPrivKeyBytes());
        Assert.assertArrayEquals(key.getPubKeyHash(), keyDeprecated.getPubKeyHash());
    }

    @Test
    public void toDeprecated_btcEcKey_pubKeyOnly() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        BtcECKey key = BtcECKey.fromPublicOnly((new BtcECKey()).getPubKey());
        Assert.assertTrue(key.isPubKeyOnly());

        co.rsk.bitcoinj.deprecated.core.BtcECKey keyDeprecated = DeprecatedConverter.toDeprecated(key);

        Assert.assertTrue(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(key.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(key.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(key.getPubKeyHash(), keyDeprecated.getPubKeyHash());
    }

    @Test
    public void toDeprecatedKeys() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(NetworkParameters.ID_TESTNET);

        BtcECKey keyWithPrivate = new BtcECKey();
        BtcECKey keyPubKeyOnly = BtcECKey.fromPublicOnly((new BtcECKey()).getPubKey());
        Assert.assertFalse(keyWithPrivate.isPubKeyOnly());
        Assert.assertTrue(keyPubKeyOnly.isPubKeyOnly());

        List<BtcECKey> keys = Arrays.asList(keyWithPrivate, keyPubKeyOnly);
        List<co.rsk.bitcoinj.deprecated.core.BtcECKey> keysDeprecated = DeprecatedConverter.toDeprecatedKeys(keys);

        co.rsk.bitcoinj.deprecated.core.BtcECKey keyDeprecated = keysDeprecated.get(0);
        Assert.assertFalse(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(keyWithPrivate.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(keyWithPrivate.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(keyWithPrivate.getPrivKeyBytes(), keyDeprecated.getPrivKeyBytes());
        Assert.assertArrayEquals(keyWithPrivate.getPubKeyHash(), keyDeprecated.getPubKeyHash());

        keyDeprecated = keysDeprecated.get(1);
        Assert.assertTrue(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(keyPubKeyOnly.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(keyPubKeyOnly.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(keyPubKeyOnly.getPubKeyHash(), keyDeprecated.getPubKeyHash());
    }

    @Test
    public void toDeprecated_script() {
        Script script = ScriptBuilder.createRedeemScript(3, Arrays.asList(new BtcECKey(), new BtcECKey(), new BtcECKey(), new BtcECKey(), new BtcECKey()));
        co.rsk.bitcoinj.deprecated.script.Script scriptDeprecated = DeprecatedConverter.toDeprecated(script);

        Assert.assertArrayEquals(script.getProgram(), scriptDeprecated.getProgram());
    }

    @Test
    public void toStandard_utxo() throws IOException {
        co.rsk.bitcoinj.deprecated.core.UTXO utxoDeprecated = new co.rsk.bitcoinj.deprecated.core.UTXO(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("abc".getBytes(StandardCharsets.UTF_8)),
            0,
            co.rsk.bitcoinj.deprecated.core.Coin.FIFTY_COINS,
            1000,
            false,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createOutputScript(new co.rsk.bitcoinj.deprecated.core.BtcECKey())
        );

        UTXO utxoStandard = DeprecatedConverter.toStandard(utxoDeprecated);

        Assert.assertArrayEquals(utxoDeprecated.getHash().getBytes(), utxoStandard.getHash().getBytes());
        Assert.assertEquals(utxoDeprecated.getIndex(), utxoStandard.getIndex());
        Assert.assertEquals(utxoDeprecated.getValue().value, utxoStandard.getValue().value);
        Assert.assertEquals(utxoDeprecated.getHeight(), utxoStandard.getHeight());
        Assert.assertEquals(utxoDeprecated.isCoinbase(), utxoStandard.isCoinbase());
        Assert.assertArrayEquals(utxoDeprecated.getScript().getProgram(), utxoStandard.getScript().getProgram());
    }

    @Test
    public void toStandardUtxos() throws IOException {
        co.rsk.bitcoinj.deprecated.core.UTXO utxoDeprecated1 = new co.rsk.bitcoinj.deprecated.core.UTXO(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("abc".getBytes(StandardCharsets.UTF_8)),
            0,
            co.rsk.bitcoinj.deprecated.core.Coin.FIFTY_COINS,
            1000,
            false,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createOutputScript(new co.rsk.bitcoinj.deprecated.core.BtcECKey())
        );

        co.rsk.bitcoinj.deprecated.core.UTXO utxoDeprecated2 = new co.rsk.bitcoinj.deprecated.core.UTXO(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("def".getBytes(StandardCharsets.UTF_8)),
            1,
            co.rsk.bitcoinj.deprecated.core.Coin.COIN,
            500,
            false,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createOutputScript(new co.rsk.bitcoinj.deprecated.core.BtcECKey())
        );

        List<co.rsk.bitcoinj.deprecated.core.UTXO> utxosDeprecated = Arrays.asList(utxoDeprecated1, utxoDeprecated2);
        List<UTXO> utxosStandard = DeprecatedConverter.toStandardUtxos(utxosDeprecated);

        UTXO utxoStandard = utxosStandard.get(0);
        Assert.assertArrayEquals(utxoDeprecated1.getHash().getBytes(), utxoStandard.getHash().getBytes());
        Assert.assertEquals(utxoDeprecated1.getIndex(), utxoStandard.getIndex());
        Assert.assertEquals(utxoDeprecated1.getValue().value, utxoStandard.getValue().value);
        Assert.assertEquals(utxoDeprecated1.getHeight(), utxoStandard.getHeight());
        Assert.assertEquals(utxoDeprecated1.isCoinbase(), utxoStandard.isCoinbase());
        Assert.assertArrayEquals(utxoDeprecated1.getScript().getProgram(), utxoStandard.getScript().getProgram());

        utxoStandard = utxosStandard.get(1);
        Assert.assertArrayEquals(utxoDeprecated2.getHash().getBytes(), utxoStandard.getHash().getBytes());
        Assert.assertEquals(utxoDeprecated2.getIndex(), utxoStandard.getIndex());
        Assert.assertEquals(utxoDeprecated2.getValue().value, utxoStandard.getValue().value);
        Assert.assertEquals(utxoDeprecated2.getHeight(), utxoStandard.getHeight());
        Assert.assertEquals(utxoDeprecated2.isCoinbase(), utxoStandard.isCoinbase());
        Assert.assertArrayEquals(utxoDeprecated2.getScript().getProgram(), utxoStandard.getScript().getProgram());
    }

    @Test
    public void toStandard_btcTransaction_testnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(
            co.rsk.bitcoinj.deprecated.core.NetworkParameters.ID_TESTNET);

        co.rsk.bitcoinj.deprecated.core.BtcTransaction btcTransactionDeprecated = new co.rsk.bitcoinj.deprecated.core.BtcTransaction(networkParametersDeprecated);
        btcTransactionDeprecated.addInput(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("a".getBytes(StandardCharsets.UTF_8)),
            0,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createRedeemScript(2, Arrays.asList(
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey()
            ))
        );
        btcTransactionDeprecated.addInput(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("b".getBytes(StandardCharsets.UTF_8)),
            2,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createRedeemScript(2, Arrays.asList(
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey()
            ))
        );
        btcTransactionDeprecated.addOutput(
            co.rsk.bitcoinj.deprecated.core.Coin.COIN,
            co.rsk.bitcoinj.deprecated.core.Address.fromBase58(networkParametersDeprecated, "mweMRzr7EvTCkYLvrHLTwjuRMcpZexjBaL")
        );

        BtcTransaction btcTransactionStandard = DeprecatedConverter.toStandard(networkParameters, btcTransactionDeprecated);

        Assert.assertArrayEquals(btcTransactionDeprecated.getHash().getBytes(), btcTransactionStandard.getHash().getBytes());
        Assert.assertEquals(btcTransactionDeprecated.toString(), btcTransactionStandard.toString());
    }

    @Test
    public void toStandard_btcTransaction_mainnet() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(
            co.rsk.bitcoinj.deprecated.core.NetworkParameters.ID_MAINNET);

        co.rsk.bitcoinj.deprecated.core.BtcTransaction btcTransactionDeprecated = new co.rsk.bitcoinj.deprecated.core.BtcTransaction(networkParametersDeprecated);
        btcTransactionDeprecated.addInput(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("a".getBytes(StandardCharsets.UTF_8)),
            0,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createRedeemScript(2, Arrays.asList(
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey()
            ))
        );
        btcTransactionDeprecated.addInput(
            co.rsk.bitcoinj.deprecated.core.Sha256Hash.of("b".getBytes(StandardCharsets.UTF_8)),
            2,
            co.rsk.bitcoinj.deprecated.script.ScriptBuilder.createRedeemScript(2, Arrays.asList(
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey(),
                new co.rsk.bitcoinj.deprecated.core.BtcECKey()
            ))
        );
        btcTransactionDeprecated.addOutput(
            co.rsk.bitcoinj.deprecated.core.Coin.COIN,
            co.rsk.bitcoinj.deprecated.core.Address.fromBase58(networkParametersDeprecated, "38VeCQjDENgfnipCfQMDbkWPHsQVRAPnmt")
        );

        BtcTransaction btcTransactionStandard = DeprecatedConverter.toStandard(networkParameters, btcTransactionDeprecated);

        Assert.assertArrayEquals(btcTransactionDeprecated.getHash().getBytes(), btcTransactionStandard.getHash().getBytes());
        Assert.assertEquals(btcTransactionDeprecated.toString(), btcTransactionStandard.toString());
    }

    @Test
    public void toStandard_btcEcKey_withPrivateKey() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        BtcECKey key = new BtcECKey();
        Assert.assertFalse(key.isPubKeyOnly());

        co.rsk.bitcoinj.deprecated.core.BtcECKey keyDeprecated = DeprecatedConverter.toDeprecated(key);

        Assert.assertFalse(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(key.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(key.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(key.getPrivKeyBytes(), keyDeprecated.getPrivKeyBytes());
        Assert.assertArrayEquals(key.getPubKeyHash(), keyDeprecated.getPubKeyHash());
    }

    @Test
    public void toDeprecated_btcEcKey_pubKeyOnly() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        BtcECKey key = BtcECKey.fromPublicOnly((new BtcECKey()).getPubKey());
        Assert.assertTrue(key.isPubKeyOnly());

        co.rsk.bitcoinj.deprecated.core.BtcECKey keyDeprecated = DeprecatedConverter.toDeprecated(key);

        Assert.assertTrue(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(key.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(key.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(key.getPubKeyHash(), keyDeprecated.getPubKeyHash());
    }

    @Test
    public void toDeprecatedKeys() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        co.rsk.bitcoinj.deprecated.core.NetworkParameters networkParametersDeprecated = co.rsk.bitcoinj.deprecated.core.NetworkParameters.fromID(NetworkParameters.ID_TESTNET);

        BtcECKey keyWithPrivate = new BtcECKey();
        BtcECKey keyPubKeyOnly = BtcECKey.fromPublicOnly((new BtcECKey()).getPubKey());
        Assert.assertFalse(keyWithPrivate.isPubKeyOnly());
        Assert.assertTrue(keyPubKeyOnly.isPubKeyOnly());

        List<BtcECKey> keys = Arrays.asList(keyWithPrivate, keyPubKeyOnly);
        List<co.rsk.bitcoinj.deprecated.core.BtcECKey> keysDeprecated = DeprecatedConverter.toDeprecatedKeys(keys);

        co.rsk.bitcoinj.deprecated.core.BtcECKey keyDeprecated = keysDeprecated.get(0);
        Assert.assertFalse(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(keyWithPrivate.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(keyWithPrivate.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(keyWithPrivate.getPrivKeyBytes(), keyDeprecated.getPrivKeyBytes());
        Assert.assertArrayEquals(keyWithPrivate.getPubKeyHash(), keyDeprecated.getPubKeyHash());

        keyDeprecated = keysDeprecated.get(1);
        Assert.assertTrue(keyDeprecated.isPubKeyOnly());
        Assert.assertArrayEquals(keyPubKeyOnly.getPubKey(), keyDeprecated.getPubKey());
        Assert.assertEquals(keyPubKeyOnly.toAddress(networkParameters).toBase58(), keyDeprecated.toAddress(networkParametersDeprecated).toBase58());
        Assert.assertArrayEquals(keyPubKeyOnly.getPubKeyHash(), keyDeprecated.getPubKeyHash());
    }

    @Test
    public void toDeprecated_script() {
        Script script = ScriptBuilder.createRedeemScript(3, Arrays.asList(new BtcECKey(), new BtcECKey(), new BtcECKey(), new BtcECKey(), new BtcECKey()));
        co.rsk.bitcoinj.deprecated.script.Script scriptDeprecated = DeprecatedConverter.toDeprecated(script);

        Assert.assertArrayEquals(script.getProgram(), scriptDeprecated.getProgram());
    }
}
