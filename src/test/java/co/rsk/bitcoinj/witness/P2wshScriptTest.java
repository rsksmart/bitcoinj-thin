package co.rsk.bitcoinj.witness;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.script.P2shP2wshErpFederationRedeemScriptParser;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import co.rsk.bitcoinj.witness.utils.BitcoinTestUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static co.rsk.bitcoinj.script.ScriptOpCodes.OP_0;

public class P2wshScriptTest {
        @Test
        public void spendFromP2wshErpFedStandard() {
            NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
            long activationDelay = 30;

            List<BtcECKey> standardKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
                new String[]{
                    "fed1", "fed2", "fed3", "fed4", "fed5", "fed6", "fed7", "fed8", "fed9", "fed10",
                    "fed11", "fed12", "fed13", "fed14", "fed15", "fed16", "fed17", "fed18", "fed19", "fed20"
                },
                true
            );

            List<BtcECKey> emergencyKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
                new String[]{
                    "erp1", "erp2", "erp3", "erp4","erp5", "erp6", "erp7", "erp8","erp9", "erp10",
                    "erp11", "erp12", "erp13", "erp14","erp15", "erp16", "erp17", "erp18","erp19", "erp20"
                },
                true
            );

            Script standardRedeem = new ScriptBuilder().createRedeemScript(standardKeys.size()/2+1, standardKeys);
            Script emergencyRedeem = new ScriptBuilder().createRedeemScript(emergencyKeys.size()/2+1, emergencyKeys);
            Script redeemScript = P2shP2wshErpFederationRedeemScriptParser.createP2shP2wshErpRedeemScript(standardRedeem, emergencyRedeem, activationDelay);

            byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
            SegwitAddress segwitNativeAddress = SegwitAddress.fromHash(networkParameters, redeemScriptHash);
            System.out.println(segwitNativeAddress);

            Sha256Hash fundTxHash = Sha256Hash.wrap("ffb564302f3b3ad8a8ba7960645074bf7bbbc3befcd68c37693e5626705bb349");
            Coin value = Coin.valueOf(10_000);
            int outputIndex = 0;
            Coin fee = Coin.valueOf(5_000);
            LegacyAddress receiver = LegacyAddress.fromBase58(networkParameters,"msgc5Gtz2L9MVhXPDrFRCYPa16QgoZ2EjP");

            BtcTransaction spendTx = new BtcTransaction(networkParameters);
            spendTx.addInput(fundTxHash, outputIndex, new Script(new byte[]{}));
            spendTx.addOutput(value.minus(fee), receiver);
            spendTx.setVersion(2);

            // Create signatures
            int inputIndex = 0;
            Sha256Hash sigHash = spendTx.hashForWitnessSignature(
                inputIndex,
                redeemScript,
                value,
                BtcTransaction.SigHash.ALL,
                false
            );

            //byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
            // Script scriptSig = new ScriptBuilder().number(OP_0).data(redeemScriptHash).build();
            //Script segwitScriptSig = new ScriptBuilder().data(scriptSig.getProgram()).build();
            // spendTx.getInput(0).setScriptSig(segwitScriptSig);

            int requiredSignatures = standardKeys.size() / 2 + 1;
            List<TransactionSignature> signatures = new ArrayList<>();

            for (int i = 0; i < requiredSignatures; i++) {
                BtcECKey keyToSign = standardKeys.get(i);
                BtcECKey.ECDSASignature signature = keyToSign.sign(sigHash);
                TransactionSignature txSignature = new TransactionSignature(
                    signature,
                    BtcTransaction.SigHash.ALL,
                    false
                );
                signatures.add(txSignature);
            }

            TransactionWitness txWitness = TransactionWitness.createWitnessErpStandardScript(redeemScript, signatures);
            spendTx.setWitness(inputIndex, txWitness);

            // Uncomment to print the raw tx in console and broadcast https://blockstream.info/testnet/tx/push
            System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
        }

        @Test
        public void spendFromP2shP2wshErpFedEmergency() {
            NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
            long activationDelay = 30;

            List<BtcECKey> standardKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
                new String[]{
                    "fed1", "fed2", "fed3", "fed4", "fed5", "fed6", "fed7", "fed8", "fed9", "fed10",
                    "fed11", "fed12", "fed13", "fed14", "fed15", "fed16", "fed17", "fed18", "fed19", "fed20"
                },
                true
            );

            List<BtcECKey> emergencyKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
                new String[]{
                    "erp1", "erp2", "erp3", "erp4","erp5", "erp6", "erp7", "erp8","erp9", "erp10",
                    "erp11", "erp12", "erp13", "erp14","erp15", "erp16", "erp17", "erp18","erp19", "erp20"
                },
                true
            );

            Script standardRedeem = new ScriptBuilder().createRedeemScript(standardKeys.size()/2+1, standardKeys);
            Script emergencyRedeem = new ScriptBuilder().createRedeemScript(emergencyKeys.size()/2+1, emergencyKeys);
            Script redeemScript = P2shP2wshErpFederationRedeemScriptParser.createP2shP2wshErpRedeemScript(standardRedeem, emergencyRedeem, activationDelay);

            Script p2shP2wshOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
            LegacyAddress segwitAddress = LegacyAddress.fromP2SHScript(
                networkParameters,
                p2shP2wshOutputScript
            );
            System.out.println(segwitAddress);

            Sha256Hash fundTxHash = Sha256Hash.wrap("");
            Coin value = Coin.valueOf(10_000);
            int outputIndex = 0;
            Coin fee = Coin.valueOf(1_000);
            LegacyAddress receiver = LegacyAddress.fromBase58(networkParameters,"msgc5Gtz2L9MVhXPDrFRCYPa16QgoZ2EjP");

            BtcTransaction spendTx = new BtcTransaction(networkParameters);
            spendTx.addInput(fundTxHash, outputIndex, new Script(new byte[]{}));
            spendTx.getInput(0).setSequenceNumber(activationDelay);
            spendTx.addOutput(value.minus(fee), receiver);
            spendTx.setVersion(2);

            // Create signatures
            int inputIndex = 0;
            Sha256Hash sigHash = spendTx.hashForWitnessSignature(
                inputIndex,
                redeemScript,
                value,
                BtcTransaction.SigHash.ALL,
                false
            );

            byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
            Script scriptSig = new ScriptBuilder().number(OP_0).data(redeemScriptHash).build();
            Script segwitScriptSig = new ScriptBuilder().data(scriptSig.getProgram()).build();
            spendTx.getInput(0).setScriptSig(segwitScriptSig);

            int requiredSignatures = emergencyKeys.size() / 2 + 1;
            List<TransactionSignature> signatures = new ArrayList<>();

            for (int i = 0; i < requiredSignatures; i++) {
                BtcECKey keyToSign = emergencyKeys.get(i);
                BtcECKey.ECDSASignature signature = keyToSign.sign(sigHash);
                TransactionSignature txSignature = new TransactionSignature(
                    signature,
                    BtcTransaction.SigHash.ALL,
                    false
                );
                signatures.add(txSignature);
            }

            TransactionWitness txWitness = TransactionWitness.createWitnessErpEmergencyScript(redeemScript, signatures);
            spendTx.setWitness(inputIndex, txWitness);

            // Uncomment to print the raw tx in console and broadcast https://blockstream.info/testnet/tx/push
            System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
        }

        @Test
        public void spendFromP2shP2wshErpFedStandardWithFlyover() throws Exception {
            NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
            long activationDelay = 30;

            List<BtcECKey> standardKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
                new String[]{
                    "fed1", "fed2", "fed3", "fed4", "fed5", "fed6", "fed7", "fed8", "fed9", "fed10",
                    "fed11", "fed12", "fed13", "fed14", "fed15", "fed16", "fed17", "fed18", "fed19", "fed20"
                },
                true
            );

            List<BtcECKey> emergencyKeys = BitcoinTestUtils.getBtcEcKeysFromSeeds(
                new String[]{
                    "erp1", "erp2", "erp3", "erp4", "erp5", "erp6", "erp7", "erp8", "erp9", "erp10",
                    "erp11", "erp12", "erp13", "erp14", "erp15", "erp16", "erp17", "erp18", "erp19", "erp20"
                },
                true
            );

            Sha256Hash flyoverDerivationPath = Sha256Hash.wrap("0100000000000000000000000000000000000000000000000000000000000000");

            Script standardRedeem = new ScriptBuilder().createRedeemScript(standardKeys.size()/2+1, standardKeys);
            Script emergencyRedeem = new ScriptBuilder().createRedeemScript(emergencyKeys.size()/2+1, emergencyKeys);
            Script redeemScript = P2shP2wshErpFederationRedeemScriptParser.createP2shP2wshErpRedeemScriptWithFlyover(standardRedeem, emergencyRedeem, flyoverDerivationPath, activationDelay);

            Script p2shP2wshOutputScript = ScriptBuilder.createP2SHP2WSHOutputScript(redeemScript);
            LegacyAddress segwitAddress = LegacyAddress.fromP2SHScript(
                networkParameters,
                p2shP2wshOutputScript
            );
            System.out.println(segwitAddress);

            Sha256Hash fundTxHash = Sha256Hash.wrap("ca9c3ff2685d65a0adf571a30d77c6d12857af9edfbf8f3a19ca4c1fc1eb47f5");
            Coin value = Coin.valueOf(10_000);
            int outputIndex = 0;
            Coin fee = Coin.valueOf(1_000);
            LegacyAddress receiver = LegacyAddress.fromBase58(networkParameters,"msgc5Gtz2L9MVhXPDrFRCYPa16QgoZ2EjP");

            BtcTransaction spendTx = new BtcTransaction(networkParameters);
            spendTx.addInput(fundTxHash, outputIndex, new Script(new byte[]{}));
            spendTx.addOutput(value.minus(fee), receiver);
            spendTx.setVersion(2);

            // Create signatures
            int inputIndex = 0;
            Sha256Hash sigHash = spendTx.hashForWitnessSignature(
                inputIndex,
                redeemScript,
                value,
                BtcTransaction.SigHash.ALL,
                false
            );

            byte[] redeemScriptHash = Sha256Hash.hash(redeemScript.getProgram());
            Script scriptSig = new ScriptBuilder().number(OP_0).data(redeemScriptHash).build();
            Script segwitScriptSig = new ScriptBuilder().data(scriptSig.getProgram()).build();
            spendTx.getInput(0).setScriptSig(segwitScriptSig);

            int requiredSignatures = standardKeys.size() / 2 + 1;
            List<TransactionSignature> signatures = new ArrayList<>();

            for (int i = 0; i < requiredSignatures; i++) {
                BtcECKey keyToSign = standardKeys.get(i);
                BtcECKey.ECDSASignature signature = keyToSign.sign(sigHash);
                TransactionSignature txSignature = new TransactionSignature(
                    signature,
                    BtcTransaction.SigHash.ALL,
                    false
                );
                signatures.add(txSignature);
            }

            TransactionWitness txWitness = TransactionWitness.createWitnessErpStandardScript(redeemScript, signatures);
            spendTx.setWitness(inputIndex, txWitness);

            // Uncomment to print the raw tx in console and broadcast https://blockstream.info/testnet/tx/push
            System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
        }

    }
