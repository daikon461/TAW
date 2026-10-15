package dev.dualpower;
public class PowerIdSmokeTest {
    public static void main(String[] args) {
        if (!"awakened:pyro".equals(PowerIdValidator.requireValid("awakened:pyro"))) throw new AssertionError();
        for (String s : new String[]{"", "Fire", "../../foo", "awakened:bad space", "awakened:"}) {
            try { PowerIdValidator.requireValid(s); throw new AssertionError("Accepted: " + s); }
            catch (IllegalArgumentException expected) {}
        }
        System.out.println("PASS: power identifier validation");
    }
}
