package com.masasilam.app.util.file;

import com.jcraft.jsch.*;
import com.masasilam.app.config.VpsStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class VpsFileStorage {
    private final VpsStorageProperties props;

    public String upload(byte[] data, String remotePath) {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = createSession();
            session.connect(15_000);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(10_000);

            String fullRemotePath = props.getBasePath() + "/" + remotePath;
            String remoteDir = fullRemotePath.substring(0, fullRemotePath.lastIndexOf('/'));

            mkdirs(channel, remoteDir);

            try (InputStream is = new ByteArrayInputStream(data)) {
                channel.put(is, fullRemotePath, ChannelSftp.OVERWRITE);
            }

            String publicUrl = props.getBaseUrl() + "/" + remotePath;
            log.info("Uploaded to VPS: {} → {}", fullRemotePath, publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("VPS upload failed for path '{}': {}", remotePath, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to VPS: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    public void delete(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return;

        String relativePath = publicUrl.replace(props.getBaseUrl(), "");
        String fullPath = props.getBasePath() + relativePath;

        Session session = null;
        ChannelSftp channel = null;

        try {
            session = createSession();
            session.connect(15_000);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(10_000);

            channel.rm(fullPath);
            log.info("Deleted from VPS: {}", fullPath);

        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                log.warn("File not found on VPS (skip delete): {}", fullPath);
            } else {
                log.warn("Failed to delete from VPS '{}': {}", fullPath, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to delete from VPS '{}': {}", fullPath, e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    public boolean exists(String remotePath) {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = createSession();
            session.connect(15_000);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(10_000);
            channel.lstat(props.getBasePath() + "/" + remotePath);
            return true;
        } catch (SftpException e) {
            return false;
        } catch (Exception e) {
            log.warn("Failed to check file existence '{}': {}", remotePath, e.getMessage());
            return false;
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(props.getUsername(), props.getHost(), props.getPort());
        session.setPassword(props.getPassword());

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        return session;
    }

    private void mkdirs(ChannelSftp channel, String remotePath) {
        String[] parts = remotePath.split("/");
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                current.append("/");
                continue;
            }
            current.append(part).append("/");
            try {
                channel.mkdir(current.toString());
            } catch (SftpException e) {
                if (e.id != ChannelSftp.SSH_FX_FAILURE && e.id != ChannelSftp.SSH_FX_PERMISSION_DENIED) {
                    log.debug("mkdir '{}': {}", current, e.getMessage());
                }
            }
        }
    }
}