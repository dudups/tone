package com.ezone.ezproject.modules.card.rank;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典序排序器
 */
@Builder
public class DictRanker {
    public static final String DEFAULT_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final DictRanker DEFAULT = DictRanker.builder().chars(DEFAULT_CHARS).build();

    /**
     * 想要返回结果字典有序，chars必须字典有序！！！
     */
    private String chars = DEFAULT_CHARS;

    /**
     * 按字典序从start和end中间返回num个有序序列
     * @param start
     * @param end
     * @param num
     * @return
     */
    public List<String> ranks(String start, String end, int num) {
        List<String> ranks = new ArrayList<>();
        if (start.compareTo(end) >= 0) {
            throw new IllegalArgumentException("Rank require params: start < end!");
        }
        if (num <= 0) {
            throw new IllegalArgumentException("Rank require param: num >= 1!");
        }
        String prefix = prefix(start, end);
        // 去掉共同前缀，防止下面转换long时太大溢出
        int length = Math.max(start.length(), end.length()) - prefix.length();
        long startNum = dict2Long(start.substring(prefix.length()), length);
        long endNum = dict2Long(end.substring(prefix.length()), length);
        // 留出num个位置需要满足条件：end - start -1 >= num
        while (endNum - startNum -1 < num) {
            startNum *= chars.length();
            endNum *= chars.length();
            length ++;
        }
        // 无法均匀插入时的策略:
        // 当前策略：前面n-1个小step，最后距离end较远>>所以"1"和"4"之间填充1个返回"2"而非"3"
        // 另一个策略：step = (endNum - startNum -1) / num; step较大，最后距离end较近>>所以"1"和"4"之间填充1个返回"3"而非"2"
        long step = (endNum - startNum) / (num + 1);
        for (int i = 1; i <= num; i++) {
            ranks.add(new StringBuilder(prefix)
                    .append(long2Dict(startNum + i * step, length, false))
                    .toString());
        }
        return ranks;
    }

    /**
     * 字典序加1，如1下一个是2，1z下一个是20;
     * 如果length为2：则111下一个是12
     * @param rank
     * @param length
     * @return
     */
    public String next(String rank, int length) {
        if (rank.length() < length) {
            rank = StringUtils.repeat(chars.charAt(0), length - rank.length()) + rank;
        }
        return long2Dict(dict2Long(rank, length) + 1, length, true);
    }

    /**
     * see this.next(String rank, int length)
     * @return 升序
     */
    public List<String> nextRanks(String rank, int length, int num) {
        if (rank.length() < length) {
            rank = StringUtils.repeat(chars.charAt(0), length - rank.length()) + rank;
        }
        List<String> ranks = new ArrayList<>();
        long base = dict2Long(rank, length);
        for (int i = 1; i <= num; i++) {
            ranks.add(long2Dict(base + i, length, true));
        }
        return ranks;
    }

    /**
     * 返回共同前缀
     * @param start
     * @param end
     * @return
     */
    String prefix(String start, String end) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < start.length(); i++) {
            if (start.charAt(i) == end.charAt(i)) {
                sb.append(start.charAt(i));
            } else {
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 思路是按进制转换10进制为chars.length进制
     * @param number
     * @param length
     * @param retain0Suffix
     * @return
     */
    String long2Dict(long number, int length, boolean retain0Suffix) {
        StringBuilder dict = new StringBuilder();
        boolean is0Continuous = true;
        int dictLength = 0;
        while (number > 0) {
            dictLength ++;
            int index = (int)(number % chars.length());
            is0Continuous = is0Continuous && index ==0;
            if (retain0Suffix || !is0Continuous) {
                dict.insert(0, chars.charAt(index));
            }
            number /= chars.length();
        }
        if (dictLength > length) {
            throw new IllegalArgumentException(String.format("Param number:[%s] is too large for length:[%s]!", number, length));
        }
        if (dictLength < length) {
            dict.insert(0, StringUtils.repeat(chars.charAt(0), length - dictLength));
        }
        return dict.toString();
    }

    /**
     * 思路是按进制转换chars.length进制为10进制
     * @param dict
     * @param length
     * @return
     */
    long dict2Long(String dict, int length) {
        long number = 0;
        for (int i = 0; i < length; i++) {
            number *= chars.length();
            if (i < dict.length()) {
                int index = chars.indexOf(dict.charAt(i));
                if (index < 0) {
                    throw new IllegalArgumentException(String.format("Invalid char:[%s]!", dict.charAt(i)));
                }
                number += index;
            }
        }
        return number;
    }
}
