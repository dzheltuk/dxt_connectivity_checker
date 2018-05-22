package mina.sshd;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.simple.SimpleClient;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.common.util.*;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;

public class SshdShell {
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);
    private static final long AUTH_TIMEOUT = TimeUnit.SECONDS.toMillis(7L);

    private SshServer sshd;
    private SshClient client;
    private SimpleClient simple;

    public void init() {
        client = setupTestClient(getClass());
        simple = SshClient.wrapAsSimpleClient(client);
        simple.setConnectTimeout(CONNECT_TIMEOUT);
        simple.setAuthenticationTimeout(AUTH_TIMEOUT);
    }

    public void close() throws Exception {
        if (sshd != null) {
            sshd.stop(true);
        }
        if (simple != null) {
            simple.close();
        }
        if (client != null) {
            client.stop();
        }
    }

    public void executeTelnetCommand(final String host, final String username, final String password,
                                      final String command) throws IOException {
        client.start();

        try (ClientSession session = simple.sessionLogin(host, 22, username, password)) {
            //assertEquals("Mismatched session username", getCurrentTestName(), session.getUsername());
            System.out.println("Session is : " + session.isAuthenticated());

            ChannelExec channel = session.createExecChannel(command);
            channel.setPtyType("vt102");

            channel.setIn(new ByteArrayInputStream(new byte[0]));
            channel.setAgentForwarding(true);

            session.executeRemoteCommand(command);
        }
    }

    public static SshClient setupTestClient(Class<?> anchor) {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        client.setKeyPairProvider(KeyPairProvider.EMPTY_KEYPAIR_PROVIDER);
        return client;
    }
}
