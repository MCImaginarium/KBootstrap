package pw.prok.bootstrap;

import java.io.*;
import java.security.MessageDigest;

public class Utils {
    public static String binToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            builder.append(String.format("%02X", b & 0xFF));
        return builder.toString();
    }

    public static String readFile(File file) {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            reader.close();
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sha1(File file) {
        return digest("SHA-1", file);
    }

    public static String md5(File file) {
        return digest("MD5", file);
    }

    public static String digest(String algorithm, File file) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm.toUpperCase());
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[4096];
            int c;
            while ((c = is.read(buffer)) > 0) {
                md.update(buffer, 0, c);
            }
            is.close();
            return Utils.binToHex(md.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void removeDir(File dir) {
        if (dir == null) return;
        File[] files = dir.listFiles();
        if (files != null && files.length != 0) {
            for (File f : files) {
                if (f.isDirectory()) {
                    removeDir(f);
                }
                f.delete();
            }
        }
        dir.delete();
    }

    public static void writeToFile(File file, String s) {
        try {
            file.getParentFile().mkdirs();
            OutputStream os = new FileOutputStream(file);
            Writer writer = new OutputStreamWriter(os, "utf-8");
            writer.write(s);
            writer.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File checksumFile(String algo, File file) {
        return new File(file.getAbsolutePath() + "." + algo);
    }

    public static String readChecksum(String algo, File file) {
        String checksum = readFile(checksumFile(algo, file));
        return checksum == null ? null : checksum.trim();
    }

    public static void writeChecksum(String algo, File file) {
        writeToFile(checksumFile(algo, file), digest(algo, file));
    }

    public static void copyFile(File in, File out) throws IOException {
        copyStream(new FileInputStream(in), new FileOutputStream(out));
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = new byte[4096];
        int c;
        while ((c = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, c);
        }
        outputStream.close();
        inputStream.close();
    }
}
