package com.timestored.qdoc.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QNode {
    public enum Type {
        STAT_LIST, ASSIGN, APPLY, BINARY, ADVERB, FUNCTION, TABLE, QUERY,
        LITERAL, ID, OPERATOR, COND, CONTROL, SLASH, PARENS, PROJECTION, EMPTY
    }

    public final Type type;
    public final List<QNode> children;
    public final String text;
    public final int offset;

    public QNode(Type type, String text, List<QNode> children, int offset) {
        this.type = type;
        this.text = text;
        this.children = children != null ? children : Collections.emptyList();
        this.offset = offset;
    }

    public static QNode leaf(Type type, QToken token, String text) {
        return new QNode(type, text, null, token != null ? token.pos : -1);
    }

    public static QNode node(Type type, QToken token, String text, List<QNode> children) {
        return new QNode(type, text, children, token != null ? token.pos : -1);
    }

    public static QNode node(Type type, QToken token, QNode... children) {
        List<QNode> list = new ArrayList<>();
        for (QNode c : children) if (c != null) list.add(c);
        return new QNode(type, null, list, token != null ? token.pos : -1);
    }

    public int childCount() { return children.size(); }
    public QNode child(int i) { return children.get(i); }

    @Override
    public String toString() {
        if (children.isEmpty()) return type + "(" + (text != null ? text : "") + ")";
        StringBuilder sb = new StringBuilder();
        sb.append(type).append("(");
        if (text != null) sb.append(text).append(" ");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(children.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}
