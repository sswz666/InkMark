package com.inkmark.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 基于快照的撤销 / 重做栈。一次手势开始前拍一张快照，撤销即回到上一张快照。
 * 纯 Java 实现，方便做单元测试。
 */
public final class History<T> {

    private static final int LIMIT = 40;

    private final ArrayList<T> state = new ArrayList<>();
    private final Deque<ArrayList<T>> undo = new ArrayDeque<>();
    private final Deque<ArrayList<T>> redo = new ArrayDeque<>();

    /** 当前内容（可直接增删）。 */
    public List<T> state() {
        return state;
    }

    public int size() {
        return state.size();
    }

    /** 在修改前记录一次现场。 */
    public void snapshot() {
        undo.push(new ArrayList<>(state));
        while (undo.size() > LIMIT) {
            undo.removeLast();
        }
        redo.clear();
    }

    /** 本次手势没有产生任何变化时，丢弃刚才那张快照。 */
    public void discardLastSnapshot() {
        if (!undo.isEmpty()) {
            undo.pop();
        }
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.push(new ArrayList<>(state));
        replace(undo.pop());
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.push(new ArrayList<>(state));
        replace(redo.pop());
        return true;
    }

    public void clear() {
        state.clear();
        undo.clear();
        redo.clear();
    }

    private void replace(ArrayList<T> snapshot) {
        state.clear();
        state.addAll(snapshot);
    }
}
