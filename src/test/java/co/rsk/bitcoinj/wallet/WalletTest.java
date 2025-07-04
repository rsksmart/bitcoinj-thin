package co.rsk.bitcoinj.wallet;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;

public class WalletTest {
    // data from tx https://mempool.space/testnet/tx/1744459aeaf7369aadc9fc40de9ab2bf575b14e35029b35a7ee4bbd3de65af7f
    private static final NetworkParameters TESTNET = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
    private static final Address ADDRESS_TO = Address.fromBase58(TESTNET, "mwUXQVdcCwCeYJx7mBhH4yLBU5N6QDSBZK");
    private static List<BtcECKey> PUBLIC_KEYS;

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

    private static Script REDEEM_SCRIPT;
    private static int AMOUNT_OF_UTXOS_TO_USE;
    private static Coin AMOUNT_IN_EACH_UTXO;
    private static Coin VALUE_TO_SEND;

    private Wallet wallet;
    private BtcTransaction tx;
    private SendRequest sr;

    private void setUp(Script scriptPubKey) {
        // network is UNITTESTNET by default and we need it to match wallet impl network
        Context.propagate(new Context(TESTNET));

        wallet = new Wallet(TESTNET) {
            @Override
            public RedeemData findRedeemDataFromScriptHash(byte[] payToScriptHash) {
                return RedeemData.of(PUBLIC_KEYS, REDEEM_SCRIPT);
            }
        };
        wallet.setCoinSelector(COIN_SELECTOR);

        UTXOProvider utxoProvider = getUtxosProvider(scriptPubKey);
        wallet.setUTXOProvider(utxoProvider);

        Address address = Address.fromP2SHScript(TESTNET, scriptPubKey);
        wallet.addWatchedAddress(address);

        tx = new BtcTransaction(TESTNET);
        tx.addOutput(VALUE_TO_SEND, ADDRESS_TO);

        sr = SendRequest.forTx(tx);
        sr.signInputs = false;
        sr.recipientsPayFees = true;
        sr.feePerKb = Coin.valueOf(10000L);
        sr.changeAddress = Address.fromP2SHScript(TESTNET, scriptPubKey);
        sr.shuffleOutputs = false;
    }

