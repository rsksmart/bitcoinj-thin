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
import org.junit.Before;
import org.junit.Test;

public class FlyoverRedeemScriptParserTest {

    private final List<BtcECKey> defaultRedeemScriptKeys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private final List<BtcECKey> emergencyRedeemScriptKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    private final Sha256Hash derivationArgumentsHash = Sha256Hash.of(new byte[]{1});
    private Script standardRedeemScript;
    private Script flyoverStandardRedeemScript;
    private Script erpRedeemScript;
    private Script flyoverErpRedeemScript;
    private Script p2shErpRedeemScript;
    private Script flyoverP2shErpRedeemScript;

    @Before
    public void setUp() {
        final long CSV_VALUE = 52_560L;
        standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(defaultRedeemScriptKeys);
        flyoverStandardRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), standardRedeemScript);

        erpRedeemScript = RedeemScriptUtils.createErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, CSV_VALUE);
        flyoverErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), erpRedeemScript);

        p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(defaultRedeemScriptKeys, emergencyRedeemScriptKeys, CSV_VALUE);
        flyoverP2shErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(derivationArgumentsHash.getBytes(), p2shErpRedeemScript);
    }

    @Test
    public void getMultiSigType_whenIsStandardRedeemScript_shouldReturnFlyoverMultiSigType() {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

        // Act
        MultiSigType actualMultiSigType = flyoverRedeemScriptParser.getMultiSigType();

        // Assert
        assertEquals(MultiSigType.FLYOVER, actualMultiSigType);
    }

    @Test
    public void getMultiSigType_whenIsErpRedeemScript_shouldReturnFlyoverMultiSigType() {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverErpRedeemScript.getChunks());

        // Act
        MultiSigType actualMultiSigType = flyoverRedeemScriptParser.getMultiSigType();

        // Assert
        assertEquals(MultiSigType.FLYOVER, actualMultiSigType);
    }

    @Test
    public void getMultiSigType_whenIsP2shErpRedeemScript_shouldReturnFlyoverMultiSigType() {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverP2shErpRedeemScript.getChunks());

        // Act
        MultiSigType actualMultiSigType = flyoverRedeemScriptParser.getMultiSigType();

        // Assert
        assertEquals(MultiSigType.FLYOVER, actualMultiSigType);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsStandardRedeemScript_shouldReturnMValue() {
        // Arrange
        final int EXPECTED_M = 5;
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsErpRedeemScript_shouldReturnMValue() {
        // Arrange
        final int EXPECTED_M = 5;
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverErpRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsP2shErpRedeemScript_shouldReturnMValue() {
        // Arrange
        final int EXPECTED_M = 5;
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverP2shErpRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void findKeyInRedeem_whenKeyIsInRedeemScript_shouldReturnKeyIndexPosition() {
        // Arrange
        final int EXPECTED_KEY_INDEX = 5;
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

        // Act
        int actualKeyIndex = flyoverRedeemScriptParser.findKeyInRedeem(defaultRedeemScriptKeys.get(EXPECTED_KEY_INDEX));

        // Assert
        assertEquals(EXPECTED_KEY_INDEX, actualKeyIndex);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyIsNotInRedeemScript_shouldThrowIllegalStateException() {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

        // Act / Assert
        flyoverRedeemScriptParser.findKeyInRedeem(emergencyRedeemScriptKeys.get(0));
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsStandardRedeemScript_shouldReturnPubKeys() {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

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
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverErpRedeemScript.getChunks());

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
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverP2shErpRedeemScript.getChunks());

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
        final int EXPECTED_SIGNATURE_INDEX = 0;
        final NetworkParameters mainNetParams = MainNetParams.get();
        BtcECKey privateKey = defaultRedeemScriptKeys.get(EXPECTED_SIGNATURE_INDEX);

        // Creating a transaction
        BtcTransaction fundTx = new BtcTransaction(mainNetParams);
        Address userAddress = privateKey.toAddress(mainNetParams);
        fundTx.addOutput(Coin.FIFTY_COINS, userAddress);

        BtcTransaction spendTx = new BtcTransaction(mainNetParams);
        spendTx.addInput(fundTx.getOutput(EXPECTED_SIGNATURE_INDEX));

        // Getting the transaction hash for the signature
        Sha256Hash hashForSignatureHash = spendTx.hashForSignature(
            EXPECTED_SIGNATURE_INDEX,
            standardRedeemScript,
            BtcTransaction.SigHash.ALL,
            false
        );

        // Signing the transaction hash
        ECDSASignature signature = privateKey.sign(hashForSignatureHash);
        TransactionSignature transactionSignature = new TransactionSignature(signature, BtcTransaction.SigHash.ALL, false);

        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

        // Act
        int actualSignatureIndex = flyoverRedeemScriptParser.findSigInRedeem(transactionSignature.encodeToBitcoin(), hashForSignatureHash);

        // Assert
        assertEquals(EXPECTED_SIGNATURE_INDEX, actualSignatureIndex);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsStandardRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        List<ScriptChunk> expectedRedeemScriptChunks = standardRedeemScript.getChunks();
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverStandardRedeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsErpRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        List<ScriptChunk> expectedRedeemScriptChunks = ErpFederationRedeemScriptParser.extractStandardRedeemScriptChunks(erpRedeemScript.getChunks());
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverErpRedeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsP2shErpRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        List<ScriptChunk> expectedRedeemScriptChunks = P2shErpFederationRedeemScriptParser.extractStandardRedeemScriptChunks(p2shErpRedeemScript.getChunks());
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverP2shErpRedeemScript.getChunks());

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
