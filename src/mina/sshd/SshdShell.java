package mina.sshd;

import jdk.nashorn.internal.ir.Terminal;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.simple.SimpleClient;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import util.StreamPrinter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;

public class SshdShell {
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);
    private static final long AUTH_TIMEOUT = TimeUnit.SECONDS.toMillis(7L);

    private SshClient client;
    private SimpleClient simple;
    private StreamPrinter reader;

    public void init() {
        client = setupTestClient(getClass());
        simple = SshClient.wrapAsSimpleClient(client);
        simple.setConnectTimeout(CONNECT_TIMEOUT);
        simple.setAuthenticationTimeout(AUTH_TIMEOUT);
    }

    public void close() throws Exception {
        if (simple != null) {
            simple.close();
        }
        if (client != null) {
            client.stop();
        }
    }

    public String executeTelnetCommand(final String host, final String username, final String password,
                                      final String command) throws IOException, InterruptedException {
        client.start();

        try (ClientSession session = simple.sessionLogin(host, 22, username, password)) {
            System.out.println("Session is : " + session.isAuthenticated());

            ChannelShell shell = session.createShellChannel();
            shell.setPtyType("vt102");

            //shell.setIn(new ByteArrayInputStream(new byte[0]));
            //shell.setAgentForwarding(true);

            if (reader == null) {
                reader = new StreamPrinter(shell.getIn());
                reader.start();
            }
            shell.getOut().write((command).getBytes());
            shell.getOut().flush();
            while (reader.isProcessing) {
                Thread.sleep(1000);
            }


//            try (Terminal terminal = TerminalBuilder.terminal()) {
//                Attributes attributes = terminal.enterRawMode();
//
//                channel.setOut(terminal.output());
//                channel.setErr(terminal.output());
//                channel.open().verify();
//                if (channel instanceof PtyCapableChannelSession) {
//                    //registerSignalHandler(terminal, (PtyCapableChannelSession) channel);
//                }
//                channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
//                if (channel.getExitStatus() != null) {
//                    //exitStatus = channel.getExitStatus();
//                }
//            } finally {
//                terminal.setAttributes(attributes);
//            }
        }

        return reader.getText();
    }

    public static SshClient setupTestClient(Class<?> anchor) {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        client.setKeyPairProvider(KeyPairProvider.EMPTY_KEYPAIR_PROVIDER);
        return client;
    }
}
