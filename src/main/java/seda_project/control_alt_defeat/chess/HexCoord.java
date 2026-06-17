package seda_project.control_alt_defeat.chess;

public class HexCoord {
    public static boolean isValid(int q, int r) {
        return Math.abs(q) <= 5 && Math.abs(r) <= 5 && (q + r) >= -5 && (q + r) <= 5;
    }

    public static String toAlgebraic(int q, int r) {
        if (!isValid(q, r)) {
            return null;
        }
        String files = "abcdefghikl";
        int fileIndex = q + 5;
        if (fileIndex < 0 || fileIndex >= files.length()) {
            return null;
        }
        char fileChar = files.charAt(fileIndex);
        int rank = (q < 0) ? (r + 6 + q) : (r + 6);
        return "" + fileChar + rank;
    }

    public static int[] parseAlgebraic(String code) {
        if (code == null || code.length() < 2) {
            return null;
        }
        char fileChar = code.charAt(0);
        int rank;
        try {
            rank = Integer.parseInt(code.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }

        String files = "abcdefghikl";
        int fileIndex = files.indexOf(fileChar);
        if (fileIndex == -1) {
            return null;
        }
        int q = fileIndex - 5;
        int r = (q < 0) ? (rank - 6 - q) : (rank - 6);

        if (!isValid(q, r)) {
            return null;
        }
        return new int[]{q, r};
    }
}
