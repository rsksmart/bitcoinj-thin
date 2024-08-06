
package co.rsk.bitcoinj.witness;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.script.P2shP2wshErpFederationNewRedeemScriptParser;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.witness.utils.BitcoinTestUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_0;

public class P2shP2wshNewRedeemScriptTest {

    @Test
    void spendFromP2shP2wshNewRedeemErpFedStandard() {

        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET); // testnet
        long activationDelay = 30;

        String[] seeds = new String[62];
        for (int i = 0; i < 62; i++ ) {
            int j = i + 1;
            seeds[i] = ("fed" + j);
        }
        List<BtcECKey> standardKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            seeds,
            true
        );

        String[] emergencySeeds = new String[4];
        for (int i = 0; i < 4; i++ ) {
            int j = i + 1;
            emergencySeeds[i] = ("erp" + j);
        }
        List<BtcECKey> emergencyKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            emergencySeeds,
            true
        );

        Script standardRedeemScript = new ScriptBuilder().createNewRedeemScript(standardKeys.size() / 2 + 1, standardKeys);
        Script emergencyRedeemScript = new ScriptBuilder().createNewRedeemScript(emergencyKeys.size() / 2 + 1, emergencyKeys);
        Script redeemScript = P2shP2wshErpFederationNewRedeemScriptParser.createErpP2shP2wshNewRedeemScript(standardRedeemScript, emergencyRedeemScript, activationDelay);

        Script p2shP2wshOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
        Address segwitAddress = Address.fromP2SHScript(
            networkParameters,
            p2shP2wshOutputScript
        );
        System.out.println(segwitAddress);

        Sha256Hash fundTxHash = Sha256Hash.wrap("3661d179d4dfce84279dafd66d44f1dd8a002b24994e9f58902934806da01e5c");
        int outputIndex = 0;
        //Address destinationAddress = Address.fromBase58(networkParameters, "12MXsCtte9onzqaHwN5VcnwZKGd7oDSsQq"); // mainnet
        Address destinationAddress = Address.fromBase58(networkParameters, "msgc5Gtz2L9MVhXPDrFRCYPa16QgoZ2EjP"); // testnet

        Coin value = Coin.valueOf(10_000);
        Coin fee = Coin.valueOf(2_000);

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTxHash, outputIndex, new Script(new byte[]{}));
        spendTx.addOutput(value.minus(fee), destinationAddress);
        spendTx.setVersion(2);

        byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
        Script scriptSig = new ScriptBuilder().number(OP_0).data(redeemScriptHash).build();
        Script segwitScriptSig = new ScriptBuilder().data(scriptSig.getProgram()).build();
        spendTx.getInput(0).setScriptSig(segwitScriptSig);

        // Create signatures
        int inputIndex = 0;
        Sha256Hash sigHash = spendTx.hashForWitnessSignature(
            inputIndex,
            redeemScript,
            value,
            BtcTransaction.SigHash.ALL,
            false
        );

        int thresholdSignaturesSize = standardKeys.size() / 2 + 1;
        List<TransactionSignature> thresholdSignatures = new ArrayList<>();

        for (int i = 0; i < thresholdSignaturesSize; i++) {
            BtcECKey keyToSign = standardKeys.get(i);
            BtcECKey.ECDSASignature signature = keyToSign.sign(sigHash);
            TransactionSignature txSignature = new TransactionSignature(
                signature,
                BtcTransaction.SigHash.ALL,
                false
            );
            thresholdSignatures.add(txSignature);
        }

        TransactionWitness txWitness = TransactionWitness.createWitnessErpStandardNewScript(redeemScript, thresholdSignatures, standardKeys.size());
        spendTx.setWitness(inputIndex, txWitness);


        // Uncomment to print the raw tx in console and broadcast https://blockstream.info/testnet/tx/push
        System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
    }

    @Test
    void spendFromP2shP2wshNewRedeemErpFedEmergency() {

        NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET); // testnet
        long activationDelay = 30;

        String[] seeds = new String[60];
        for (int i = 0; i < 60; i++ ) {
            int j = i + 1;
            seeds[i] = ("fed" + j);
        }
        List<BtcECKey> standardKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            seeds,
            true
        );

        String[] emergencySeeds = new String[4];
        for (int i = 0; i < 4; i++ ) {
            int j = i + 1;
            emergencySeeds[i] = ("erp" + j);
        }
        List<BtcECKey> emergencyKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
            emergencySeeds,
            true
        );

        Script standardRedeemScript = new ScriptBuilder().createNewRedeemScript(standardKeys.size() / 2 + 1, standardKeys);
        Script emergencyRedeemScript = new ScriptBuilder().createNewRedeemScript(emergencyKeys.size() / 2 + 1, emergencyKeys);
        Script redeemScript = P2shP2wshErpFederationNewRedeemScriptParser.createErpP2shP2wshNewRedeemScript(standardRedeemScript, emergencyRedeemScript, activationDelay);

        Script p2shP2wshOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
        Address segwitAddress = Address.fromP2SHScript(
            networkParameters,
            p2shP2wshOutputScript
        );
        System.out.println(segwitAddress);

        Sha256Hash fundTxHash = Sha256Hash.wrap("84538a84c4026e3887f3232fad35114603aa7a18530ae4b3083471f846d252c8");
        int outputIndex = 0;
        //Address destinationAddress = Address.fromBase58(networkParameters, "12MXsCtte9onzqaHwN5VcnwZKGd7oDSsQq"); // mainnet
        Address destinationAddress = Address.fromBase58(networkParameters, "msgc5Gtz2L9MVhXPDrFRCYPa16QgoZ2EjP"); // testnet

        Coin value = Coin.valueOf(10_000);
        Coin fee = Coin.valueOf(2_000);

        BtcTransaction spendTx = new BtcTransaction(networkParameters);
        spendTx.addInput(fundTxHash, outputIndex, new Script(new byte[]{}));
        spendTx.getInput(0).setSequenceNumber(activationDelay);
        spendTx.addOutput(value.minus(fee), destinationAddress);
        spendTx.setVersion(2);

        byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
        Script scriptSig = new ScriptBuilder().number(OP_0).data(redeemScriptHash).build();
        Script segwitScriptSig = new ScriptBuilder().data(scriptSig.getProgram()).build();
        spendTx.getInput(0).setScriptSig(segwitScriptSig);

        // Create signatures
        int inputIndex = 0;
        Sha256Hash sigHash = spendTx.hashForWitnessSignature(
            inputIndex,
            redeemScript,
            value,
            BtcTransaction.SigHash.ALL,
            false
        );

        int thresholdSignaturesSize = emergencyKeys.size() / 2 + 1;
        List<TransactionSignature> thresholdSignatures = new ArrayList<>();

        for (int i = 0; i < thresholdSignaturesSize; i++) {
            BtcECKey keyToSign = emergencyKeys.get(i);
            BtcECKey.ECDSASignature signature = keyToSign.sign(sigHash);
            TransactionSignature txSignature = new TransactionSignature(
                signature,
                BtcTransaction.SigHash.ALL,
                false
            );
            thresholdSignatures.add(txSignature);
        }

        TransactionWitness txWitness = TransactionWitness.createWitnessErpEmergencyNewScript(redeemScript, thresholdSignatures, emergencyKeys.size());
        spendTx.setWitness(inputIndex, txWitness);

        // Uncomment to print the raw tx in console and broadcast https://blockstream.info/testnet/tx/push
        System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
    }
}

