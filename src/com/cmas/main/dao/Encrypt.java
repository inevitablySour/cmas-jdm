package com.cmas.main.dao;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class Encrypt {

    private static final String SECRET_KEY = "1234567890123456"; // store this safely

    // Encrypts config.properties and writes to config.enc
    public static void encryptConfigFile(String inputPath, String outputPath) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(inputPath));

        StringBuilder data = new StringBuilder();
        for (String name : props.stringPropertyNames()) {
            data.append(name).append("=").append(props.getProperty(name)).append("\n");
        }

        byte[] encrypted = encrypt(data.toString(), SECRET_KEY);
        Files.write(Paths.get(outputPath), encrypted);

        System.out.println("Encrypted config written to " + outputPath);
    }

    // Decrypts config.enc and returns a Properties object
    public static Properties loadDecryptedConfig(String path) throws Exception {
        byte[] encrypted = Files.readAllBytes(Paths.get(path));
        String decrypted = decrypt(encrypted, SECRET_KEY);

        Properties props = new Properties();
        try (StringReader reader = new StringReader(decrypted)) {
            props.load(reader);
        }
        return props;
    }

    private static byte[] encrypt(String data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"));
        return cipher.doFinal(data.getBytes());
    }

    private static String decrypt(byte[] data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"));
        byte[] decrypted = cipher.doFinal(data);
        return new String(decrypted);
    }

}