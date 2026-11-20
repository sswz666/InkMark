package com.inkmark.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HistoryTest {

    @Test
    public void startsEmpty() {
        History<String> h = new History<>();
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
        assertEquals(0, h.size());
    }

    @Test
    public void undoRestoresPreviousSnapshot() {
        History<String> h = new History<>();
        h.snapshot();
        h.state().add("a");
        h.snapshot();
        h.state().add("b");
        assertEquals(2, h.size());

        assertTrue(h.undo());
        assertEquals(1, h.size());
        assertEquals("a", h.state().get(0));

        assertTrue(h.undo());
        assertEquals(0, h.size());
        assertFalse(h.canUndo());
    }

    @Test
    public void redoReplaysForward() {
        History<String> h = new History<>();
        h.snapshot();
        h.state().add("a");
        h.undo();
        assertTrue(h.canRedo());
        assertTrue(h.redo());
        assertEquals(1, h.size());
        assertEquals("a", h.state().get(0));
        assertFalse(h.canRedo());
    }

    @Test
    public void newEditClearsRedo() {
        History<String> h = new History<>();
        h.snapshot();
        h.state().add("a");
        h.undo();
        assertTrue(h.canRedo());

        h.snapshot();
        h.state().add("b");
        assertFalse(h.canRedo());
    }

    @Test
    public void discardLastSnapshotDropsEmptyGesture() {
        History<String> h = new History<>();
        h.state().add("a");
        h.snapshot();
        // 手势没有产生变化
        h.discardLastSnapshot();
        assertFalse(h.canUndo());
        assertEquals(1, h.size());
    }

    @Test
    public void undoKeepsContentInSameListInstance() {
        History<String> h = new History<>();
        java.util.List<String> live = h.state();
        h.state().add("a");
        h.snapshot();
        h.state().add("b");
        h.undo();

        // state() 返回的是同一个列表对象，撤销后引用依然有效
        assertEquals(1, live.size());
        assertEquals("a", live.get(0));
    }

    @Test
    public void clearResetsEverything() {
        History<String> h = new History<>();
        h.snapshot();
        h.state().add("a");
        h.snapshot();
        h.state().add("b");
        h.clear();
        assertEquals(0, h.size());
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
    }
}
