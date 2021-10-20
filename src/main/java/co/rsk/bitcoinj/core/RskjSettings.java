package co.rsk.bitcoinj.core;

public class RskjSettings {
    private boolean csvLengthValidation;

    public boolean isCsvLengthValidation() {
        return csvLengthValidation;
    }

    public void setCsvLengthValidation(boolean csvLengthValidation) {
        this.csvLengthValidation = csvLengthValidation;
    }
}
