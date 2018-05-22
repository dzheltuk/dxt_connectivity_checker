package sshj;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.PTYMode;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SshjShell {

    private static SSHClient client;
    private static Session session;
    private static Session.Shell shell;
    private static StreamPrinter reader;

    public String executeTelnetCommand(final String host, final String username, final String password,
                                              final String command) throws IOException, InterruptedException {
        if (session == null) {
            session = getSession(host, username, password);
            shell = session.startShell();
        }

        if (reader == null) {
            reader = new StreamPrinter(shell.getInputStream());
            reader.start();
        }
        shell.getOutputStream().write((command).getBytes());
        shell.getOutputStream().flush();
        while (reader.isProcessing) {
            Thread.sleep(1000);
        }
        return reader.getText();
    }

    private Session getSession(String host, String username, String password) throws IOException {
        client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host);
        client.authPassword(username, password);
        Session session = client.startSession();
        Map<PTYMode, Integer> modes = new HashMap<>();
        session.allocatePTY("vt102", 160, 80, 0, 0, modes);
        return session;
    }

    public void close() {
        try {
            session.close();
        } catch (Exception e) {

        }
        try {
            client.close();
        } catch (Exception e) {

        }
        reader.close();
    }

    private static class StreamPrinter extends Thread {
        InputStream is;
        List<Character> chars = new ArrayList<>();
        boolean isProcessing = false;

        private StreamPrinter(InputStream is) {
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
}
