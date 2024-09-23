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
import java.math.BigInteger;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class FlyoverRedeemScriptParserTest {

    private final List<BtcECKey> keys = RedeemScriptUtils.getDefaultRedeemScriptKeys();
    private final List<BtcECKey> emergencyKeys = RedeemScriptUtils.getEmergencyRedeemScriptKeys();
    private final Sha256Hash flyoverDerivationHash = Sha256Hash.of(new byte[]{1});
    private Script standardRedeemScript;
    private Script flyoverStandardRedeemScript;
    private Script erpRedeemScript;
    private Script flyoverErpRedeemScript;
    private Script p2shErpRedeemScript;
    private Script flyoverP2shErpRedeemScript;

    @Before
    public void setUp() {
        final long CSV_VALUE = 52_560L;
        standardRedeemScript = RedeemScriptUtils.createStandardRedeemScript(keys);
        flyoverStandardRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            flyoverDerivationHash.getBytes(), standardRedeemScript);

        erpRedeemScript = RedeemScriptUtils.createNonStandardErpRedeemScript(keys, emergencyKeys, CSV_VALUE);
        flyoverErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(flyoverDerivationHash.getBytes(), erpRedeemScript);

        p2shErpRedeemScript = RedeemScriptUtils.createP2shErpRedeemScript(keys, emergencyKeys, CSV_VALUE);
        flyoverP2shErpRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            flyoverDerivationHash.getBytes(), p2shErpRedeemScript);
    }

    @Test
    public void getMultiSigType_whenIsStandardRedeemScript_shouldReturnFlyoverMultiSigType() {
        assertIsFlyoverMultiSigType(flyoverStandardRedeemScript);
    }

    @Test
    public void getMultiSigType_whenIsErpRedeemScript_shouldReturnFlyoverMultiSigType() {
        assertIsFlyoverMultiSigType(flyoverErpRedeemScript);
    }

    @Test
    public void getMultiSigType_whenIsP2shErpRedeemScript_shouldReturnFlyoverMultiSigType() {
        assertIsFlyoverMultiSigType(flyoverP2shErpRedeemScript);
    }

    private void assertIsFlyoverMultiSigType(Script flyoverRedeemScript) {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        MultiSigType actualMultiSigType = flyoverRedeemScriptParser.getMultiSigType();

        // Assert
        assertEquals(MultiSigType.FLYOVER, actualMultiSigType);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsStandardRedeemScript_shouldReturnMValue() {
        assertGetMValue(flyoverStandardRedeemScript);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsErpRedeemScript_shouldReturnMValue() {
        assertGetMValue(flyoverErpRedeemScript);
    }

    @Test
    public void getM_whenFlyoverRedeemScriptContainsP2shErpRedeemScript_shouldReturnMValue() {
        assertGetMValue(flyoverP2shErpRedeemScript);
    }

    private void assertGetMValue(Script flyoverRedeemScript) {
        // Arrange
        final int EXPECTED_M = 5;
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        int actualM = flyoverRedeemScriptParser.getM();

        // Assert
        assertEquals(EXPECTED_M, actualM);
    }

    @Test
    public void findKeyInRedeem_whenKeyIsInStandardRedeemScript_shouldReturnKeyIndexPosition() {
        assertKeyInRedeem(flyoverStandardRedeemScript);
    }

    @Test
    public void findKeyInRedeem_whenKeyIsInErpRedeemScript_shouldReturnKeyIndexPosition() {
        assertKeyInRedeem(flyoverErpRedeemScript);
    }

    @Test
    public void findKeyInRedeem_whenKeyIsInP2shErpRedeemScript_shouldReturnKeyIndexPosition() {
        assertKeyInRedeem(flyoverP2shErpRedeemScript);
    }

    private void assertKeyInRedeem(Script flyoverRedeemScript) {
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());
        for (int expectedIndex = 0; expectedIndex < keys.size(); expectedIndex++) {
            BtcECKey key = keys.get(expectedIndex);
            int actualKeyIndex = flyoverRedeemScriptParser.findKeyInRedeem(key);
            assertEquals(expectedIndex, actualKeyIndex);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyIsNotInStandardRedeemScript_shouldThrowIllegalStateException() {
        assertThrowsIllegalStateException(flyoverStandardRedeemScript);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyIsNotInNonStandardErpRedeemScript_shouldThrowIllegalStateException() {
        assertThrowsIllegalStateException(flyoverErpRedeemScript);
    }

    @Test(expected = IllegalStateException.class)
    public void findKeyInRedeem_whenKeyIsNotInP2shErpRedeemScript_shouldThrowIllegalStateException() {
        assertThrowsIllegalStateException(flyoverP2shErpRedeemScript);
    }

    private void assertThrowsIllegalStateException(Script flyoverRedeemScript) {
        // Arrange
        final BtcECKey differentKey = BtcECKey.fromPrivate(BigInteger.valueOf(1000));
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act - Assert
        flyoverRedeemScriptParser.findKeyInRedeem(differentKey);
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsStandardRedeemScript_shouldReturnPubKeys() {
        assertPubKeys(flyoverStandardRedeemScript);
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsErpRedeemScript_shouldReturnPubKeys() {
        assertPubKeys(flyoverErpRedeemScript);
    }

    @Test
    public void getPubKeys_whenFlyoverRedeemScriptContainsP2shErpRedeemScript_shouldReturnPubKeys() {
        assertPubKeys(flyoverP2shErpRedeemScript);
    }

    private void assertPubKeys(Script flyoverRedeemScript) {
        // Arrange
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

        // Act
        List<BtcECKey> actualPubKeys = flyoverRedeemScriptParser.getPubKeys();

        // Assert
        keys.forEach(expectedPubKey -> {
            int btcECKeyIndex = keys.indexOf(expectedPubKey);
            BtcECKey btcECKey = actualPubKeys.get(btcECKeyIndex);
            byte[] actualPubKey = btcECKey.getPubKey();
            assertThat(actualPubKey, equalTo(expectedPubKey.getPubKey()));
        });
    }

    @Test
    public void findSigInRedeem_whenSignatureIsInStandardRedeemScript_shouldReturnSignatureIndexPosition() {
        assertSigInRedeem(flyoverStandardRedeemScript);
    }

    @Test
    public void findSigInRedeem_whenSignatureIsInErpRedeemScript_shouldReturnSignatureIndexPosition() {
        assertSigInRedeem(flyoverErpRedeemScript);
    }

    @Test
    public void findSigInRedeem_whenSignatureIsInP2shErpRedeemScript_shouldReturnSignatureIndexPosition() {
        assertSigInRedeem(flyoverP2shErpRedeemScript);
    }

    private void assertSigInRedeem(Script flyoverRedeemScript) {
        // Arrange
        final int EXPECTED_SIGNATURE_INDEX = 0;
        final int SIGNATURE_INPUT_INDEX = 0;
        final int OUTPUT_INDEX = 0;
        final int KEY_INDEX = 0;
        final NetworkParameters mainNetParams = MainNetParams.get();
        BtcECKey privateKey = keys.get(KEY_INDEX);

        // Creating a transaction
        BtcTransaction fundTx = new BtcTransaction(mainNetParams);
        Address userAddress = privateKey.toAddress(mainNetParams);
        fundTx.addOutput(Coin.FIFTY_COINS, userAddress);

        BtcTransaction spendTx = new BtcTransaction(mainNetParams);
        spendTx.addInput(fundTx.getOutput(OUTPUT_INDEX));

        // Getting the transaction hash for the signature
        Sha256Hash hashForSignatureHash = spendTx.hashForSignature(
            SIGNATURE_INPUT_INDEX,
            standardRedeemScript,
            BtcTransaction.SigHash.ALL,
            false
        );

        // Signing the transaction hash
        ECDSASignature signature = privateKey.sign(hashForSignatureHash);
        TransactionSignature transactionSignature = new TransactionSignature(signature, BtcTransaction.SigHash.ALL, false);

        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverRedeemScript.getChunks());

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
        NonStandardErpRedeemScriptParser nonStandardErpRedeemScriptParser = new NonStandardErpRedeemScriptParser(erpRedeemScript.getChunks());
        List<ScriptChunk> expectedStandardRedeemScriptChunks = nonStandardErpRedeemScriptParser.extractStandardRedeemScriptChunks();
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverErpRedeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedStandardRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test
    public void extractStandardRedeemScriptChunks_whenIsP2shErpRedeemScript_shouldReturnStandardRedeemScriptChunks() {
        // Arrange
        P2shErpRedeemScriptParser p2shErpFederationRedeemScriptParser = new P2shErpRedeemScriptParser(p2shErpRedeemScript.getChunks());
        List<ScriptChunk> expectedStandardRedeemScriptChunks = p2shErpFederationRedeemScriptParser.extractStandardRedeemScriptChunks();
        FlyoverRedeemScriptParser flyoverRedeemScriptParser = new FlyoverRedeemScriptParser(flyoverP2shErpRedeemScript.getChunks());

        // Act
        List<ScriptChunk> actualRedeemScriptChunks = flyoverRedeemScriptParser.extractStandardRedeemScriptChunks();

        // Assert
        assertEquals(expectedStandardRedeemScriptChunks, actualRedeemScriptChunks);
    }

    @Test(expected = VerificationException.class)
    public void flyoverRedeemScriptParser_whenRedeemScriptChunksSizeIsZero_shouldThrowVerificationException() {
        Script malformedScriptZeroSize = new Script(new byte[0]);

        // Act
        new FlyoverRedeemScriptParser(malformedScriptZeroSize.getChunks());
    }

    @Test(expected = VerificationException.class)
    public void flyoverRedeemScriptParser_whenRedeemScriptChunksSizeIsTwo_shouldThrowVerificationException() {
        Script malformedScriptTwoSize = new Script(new byte[2]);

        // Act
        new FlyoverRedeemScriptParser(malformedScriptTwoSize.getChunks());
    }

    @Test(expected = VerificationException.class)
    public void flyoverRedeemScriptParser_whenScriptSig_shouldThrowVerificationException() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(keys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            flyoverDerivationHash.getBytes(),
            redeemScript
        );
        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);
        Script scriptSig = p2SHOutputScript.createEmptyInputScript(null, flyoverRedeemScript);

        new FlyoverRedeemScriptParser(scriptSig.getChunks());
    }

    @Test(expected = VerificationException.class)
    public void flyoverRedeemScriptParser_whenP2shOutputScript_shouldThrowVerificationException() {
        Script redeemScript = RedeemScriptUtils.createStandardRedeemScript(keys);
        Script flyoverRedeemScript = RedeemScriptUtils.createFlyoverRedeemScript(
            flyoverDerivationHash.getBytes(),
            redeemScript
        );

        Script p2SHOutputScript = ScriptBuilder.createP2SHOutputScript(flyoverRedeemScript);

        new FlyoverRedeemScriptParser(p2SHOutputScript.getChunks());
    }
}
