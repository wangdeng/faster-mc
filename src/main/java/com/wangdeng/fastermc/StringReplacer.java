package com.wangdeng.fastermc;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * @author 王澄
 * @create 2022-06-17 22:46
 **/
public class StringReplacer {
    private static class CharNode {
        CharNode failNode;
        String pattern =null;
        Map<Character,CharNode> children=new HashMap<>();
        CharNode get(Character key){
            return children.get(key);
        }
        public CharNode findFailNode(Character key){
            CharNode next=failNode;
            while (next!=null && !next.children.containsKey(key)){
                next=next.failNode;
            }
            return next;
        }
    }
    private final CharNode root=new CharNode();
    private void insert(String word){
        CharNode current=root;
        for(int i=0;i<word.length();i++){
            Character key=word.charAt(i);
            CharNode value;
            Map<Character,CharNode> children=current.children;
            if(children.containsKey(key)){
                value=children.get(key);
            }else{
                value=new CharNode();
                children.put(key,value);
            }
            current=value;
        }
        current.pattern=word;
    }
    private void getFail(){
        Queue<CharNode> queue=new ArrayDeque<>();
        queue.offer(root);
        while(!queue.isEmpty()){
            CharNode current= queue.poll();
            Map<Character,CharNode> children=current.children;
            for(Map.Entry<Character,CharNode> entry:children.entrySet()){
                CharNode value=entry.getValue();
                Character key=entry.getKey();
                CharNode temp=current.findFailNode(key);
                if (temp == null) {
                    value.failNode=root;
                }else{
                    value.failNode=temp.get(key);
                }
                queue.offer(value);
            }
        }
    }
    private int count(String word){
        int count=0;
        CharNode current=root;
        for(int i=0,len=word.length();i<len;i++){
            char key=word.charAt(i);
            CharNode next=current.get(key);
            if(next!=null){
                current=next;
                if(current.pattern!=null){
                    count++;
                }
            }else{
                next=current.findFailNode(key);
                if (next == null) {
                    current=root;
                }else{
                    current=next.get(key);
                }
                if(current.pattern!=null){
                    count++;
                }
            }
        }
        return count;
    }
    private static StringReplacer searcher=new StringReplacer();

    public static void main(String[] args) {
        searcher.insert("abc");
        searcher.insert("dec");
        searcher.insert("zzw");
        searcher.getFail();
        int n=searcher.count("dadfueabcirigdec,a,asdzzwadfj");
        System.out.println(n);
    }
}
