package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.NetworkParameters;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class P2shP2WSHScriptTest {

    @Test
    public void getAddressFromP2shP2wshScript() {
        List<BtcECKey> keys = Arrays.asList(new String[]{
            "027de2af71862e0c64bf0ec5a66e3abc3b01fc57877802e6a6a81f6ea1d3561007",
            "02d9c67fef9f8d0707cbcca195eb5f26c6a65da6ca2d6130645c434bb924063856",
            "0346f033b8652a17d319d3ecbbbf20fd2cd663a6548173b9419d8228eef095012e"
        }).stream().map(k -> BtcECKey.fromPublicOnly(Hex.decode(k))).collect(Collectors.toList());

        Script redeemNuestro = new ScriptBuilder().createRedeemScript(
            keys.size() / 2 + 1,
            keys
        );

        Script p2SHP2WSHOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemNuestro);
        Address segwitAddress = Address.fromP2SHScript(
            NetworkParameters.fromID(NetworkParameters.ID_TESTNET),
            p2SHP2WSHOutputScript
        );

        Assert.assertEquals("2NCQHJJuG2iQjN2Be3QYXwWvFgS6AZ4MEkL", segwitAddress.toBase58());
        // https://mempool.space/testnet/tx/1744459aeaf7369aadc9fc40de9ab2bf575b14e35029b35a7ee4bbd3de65af7f
    }

}
