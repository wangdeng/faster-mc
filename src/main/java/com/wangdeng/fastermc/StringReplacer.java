package com.wangdeng.fastermc;

import java.util.*;

/**
 * @author 王澄
 * @create 2022-06-17 22:46
 **/
public class StringReplacer {
    private static class CharNode {
        public List<PatternInfo> infos;
        CharNode failNode;
        String pattern = null;
        Map<Character, CharNode> children = new HashMap<>();

        CharNode get(Character key) {
            return children.get(key);
        }

        public CharNode findFailNode(Character key) {
            CharNode next = failNode;
            while (next != null && !next.children.containsKey(key)) {
                next = next.failNode;
            }
            return next;
        }
    }

    private static class KeyWordInfo {
        String prefix;
        String suffix;

        public KeyWordInfo(String a, String b) {
            prefix = a;
            suffix = b;
        }
    }

    private static class PatternInfo {
        char[] charArr;
        int[] nextArr;
        int suffixLen;
        int prefixLen;

        private PatternInfo(KeyWordInfo keyWordInfo) {
            prefixLen = keyWordInfo.prefix.length();
            String suffix = keyWordInfo.suffix;
            suffixLen = suffix.length();
            charArr = suffix.toCharArray();
            nextArr = buildNextArr(charArr, suffixLen);
        }

        private int[] buildNextArr(char[] p, int pLen) {
            // 已知next[j] = k,利用递归的思想求出next[j+1]的值
            // 如果已知next[j] = k,如何求出next[j+1]呢?具体算法如下:
            // 1. 如果p[j] = p[k], 则next[j+1] = next[k] + 1;
            // 2. 如果p[j] != p[k], 则令k=next[k],如果此时p[j]==p[k],则next[j+1]=k+1,
            // 如果不相等,则继续递归前缀索引,令 k=next[k],继续判断,直至k=-1(即k=next[0])或者p[j]=p[k]为止
            int[] next = new int[pLen];
            int k = -1;
            int j = 0;
            next[0] = -1; // next数组中next[0]为-1
            while (j < pLen - 1) {
                if (k == -1 || p[j] == p[k]) {
                    k++;
                    j++;
                    // 修改next数组求法
                    if (p[j] != p[k]) {
                        next[j] = k;// KMPStringMatcher中只有这一行
                    } else {
                        // 不能出现p[j] = p[next[j]],所以如果出现这种情况则继续递归,如 k = next[k],
                        // k = next[[next[k]]
                        next[j] = next[k];
                    }
                } else {
                    k = next[k];
                }
            }
            return next;
        }
    }

    private final CharNode root = new CharNode();

    public void build(List<String> wordList, List<KeyWordInfo> keyWordInfoList) {
        Map<String, Set<PatternInfo>> map = new HashMap<>();
        for (KeyWordInfo info : keyWordInfoList) {
            String prefix = info.prefix;
            String suffix = info.suffix;
            Set<PatternInfo> set;
            if (map.containsKey(prefix)) {
                set = map.get(prefix);
            } else {
                set = new HashSet<>();
                map.put(prefix, set);
            }
            set.add(new PatternInfo(info));
        }
        Set<String> set = map.keySet();
        StringBuilder sb = new StringBuilder();
        for (String word : wordList) {
            int len = word.length();
            sb.append(word);
            for (Map.Entry<String, Set<PatternInfo>> e : map.entrySet()) {
                sb.append(e.getKey());
                insert(sb.toString(), e.getValue());
                sb.delete(len, sb.length());
            }
            sb.delete(0, sb.length());
        }
        getFail();
    }

    private void insert(String word, Set<PatternInfo> infos) {
        CharNode current = root;
        for (int i = 0; i < word.length(); i++) {
            Character key = word.charAt(i);
            CharNode value;
            Map<Character, CharNode> children = current.children;
            if (children.containsKey(key)) {
                value = children.get(key);
            } else {
                value = new CharNode();
                children.put(key, value);
            }
            current = value;
        }
        current.pattern = word;
        current.infos = new ArrayList<>(infos);
    }

