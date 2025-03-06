package co.rsk.bitcoinj.wallet;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WalletTest {
    // data from tx https://mempool.space/testnet/tx/1744459aeaf7369aadc9fc40de9ab2bf575b14e35029b35a7ee4bbd3de65af7f
    private static final NetworkParameters TESTNET = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    private static final Address ADDRESS_TO = Address.fromBase58(TESTNET, "mwUXQVdcCwCeYJx7mBhH4yLBU5N6QDSBZK");

    private static final BtcECKey PUBLIC_KEY_1 = BtcECKey.fromPublicOnly(Hex.decode("027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d3561007"));
    private static final BtcECKey PUBLIC_KEY_2 = BtcECKey.fromPublicOnly(Hex.decode("02d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856"));
    private static final BtcECKey PUBLIC_KEY_3 = BtcECKey.fromPublicOnly(Hex.decode("0346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e"));
    private static final List<BtcECKey> PUBLIC_KEYS = Arrays.asList(PUBLIC_KEY_1, PUBLIC_KEY_2, PUBLIC_KEY_3);
    private static final Script REDEEM_SCRIPT = new Script(
        Hex.decode("5221027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d35610072102d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856210346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e53ae")
    );

    private static final Sha256Hash UTXO_HASH = Sha256Hash.of("hash".getBytes(StandardCharsets.UTF_8));
    private static final Coin AVAILABLE_AMOUNT = Coin.FIFTY_COINS;

    // CoinSelector and UTXOProvider are just interfaces,
    // so rskj has its own implementations for them.
    // It also has a custom impl for a BridgeBtcWallet.
    // So them have to be overridden (copying rskj behavior) for proper testing.
    private static final CoinSelector COIN_SELECTOR = (target, candidates) -> {
        ArrayList<TransactionOutput> selected = new ArrayList<>();
        ArrayList<TransactionOutput> outputs = new ArrayList<>(candidates);
        long total = 0;
        for (TransactionOutput output : outputs) {
            if (total >= target.value) {
                break;
            }
            selected.add(output);
            total += output.getValue().value;
        }
        return new CoinSelection(Coin.valueOf(total), selected);
    };

    private Wallet wallet;
    private BtcTransaction tx;
    private SendRequest sr;

    private void setUp(Script scriptPubKey) {
        wallet = new Wallet(TESTNET) {
            @Override
            public RedeemData findRedeemDataFromScriptHash(byte[] payToScriptHash) {
                return RedeemData.of(PUBLIC_KEYS, REDEEM_SCRIPT);
            }
        };
        wallet.setCoinSelector(COIN_SELECTOR);

        UTXOProvider utxoProvider = getUtxoProvider(scriptPubKey);
        wallet.setUTXOProvider(utxoProvider);

        Address address = Address.fromP2SHScript(TESTNET, scriptPubKey);
        wallet.addWatchedAddress(address);

        tx = new BtcTransaction(TESTNET);
        Coin valueToSend = Coin.FIFTY_COINS;
        tx.addOutput(valueToSend, ADDRESS_TO);

        sr = SendRequest.forTx(tx);
        sr.signInputs = false;
        sr.recipientsPayFees = true;
        sr.feePerKb = Coin.valueOf(10000L);
    }

    private static UTXOProvider getUtxoProvider(Script scriptPubKey) {
        UTXO utxo = new UTXO(UTXO_HASH, 0, AVAILABLE_AMOUNT, 10, false, scriptPubKey);

        return new UTXOProvider() {
            @Override
            public List<UTXO> getOpenTransactionOutputs(List<Address> addresses) {
                return Collections.singletonList(utxo);
            }

            @Override
            public int getChainHeadHeight() {
                return Integer.MAX_VALUE;
            }

            @Override
            public NetworkParameters getParams() {
                return TESTNET;
            }
        };
    }

    @Test
    public void completeTx_legacy() throws InsufficientMoneyException {
        // arrange
        Script legacyScriptPubKey = ScriptBuilder.createP2SHOutputScript(REDEEM_SCRIPT);
        setUp(legacyScriptPubKey);

        // act
        assertFalse(sr.isSegwitCompatible);
        wallet.completeTx(sr);

        // assert
        double expectedSize = 375;
        // for legacy tx, the calculation has a 10% diff approx,
        // but we have to keep the same implementation for backwards compatibility
        double allowedPercentageError = 10.0;
        assertCalculatedSizeIsCloseToExpectedSize(expectedSize, allowedPercentageError);
    }

    @Test
    public void completeTx_segwit() throws InsufficientMoneyException {
        // arrange
        Script scriptPubKey = ScriptBuilder.createP2SHP2WSHOutputScript(REDEEM_SCRIPT);
        setUp(scriptPubKey);

        // act
        sr.isSegwitCompatible = true;
        wallet.completeTx(sr);

        // assert
        double expectedSize = 183.75;
        // for segwit, the calculation seems to be pretty accurate
        double allowedPercentageError = 1.0;
        assertCalculatedSizeIsCloseToExpectedSize(expectedSize, allowedPercentageError);
    }

    private void assertCalculatedSizeIsCloseToExpectedSize(double expectedSize, double allowedPercentageError) {
        double allowedError = Math.ceil(expectedSize * allowedPercentageError / 100);

        Coin actualValueSent = tx.getOutputs().get(0).getValue();
        Coin fee = AVAILABLE_AMOUNT.minus(actualValueSent);
        // fee = (tx size * feePerKb) / 1000 => fee = (tx size * 10_000) / 1000
        // => fee = tx size * 10 => tx size = fee / 10
        long size = fee.divide(10L).getValue();

        double diff = Math.abs(expectedSize - size);
        assertTrue(diff <= allowedError);
    }
}
