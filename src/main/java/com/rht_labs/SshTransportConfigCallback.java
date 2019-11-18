package com.rht_labs;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

public class SshTransportConfigCallback implements TransportConfigCallback {

    private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
            JSch jSch = new JSch();
            // if private key has a passphrase:
            // jSch.addIdentity("/path/to/key", "super-secret-passphrase".getBytes());
            // should be created using: ssh-keygen -b 4096 -t rsa -m pem -f git-key
            // other parameters may not work with JGit!!
            jSch.addIdentity(System.getenv("GIT_PRIVATE_KEY_PATH"));
            return jSch;
        }
    };

    @Override
    public void configure(Transport transport) {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);
    }

}
