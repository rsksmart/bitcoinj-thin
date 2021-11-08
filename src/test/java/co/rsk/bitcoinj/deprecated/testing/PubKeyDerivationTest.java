package co.rsk.bitcoinj.deprecated.testing;

import co.rsk.bitcoinj.deprecated.core.NetworkParameters;
import co.rsk.bitcoinj.deprecated.crypto.ChildNumber;
import co.rsk.bitcoinj.deprecated.crypto.DeterministicKey;
import co.rsk.bitcoinj.deprecated.crypto.HDKeyDerivation;
import co.rsk.bitcoinj.deprecated.crypto.HDUtils;
import com.google.common.collect.ImmutableList;
import co.rsk.bitcoinj.deprecated.core.BtcECKey;
import co.rsk.bitcoinj.deprecated.script.Script;
import co.rsk.bitcoinj.deprecated.script.ScriptBuilder;
import co.rsk.bitcoinj.deprecated.core.Address;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PubKeyDerivationTest {

    @Test
    public void multiSigDerivation() {
        String btcPublicKey0 = "xpub6DkSUtaeFFTubXQEtMDcQJMijLCFnDw8GxhMD6iruhcUA4cgM6fhMFSMQnuS3X7jyw9iBrJz2jzX4p17PiTq3R9g2MgWZnFaeVER2SZkc9A";
        String btcPublicKey1 = "xpub6EHA5RQJUwWqynegfs9vYYEEHE4KE3oNV24rDDoDSGqdpD4h9ofP2nzUABddd8e6LpoRt8Bq5PAhFuVAKgGAsgdxG94GujU1GC5bKXpWS4S";
        String btcPublicKey2 = "xpub6Du9RssVNLnrG3k1ws24h8pFH9jecyqCoD8hLViQd3M4zvv95DsaSy8o7GrHnoLKHsTai153LkMr7qMSggHWH5uqhX94MyMWS3X3HhpboAB";

        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        //The xpub was created with path m/44'/0'/0' so address for receiving are like x/0/0
        List<ChildNumber> childPathNumbers = HDUtils.parsePath("0/0");

        //DeterministicKey keyChainSeed = HDKeyDerivation.createMasterPubKeyFromBytes(btcPublicKeyBytes, chainCode);
        DeterministicKey keyChainSeed0 = DeterministicKey.deserializeB58(null, btcPublicKey0, params);
        DeterministicKey key0 = keyChainSeed0;
        DeterministicKey keyChainSeed1 = DeterministicKey.deserializeB58(null, btcPublicKey1, params);
        DeterministicKey key1 = keyChainSeed1;
        DeterministicKey keyChainSeed2 = DeterministicKey.deserializeB58(null, btcPublicKey2, params);
        DeterministicKey key2 = keyChainSeed2;
        for (ChildNumber childPathNumber : childPathNumbers) {
            key0 = HDKeyDerivation.deriveChildKey(key0, childPathNumber);
            key1 = HDKeyDerivation.deriveChildKey(key1, childPathNumber);
            key2 = HDKeyDerivation.deriveChildKey(key2, childPathNumber);
        }

        List<BtcECKey> keys = ImmutableList.of((BtcECKey) key1, (BtcECKey) key0, (BtcECKey) key2);

        Script redeemScript = ScriptBuilder.createRedeemScript(2, keys);
        Script script = ScriptBuilder.createP2SHOutputScript(redeemScript);

        Address multisig = Address.fromP2SHScript(params, script);
        assertEquals("3KwuACPswasN8iHybeJ7Vfs5rUBwQ5Y8UY", multisig.toString());
    }

    @Test
    public void SimpleDerivation() {
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        String xpriv = "xprv9s21ZrQH143K25QhxbucbDDuQ4naNntJRi4KUfWT7xo4EKsHt2QJDu7KXp1A3u7Bi1j8ph3EGsZ9Xvz9dGuVrtHHs7pXeTzjuxBrCmmhgC6";
        DeterministicKey chainSeed =  DeterministicKey.deserializeB58(null, xpriv, params);
        List<ChildNumber>  childPathNumbers = HDUtils.parsePath("M/44H/0H/0H/0/0");
        DeterministicKey child = chainSeed;
        for(ChildNumber childPathNumber : childPathNumbers) {
            child = HDKeyDerivation.deriveChildKey(child, childPathNumber);
        }
        assertEquals("17rxURoF96VhmkcEGCj5LNQkmN9HVhWb7F", child.toAddress(params).toBase58());

    }


}
