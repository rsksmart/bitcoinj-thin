package co.rsk.bitcoinj.utils;

import co.rsk.bitcoinj.core.VerificationException;

public class NumberConversions {

    public static byte[] unsignedLongToByteArray(long number, int length) {
        if (number < 0) {
            throw new VerificationException("Number needs to be positive");
        }
        validateLength(number, length);

        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = (byte)(number & 0xFF);
            number >>= Byte.SIZE;
        }
        return result;
    }

    public static long byteArrayToUnsignedLong(byte[] byteArray) {
        long result = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            result <<= Byte.SIZE;
            result |= (byteArray[i] & 0xFF);
        }
        return result;
    }

    private static void validateLength(long number, int length) {
        double maxNumberRepresentation = Math.pow(2, length * Byte.SIZE) - 1;
        if (number > maxNumberRepresentation) {
            throw new VerificationException(
                String.format("%d can not be represented with %d bytes", number, length)
            );
        }
    }
}
