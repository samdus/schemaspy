package org.schemaspy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.schemaspy.testing.ExitCodeExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
public class MainTest {

    @RegisterExtension
    public ExitCodeExtension exitCodeExtension = new ExitCodeExtension();

    @Test
    public void callsSystemExit(CapturedOutput output) {
        assertThat(output).contains("StackTraces have been omitted");
        try {
            Main.main(
                    "-t", "mysql",
                    "-sso",
                    "-o", "target/tmp",
                    "-host", "localhost",
                    "-port", "123154",
                    "-db", "qwerty",
                    "--logging.config="+ Paths.get("src","test","resources","logback-debugEx.xml").toString()
            );
        } catch (SecurityException ignore) { }
        assertThat(exitCodeExtension.getExitCode()).isEqualTo(3);
    }

}
