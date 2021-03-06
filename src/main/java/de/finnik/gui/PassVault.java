package de.finnik.gui;

import de.finnik.AES.AES;
import de.finnik.api.PassAPI;
import de.finnik.drive.DriveLocalHelper;
import de.finnik.gui.dialogs.PassDialog;
import de.finnik.gui.hints.Hints;
import de.finnik.gui.mainFrame.PassFrame;
import de.finnik.passvault.InactivityListener;
import de.finnik.passvault.LogErrorStream;
import de.finnik.passvault.PassProperty;
import de.finnik.passvault.passwords.Password;
import de.finnik.passvault.utils.Utils;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

import static de.finnik.gui.Var.*;

/**
 * The PassVault program implements an application that saves passwords encrypted
 * with a main password. It also generates new passwords with a given length and given parameters.
 *
 * @author finnik
 * @version 3.0
 * @since 06.11.2020
 */

public class PassVault {

    public static void main(String[] args) {
        // Creates the matching logger object
        LOG = LoggerFactory.getLogger(args.length == 0 ? "APPLICATION" : "API");
        System.setErr(new PrintStream(new LogErrorStream(LOG)));

        LOG.info("Welcome to PassVault, we're happy to see you!");

        // Launches api if necessary
        if (args.length > 0) {
            init();
            EventQueue.invokeLater(() -> {
                try (BufferedReader br = new BufferedReader(new FileReader(PASSWORDS))) {
                    String file = br.readLine();
                    if (file != null) {
                        new PassAPI(args);
                    } else {
                        System.out.println(404);
                    }
                } catch (Exception e) {
                    LOG.error("Error with API input", e);
                }
            });
            return;
        }


        PassVault main = new PassVault();
        main.run();
    }

