package co.rsk.bitcoinj.testing;

import co.rsk.bitcoinj.core.*;
import co.rsk.bitcoinj.script.ScriptPattern;
import com.google.common.collect.ImmutableList;
import co.rsk.bitcoinj.crypto.*;
import co.rsk.bitcoinj.script.Script;
import co.rsk.bitcoinj.script.ScriptBuilder;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PubKeyDerivationTest {

    @Test
    public void multiSigDerivation() {
        //Used this seeds for this test
        //https://github.com/thephez/copay-hd-multisig-recovery/blob/master/Copay%20HD%20Multisig%20Address%20Derivation%20and%20Manual%20Transaction%20Creation.md
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

        LegacyAddress multisig = LegacyAddress.fromScriptHash(params, script.getPubKeyHash());
        assertEquals("3KwuACPswasN8iHybeJ7Vfs5rUBwQ5Y8UY", multisig.toString());
    }

    @Test
    public void multiSigSegwitCompatible() {
        //Created using the same seeds from the previous test but selecting p2wsh in electrum
        String btcPublicKey0 = "Ypub6htVs57DayHPaLDkHkwDWRz35H2NDTbjc6SGxnizfNN9YFZ1Lv9pw4dkFQnd9HSnYetdPuwb6NM5PsdRB4VyccB5Fs4E9piGTt3VeyK12EP";
        String btcPublicKey1 = "Ypub6i9j287qA4XhsBtzNtnSXfvDZGmmg2H1Gx2uPWw23DUUeHJR7q6KqqHoFsg6qRtqF5zEpRuQVUWnc4s48s6nRELaPpBStbkW7EVEMDVy64f";
        String btcPublicKey2 = "Ypub6i7GyceHYjnYg8V1WvW1aNSrZRayFragUsLcbS1ajfUBabfJAg1X3vCYxfRyLE1jzqt4xFRhXWAFM1hs8gNXQj75p6pZFqKghDxKu9LRDdu";

        //Convert ypub to xpub https://github.com/trezor/connect/issues/98
        int bip32HeaderPub = 76067358;
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

        //The xpub was created with path m/44'/0'/0' so address for receiving are like x/0/0
        List<ChildNumber> childPathNumbers = HDUtils.parsePath("0/0");

        byte[] xpubDecoded = Base58.decodeChecked(btcPublicKey0);
        ByteBuffer buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);
        DeterministicKey keyChainSeed0 = DeterministicKey.deserialize(params, buf.array(), null);
        DeterministicKey key0 = keyChainSeed0;

        xpubDecoded = Base58.decodeChecked(btcPublicKey1);
        buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);
        DeterministicKey keyChainSeed1 = DeterministicKey.deserialize(params, buf.array(), null);
        DeterministicKey key1 = keyChainSeed1;

        xpubDecoded = Base58.decodeChecked(btcPublicKey2);
        buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);
        DeterministicKey keyChainSeed2 = DeterministicKey.deserialize(params, buf.array(), null);
        DeterministicKey key2 = keyChainSeed2;

        for (ChildNumber childPathNumber : childPathNumbers) {
            key0 = HDKeyDerivation.deriveChildKey(key0, childPathNumber);
            key1 = HDKeyDerivation.deriveChildKey(key1, childPathNumber);
            key2 = HDKeyDerivation.deriveChildKey(key2, childPathNumber);
        }

        List<BtcECKey> keys = ImmutableList.of((BtcECKey) key1, (BtcECKey) key0, (BtcECKey) key2);

        //https://github.com/libbitcoin/libbitcoin-system/wiki/P2WSH-Transactions
        Script redeemScript = ScriptBuilder.createRedeemScript(2, keys);
        Script p2sh_operations = new ScriptBuilder().smallNum(0).data(Sha256Hash.hash(redeemScript.getProgram())).build();
        Script script = ScriptBuilder.createP2SHOutputScript(p2sh_operations);

        byte[] scriptHash = ScriptPattern.extractHashFromPayToScriptHash(script);
        LegacyAddress multisig = LegacyAddress.fromScriptHash(params, scriptHash);

        assertEquals("38xizxq4uQStrjZX2hQp15rGLAv867rtqL", multisig.toBase58());
    }

    @Test
    public void multiSigSegwitNative() {
        //Created using the same seeds from the previous test but selecting native segwit in electrum
        // Root = m/48'/0'/0'/2'

        //Created using the same seeds from the previous test but selecting p2wsh in electrum
        String btcPublicKey0 = "Zpub755i2kSS8sSgH7cBigvFwfiFM6tjQpGhezn8ED5qkCb2iiDGUK6qehFhjhN1ThVxrEZaAFA9QyJJyHqb3y465RUZL1DasDbZ4cH7GPwLGS7";
        String btcPublicKey1 = "Zpub749P6YWShgBbnvm2qB6J87wSVLdCWetkyX1pj6cKwuWRoDoBnYCxgPuxii1i8fQTHCGRx7AL6Fpa4eRV5tQLUxAZcgDaV9ZqYEdF7sAEj6F";
        String btcPublicKey2 = "Zpub75uGiauiqgEr94xPJq2Z3eu3p6D92jxhJE7UB67Cf73xeKfeWX2vDGZ6ax8kPCHS4NeowzgKAECAz2PDe62vo59ytcMKzJBrgnMAgpPTQeG";

        //Convert ypub to xpub https://github.com/trezor/connect/issues/98
        int bip32HeaderPub = 76067358;
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

        //The xpub was created with path m/44'/0'/0' so address for receiving are like x/0/0
        List<ChildNumber> childPathNumbers = HDUtils.parsePath("0/0");

        byte[] xpubDecoded = Base58.decodeChecked(btcPublicKey0);
        ByteBuffer buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);
        DeterministicKey keyChainSeed0 = DeterministicKey.deserialize(params, buf.array(), null);
        DeterministicKey key0 = keyChainSeed0;

        xpubDecoded = Base58.decodeChecked(btcPublicKey1);
        buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);
        DeterministicKey keyChainSeed1 = DeterministicKey.deserialize(params, buf.array(), null);
        DeterministicKey key1 = keyChainSeed1;

        xpubDecoded = Base58.decodeChecked(btcPublicKey2);
        buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);
        DeterministicKey keyChainSeed2 = DeterministicKey.deserialize(params, buf.array(), null);
        DeterministicKey key2 = keyChainSeed2;

        for (ChildNumber childPathNumber : childPathNumbers) {
            key0 = HDKeyDerivation.deriveChildKey(key0, childPathNumber);
            key1 = HDKeyDerivation.deriveChildKey(key1, childPathNumber);
            key2 = HDKeyDerivation.deriveChildKey(key2, childPathNumber);
        }

        List<BtcECKey> keys = ImmutableList.of((BtcECKey) key1, (BtcECKey) key0, (BtcECKey) key2);

        //https://github.com/libbitcoin/libbitcoin-system/wiki/P2WSH-Transactions
        Script redeemScript = ScriptBuilder.createRedeemScript(2, keys);

        SegwitAddress address = SegwitAddress.fromHash(params, Sha256Hash.hash(redeemScript.getProgram()));

        assertEquals("bc1qaze7esngmhclnep7uclhyf7q0zn3qsrlquwlywgmwz3gt87cn8zq8xhs5a", address.toBech32());
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
        assertEquals("17rxURoF96VhmkcEGCj5LNQkmN9HVhWb7F", LegacyAddress.fromKey(params, child).toBase58());

    }

    @Test
    public void SimpleDerivationSegwitCompatible() {
        //xpriv from https://github.com/bitcoin/bips/blob/master/bip-0049.mediawiki#test-vectors
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        // Account 0, root = m/49'/1'/0'
        String xpriv = "tprv8gRrNu65W2Msef2BdBSUgFdRTGzC8EwVXnV7UGS3faeXtuMVtGfEdidVeGbThs4ELEoayCAzZQ4uUji9DUiAs7erdVskqju7hrBcDvDsdbY";
        DeterministicKey chainSeed =  DeterministicKey.deserializeB58(null, xpriv, params);
        // Account 0, first receiving private key = m/49'/1'/0'/0/0 so we derivate by 0/0
        List<ChildNumber>  childPathNumbers = HDUtils.parsePath("0/0");
        DeterministicKey child = chainSeed;
        for(ChildNumber childPathNumber : childPathNumbers) {
            child = HDKeyDerivation.deriveChildKey(child, childPathNumber);
        }

        //Create script for p2wpkh
        Script redeemScript = new ScriptBuilder().smallNum(0).data(child.getPubKeyHash()).build();
        Script script = ScriptBuilder.createP2SHOutputScript(redeemScript);
        byte[] scriptHash = ScriptPattern.extractHashFromPayToScriptHash(script);
        LegacyAddress p2wpkh = LegacyAddress.fromScriptHash(params, scriptHash);

        assertEquals("2Mww8dCYPUpKHofjgcXcBCEGmniw9CoaiD2", p2wpkh.toBase58());

    }

    @Test
    public void SimpleDerivationSegwitNative() {
        //Seed: polar beauty wave shock alley weird front unit blanket stamp paper token
        // Root = m/84'/0'/0'
        String xpub = "zpub6rjC8zoyfbmeLyw4aWxMduEHxHWXywhHJVy3P8Se3tEJscm4Ux8Vru8q8v93SgfTRJo8kVUBnMLizkkhhX8DMyxAitiepq7KpzBDoJfxyAq";
        //Convert ypub to xpub https://github.com/trezor/connect/issues/98
        int bip32HeaderPub = 76067358;
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        byte[] xpubDecoded = Base58.decodeChecked(xpub);
        ByteBuffer buf = ByteBuffer.wrap(xpubDecoded);
        buf.putInt(bip32HeaderPub);

        DeterministicKey chainSeed =  DeterministicKey.deserialize(params, buf.array(), null);
        // Account 0, first receiving private key = m/84'/0'/0'/0/0 so we derivate by 0/0
        List<ChildNumber>  childPathNumbers = HDUtils.parsePath("0/0");
        DeterministicKey child = chainSeed;
        for(ChildNumber childPathNumber : childPathNumbers) {
            child = HDKeyDerivation.deriveChildKey(child, childPathNumber);
        }

        SegwitAddress address = SegwitAddress.fromHash(params, child.getPubKeyHash());

        assertEquals("bc1qysshr2vz5jkksl5r2rqxlsz8x28zum3p7yy8yn", address.toBech32());
    }

    @Test
    public void DerivationErrorHardenedFromPublic() {
        try {
            NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
            //This is obtained from ledger without a derivatio path
            String btcPublicKey0 = "xpub67tVq9TLPPocEYfAtoumhGdJner1eG1PSTmf2aqJ8UuDCtXU2gYUyKWCTDSJV1ARRXBcPV3NYkahUnCWX6s4To8FUbH1LDkWHYMRqvnArHR";
            DeterministicKey chainSeed = DeterministicKey.deserializeB58(null, btcPublicKey0, params);
            //original derivatio path m/44'
            List<ChildNumber> childPathNumbers = HDUtils.parsePath("0H/0H/0/0");
            DeterministicKey child = chainSeed;
            for (ChildNumber childPathNumber : childPathNumbers) {
                child = HDKeyDerivation.deriveChildKey(child, childPathNumber);
            }
           assertTrue(false);
        } catch( IllegalArgumentException e){
            assertTrue(e.getMessage().contains("Can't use private derivation with public keys only"));
        }

    }

    @Test
    public void MoreThanBip32Derivations() {
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        String btcPublicKey0 = "xpub67tVq9TLPPocEYfAtoumhGdJner1eG1PSTmf2aqJ8UuDCtXU2gYUyKWCTDSJV1ARRXBcPV3NYkahUnCWX6s4To8FUbH1LDkWHYMRqvnArHR";
        DeterministicKey chainSeed = DeterministicKey.deserializeB58(null, btcPublicKey0, params);
        //original derivatio path m/44'
        List<ChildNumber> childPathNumbers = HDUtils.parsePath("0/0/0/0/0/0/0");
        DeterministicKey child = chainSeed;
        for(ChildNumber childPathNumber : childPathNumbers) {
            child = HDKeyDerivation.deriveChildKey(child, childPathNumber);
        }
        assertTrue(true);
    }


}
