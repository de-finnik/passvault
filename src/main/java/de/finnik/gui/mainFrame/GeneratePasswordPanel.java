package de.finnik.gui.mainFrame;

import de.finnik.AES.RealRandom;
import de.finnik.gui.Var;
import de.finnik.gui.customComponents.slider.RangeSlider;
import de.finnik.gui.dialogs.SavePassDialog;
import de.finnik.passvault.PassProperty;
import de.finnik.passvault.passwords.PasswordGenerator;
import de.finnik.passvault.utils.PassUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.finnik.gui.Var.*;

/**
 * Generate a password by selecting the characters to use and the password length.
 */
public class GeneratePasswordPanel extends JPanel {

    /**
     * Creates the panel
     */
    public GeneratePasswordPanel() {
        setBackground(BACKGROUND);
        setLayout(null);

        components();
        textComponents();

        // Sets the color for all generated components
        PassUtils.GUIUtils.colorComponents(getMatchingComponents("generate"), FOREGROUND, BACKGROUND);

        // Loads the icons for the checkboxes
        Arrays.stream(getMatchingComponents("generate.checkBox"))
                .map(c -> ((JCheckBox) c))
                .forEach(checkBox -> {
                    checkBox.setIcon(new ImageIcon(NOT_SELECTED));
                    checkBox.setSelectedIcon(new ImageIcon(SELECTED));
                });

        // Sets the font for labels and checkboxes
        Arrays.stream(getMatchingComponents("generate.checkBox", "generate.lbl"))
                .forEach(c -> c.setFont(raleway(14)));

        // Sets the font for buttons
        Arrays.stream(getMatchingComponents("generate.btn"))
                .forEach(c -> c.setFont(raleway(15)));
    }


