package mina.sshd;

import jdk.nashorn.internal.ir.Terminal;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.simple.SimpleClient;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import util.StreamPrinter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SshdShell {
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);
    private static final long AUTH_TIMEOUT = TimeUnit.SECONDS.toMillis(7L);

    private SshClient client;
    private SimpleClient simple;
    private static StreamPrinter reader;

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

    public  String executeCommand(final String host, final String username, final String password,
                                  final String command) throws IOException {

        client.start();

        try (ClientSession session = simple.sessionLogin(host, 22, username, password)) {
            System.out.println("Session is : " + session.isAuthenticated());

            ChannelShell channel = session.createShellChannel();

            channel.setIn(new NoCloseInputStream(new ByteArrayInputStream(command.getBytes())));
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            channel.setOut(new NoCloseOutputStream(out));
            channel.setErr(new NoCloseOutputStream(out));
            channel.open();
            //channel.waitFor(ClientChannel.CLOSED, 0);

            String[] lines=new String(out.toByteArray()).split("\r\n");
            StringBuffer result=new StringBuffer();
            for(int i=21;i<lines.length-1;i++) {
                result.append(lines[i]).append("\n");
            }

            // visual output of Fuse/Karaf interactions
            if (!"".equals(result.toString().trim()))
                System.out.println("out >> "+result.toString().trim());
//        System.out.println("out >> "+result.toString().trim());
            return result.toString();
        }
    }

    public String executeTelnetCommand(final String host, final String username, final String password,
                                      final String command) throws IOException, InterruptedException {
        client.start();

        try (ClientSession session = simple.sessionLogin(host, 22, username, password)) {
            System.out.println("Session is : " + session.isAuthenticated());

            ChannelShell shell = session.createShellChannel();
            shell.setUsePty(true);
            shell.setPtyType("vt102");

            shell.setIn(new ByteArrayInputStream(new byte[0]));
            shell.setAgentForwarding(true);

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
