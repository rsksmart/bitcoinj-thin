package co.rsk.bitcoinj.script;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import co.rsk.bitcoinj.core.Address;
import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.BtcECKey.ECDSASignature;
import co.rsk.bitcoinj.core.BtcTransaction;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.bitcoinj.core.Sha256Hash;
import co.rsk.bitcoinj.core.VerificationException;
import co.rsk.bitcoinj.crypto.TransactionSignature;
import co.rsk.bitcoinj.params.MainNetParams;
import co.rsk.bitcoinj.script.RedeemScriptParser.MultiSigType;
import java.util.List;
import org.junit.Test;

public class FlyoverRedeemScriptParserTest {

    private final List<BtcECKey> defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private final List<BtcECKey> emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();

    @Test
    public void getMultiSigType_whenIsStandardRedeemScript_shouldReturnFlyoverMultiSigType() {
        // Arrange
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(standardRedeemScript.getChunks());

        // Act
        MultiSigType actualMultiSigType = flyoverRedeemScriptParser.getMultiSigType();

        // Assert
        assertEquals(MultiSigType.FLYOVER, actualMultiSigType);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsStandardRedeemScript_shouldReturnMValue() {
        // Arrange
        final int EXPECTED_M = 5;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsErpRedeemScript_shouldReturnMValue() {
        // Arrange
        final int EXPECTED_M = 5;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, 500L
        );
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), erpRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsP2shErpRedeemScript_shouldReturnMValue() {
        // Arrange
        final int EXPECTED_M = 5;
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, 500L);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), p2shErpRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void findKeyInRedeem_whenKeyIsInRedeemScript_shouldReturnKeyIndexPosition() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        int actualKeyIndex = flyoverRedeemScriptParser.findKeyInRedeem(defaultRedeemScriptKeys.get(0));

        // Assert
        assertEquals(0, actualKeyIndex);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyIsNotInRedeemScript_shouldThrowIllegalStateException() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act / Assert
        flyoverRedeemScriptParser.findKeyInRedeem(emergencyRedeemScriptKeys.get(0));
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsStandardRedeemScript_shouldReturnPubKeys() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        List<BtcECKey> actualPubKeys = flyoverRedeemScriptParser.getPubKeys();

        // Assert
        defaultRedeemScriptKeys.forEach(
            expectedPubKey -> assertPublicKey(expectedPubKey, actualPubKeys)
        );
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsErpRedeemScript_shouldReturnPubKeys() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, 500L);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), erpRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        List<BtcECKey> actualPubKeys = flyoverRedeemScriptParser.getPubKeys();

        // Assert
        defaultRedeemScriptKeys.forEach(
            expectedPubKey -> assertPublicKey(expectedPubKey, actualPubKeys)
        );
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsP2shErpRedeemScript_shouldReturnPubKeys() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, 500L);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), p2shErpRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        List<BtcECKey> actualPubKeys = flyoverRedeemScriptParser.getPubKeys();

        // Assert
        defaultRedeemScriptKeys.forEach(
            expectedPubKey -> assertPublicKey(expectedPubKey, actualPubKeys)
        );
    }

    private void assertPublicKey(BtcECKey expectedPubKey, List<BtcECKey> actualPubKeys) {
        int btcECKeyIndex = defaultRedeemScriptKeys.indexOf(expectedPubKey);
        BtcECKey btcECKey = actualPubKeys.get(btcECKeyIndex);
        byte[] actualPubKey = btcECKey.getPubKey();
        assertThat(actualPubKey, equalTo(expectedPubKey.getPubKey()));
    }

    @Test
    public void findSigInRedeem_whenSignatureIsInRedeemScript_shouldReturnSignatureIndexPosition() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        final NetworkParameters mainNetParams = MainNetParams.get();
        BtcECKey privateKey = defaultRedeemScriptKeys.get(0);

        BtcTransaction fundTx = new BtcTransaction(mainNetParams);
        Address userAddress = privateKey.toAddress(mainNetParams);
        fundTx.addOutput(Coin.FIFTY_COINS, userAddress);

        BtcTransaction spendTx = new BtcTransaction(mainNetParams);
        spendTx.addInput(fundTx.getOutput(0));

        Sha256Hash hashForSignatureHash = spendTx.hashForSignature(
            0,
            standardRedeemScript,
            BtcTransaction.SigHash.ALL,
            false);

        ECDSASignature signature = privateKey.sign(hashForSignatureHash);
        TransactionSignature transactionSignature = new TransactionSignature(signature, BtcTransaction.SigHash.ALL, false);

        // Act
        int actualSignatureIndex = flyoverRedeemScriptParser.findSigInRedeem(transactionSignature.encodeToBitcoin(), hashForSignatureHash);

        // Assert
        assertEquals(0, actualSignatureIndex);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsStandardRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());
        List<ScriptChunk> expectedRedeemScriptChunks = standardRedeemScript.getChunks();

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsErpRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        Script erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, 500L);
        List<ScriptChunk> expectedRedeemScriptChunks = ErpFederationRedeemScriptParser.extractStandardRedeemScriptChunks(erpRedeemScript.getChunks());

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), erpRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsP2shErpRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        Script p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, 500L);
        List<ScriptChunk> expectedRedeemScriptChunks = P2shErpFederationRedeemScriptParser.extractStandardRedeemScriptChunks(p2shErpRedeemScript.getChunks());

        Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), p2shErpRedeemScript);
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test(expected = VerificationException.class)
    public void extractInternalRedeemScriptChunks_whenRedeemScriptChunksSizeIsZero_shouldThrowVerificationException(){
        // Arrange
        Script redeemScript = new Script(new byte[0]);
        List<ScriptChunk> redeemScriptChunks = redeemScript.getChunks();

        // Act / Assert
        // Executed the private method extractInternalRedeemScriptChunks through the constructor
        new FlyoverRedeemScriptParser(redeemScriptChunks);
    }

    @Test(expected = VerificationException.class)
    public void extractInternalRedeemScriptChunks_whenRedeemScriptChunksSizeIsTwo_shouldThrowVerificationException(){
        // Arrange
        Script redeemScript = new Script(new byte[2]);
        List<ScriptChunk> redeemScriptChunks = redeemScript.getChunks();

        // Act / Assert
        // Executed the private method extractInternalRedeemScriptChunks through the constructor
        new FlyoverRedeemScriptParser(redeemScriptChunks);
    }
}