    private void getFail() {
        Queue<CharNode> queue = new ArrayDeque<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            CharNode current = queue.poll();
            Map<Character, CharNode> children = current.children;
            for (Map.Entry<Character, CharNode> entry : children.entrySet()) {
                CharNode value = entry.getValue();
                Character key = entry.getKey();
                CharNode temp = current.findFailNode(key);
                if (temp == null) {
                    value.failNode = root;
                } else {
                    value.failNode = temp.get(key);
                }
                queue.offer(value);
            }
        }
    }

    private LinkedHashMap<Integer, List<PatternInfo>> findAllPattern(String word) {
        LinkedHashMap<Integer, List<PatternInfo>> map = new LinkedHashMap<>();
        CharNode current = root;
        int len = word.length();
        for (int i = 0; i < len; i++) {
            char key = word.charAt(i);
            CharNode next = current.get(key);
            if (next != null) {
                current = next;
                addPattern(map, current, i, len);
            } else {
                next = current.findFailNode(key);
                if (next == null) {
                    current = root;
                } else {
                    current = next.get(key);
                }
                addPattern(map, current, i, len);
            }
        }
        return map;
    }

    private void addPattern(Map<Integer, List<PatternInfo>> map, CharNode current, int index, int len) {
        if (current.pattern == null) {
            return;
        }
        if (++index < len) {
            map.put(index, current.infos);
        }
    }

    private String maskSensitiveData(String word, String mask) {
        LinkedHashMap<Integer, List<PatternInfo>> map = findAllPattern(word);
        if (map.isEmpty()) {
            return word;
        }
        StringBuilder sb = new StringBuilder();
        List<Integer> list = new ArrayList<>(map.keySet());
        list.add(word.length());
        int prefixIndex = list.get(0);
        sb.append(word, 0, prefixIndex);
        for (int i = 1; i < list.size(); i++) {
            int nextPrefixIndex = list.get(i);
            List<PatternInfo> infos = map.get(prefixIndex);
            int suffixIndex = -1;
            for (PatternInfo info : infos) {
                suffixIndex = indexOf(word, prefixIndex, nextPrefixIndex, info.charArr, info.nextArr);
                if (suffixIndex > -1) {
                    break;
                }
            }
            if (suffixIndex > -1) {
                sb.append(mask);
                sb.append(word, suffixIndex, nextPrefixIndex);
            } else {
                if (nextPrefixIndex < prefixIndex) {
                    System.out.println();
                }
                sb.append(word, prefixIndex, nextPrefixIndex);

            }
            prefixIndex = nextPrefixIndex;
        }
        return sb.toString();
    }

    public int indexOf(String source, int start, int end, char[] ptn, int[] next) {
        int j = 0;
        int pLen = ptn.length;
        while (start < end && j < pLen) {
            // 如果j = -1,或者当前字符匹配成功(src[start] = ptn[j]),都让start++,j++
            if (j == -1 || source.charAt(start) == ptn[j]) {
                start++;
                j++;
            } else {
                // 如果j!=-1且当前字符匹配失败,则令i不变,j=next[j],即让pattern模式串右移j-next[j]个单位
                j = next[j];
            }
        }
        if (j == pLen)
            return start - j;
        return -1;
    }

    private static StringReplacer replacer = new StringReplacer();

    public static void main(String[] args) {
        List<String> keywordList = List.of("secret", "password", "new_password", "token");
        Map<String, String> map = new HashMap<>();
        List<KeyWordInfo> list = new ArrayList<>();

        list.add(new KeyWordInfo("\":\"", "\""));// json的value值
        list.add(new KeyWordInfo("\": \"", "\""));//格式化的json的value值

        list.add(new KeyWordInfo("':'", "'")); // 单引号的json
//        list.add(new KeyWordInfo("': '", "'")); // 格式化的单引号的json
//
        list.add(new KeyWordInfo("':", ",")); // 单引号的json数值
//        list.add(new KeyWordInfo("': ", ",")); // 格式化的单引号的json数值
//
        list.add(new KeyWordInfo("':", "}")); // 单引号的json末尾数值
//        list.add(new KeyWordInfo("': ", "}")); // 格式化的单引号的json末尾数值


        list.add(new KeyWordInfo("=\"", "\""));//map.toString() 字符串
        list.add(new KeyWordInfo("\":", ","));//json的value值

        list.add(new KeyWordInfo("\": ", ","));//格式化的json的数值



        list.add(new KeyWordInfo("=", ",")); // map.toString() 数值

        list.add(new KeyWordInfo("=", "}")); // map.toString() 末尾数值


        list.add(new KeyWordInfo("\\\\\":\\\\\"", "\\\\\""));//json作为value后的json的value值
        list.add(new KeyWordInfo("\\\\\": \\\\\"", "\\\\\"")); //格式化的json作为value后的json的value值
        replacer.build(keywordList, list);
        //{json={"json":"{\\"info\\":{\\"secret\\":\\"mySecret\\",\\"password\\":123},\\"name\\":\\"张三\\"}","json1":
        // {"info":{"secret":"ddd","new_password":111,"password":445},"name":"李四"}}, secret=1234, token=hello, new_password=hai}
        String content = "{json={\"json\":\"{\\\\\"info\\\\\":{\\\\\"secret\\\\\":\\\\\"mysecret\\\\\",\\\\\"password\\\\\":123},\\\\\"name\\\\\":\\\\\"张三\\\\\"}\",\"json1\":{\"info\":{\"secret\":\"ddd\",\"new_password\":111,\"password\":445},\"name\":\"李四\"}}, secret=1234, token=hello, new_password=hai}";
//        System.out.println(content);
        System.out.println(replacer.maskSensitiveData(content, "*****"));
    }
}
