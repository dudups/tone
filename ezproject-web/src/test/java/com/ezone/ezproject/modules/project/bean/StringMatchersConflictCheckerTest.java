package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.common.exception.CodedException;
import org.junit.Test;

public class StringMatchersConflictCheckerTest {
    @Test
    public void testEmptyOk() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("");
        checker.addPrecise("123");
        checker.addPrefix("abc");
    }

    @Test
    public void testNullOk() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("123");
        checker.addPrefix("abc");
        checker.addPrecise(null);
    }

    @Test(expected = CodedException.class)
    public void testEmptyConflict() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrefix("");
        checker.addPrecise("123");
    }

    @Test(expected = CodedException.class)
    public void testNullConflict() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("123");
        checker.addPrefix(null);
    }

    @Test
    public void testPreciseOk() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("abc");
        checker.addPrecise("123");
        checker.addPrecise("abc123");
        checker.addPrecise("a1bc123");
    }

    @Test(expected = CodedException.class)
    public void testPreciseConflict() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("abc");
        checker.addPrecise("123");
        checker.addPrecise("abc");
    }

    @Test
    public void testPrefixOk() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrefix("abc");
        checker.addPrefix("123");
        checker.addPrefix("a1bc");
    }

    @Test(expected = CodedException.class)
    public void testPrefixCodedException() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrefix("abc");
        checker.addPrefix("123");
        checker.addPrefix("a1bc");
        checker.addPrefix("abc1");
    }

    @Test(expected = CodedException.class)
    public void testPrefixRepeatCodedException() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrefix("abc");
        checker.addPrefix("abc");
    }

    @Test
    public void testPrecisePrefixOk() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("abc");
        checker.addPrefix("abc123");
        checker.addPrefix("a1bc");
    }

    @Test(expected = CodedException.class)
    public void testPrecisePrefixCodedException() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrecise("abc123");
        checker.addPrefix("abc");
    }

    @Test(expected = CodedException.class)
    public void testPrefixPreciseCodedException() {
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        checker.addPrefix("abc");
        checker.addPrecise("abc123");
    }
}
