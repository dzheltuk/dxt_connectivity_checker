package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class StreamPrinter extends Thread {
    private InputStream is;
    private List<Character> chars = new ArrayList<>();
    public boolean isProcessing = false;

    public StreamPrinter(InputStream is) {
        this.is = is;
    }

    public void close() {
        try {
            is.close();
        } catch (Exception e) {

        }
    }

    public String getText() {
        char[] charsResult = new char[chars.size()];
        int i = 0;
        for (Character ch : chars) {
            charsResult[i++] = ch;
        }
        return new String(charsResult);
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            int line = -2;
            while ((line = br.read()) != -2) {
                isProcessing = true;
                chars.add((char) line);
                if (line == -1) {
                    System.out.print(".");
                    isProcessing = false;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (IOException ioe) {
        }
    }
}
