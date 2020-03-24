package de.finnik.gui;

import de.finnik.passvault.*;

import javax.swing.*;
import javax.swing.plaf.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.List;
import java.util.*;
import java.util.stream.*;

import static de.finnik.gui.Var.*;

/**
 * Change settings, get information and help about the application
 */
class SettingsDialog extends JDialog {

    /**
     * A list of {@link javax.swing.JComponent}s which are generated in {@link de.finnik.gui.SettingsDialog#components()}
     * to be added to the content pane in the right order {@link de.finnik.gui.SettingsDialog#positionComponents(List)}
     */
    private List<JComponent> components;

    /**
     * Creates the frame
     */
    SettingsDialog(Window owner) {
        super(owner, ModalityType.APPLICATION_MODAL);

        setContentPane(new JPanel());
        ((JPanel) getContentPane()).setBorder(BorderFactory.createLineBorder(FOREGROUND));
        getContentPane().setBackground(BACKGROUND);

        BoxLayout boxLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(boxLayout);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setUndecorated(true);

        components = new ArrayList<>();
        components();
        textComponents();

        positionComponents(components);

        // Sets color for labels and buttons
        PassUtils.GUIUtils.colorComponents(getMatchingComponents("settings.lbl", "settings.btn"), FOREGROUND, BACKGROUND);

        setSize(new Dimension(boxLayout.preferredLayoutSize(getContentPane()).width + 100, boxLayout.preferredLayoutSize(getContentPane()).height + 60));

        setLocationRelativeTo(null);
    }

    /**
     * Lets the user change his main password by entering it two times
     */
    static void changeMainPass() {
        DIALOG.input(FRAME, LANG.getProperty("jop.enterNewMainPass"), first -> {
            if (first != null && !first.equals("")) {
                DIALOG.input(FRAME, LANG.getProperty("jop.repeatEnteringNewMainPass"), second -> {
                    if (first.equals(second)) {
                        PassFrame.password = first;
                        LOG.info("Changed main password!");
                        PassFrame.savePasswords();
                    } else {
                        DIALOG.message(FRAME, LANG.getProperty("jop.wrongPass"));
                    }
                }, true);
            }
        }, true);
    }

