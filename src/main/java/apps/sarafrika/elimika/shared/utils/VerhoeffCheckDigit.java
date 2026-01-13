package apps.sarafrika.elimika.shared.utils;

public final class VerhoeffCheckDigit {
    private static final int[][] D_TABLE = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    private static final int[][] P_TABLE = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    private static final int[] INV_TABLE = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

    private VerhoeffCheckDigit() {
    }

    public static int compute(String numericValue) {
        if (numericValue == null || numericValue.isBlank()) {
            throw new IllegalArgumentException("Numeric value is required");
        }

        int c = 0;
        int length = numericValue.length();
        for (int i = 0; i < length; i++) {
            char ch = numericValue.charAt(length - 1 - i);
            if (ch < '0' || ch > '9') {
                throw new IllegalArgumentException("Numeric value must contain digits only");
            }
            int digit = ch - '0';
            c = D_TABLE[c][P_TABLE[i % 8][digit]];
        }
        return INV_TABLE[c];
    }

    public static String append(String numericValue) {
        return numericValue + compute(numericValue);
    }
}
