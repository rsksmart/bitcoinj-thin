package co.rsk.bitcoinj.utils;

import co.rsk.bitcoinj.core.VerificationException;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class NumberConversionsTest {

    @Test
    public void unsignedLongToByteArray_twoBytesLength() {
        final int length = 2;

        long value = 1L;
        byte[] conversion = NumberConversions.unsignedLongToByteArray(value, length);
        long obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        Assert.assertEquals(length, conversion.length);
        Assert.assertEquals(value, obtainedValue);

        value = 255L;
        conversion = NumberConversions.unsignedLongToByteArray(value, length);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        Assert.assertEquals(length, conversion.length);
        Assert.assertEquals(value, obtainedValue);

        value = 256L;
        conversion = NumberConversions.unsignedLongToByteArray(value, length);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        Assert.assertEquals(length, conversion.length);
        Assert.assertEquals(value, obtainedValue);

        value = 65535L;
        conversion = NumberConversions.unsignedLongToByteArray(value, length);
        obtainedValue = Long.parseLong(Hex.toHexString(conversion), 16);
        Assert.assertEquals(length, conversion.length);
        Assert.assertEquals(value, obtainedValue);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArray_insufficientLengthOf1() {
        NumberConversions.unsignedLongToByteArray(260, 1);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArray_insufficientLengthOf2() {
        NumberConversions.unsignedLongToByteArray(65536, 2);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArray_negativeNumber() {
        NumberConversions.unsignedLongToByteArray(-100, 2);
    }

    @Test(expected = VerificationException.class)
    public void unsignedLongToByteArray_negativeLength() {
        NumberConversions.unsignedLongToByteArray(260, -1);
    }

    @Test
    public void byteArrayToUnsignedLong() {
        byte[] value = new byte[]{0, 0, 0, 0, 0, 0, -1, -1};
        long conversion = NumberConversions.byteArrayToUnsignedLong(value);

        long obtainedValue = Long.valueOf(Hex.toHexString(value), 16);
        Assert.assertEquals(obtainedValue, conversion);
    }
}
