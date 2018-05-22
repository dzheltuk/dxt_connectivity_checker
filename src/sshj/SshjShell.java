package sshj;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.PTYMode;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import util.StreamPrinter;

import java.io.IOException;
import java.util.HashMap;
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
}