    private void components() {
        JPanel toolBar = new JPanel();
        toolBar.setBackground(BACKGROUND);
        toolBar.setLayout(new FlowLayout());
        ((FlowLayout) toolBar.getLayout()).setAlignment(FlowLayout.RIGHT);
        add(toolBar, "settings.toolBar");

        JLabel lblExtract = new JLabel();
        lblExtract.setIcon(new ImageIcon(EXTRACT));
        lblExtract.setCursor(Var.HAND_CURSOR);
        lblExtract.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // Choose the directory to store the update to
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = jfc.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION && PassFrame.password.length() > 0 && PassFrame.passwordList.size()>0) {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
                    Date date = new Date();
                    File backup = new File(jfc.getSelectedFile(), "PassVault_" + formatter.format(date) + ".bin");

                    // Check whether target file exists already
                    if (!backup.exists()) {
                        Password.savePasswords(PassFrame.passwordList, backup, PassFrame.password);
                        LOG.info("Exported password to {}!", jfc.getSelectedFile().getAbsolutePath());
                    } else {
                        DIALOG.message(FRAME, String.format(LANG.getProperty("jop.fileExistsAlready"), backup.getAbsolutePath()));
                    }
                }
            }
        });
        toolBar.add(lblExtract);

        JLabel lblHelp = new JLabel();
        lblHelp.setIcon(new ImageIcon(HELP));
        lblHelp.setCursor(HAND_CURSOR);
        lblHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Open help page
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("http://www.example.com"));
                    } catch (Exception ex) {
                        LOG.error("Error while opening help page!", ex);
                    }

                }
            }
        });
        toolBar.add(lblHelp);

        JLabel lblClose = new JLabel();
        lblClose.setIcon(new ImageIcon(Utils.resizeImage(CLOSE, 20, 20)));
        lblClose.setCursor(HAND_CURSOR);
        lblClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        toolBar.add(lblClose);

        JLabel lblIcon = new JLabel();
        lblIcon.setIcon(new ImageIcon(ICON));
        lblIcon.setSize(lblIcon.getPreferredSize());
        add(lblIcon, "settings.lbl.icon");

        JPanel panelVersion = new JPanel(new FlowLayout());
        ((FlowLayout) panelVersion.getLayout()).setAlignment(FlowLayout.CENTER);
        panelVersion.setBackground(BACKGROUND);

        JLabel lblVersion = new JLabel(APP_INFO.getProperty("app.name") + " " + APP_INFO.getProperty("app.version") + " \u00a9 "/* + APP_INFO.getProperty("app.author")*/);
        lblVersion.setFont(raleway(13));
        lblVersion.setSize(lblVersion.getPreferredSize());
        COMPONENTS.put("settings.lbl.version", lblVersion);
        panelVersion.add(lblVersion);

        JLabel lblFinnik = new JLabel();
        lblFinnik.setIcon(new ImageIcon(FINNIK));
        lblFinnik.setSize(lblFinnik.getPreferredSize());
        panelVersion.add(lblFinnik);

        panelVersion.setSize(new Dimension(lblVersion.getSize().width+lblFinnik.getSize().width,lblVersion.getSize().height+lblFinnik.getSize().height));
        components.add(panelVersion);

        JButton btnChangeMainPass = new JButton();
        btnChangeMainPass.addActionListener(action -> {
            if (!PassFrame.password.equals("")) {
                // Validates the user via inserting current main pass
                DIALOG.input(FRAME, LANG.getProperty("check.lbl.pass"), main -> {
                    if (main.equals(PassFrame.password)) {
                        changeMainPass();
                    } else {
                        DIALOG.message(this, LANG.getProperty("jop.wrongPass"));
                    }
                }, true);
            } else {
                changeMainPass();
            }
        });
        add(btnChangeMainPass, "settings.btn.changeMainPass");

        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(Color.white));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Color.black));

        JComboBox<String> comboBoxLanguage = new JComboBox<>();
        DefaultComboBoxModel<String> comboBoxLanguageModel = new DefaultComboBoxModel<>();
        comboBoxLanguage.setModel(comboBoxLanguageModel);
        comboBoxLanguage.setFont(raleway(15));
        List<Locale> locales = PassUtils.FileUtils.availableLanguages().stream().map(Locale::new).collect(Collectors.toList());
        locales.stream().map(Locale::getDisplayName).forEach(comboBoxLanguageModel::addElement);
        comboBoxLanguage.setSelectedItem(new Locale(PROPS.getProperty("lang")).getDisplayLanguage());
        comboBoxLanguage.addActionListener(action -> {
            // Change language
            locales.stream()
                    .filter(locale -> locale.getDisplayLanguage().equals(comboBoxLanguage.getSelectedItem()))
                    .forEach(locale -> PROPS.setProperty("lang", locale.getLanguage()));
            LOG.info("Changed property {} to {}!", "lang", PROPS.getProperty("lang"));
            try {
                PROPS.store(new FileWriter(Var.PROPERTIES), "PassVault settings");
            } catch (Exception e) {
                LOG.error("Error while saving properties!", e);
            }
            LANG = loadLang();
            textComponents();
        });
        comboBoxLanguage.setBackground(FOREGROUND);
        comboBoxLanguage.setForeground(BACKGROUND);
        comboBoxLanguage.setMaximumSize(comboBoxLanguage.getPreferredSize());
        add(comboBoxLanguage, "settings.combo.language");
    }

    /**
     * Adds a component with its name to the {@link de.finnik.gui.Var#COMPONENTS} map and adds the component to the panel
     * The method kind of overwrites {@link java.awt.Container#add(Component)} method in order to handle the components later
     *
     * @param c   The component
     * @param key The component´s matching name
     */
    private void add(JComponent c, String key) {
        COMPONENTS.put(key, c);
        components.add(c);
    }

    /**
     * Adds a set of {@link javax.swing.JComponent}s to the pane and creates a gap between them
     *
     * @param components The components
     */
    private void positionComponents(List<JComponent> components) {
        components.forEach(component -> {
            component.setAlignmentX(Component.CENTER_ALIGNMENT);
            getContentPane().add(component);
            getContentPane().add(Box.createVerticalGlue());
        });
    }
}
