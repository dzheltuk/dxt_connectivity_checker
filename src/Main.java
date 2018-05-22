import sshj.SshjShell;

import java.io.IOException;

public class Main {

    private static final String HOST_NAME = "10.90.109.2";
    private static final String USERNAME = "XTUSER";
    private static final String PASSWORD = "USERXT";
    private static final String COMMAND = "ZAHO;\n\r";
    private static final String COMMAND_EXIT = "ZZZ;\n\r";

    public static void main(String[] args) throws IOException, InterruptedException {

        SshjShell sshjShell = new SshjShell();

        try {
            System.out.println(sshjShell.executeTelnetCommand(HOST_NAME, USERNAME, PASSWORD, COMMAND));
        } catch (Exception e) {
            System.out.println("Unable to execute " + e.getMessage());
        } finally {
            Thread.sleep(100);
            System.out.println(sshjShell.executeTelnetCommand(HOST_NAME, USERNAME, PASSWORD, COMMAND_EXIT));
            sshjShell.close();
        }
    }
}