    /**
     * Creates necessary files (if they don´t already exist), loads fonts, images and properties,
     * creates necessary objects for variables in {@link Var} and starts the application.
     */
    private static void init() {
        APP_DIR = new File(System.getProperty("user.home") + "/.passvault");
        if (APP_DIR.mkdirs()) {
            LOG.info("Created main directory {}", APP_DIR.getAbsolutePath());
        }

        PASSWORDS = new File(APP_DIR, "pass");
        try {
            if (PASSWORDS.createNewFile()) {
                LOG.info("Created pass file in main directory! ({})!", PASSWORDS.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.error("Error while creating pass file in {}!", APP_DIR.getAbsolutePath(), e);
        }

        APP_INFO = new Properties();
        try {
            APP_INFO.load(new InputStreamReader(PassVault.class.getResourceAsStream("/application.properties"), StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.error("Error while reading application.properties!", e);
        }

        PassProperty.PROPERTIES = new File(APP_DIR, "config.properties");
        try {
            if (PassProperty.PROPERTIES.createNewFile()) {
                LOG.info("Created config.properties in main directory ({})!", PassProperty.PROPERTIES.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.error("Error while creating config.properties");
        }

        // Loads non-encrypted properties
        PassProperty.load(null);

        LANG = loadLang();

        try (InputStream is = PassVault.class.getResourceAsStream("/fonts/Raleway-Regular.ttf")) {
            RALEWAY = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            LOG.error("Error while loading Raleway font!", e);
        }

        try {
            HINTS = new Hints(new File(APP_DIR, "hints"));
        } catch (IOException e) {
            LOG.error("Error while loading hints!", e);
        }

        loadImages(Arrays.stream(Var.class.getFields()).filter(field -> field.getType() == BufferedImage.class).toArray(Field[]::new));

        DIALOG = new PassDialog(FOREGROUND, BACKGROUND, RALEWAY, CLOSE, WARNING, ICON_SMALL, QUESTION_MARK, CHECK_MARK);

        COMPONENTS = new HashMap<>();

        DRIVE = new DriveLocalHelper();
    }

    /**
     * Loads all images from its fields (The matching file has the same name as the field)
     *
     * @param images The image fields that should be loaded
     */
    private static void loadImages(Field[] images) {
        for (Field img : images) {
            String res = String.format("/images/%s.png", img.getName().toLowerCase());
            try (InputStream is = PassVault.class.getResourceAsStream(res)) {
                img.set(null, ImageIO.read(is));
            } catch (Exception e) {
                LOG.error("Error while reading {}!", res, e);
            }
        }
    }

    /**
     * Runs {@link PassVault#init()} and then launches either {@link CheckFrame} or {@link PassFrame}
     */
    private void run() {
        init();

        EventQueue.invokeLater(() -> {
            // Put some default values to UIManager
            UIManager.put("ToolTip.background", FOREGROUND);
            UIManager.put("ToolTip.foreground", BACKGROUND);
            UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(2, 2, 2, 2));
            UIManager.put("MenuItem.selectionBackground", FOREGROUND);
            UIManager.put("MenuItem.selectionForeground", BACKGROUND);
            UIManager.put("ComboBox.selectionBackground", FOREGROUND);
            UIManager.put("ComboBox.selectionForeground", BACKGROUND);

            /*
            Reads the {@link Var#PASSWORDS} file, checks whether passwords are already saved and creates either the login dialog
            or the main frame with an empty string as the main password
             */
            try (BufferedReader br = new BufferedReader(new FileReader(PASSWORDS))) {
                String file = br.readLine();
                if (file != null && !file.isEmpty()) {
                    FRAME = new CheckFrame((pass, passList) -> {
                        PassProperty.load(pass);
                        INACTIVITY_LISTENER = new InactivityListener(Integer.parseInt(PassProperty.INACTIVITY_TIME.getValue()), () -> ((PassFrame) FRAME).inactive());
                        FRAME = new PassFrame(pass, passList);
                        FRAME.setVisible(true);
                    });
                } else {
                    FRAME = new PassFrame(new AES(""), new ArrayList<>());
                }
                FRAME.setVisible(true);
            } catch (Exception e) {
                LOG.error("Error while reading password file!", e);
            }
        });
    }

    /**
     * Login frame that wants you to enter your main password that is used to encrypt all passwords.
     * It´s created on beginning and you won´t enter {@link PassFrame} unless you haven´t typed in the correct password.
     */
    public static class CheckFrame extends JDialog {
        private final BiConsumer<AES, List<Password>> todo;
        private final String message;

        /**
         * Creates the CheckFrame. Second parameter can be null -> No additional message will be displayed
         *
         * @param after   The BiConsumer that will handle the inputted AES and password list
         * @param message An additional message that will be displayed while input
         */
        public CheckFrame(BiConsumer<AES, List<Password>> after, String message) {
            this.todo = after;
            this.message = message;

            setSize(380, 200);
            setTitle("Login");

            JPanel contentPane = new JPanel(null);
            contentPane.setBorder(BorderFactory.createLineBorder(FOREGROUND));
            contentPane.setBackground(BACKGROUND);
            setContentPane(contentPane);

            setResizable(false);
            setUndecorated(true);
            setIconImage(FRAME_ICON);

            if (message != null) {
                setSize(Math.max(getWidth(), getFontMetrics(raleway(14)).stringWidth(message) + 30), getHeight());
            }

            setLocationRelativeTo(null);

            components();
            textComponents();
        }

        /**
         * Calls {@link CheckFrame#CheckFrame(BiConsumer, String)} with the string object as null
         *
         * @param after The BiConsumer that will handle the inputted AES and password list
         */
        public CheckFrame(BiConsumer<AES, List<Password>> after) {
            this(after, null);
        }

        /**
         * Generates the components
         */
        private void components() {
            JLabel lblIcon = new JLabel();
            lblIcon.setIcon(new ImageIcon(ICON));
            lblIcon.setSize(lblIcon.getPreferredSize());
            lblIcon.setBounds(Utils.getCentralPosition(getWidth(), lblIcon.getWidth()), 10, lblIcon.getWidth(), lblIcon.getHeight());
            add(lblIcon, "check.lbl.icon");

            JLabel lblClose = new JLabel();
            lblClose.setIcon(new ImageIcon(CLOSE));
            lblClose.setSize(lblClose.getPreferredSize());
            lblClose.setBounds(getWidth() - lblClose.getWidth() - 10, 10, lblClose.getWidth(), lblClose.getHeight());
            lblClose.setCursor(HAND_CURSOR);
            lblClose.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    System.exit(0);
                }
            });
            add(lblClose, "check.lbl.close");

            JLabel lblPass = new JLabel();
            add(lblPass, "check.lbl.pass");
            // Is needed to calculate label's width
            textComponents();
            lblPass.setForeground(FOREGROUND);
            lblPass.setFont(raleway(15));
            lblPass.setSize(lblPass.getFontMetrics(lblPass.getFont()).stringWidth(lblPass.getText()), 50);
            lblPass.setBounds(Utils.getCentralPosition(getWidth(), lblPass.getWidth()), message == null ? 70 : 60, lblPass.getWidth(), lblPass.getHeight());

            if (message != null) {
                JLabel lblWarning = new JLabel(message);
                lblWarning.setFont(raleway(14));
                lblWarning.setForeground(new Color(189, 0, 12));
                lblWarning.setSize(lblWarning.getPreferredSize());
                lblWarning.setBounds(Utils.getCentralPosition(getWidth(), lblWarning.getWidth()), 96, lblWarning.getWidth(), lblWarning.getHeight());
                add(lblWarning, "check.lbl.warning");
            }

            JButton btnLogin = new JButton();

            JPasswordField passwordField = new JPasswordField() {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(FOREGROUND);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (Boolean.parseBoolean(PassProperty.SHOW_MAIN_PASSWORD.getValue())) {
                        super.paintComponent(g);
                    }
                }
            };
            passwordField.setBounds(10, 120, getWidth() - 20, 30);
            passwordField.setFont(raleway(20));
            passwordField.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            passwordField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        btnLogin.doClick();
                    }
                }
            });
            add(passwordField, "check.pf.password");

            btnLogin.setFont(RALEWAY.deriveFont(15f));
            btnLogin.setSize(200, 30);
            btnLogin.setForeground(BACKGROUND);
            btnLogin.setBackground(FOREGROUND);
            btnLogin.setBounds(Utils.getCentralPosition(getWidth(), btnLogin.getWidth()), 160, btnLogin.getWidth(), btnLogin.getHeight());
            btnLogin.addActionListener(action -> {
                // The login
                List<Password> passwordList;
                try {
                    passwordList = Password.readPasswords(PASSWORDS, new AES(new String(passwordField.getPassword())));
                } catch (AES.WrongPasswordException e) {
                    // Exception -> Wrong password
                    LOG.info("User tried to log in with wrong password!");
                    passwordField.setText("");
                    DIALOG.message(CheckFrame.this, LANG.getString("jop.wrongPass"));
                    return;
                }
                LOG.info("User logged in!");
                todo.accept(new AES(new String(passwordField.getPassword())), passwordList);
                dispose();
            });
            add(btnLogin, "check.btn.login");
        }

        /**
         * Adds a component with its name to the {@link Var#COMPONENTS} map and adds the component to the panel
         * The method kind of overwrites {@link java.awt.Container#add(Component)} method in order to handle the components later
         *
         * @param c   The component
         * @param key The component´s matching name
         */
        private void add(Component c, String key) {
            COMPONENTS.put(key, c);
            getContentPane().add(c);
        }
    }
}