    private static UTXOProvider getUtxosProvider(Script scriptPubKey) {
        List<UTXO> utxos = new ArrayList<>();
        for (int i = 0; i < AMOUNT_OF_UTXOS_TO_USE; i++) {
            String hash = "hash" + "i";
            Sha256Hash utxoHash = Sha256Hash.of(hash.getBytes(StandardCharsets.UTF_8));
            UTXO utxo = new UTXO(utxoHash, i, AMOUNT_IN_EACH_UTXO, 10, false, scriptPubKey);
            utxos.add(utxo);
        }

        return new UTXOProvider() {
            @Override
            public List<UTXO> getOpenTransactionOutputs(List<Address> addresses) {
                return utxos;
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
    public void completeTx_legacyTx_shouldCalculateTxSizeCorrectly() throws InsufficientMoneyException {
        // arrange
        final BtcECKey publicKey1 = BtcECKey.fromPublicOnly(Hex.decode("027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d3561007"));
        final BtcECKey publicKey2 = BtcECKey.fromPublicOnly(Hex.decode("02d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856"));
        final BtcECKey publicKey3 = BtcECKey.fromPublicOnly(Hex.decode("0346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e"));
        PUBLIC_KEYS = Arrays.asList(publicKey1, publicKey2, publicKey3);
        REDEEM_SCRIPT = new Script(
            Hex.decode("5221027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d35610072102d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856210346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e53ae")
        );
        Script legacyScriptPubKey = ScriptBuilder.createP2SHOutputScript(REDEEM_SCRIPT);

        VALUE_TO_SEND = Coin.FIFTY_COINS;
        AMOUNT_OF_UTXOS_TO_USE = 1;
        AMOUNT_IN_EACH_UTXO = Coin.FIFTY_COINS;
        setUp(legacyScriptPubKey);

        // act
        assertFalse(sr.isSegwitCompatible);
        wallet.completeTx(sr);

        // assert
        double realSize = 375;
        int expectedCalculatedSize = 340;
        // for legacy tx, the calculation has a 10% diff approx,
        // but we have to keep the same implementation for backwards compatibility
        double allowedPercentageError = 10.0;
        assertCalculatedSizeIsCloseToExpectedSize(realSize, allowedPercentageError, expectedCalculatedSize);
    }

    @Test
    public void completeTx_segwitTx_shouldCalculateTxSizeCorrectly() throws InsufficientMoneyException {
        // arrange
        final BtcECKey publicKey1 = BtcECKey.fromPublicOnly(Hex.decode("027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d3561007"));
        final BtcECKey publicKey2 = BtcECKey.fromPublicOnly(Hex.decode("02d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856"));
        final BtcECKey publicKey3 = BtcECKey.fromPublicOnly(Hex.decode("0346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e"));
        PUBLIC_KEYS = Arrays.asList(publicKey1, publicKey2, publicKey3);
        REDEEM_SCRIPT = new Script(
            Hex.decode("5221027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d35610072102d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856210346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e53ae")
        );
        Script scriptPubKey = ScriptBuilder.createP2SHP2WSHOutputScript(REDEEM_SCRIPT);

        VALUE_TO_SEND = Coin.FIFTY_COINS;
        AMOUNT_OF_UTXOS_TO_USE = 1;
        AMOUNT_IN_EACH_UTXO = Coin.FIFTY_COINS;
        setUp(scriptPubKey);

        // act
        sr.isSegwitCompatible = true;
        wallet.completeTx(sr);

        // assert
        double realSize = 183.75;
        int expectedCalculatedSize = 184;
        // for segwit, the calculation seems to be pretty accurate
        double allowedPercentageError = 1.0;
        assertCalculatedSizeIsCloseToExpectedSize(realSize, allowedPercentageError, expectedCalculatedSize);
    }

    @Test
    public void completeTx_forTxThatExceedsMaximumStandardSize_throwsExceededMaxTransactionSizeException() {
        // inspired from tx https://mempool.space/testnet/tx/2cadd21b8b5a188afed27754a3f1a97324bd45ac8ec61ce57ff79e90b9fad8a7

        // arrange
        final BtcECKey publicKey1 = BtcECKey.fromPublicOnly(Hex.decode("0211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c"));
        final BtcECKey publicKey2 = BtcECKey.fromPublicOnly(Hex.decode("0238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c5"));
        final BtcECKey publicKey3 = BtcECKey.fromPublicOnly(Hex.decode("024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881"));
        final BtcECKey publicKey4 = BtcECKey.fromPublicOnly(Hex.decode("0274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f22"));
        final BtcECKey publicKey5 = BtcECKey.fromPublicOnly(Hex.decode("02867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec59"));
        final BtcECKey publicKey6 = BtcECKey.fromPublicOnly(Hex.decode("02881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb"));
        final BtcECKey publicKey7 = BtcECKey.fromPublicOnly(Hex.decode("029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc1"));
        final BtcECKey publicKey8 = BtcECKey.fromPublicOnly(Hex.decode("02a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db652033"));
        final BtcECKey publicKey9 = BtcECKey.fromPublicOnly(Hex.decode("02d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f89"));
        final BtcECKey publicKey10 = BtcECKey.fromPublicOnly(Hex.decode("02d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a3575"));
        final BtcECKey publicKey11 = BtcECKey.fromPublicOnly(Hex.decode("03163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d"));
        final BtcECKey publicKey12 = BtcECKey.fromPublicOnly(Hex.decode("033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e"));
        final BtcECKey publicKey13 = BtcECKey.fromPublicOnly(Hex.decode("0343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed"));
        final BtcECKey publicKey14 = BtcECKey.fromPublicOnly(Hex.decode("034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca"));
        final BtcECKey publicKey15 = BtcECKey.fromPublicOnly(Hex.decode("036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f69"));
        final BtcECKey publicKey16 = BtcECKey.fromPublicOnly(Hex.decode("03ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b"));
        final BtcECKey publicKey17 = BtcECKey.fromPublicOnly(Hex.decode("03bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd04"));
        final BtcECKey publicKey18 = BtcECKey.fromPublicOnly(Hex.decode("03be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf35"));
        final BtcECKey publicKey19 = BtcECKey.fromPublicOnly(Hex.decode("03e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b"));
        final BtcECKey publicKey20 = BtcECKey.fromPublicOnly(Hex.decode("03ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c3"));
        PUBLIC_KEYS = Arrays.asList(
            publicKey1, publicKey2, publicKey3, publicKey4, publicKey5, publicKey6, publicKey7, publicKey8, publicKey9, publicKey10,
            publicKey11, publicKey12, publicKey13, publicKey14, publicKey15, publicKey16, publicKey17, publicKey18, publicKey19, publicKey20
        );
        byte[] rawRedeemScript = Hex.decode("645b210211310637a4062844098b46278059298cc948e1ff314ca9ec75c82e0d0b8ad22c210238de69e208565fd82e4b76c4eff5d817a51679b8a90c41709e49660ba23501c521024b120731b26ec7165cddd214fc8e3f0c844a03dc0e533fb0cf9f89ad2f68a881210274564db76110474ac0d7e09080c182855b22a864cc201ed55217b23301f52f222102867f0e693a2553bf2bc13a5efa0b516b28e66317fbe8e484dd3f375bcb48ec592102881af2910c909f224557353dd28e3729363cf5c24232f26d25c92dac72a3dcdb21029c75b3257e0842c4be48e57e39bf2735c74a76c4b1c0b08d1cc66bf5b8748cc12102a46cbe93287cb51a398a157de2b428f21a94f46affdd916ce921bd10db6520332102d335ef4eeb74330c3a53f529f9741fa096412c7982ed681fcf69763894f34f892102d3f5fd6e107cf68b1be8dce0e16a0a8afb8dcef9a76c851d7eaf6d51c46a35752103163b86a62b4eeeb52f67cb16ce13a8622a066f2a063280749b956a97705dfc3d21033267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e210343e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed21034461d4263b907cfc5ebb468f19d6a133b567f3cc4855e8725faaf60c6e388bca21036e92e6555d2e70af4f5a4f888145356e60bb1a5bc00786a8e9f50152090b2f692103ab54da6b69407dcaaa85f6904687052c93f1f9dd0633f1321b3e624fcd30144b2103bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd042103be060191c9632184f2a0ab2638eeed04399372f37fc7a3cff5291cfd6426cf352103e6def9ef0597336eb58d24f955b6b63756cf7b3885322f9d0cf5a2a12f7e459b2103ef03253b7b4f33d68c39141eb016df15fafbb1d0fa4a2e7f208c94ea154ab8c30114ae67011eb2755b21021a560245f78312588f600315d75d493420bed65873b63d0d4bb8ca1b9163a35b2102218e9dc07ac4190a1d7df94fc75953b36671129f12668a94f1f504fe47399ead210272ed6e14e70f6b4757d412729730837bc63b6313276be8308a5a96afd63af9942102872f69892a74d60f6185c2908414dcddb24951c035a1a8466c6c56f55043e7602102886d7d8e865f75dfda3ddf94619af87ad8aa71e8ef393e1e57593576b7d7af1621028e59462fb53ba31186a353b7ea77ebefda9097392e45b7ca7a216168230d05af21028f5a88b08d75765b36951254e68060759de5be7e559972c37c67fc8cedafeb262102c9ced4bbc468af9ace1645df2fd50182d5822cb4c68aae0e50ae1d45da260d2a2102deba35a96add157b6de58f48bb6e23bcb0a17037bed1beb8ba98de6b0a0d71d62102f2e00fefa5868e2c56405e188ec1d97557a7c77fb6a448352cc091c2ae9d50492102fb8c06c723d4e59792e36e6226087fcfac65c1d8a0d5c5726a64102a551528442103077c62a45ea1a679e54c9f7ad800d8e40eaf6012657c8dccd3b61d5e070d9a432103616959a72dd302043e9db2dbd7827944ecb2d555a8f72a48bb8f916ec5aac6ec210362f9c79cd0586704d6a9ea863573f3b123d90a31faaa5a1d9a69bf9631c78ae321036899d94ad9d3f24152dd4fa79b9cb8dddbd26d18297be4facb295f57c9de60bd210376e4cb35baa8c46b0dcffaf303785c5f7aadf457df30ac956234cc8114e2f47d2103a587256beec4e167aebc478e1d6502bb277a596ae9574ccb646da11fffbf36502103bb9da162c3f581ced93167f86d7e0e5962762a1188f5bd1f8b5d08fed46ef73d2103c34fcd05cef2733ea7337c37f50ae26245646aba124948c6ff8dcdf8212849982103f8ac768e683a07ac4063f72a6d856aedeae109f844abcfa34ac9519d715177460114ae68");
        REDEEM_SCRIPT = new Script(rawRedeemScript);
        Script scriptPubKey = ScriptBuilder.createP2SHP2WSHOutputScript(REDEEM_SCRIPT);

        AMOUNT_OF_UTXOS_TO_USE = 170;
        AMOUNT_IN_EACH_UTXO = Coin.valueOf(10_000);
        VALUE_TO_SEND = AMOUNT_IN_EACH_UTXO.multiply(AMOUNT_OF_UTXOS_TO_USE); // to have to use all the utxos

        setUp(scriptPubKey);
        sr.isSegwitCompatible = true;

        // act & assert
        assertThrows(Wallet.ExceededMaxTransactionSize.class, () -> wallet.completeTx(sr));
    }

    private void assertCalculatedSizeIsCloseToExpectedSize(double realSize, double allowedPercentageError, int expectedCalculatedSize) {
        double allowedError = Math.ceil(realSize * allowedPercentageError / 100);

        Coin availableAmount = AMOUNT_IN_EACH_UTXO.multiply(AMOUNT_OF_UTXOS_TO_USE);
        Coin actualValueSent = tx.getOutputs().get(0).getValue();
        Coin fee = availableAmount.minus(actualValueSent);
        // fee = (tx size * feePerKb) / 1000 => fee = (tx size * 10_000) / 1000
        // => fee = tx size * 10 => tx size = fee / 10
        long size = fee.divide(10L).getValue();
        assertEquals(expectedCalculatedSize, size); // to make the test deterministic

        double diff = Math.abs(realSize - size);
        assertTrue(diff <= allowedError);
    }
}