    /**
     * Generates the components
     */
    public void components() {
        JLabel lblHeadline = new JLabel();
        lblHeadline.setBounds(0, 0, 300, 30);
        add(lblHeadline, "generate.lbl.headline");

        RangeSlider sliderLength = new RangeSlider(5, 30, Integer.parseInt(PassProperty.GEN_LOW_LENGTH.getValue()), Integer.parseInt(PassProperty.GEN_UP_LENGTH.getValue()));
        sliderLength.setMajorTickSpacing(5);
        sliderLength.setMinorTickSpacing(1);
        sliderLength.setPaintTicks(true);
        sliderLength.setPaintLabels(true);
        sliderLength.setBounds(0, 40, 300, 50);
        sliderLength.addChangeListener(e -> {
            PassProperty.GEN_LOW_LENGTH.setValue(sliderLength.getValue(), PassFrame.aes);
            PassProperty.GEN_UP_LENGTH.setValue(sliderLength.getUpperValue(), PassFrame.aes);
        });
        add(sliderLength, "generate.slider.length");

        UIManager.put("CheckBox.focus", BACKGROUND);

        JCheckBox checkBoxBigLetters = new JCheckBox();
        checkBoxBigLetters.setSelected(Boolean.parseBoolean(PassProperty.GEN_BIG.getValue()));
        checkBoxBigLetters.setBounds(0, 100, 300, 30);
        checkBoxBigLetters.addActionListener(e -> PassProperty.GEN_BIG.setValue(checkBoxBigLetters.isSelected(), PassFrame.aes));
        add(checkBoxBigLetters, "generate.checkBox.bigLetters");

        JCheckBox checkBoxSmallLetters = new JCheckBox();
        checkBoxSmallLetters.setSelected(Boolean.parseBoolean(PassProperty.GEN_SMALL.getValue()));
        checkBoxSmallLetters.setBounds(0, 130, 300, 30);
        checkBoxSmallLetters.addActionListener(e -> PassProperty.GEN_SMALL.setValue(checkBoxSmallLetters.isSelected(), PassFrame.aes));
        add(checkBoxSmallLetters, "generate.checkBox.smallLetters");

        JCheckBox checkBoxNumbers = new JCheckBox();
        checkBoxNumbers.setSelected(Boolean.parseBoolean(PassProperty.GEN_NUM.getValue()));
        checkBoxNumbers.setBounds(0, 160, 300, 30);
        checkBoxNumbers.addActionListener(e -> PassProperty.GEN_NUM.setValue(checkBoxNumbers.isSelected(), PassFrame.aes));
        add(checkBoxNumbers, "generate.checkBox.numbers");

        JCheckBox checkBoxSpecialCharacters = new JCheckBox();
        checkBoxSpecialCharacters.setSelected(Boolean.parseBoolean(PassProperty.GEN_SPE.getValue()));
        checkBoxSpecialCharacters.setBounds(0, 190, 300, 30);
        checkBoxSpecialCharacters.addActionListener(e -> PassProperty.GEN_SPE.setValue(checkBoxSpecialCharacters.isSelected(), PassFrame.aes));
        add(checkBoxSpecialCharacters, "generate.checkBox.specials");

        JTextField tfPass = new JTextField();

        JButton btnGenerate = new JButton();
        btnGenerate.setBounds(75, 230, 150, 30);
        btnGenerate.addActionListener(action -> {
            List<PasswordGenerator.PassChars> chars = new ArrayList<>();
            if (checkBoxBigLetters.isSelected()) {
                chars.add(PasswordGenerator.PassChars.BIG_LETTERS);
            }
            if (checkBoxSmallLetters.isSelected()) {
                chars.add(PasswordGenerator.PassChars.SMALL_LETTERS);
            }
            if (checkBoxNumbers.isSelected()) {
                chars.add(PasswordGenerator.PassChars.NUMBERS);
            }
            if (checkBoxSpecialCharacters.isSelected()) {
                chars.add(PasswordGenerator.PassChars.SPECIAL_CHARACTERS);
            }
            if (chars.size() > 0) {
                if (Boolean.parseBoolean(PassProperty.REAL_RANDOM.getValue())) {
                    generate(RealRandom.seedWithUserInput(FRAME, LANG.getString("generate.jop.realRandom")), chars, sliderLength, tfPass);
                } else {
                    generate(-1, chars, sliderLength, tfPass);
                }
            } else {
                DIALOG.message(FRAME, LANG.getString("generate.jop.insufficientChars"));
            }
        });
        add(btnGenerate, "generate.btn.generate");

        tfPass.setBounds(0, 270, 300, 40);
        tfPass.setEditable(true);
        tfPass.setHorizontalAlignment(SwingConstants.CENTER);
        add(tfPass, "generate.tf.pass");

        JButton btnSavePass = new JButton();
        btnSavePass.setBounds(25, 320, 250, 30);
        btnSavePass.addActionListener(action -> {
            JDialog savePassDialog = new SavePassDialog(FRAME, tfPass.getText());
            COMPONENTS.put("savePass", savePassDialog);
            savePassDialog.setVisible(true);
        });
        add(btnSavePass, "generate.btn.savePass");
    }

    /**
     * Adds a component with its name to the {@link Var#COMPONENTS} map and adds the component to the panel
     * The method kind of overwrites {@link Container#add(Component)} method in order to handle the components later
     *
     * @param c   The component
     * @param key The component´s matching name
     */
    private void add(Component c, String key) {
        COMPONENTS.put(key, c);
        add(c);
    }

    /**
     * Generates a password via {@link PasswordGenerator#generatePassword(int, int, PasswordGenerator.PassChars...)}
     *
     * @param seed         A seed if the user activated {@link PassProperty#REAL_RANDOM} or {@code -1}
     * @param chars        A list of {@link de.finnik.passvault.passwords.PasswordGenerator.PassChars} that the user selected
     * @param sliderLength The {@link RangeSlider} containing lower and upper password lengths selected by the user
     * @param tfPass       The TextField where the generated password should be displayed
     */
    private void generate(long seed, List<PasswordGenerator.PassChars> chars, RangeSlider sliderLength, JTextField tfPass) {
        tfPass.setFont(raleway(20));
        PasswordGenerator generator = seed >= 0 ? new PasswordGenerator(seed) : new PasswordGenerator();
        String pass = generator.generatePassword(sliderLength.getValue(), sliderLength.getUpperValue(), chars.toArray(new PasswordGenerator.PassChars[0]));
        while (getFontMetrics(tfPass.getFont()).stringWidth(pass) + 10 > tfPass.getWidth()) {
            tfPass.setFont(tfPass.getFont().deriveFont((float) tfPass.getFont().getSize() - 1));
        }
        tfPass.setText(pass);
        LOG.info("Generated password!");
    }
}
