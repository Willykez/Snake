/**
 * Manual decimal parsing/formatting.
 *
 * After discovering Math.random() doesn't exist in CLDC's stripped java.lang.Math,
 * it's not worth assuming Double.parseDouble/Double.toString behave the same as on
 * a desktop JDK either. Both are commonly present in CLDC 1.1, but this avoids the
 * question entirely using only integer arithmetic and java.lang.Long.toString,
 * which are unambiguously part of the spec.
 *
 * Limitations (fine for this app's calculator/converter/BMI use): plain decimal
 * strings only, optional leading '-', no exponents, no thousands separators.
 */
public final class NumberUtil {

    private NumberUtil() {
    }

    public static double parse(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        int i = 0;
        boolean negative = false;
        if (s.charAt(0) == '-') {
            negative = true;
            i = 1;
        }
        double intPart = 0;
        while (i < s.length() && s.charAt(i) != '.') {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                intPart = intPart * 10 + (c - '0');
            }
            i++;
        }
        double fracPart = 0;
        double fracDivisor = 1;
        if (i < s.length() && s.charAt(i) == '.') {
            i++;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    fracPart = fracPart * 10 + (c - '0');
                    fracDivisor *= 10;
                }
                i++;
            }
        }
        double value = intPart + (fracPart / fracDivisor);
        return negative ? -value : value;
    }

    /** Formats a double with a fixed number of decimal places. */
    public static String format(double value, int decimals) {
        boolean negative = value < 0;
        double v = negative ? -value : value;
        long scale = 1;
        for (int i = 0; i < decimals; i++) {
            scale *= 10;
        }
        long scaled = (long) (v * scale + 0.5);
        long intPart = scaled / scale;
        long fracPart = scaled % scale;
        StringBuffer sb = new StringBuffer();
        if (negative && scaled != 0) {
            sb.append('-');
        }
        sb.append(intPart);
        if (decimals > 0) {
            sb.append('.');
            String fracStr = Long.toString(fracPart);
            for (int i = fracStr.length(); i < decimals; i++) {
                sb.append('0');
            }
            sb.append(fracStr);
        }
        return sb.toString();
    }
}
