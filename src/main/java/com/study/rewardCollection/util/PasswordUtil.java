package com.study.rewardCollection.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

public final class PasswordUtil {
    private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
    // 默认迭代次数
    private static final int ITERATIONS = 600_000;
    // 输出长度：256位，即32字节
    private static final int KEY_LENGTH = 256;
    // 盐长度：32字节
    private static final int SALT_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    // 保存到数据库：算法:迭代次数:输出位数
    public static final String ALGORITHM = JCA_ALGORITHM + ":" + ITERATIONS + ":" + KEY_LENGTH;

    private PasswordUtil() {
    }

    /**
     * 生成随机盐，返回64字符的十六进制字符串。
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return HEX.formatHex(salt);
    }

    /**
     * 注册、重置密码时使用。
     * 返回64字符的十六进制哈希。
     */
    public static String hashPassword(String password, String salt) {
        return HEX.formatHex(derive(password, salt, ITERATIONS));
    }

    /**
     * 登录校验。
     *
     * password：本次输入的原始密码
     * salt：数据库中的盐
     * storedHash：数据库中的密码哈希
     * algorithm：数据库中的算法标识
     */
    public static boolean verifyPassword(String password, String salt, String storedHash, String algorithm) {
        if (password == null || salt == null || storedHash == null || algorithm == null) {
            return false;
        }
        try {
            String[] parts = algorithm.split(":", -1);
            if (parts.length != 3 || !JCA_ALGORITHM.equals(parts[0])) {
                return false;
            }
            // 按该用户保存的参数进行校验
            int iterations = Integer.parseInt(parts[1]);
            int keyLength = Integer.parseInt(parts[2]);
            if (iterations < 1 || iterations > 5_000_000 || keyLength != KEY_LENGTH) {
                return false;
            }
            byte[] expected = HEX.parseHex(storedHash);
            if (expected.length != KEY_LENGTH / 8) {
                return false;
            }
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // 盐、哈希或算法参数格式异常
            return false;
        }
    }

    /**
     * 执行PBKDF2计算。
     */
    private static byte[] derive(String password, String salt, int iterations) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("密码和盐不能为空");
        }
        byte[] saltBytes = HEX.parseHex(salt);
        if (saltBytes.length != SALT_LENGTH) {
            throw new IllegalArgumentException("盐必须为32字节");
        }
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, iterations, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(JCA_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}