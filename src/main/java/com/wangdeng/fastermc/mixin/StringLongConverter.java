package com.wangdeng.fastermc.mixin;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author 王澄
 **/
public class StringLongConverter {

    final static char[] DIGIT_CHARS = {
            '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W', 'X', 'Y', 'Z', '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final int MAX_INDEX = 122;
    private static final long NUM_114514 = 114514L;
    private final static int[] CHAR_DIGITS = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1,
            -1, -1, -1, -1, -1, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, -1, -1, -1,
            -1, 37, -1, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final Map<Long, String> extraMap = new ConcurrentHashMap<>();

    public static void main1(String[] args) {
        // 准确率测试通过
        Random r = new Random();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            long n = Math.abs(r.nextLong());
            if (toLong(toString(n)) != n) {
                System.out.println("error");
                break;
            }
        }
    }

    public static String toString(long i) {
        if (i < 0) {
            return extraMap.get(i);
        }
        char[] buf = new char[11];
        i = -i;
        int charPos = 10;
        while (i <= -64) {
            buf[charPos--] = DIGIT_CHARS[(int) (-(i % 64))];
            i = i / 64;
        }
        buf[charPos] = DIGIT_CHARS[(int) (-i)];
        return new String(buf, charPos, (11 - charPos));
    }

    public static long toLong(String s) {
        int len = s.length();
        if (len > 11) {
            return hash(s, NUM_114514);
        }
        if (len == 11 && s.charAt(0) > '6') {
            return hash(s, NUM_114514);
        }
        long result = 0;
        int i = 0;
        int digit;
        char firstChar = s.charAt(0);
        while (i < len) {
            digit = charToInt(s.charAt(i++));
            if (digit < 0) {
                return hash(s, NUM_114514);
            }
            result *= 64;
            result -= digit;
        }
        return -result;

    }

    private static long hash(String s, long num) {
        long h = num;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        if (h == 0) {
            h = -num;
        } else if (h > 0) {
            h = -h;
        }
        if (extraMap.containsKey(h) && !s.equals(extraMap.get(h))) {
            // 哈希碰撞概率概率 1/2^63
            return hash(s, num + 1);
        }
        extraMap.put(h, s);
        return h;
    }

    private static int charToInt(char code) {
        if (code > MAX_INDEX) {
            return -1;
        }
        return CHAR_DIGITS[code];
    }

    public static void main(String[] args) {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String str = RandomStringUtils.randomAlphanumeric(10);
            if (!str.equals(toString(toLong(str)))) {
                System.exit(-1);
            }
        } // 长度小于11或长度等于11且开头小于7的字母数字下划线减号都不占用map
        Runtime rt = Runtime.getRuntime();
        System.out.println((rt.totalMemory() - rt.freeMemory()) / 1048576L + "MB");
    }

}
