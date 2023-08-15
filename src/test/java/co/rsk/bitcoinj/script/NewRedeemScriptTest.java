package co.rsk.bitcoinj.script;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_0;

public class NewRedeemScriptTest {
    @Test
    void spendFromP2shP2wshAddressWithNewRedeem() {
        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        long activationDelay = 30;

        // Created with GenNodeKeyId using seed 'fed1'
        byte[] publicKeyBytes = Hex.decode("043267e382e076cbaa199d49ea7362535f95b135de181caf66b391f541bf39ab0e75b8577faac2183782cb0d76820cf9f356831d216e99d886f8a6bc47fe696939");
        BtcECKey btcKey1 = BtcECKey.fromPublicOnly(publicKeyBytes);
        BtcECKey fed1PrivKey = BtcECKey.fromPrivate(Hex.decode("529822842595a3a6b3b3e51e9cffa0db66452599f7beec542382a02b1e42be4b"));

        // Created with GenNodeKeyId using seed 'fed3', used for fed2 to keep keys sorted
        publicKeyBytes = Hex.decode("0443e106d90183e2eef7d5cb7538a634439bf1301d731787c6736922ff19e750ed39e74a76731fed620aeedbcd77e4de403fc4148efd3b5dbfc6cef550aa63c377");
        BtcECKey btcKey2 = BtcECKey.fromPublicOnly(publicKeyBytes);
        BtcECKey fed2PrivKey = BtcECKey.fromPrivate(Hex.decode("b2889610e66cd3f7de37c81c20c786b576349b80b3f844f8409e3a29d95c0c7c"));

        // Created with GenNodeKeyId using seed 'fed2', used for fed3 to keep keys sorted
        publicKeyBytes = Hex.decode("04bd5b51b1c5d799da190285c8078a2712b8e5dc6f73c799751e6256bb89a4bd04c6444b00289fc76ee853fcfa52b3083d66c42e84f8640f53a4cdf575e4d4a399");
        BtcECKey btcKey3 = BtcECKey.fromPublicOnly(publicKeyBytes);
        BtcECKey fed3PrivKey = BtcECKey.fromPrivate(Hex.decode("fa013890aa14dd269a0ca16003cabde1688021358b662d17b1e8c555f5cccc6e"));

        List<BtcECKey> keys = Arrays.asList(btcKey1, btcKey2, btcKey3);
        List<BtcECKey> privateKeys =  Arrays.asList(fed1PrivKey, fed2PrivKey, fed3PrivKey);

        Script redeemScript = new ScriptBuilder().createNewRedeemScript( keys.size() / 2 + 1, keys);

        Script p2shOutputScript = ScriptBuilder.createP2SHOutputScript(redeemScript);
        Address legacyAddress = Address.fromP2SHScript(
            networkParameters,
            p2shOutputScript
        );
        System.out.println(legacyAddress);


        Sha256Hash fundTxHash = Sha256Hash.wrap("27d02abaf5a508d17db3ed612cc0d9cd2859d53b3ce30b0535711eddcac0c992");
        int outputIndex = 0;
        Address destinationAddress = Address.fromBase58(networkParameters, "msgc5Gtz2L9MVhXPDrFRCYPa16QgoZ2EjP"); // testnet
        Coin value = Coin.valueOf(10_000);
        Coin fee = Coin.valueOf(1_000);

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTxHash, outputIndex, new Script(new byte[]{}));
        spendTx.addOutput(value.minus(fee), destinationAddress);
        spendTx.setVersion(2);

        // Create signatures
        int inputIndex = 0;
        Sha256Hash sigHash = spendTx.hashForSignature(
            inputIndex,
            redeemScript,
            BtcTransaction.SigHash.ALL,
            false
        );

        int requiredSignatures = privateKeys.size() / 2 + 1;
        List<TransactionSignature> signatures = new ArrayList<>();

        for (int i = 0; i < requiredSignatures; i++) {
            BtcECKey keyToSign = privateKeys.get(i);
            BtcECKey.ECDSASignature signature = keyToSign.sign(sigHash);
            TransactionSignature txSignature = new TransactionSignature(
                signature,
                BtcTransaction.SigHash.ALL,
                false
            );
            signatures.add(txSignature);
        }

        ScriptBuilder scriptBuilder = new ScriptBuilder();
        Script scriptSig = scriptBuilder
                                .number(0)
                                .data(signatures.get(0).encodeToBitcoin())
                                .data(signatures.get(1).encodeToBitcoin())
                                .data(redeemScript.getProgram())
                                .build();

        spendTx.getInput(0).setScriptSig(scriptSig);
        scriptSig.correctlySpends(spendTx, 0, spendTx.getOutput(0).getScriptPubKey());

        // Uncomment to print the raw tx in console and broadcast https://blockstream.info/testnet/tx/push
        System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
    }
}

