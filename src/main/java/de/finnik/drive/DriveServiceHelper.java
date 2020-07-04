package de.finnik.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import de.finnik.AES.AES;
import de.finnik.passvault.passwords.Password;
import de.finnik.passvault.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.finnik.gui.Var.APP_INFO;
import static de.finnik.gui.Var.LOG;

public class DriveServiceHelper {
    private static final String FILE_NAME = "pass";
    private final Drive mDrive;

    private List<File> passFile;

    public DriveServiceHelper(Drive mDrive) {
        this.mDrive = mDrive;
        refreshPassFile();
    }

    public boolean passFileExists() {
        refreshPassFile();
        return passFile.size() > 0;
    }

    public List<Password> readPasswords(AES aes) throws IOException {
        InputStream is = mDrive.files().get(passFileId()).executeMediaAsInputStream();
        return Password.readPasswords(is, aes);
    }

    public void savePasswords(List<Password> passwords, AES aes) throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("pass", "vault");
        Password.savePasswords(passwords, tempFile, aes);
        FileContent content = new FileContent("text/plain", tempFile);
        if (passFileExists()) {
            File old = mDrive.files().get(passFileId()).execute();
            File result = new File();
            result.setName(old.getName());
            result.setMimeType("text/plain");
            mDrive.files()
                    .update(old.getId(), result, content)
                    .execute();
        } else {
            File old = new File();
            old.setParents(Collections.singletonList("appDataFolder"));
            old.setName(FILE_NAME);

            mDrive.files()
                    .create(old, content)
                    .setFields("id")
                    .execute();
        }
        refreshPassFile();
    }

    public void deletePassFile() throws IOException {
        mDrive.files().delete(passFileId()).execute();
        refreshPassFile();
    }

    private String passFileId() {
        return passFile.get(0).getId();
    }

    private void refreshPassFile() {
        try {
            passFile = mDrive.files().list().setQ("name = '" + FILE_NAME + "'").setSpaces("appDataFolder").execute().getFiles();
        } catch (IOException e) {
            LOG.error("Error while accessing passwords on Drive: ", e);
            passFile = new ArrayList<>();
        }
    }

    public static final class Builder {
        private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

        public static Drive buildDrive() throws IOException, GeneralSecurityException {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APP_INFO.getProperty("app.name"))
                    .build();
        }

        /**
         * Creates an authorized Credential object.
         *
         * @param HTTP_TRANSPORT The network HTTP Transport.
         * @return An authorized Credential object.
         * @throws IOException If the credentials.json file cannot be found.
         */
        private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
            // Load client secrets.
            InputStream in = DriveServiceHelper.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singletonList(DriveScopes.DRIVE_APPDATA))
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("")))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver, url -> {
                try {
                    Utils.Browser.browse(url);
                } catch (URISyntaxException | IOException e) {
                    LOG.error("Error trying to open Google login page!");
                }
            }).authorize("user");
        }
    }
}
