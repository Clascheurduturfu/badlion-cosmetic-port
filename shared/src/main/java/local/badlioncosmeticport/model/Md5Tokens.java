package local.badlioncosmeticport.model;

import java.io.IOException;
import java.io.Reader;

/** Bounded token reader for MD5Version 10 text assets, including quoted names and comments. */
final class Md5Tokens {
    private static final int MAX_TEXT = 32 * 1024 * 1024;
    private final String text;
    private int offset;
    private int line = 1;

    Md5Tokens(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[8192];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            if (result.length() + count > MAX_TEXT) throw new IOException("MD5 asset exceeds 32 MiB");
            result.append(buffer, 0, count);
        }
        text = result.toString();
    }

    boolean hasNext() throws IOException {
        skipWhitespace();
        return offset < text.length();
    }

    String next() throws IOException {
        skipWhitespace();
        if (offset == text.length()) throw error("Unexpected end of file");
        char c = text.charAt(offset++);
        if (c == '{' || c == '}' || c == '(' || c == ')') return Character.toString(c);
        StringBuilder token = new StringBuilder();
        if (c == '"') {
            while (offset < text.length()) {
                c = text.charAt(offset++);
                if (c == '"') return token.toString();
                if (c == '\n' || c == '\r') throw error("Newline in quoted token");
                if (c == '\\' && offset < text.length() && text.charAt(offset) == '"') {
                    token.append('"');
                    offset++;
                } else token.append(c);
            }
            throw error("Unclosed quoted token");
        }
        token.append(c);
        while (offset < text.length()) {
            c = text.charAt(offset);
            if (Character.isWhitespace(c) || "{}()".indexOf(c) >= 0 || c == '/') break;
            token.append(c);
            offset++;
        }
        return token.toString();
    }

    String peek() throws IOException {
        int savedOffset = offset, savedLine = line;
        String token = next(); offset = savedOffset; line = savedLine;
        return token;
    }

    void expect(String expected) throws IOException {
        String actual = next();
        if (!actual.equals(expected)) throw error("Expected '" + expected + "', got '" + actual + "'");
    }

    int integer() throws IOException {
        String token = next();
        try { return Integer.parseInt(token); }
        catch (NumberFormatException e) { throw error("Invalid integer: " + token); }
    }

    int count(int maximum, String label) throws IOException {
        int value = integer();
        if (value < 0 || value > maximum) throw error(label + " must be in [0, " + maximum + "]");
        return value;
    }

    float number() throws IOException {
        String token = next();
        try {
            float value = Float.parseFloat(token);
            if (!Float.isFinite(value)) throw error("Non-finite number: " + token);
            return value;
        } catch (NumberFormatException e) { throw error("Invalid number: " + token); }
    }

    Md5Model.Vec3 vector() throws IOException {
        expect("(");
        Md5Model.Vec3 value = new Md5Model.Vec3(number(), number(), number());
        expect(")");
        return value;
    }

    IOException error(String message) { return new IOException("MD5 line " + line + ": " + message); }

    private void skipWhitespace() throws IOException {
        while (offset < text.length()) {
            char c = text.charAt(offset);
            if (Character.isWhitespace(c)) {
                if (c == '\n') line++;
                offset++;
            } else if (c == '\uFEFF' && offset == 0) offset++;
            else if (c == '/' && offset + 1 < text.length() && text.charAt(offset + 1) == '/') {
                offset += 2;
                while (offset < text.length() && text.charAt(offset) != '\n') offset++;
            } else if (c == '/' && offset + 1 < text.length() && text.charAt(offset + 1) == '*') {
                offset += 2;
                while (offset + 1 < text.length() && !(text.charAt(offset) == '*' && text.charAt(offset + 1) == '/')) {
                    if (text.charAt(offset++) == '\n') line++;
                }
                if (offset + 1 >= text.length()) throw error("Unclosed comment");
                offset += 2;
            } else break;
        }
    }
}
