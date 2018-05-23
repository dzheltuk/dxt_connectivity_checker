package mina.sshd;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.simple.SimpleClient;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SshdShell {
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);
    private static final long AUTH_TIMEOUT = TimeUnit.SECONDS.toMillis(7L);

    private SshClient client;
    private SimpleClient simple;

    public void init() {
        client = setupTestClient();
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

    public String executeCommand(final String host, final String username, final String password,
                                 final String command) throws IOException {

        client.start();

        try (ClientSession session = simple.sessionLogin(host, 22, username, password)) {
            System.out.println("Session is : " + session.isAuthenticated());

            ClientChannel channel = session.createShellChannel();
            channel.setIn(new NoCloseInputStream(new ByteArrayInputStream(command.getBytes())));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            channel.setOut(new NoCloseOutputStream(out));
            channel.setErr(new NoCloseOutputStream(out));
            channel.open();
            channel.waitFor(Collections.singleton(ClientChannelEvent.CLOSED), 10000);

            return new String(out.toByteArray());
        }
    }

    private static SshClient setupTestClient() {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        client.setKeyPairProvider(KeyPairProvider.EMPTY_KEYPAIR_PROVIDER);
        return client;
    }
}
