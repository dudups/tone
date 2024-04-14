package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.common.exception.CodedException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 前者和精确匹配规则冲突检查
 */
public class StringMatchersConflictChecker {
    private static final CodedException EXCEPTION = new CodedException(HttpStatus.CONFLICT, "分支匹配规则冲突！");

    private Node root = new Node();

    public void addPrefix(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            root.parkPrefix();
            return;
        }
        root.passPrefix();
        char[] chars = prefix.toCharArray();
        Node node = root;
        for (int i = 0; i < chars.length - 1; i++) {
            node = node.child(chars[i]);
            node.passPrefix();
        }
        node.child(chars[chars.length - 1]).parkPrefix();
    }

    public void addPrecise(String precise) {
        if (StringUtils.isEmpty(precise)) {
            root.parkPrecise();
            return;
        }
        root.passPrecise();
        char[] chars = precise.toCharArray();
        Node node = root;
        for (int i = 0; i < chars.length - 1; i++) {
            node = node.child(chars[i]);
            node.passPrecise();
        }
        node.child(chars[chars.length - 1]).parkPrecise();
    }

    private static class Node {
        private Map<Character, Node> children;
        private boolean noPass;
        private boolean noParkPrefix;
        private boolean noParkPrecise;

        private void passPrefix() {
            if (this.noPass) {
                throw EXCEPTION;
            }
            this.noParkPrefix = true;
        }

        private void parkPrefix() {
            if (this.noParkPrefix) {
                throw EXCEPTION;
            }
            this.noPass = true;
            this.noParkPrefix = true;
            this.noParkPrecise = true;
        }

        private void passPrecise() {
            if (this.noPass) {
                throw EXCEPTION;
            }
            this.noParkPrefix = true;
        }

        private void parkPrecise() {
            if (this.noParkPrecise) {
                throw EXCEPTION;
            }
            this.noParkPrefix = true;
            this.noParkPrecise = true;
        }

        private Node child(char ch) {
            Node child = null;
            if (MapUtils.isEmpty(children)) {
                children = new HashMap<>();
            } else {
                child = children.get(ch);
            }
            if (child == null) {
                child = new Node();
                children.put(ch, child);
            }
            return child;
        }
    }
}
