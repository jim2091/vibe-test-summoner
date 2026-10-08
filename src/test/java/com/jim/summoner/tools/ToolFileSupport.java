package com.jim.summoner.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** File operations shared only by the review, asset and promotion tools. */
final class ToolFileSupport {
    static final List<String> NORMALIZED = List.of("monsters", "families", "skills", "skill-effects", "leader-skills", "monster-sources");
    static final List<String> LOCALIZED = List.of("monsters", "skills", "skill-effects", "monster-sources");
    static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    static Path root() throws IOException {
        Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (path != null) {
            if (Files.isRegularFile(path.resolve("pom.xml"))) return path.toRealPath();
            path = path.getParent();
        }
        throw new IOException("Project root not found");
    }

    static Path path(Path root, String relative) throws IOException {
        Path target = root.resolve(relative).normalize().toAbsolutePath();
        if (!target.startsWith(root) || target.equals(root)) throw new IOException("Unsafe path: " + relative);
        Path cursor = root;
        for (Path part : root.relativize(target)) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) throw new IOException("Symbolic links are not supported: " + cursor);
            if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && !cursor.toRealPath().startsWith(root)) {
                throw new IOException("Path escapes project: " + cursor);
            }
        }
        return target;
    }

    static Object read(Path file) throws IOException {
        try {
            JsonNode tree = JSON.readTree(Files.readString(file));
            if (tree == null || tree.isNull()) throw new IOException("Empty/null JSON root");
            return JSON.readValue(tree.toString(), Object.class);
        } catch (Exception failure) {
            throw new IOException("Cannot read JSON: " + file + ": " + failure.getMessage(), failure);
        }
    }

    static Map<?, ?> object(Object value, String context) throws IOException {
        if (!(value instanceof Map<?, ?> map)) throw new IOException("Expected object: " + context);
        return map;
    }

    static List<?> array(Object value, String context) throws IOException {
        if (!(value instanceof List<?> list)) throw new IOException("Expected array: " + context);
        return list;
    }

    static int id(Object value, String context) throws IOException {
        if (!(value instanceof Integer id) || id <= 0) throw new IOException("Invalid positive internal ID: " + context);
        return id;
    }

    static Map<Integer, Object> entities(Path file, boolean localized, boolean optional) throws IOException {
        Map<Integer, Object> result = new TreeMap<>();
        if (!Files.exists(file) && optional) return result;
        Object input = read(file);
        if (localized) {
            for (var entry : object(input, file.toString()).entrySet()) {
                String key = String.valueOf(entry.getKey());
                int id;
                try {
                    if (!key.matches("[1-9][0-9]*")) throw new NumberFormatException();
                    id = Integer.parseInt(key);
                } catch (NumberFormatException failure) { throw new IOException("Invalid localization ID: " + file + ": " + key, failure); }
                object(entry.getValue(), file + ": " + key);
                if (result.putIfAbsent(id, entry.getValue()) != null) throw new IOException("Duplicate ID: " + file + ": " + key);
            }
        } else {
            for (Object item : array(input, file.toString())) {
                int id = id(object(item, file.toString()).get("id"), file.toString());
                if (result.putIfAbsent(id, item) != null) throw new IOException("Duplicate ID: " + file + ": " + id);
            }
        }
        return result;
    }

    static Set<String> options(String[] args, String... allowed) {
        Set<String> valid = Set.of(allowed);
        Set<String> selected = new java.util.HashSet<>();
        for (String arg : args) {
            if (!valid.contains(arg)) throw new IllegalArgumentException("Unknown argument: " + arg + "; allowed: " + valid);
            selected.add(arg);
        }
        return selected;
    }

    static String hash(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            for (int count; (count = in.read(buffer)) != -1;) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static Map<String, String> assets(Path root, String relativeDirectory) throws Exception {
        Path directory = path(root, relativeDirectory);
        Map<String, String> hashes = new TreeMap<>();
        if (!Files.exists(directory)) return hashes;
        try (var stream = Files.walk(directory)) {
            for (Path item : stream.toList()) {
                path(root, root.relativize(item).toString());
                if (Files.isDirectory(item)) continue;
                String name = directory.relativize(item).toString().replace('\\', '/');
                if (!name.matches("(?:monsters/monster-|skills/skill-|effects/effect-)[1-9][0-9]*\\.png")) {
                    throw new IOException("Unexpected asset filename: " + item);
                }
                hashes.put(name, hash(item));
            }
        }
        return hashes;
    }

    static boolean png(byte[] bytes) {
        return bytes.length >= PNG.length && Arrays.equals(Arrays.copyOf(bytes, PNG.length), PNG);
    }

    static boolean png(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return false;
        try (InputStream input = Files.newInputStream(file)) { return png(input.readNBytes(PNG.length)); }
    }

    static void write(Path root, Path target, String text) throws IOException {
        write(root, target, text.getBytes(StandardCharsets.UTF_8));
    }

    static void write(Path root, Path target, byte[] bytes) throws IOException {
        path(root, root.relativize(target).toString());
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".candidate-", ".tmp");
        try {
            Files.write(temp, bytes);
            replace(temp, target);
        } finally { Files.deleteIfExists(temp); }
    }

    static void copy(Path root, Path source, Path target) throws IOException {
        path(root, root.relativize(source).toString());
        path(root, root.relativize(target).toString());
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".promotion-", ".tmp");
        try {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            replace(temp, target);
        } finally { Files.deleteIfExists(temp); }
    }

    private static void replace(Path temp, Path target) throws IOException {
        try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException failure) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
    }

    static void report(Path root, String relativeStem, Object data, String markdown) throws IOException {
        write(root, path(root, relativeStem + ".json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(data) + "\n");
        write(root, path(root, relativeStem + ".md"), markdown);
    }

    static String md(Object value) {
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("|", "&#124;").replace("`", "&#96;").replace("\r", "").replace("\n", "<br>");
    }
}
