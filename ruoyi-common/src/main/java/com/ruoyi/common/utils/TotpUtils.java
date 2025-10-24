package com.ruoyi.common.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TOTP (Time-based One-Time Password) 工具类
 * 用于生成和验证基于时间的一次性密码
 * 
 * @author ruoyi
 */
public class TotpUtils
{
    // 时间步长（秒）
    private static final int TIME_STEP = 30;
    
    // 验证码位数
    private static final int CODE_DIGITS = 6;
    
    // HMAC算法
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    
    // Base32字符集
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    
    // 允许的时间偏移（前后各1个时间步长）
    private static final int TIME_WINDOW = 1;

    /**
     * 生成随机密钥
     * 
     * @return Base32编码的密钥
     */
    public static String generateSecretKey()
    {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 生成当前时间的TOTP验证码
     * 
     * @param secretKey Base32编码的密钥
     * @return 6位验证码
     */
    public static String generateCode(String secretKey)
    {
        long timeIndex = System.currentTimeMillis() / 1000 / TIME_STEP;
        return generateCode(secretKey, timeIndex);
    }

    /**
     * 根据时间索引生成TOTP验证码
     * 
     * @param secretKey Base32编码的密钥
     * @param timeIndex 时间索引
     * @return 6位验证码
     */
    private static String generateCode(String secretKey, long timeIndex)
    {
        try
        {
            byte[] key = base32Decode(secretKey);
            byte[] data = ByteBuffer.allocate(8).putLong(timeIndex).array();
            
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);
            
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            throw new RuntimeException("生成TOTP验证码失败", e);
        }
    }

    /**
     * 验证TOTP验证码
     * 
     * @param secretKey Base32编码的密钥
     * @param code 用户输入的验证码
     * @return 验证是否通过
     */
    public static boolean verifyCode(String secretKey, String code)
    {
        if (StringUtils.isEmpty(secretKey) || StringUtils.isEmpty(code))
        {
            return false;
        }
        
        long timeIndex = System.currentTimeMillis() / 1000 / TIME_STEP;
        
        // 允许时间窗口内的验证码
        for (int i = -TIME_WINDOW; i <= TIME_WINDOW; i++)
        {
            String generatedCode = generateCode(secretKey, timeIndex + i);
            if (code.equals(generatedCode))
            {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 生成Google Authenticator使用的二维码URL
     * 
     * @param account 账号名称
     * @param issuer 发行者（应用名称）
     * @param secretKey Base32编码的密钥
     * @return otpauth URL
     */
    public static String getQrCodeUrl(String account, String issuer, String secretKey)
    {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, account, secretKey, issuer, CODE_DIGITS, TIME_STEP);
    }

    /**
     * Base32编码
     * 
     * @param data 原始数据
     * @return Base32编码字符串
     */
    private static String base32Encode(byte[] data)
    {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bufferLength = 0;
        
        for (byte b : data)
        {
            buffer = (buffer << 8) | (b & 0xFF);
            bufferLength += 8;
            
            while (bufferLength >= 5)
            {
                int index = (buffer >> (bufferLength - 5)) & 0x1F;
                result.append(BASE32_CHARS.charAt(index));
                bufferLength -= 5;
            }
        }
        
        if (bufferLength > 0)
        {
            int index = (buffer << (5 - bufferLength)) & 0x1F;
            result.append(BASE32_CHARS.charAt(index));
        }
        
        // 添加填充
        while (result.length() % 8 != 0)
        {
            result.append('=');
        }
        
        return result.toString();
    }

    /**
     * Base32解码
     * 
     * @param encoded Base32编码字符串
     * @return 原始数据
     */
    private static byte[] base32Decode(String encoded)
    {
        // 移除填充
        encoded = encoded.replaceAll("=", "").toUpperCase();
        
        if (encoded.isEmpty())
        {
            return new byte[0];
        }
        
        int outputLength = encoded.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        int buffer = 0;
        int bufferLength = 0;
        int resultIndex = 0;
        
        for (char c : encoded.toCharArray())
        {
            int value = BASE32_CHARS.indexOf(c);
            if (value == -1)
            {
                throw new IllegalArgumentException("非法的Base32字符: " + c);
            }
            
            buffer = (buffer << 5) | value;
            bufferLength += 5;
            
            if (bufferLength >= 8)
            {
                result[resultIndex++] = (byte) (buffer >> (bufferLength - 8));
                bufferLength -= 8;
            }
        }
        
        return result;
    }
}

