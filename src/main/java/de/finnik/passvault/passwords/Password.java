package de.finnik.passvault.passwords;

import com.google.gson.Gson;
import de.finnik.AES.AES;
import de.finnik.AES.AESReader;
import de.finnik.AES.AESWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static de.finnik.gui.Var.LOG;

/**
 * A password object has four parameters:
 * 1. The password
 * 2. The website to which the password belongs
 * 3. The email/username that is used among the password
 * 4. Other information that is useful to know among the password
 */
public class Password {
    private final String ID;
    private String pass, site, user, other;
    private long lastModified;

    public Password(String pass, String site, String user, String other) {
        this.pass = pass;
        this.site = site;
        this.user = user;
        this.other = other;
        updateModified();
        ID = UUID.randomUUID().toString();
    }

    private Password() {
        pass = "";
        site = "";
        user = "";
        other = "";
        updateModified();
        ID = UUID.randomUUID().toString();
    }

    public Password(Password password) {
        pass = password.pass;
        site = password.site;
        user = password.user;
        other = password.other;
        lastModified = password.lastModified;
        ID = password.ID;
    }

    /**
     * Encrypts all {@link Password} objects with a given password and saves them in a given file,
     *
     * @param passwords List of {@link Password} objects
     * @param file      The file to save the encrypted passwords to
     * @param aes       The aes to encrypt
     */
    public static void savePasswords(List<Password> passwords, File file, AES aes) {
        try (AESWriter aesWriter = new AESWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), aes)) {
            aesWriter.write(new Gson().toJson(passwords.toArray()));
        } catch (Exception e) {
            LOG.error("Error while saving passwords to {}!", file.getAbsolutePath(), e);
        }
    }

    /**
     * This static method returns all passwords with all of their parameters that are saved to an encrypted file
     *
     * @param file The encrypted file
     * @param aes  The aes to decrypt
     * @return The List of {@link Password} objects
     * @throws AES.WrongPasswordException If password is wrong
     */
    public static List<Password> readPasswords(File file, AES aes) throws AES.WrongPasswordException {
        try (InputStream is = new FileInputStream(file)) {
            return readPasswords(is, aes);
        } catch (AES.WrongPasswordException w) {
            throw new AES.WrongPasswordException();
        } catch (IOException e) {
            LOG.error("Error while reading passwords from {}!", file.getAbsolutePath(), e);
        }
        return new ArrayList<>();
    }

    /**
     * This static method returns all passwords with all of their parameters that are saved to an encrypted file
     *
     * @param inputStream The encrypted inputStream
     * @param aes         The aes to decrypt
     * @return The List of {@link Password} objects
     */
    public static List<Password> readPasswords(InputStream inputStream, AES aes) throws AES.WrongPasswordException, IOException {
        try (AESReader aesReader = new AESReader(new InputStreamReader(inputStream), aes)) {
            return new ArrayList<>(Arrays.asList(new Gson().fromJson(aesReader.readLine(), Password[].class)));
        } catch (AES.WrongPasswordException e) {
            // Wrong password
            throw new AES.WrongPasswordException();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Creates a string containing all attributes of a {@link Password} objects excluding the password attribute
     *
     * @param password The password to convert to a string
     * @return The converted string
     */
    public static String log(Password password, String message) {
        StringBuilder out = new StringBuilder();
        String separator = ", ";
        if (password.site.length() > 0) {
            out.append("site: ");
            out.append(password.site);
            out.append(separator);
        }
        if (password.user.length() > 0) {
            out.append("user: ");
            out.append(password.user);
            out.append(separator);
        }
        if (password.other.length() > 0) {
            out.append("other information: ");
            out.append(password.other);
            out.append(separator);
        }
        if (out.length() > 0) {
            return String.format("%s: %s!", message, out.substring(0, out.length() - separator.length()));
        } else {
            return message + " with no information!";
        }
    }

    /**
     * Tests two {@link Password} objects for similarities in their main parameters: password, site, user, other
     *
     * @param one One password
     * @param two The other password
     * @return Are the two password similar in face of their main parameters?
     */
    public static boolean equalsInformation(Password one, Password two) {
        return one.getPass().equals(two.getPass()) &&
                one.getSite().equals(two.getSite()) &&
                one.getUser().equals(two.getUser()) &&
                one.getOther().equals(two.getOther());
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
        updateModified();
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
        updateModified();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
        updateModified();
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
        updateModified();
    }

    public void updateModified() {
        lastModified = System.currentTimeMillis();
    }

    public String id() {
        return ID;
    }

    public long lastModified() {
        return lastModified;
    }

    /**
     * Returns a stream of all parameters
     *
     * @return A stream of all parameters
     */
    public Stream<String> getValues() {
        return Stream.of(pass, site, user, other);
    }

    /**
     * Checks if all parameters of the password are empty
     *
     * @return {@code true} if every parameter is empty (equal to "") or {@code false} if not
     */
    public boolean isEmpty() {
        return Stream.of(pass, site, user, other).filter(String::isEmpty).count() == 4;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == getClass()) {
            Password password = (Password) obj;
            return password.getPass().equals(getPass())
                    && password.getSite().equals(getSite())
                    && password.getUser().equals(getUser())
                    && password.getOther().equals(getOther())
                    && password.id().equals(id())
                    && password.lastModified() == lastModified();
        }
        return false;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pass, site, user, other);
    }

}
