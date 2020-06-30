import de.finnik.passvault.passwords.PasswordGenerator;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.finnik.passvault.passwords.PasswordGenerator.PassChars;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PasswordGeneratorTest {
    @Test
    public void testBigLetters() {
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", PassChars.BIG_LETTERS.get());
    }

    @Test
    public void testSmallLetters() {
        assertEquals("abcdefghijklmnopqrstuvwxyz", PassChars.SMALL_LETTERS.get());
    }

    @Test
    public void testNumbers() {
        assertEquals("0123456789", PassChars.NUMBERS.get());
    }

    @Test
    public void testSpecials() {
        assertEquals("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~", PassChars.SPECIAL_CHARACTERS.get());
    }

    @Test
    public void testLengthOfGeneratedPassword() {
        int random;
        PasswordGenerator generator = new PasswordGenerator();
        for (int z = 0; z < 5; z++) {
            random = (int) (Math.random() * 100) + 1;
            assertEquals(random, generator.generatePassword(random, PassChars.BIG_LETTERS).length());
        }
    }

    @Test
    public void testGenerateContainsMatchingChars() {
        List<PassChars> all = Arrays.asList(PassChars.values());
        PasswordGenerator generator = new PasswordGenerator();
        for (int i = 0; i <= 500; i++) {
            Collections.shuffle(all);
            String password;
            password = generator.generatePassword((int) (Math.random() * 30) + 1, all.get(0));
            if (Arrays.stream(all.get(0).get().split("")).noneMatch(password::contains)) {
                fail();
            }
        }
    }
}
