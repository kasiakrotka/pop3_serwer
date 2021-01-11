package com.company.protocol;

public class POP3Responses {

    private final char CR = 13;
    private final char LF = 10;

    public String ClosingTransmission(String hostName) {
        return null;
    }

    public String SyntaxError() {
        return null;
    }

    public String LengthError() {
        return null;
    }

    public String serverGreetings(String hostName) {
        return "+OK " + hostName + " POP3 server read" + CR + LF;
    }

    public String success(String optionalMsg) {
        if (optionalMsg == null)
            return "+OK" + CR + LF;
        else
            return "+OK " + optionalMsg + CR + LF;
    }

    public String stat(int numMsg, long sizeMailbox) {
        String num = Integer.toString(numMsg);
        String size = Long.toString(sizeMailbox);
        return "+OK " + num + " " + size + CR + LF;
    }

    public String error(String optionalMsg) {
        if (optionalMsg == null)
            return "-ERR" + CR + LF;
        else
            return "-ERR " + optionalMsg + CR + LF;
    }

    public String badSequence() {
        return "-ERR bad sequence" + CR + LF;
    }
}
