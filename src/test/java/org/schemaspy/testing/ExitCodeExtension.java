package org.schemaspy.testing;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.security.Permission;

@SuppressWarnings("removal")
public class ExitCodeExtension implements AfterEachCallback, BeforeEachCallback {

    private SecurityManager securityManager;
    private int exitCode;

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        securityManager = System.getSecurityManager();
        System.setSecurityManager(new ExitPreventionSecurityManager());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        System.setSecurityManager(securityManager);
    }

    class ExitPreventionSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {

        }

        @Override
        public void checkPermission(Permission perm, Object context) {

        }

        @Override
        public void checkExit(int status) {
            exitCode = status;
            throw new SecurityException("Exit prevented by ExitCodeRule");
        }
    }

}
