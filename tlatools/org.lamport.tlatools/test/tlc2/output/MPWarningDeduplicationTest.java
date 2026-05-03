package tlc2.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tlc2.TLCGlobals;
import util.ToolIO;

/**
 * Tests that MP's warning deduplication (warningHistory) preserves the
 * original util.Set contract after migration to java.util.HashSet.
 *
 * The critical invariants:
 *   1. First occurrence of a warning is printed.
 *   2. Duplicate warnings are suppressed (not printed again).
 *   3. Two warnings with different text are both printed.
 *   4. warningHistory.isEmpty() is true before any warning, false after.
 *   5. The "-nowarning" hint appears only on the very first warning.
 */
public class MPWarningDeduplicationTest {

    private PrintStream savedOut;
    private PrintStream savedErr;
    private ByteArrayOutputStream captured;

    @Before
    public void setUp() {
        savedOut = ToolIO.out;
        savedErr = ToolIO.err;
        captured = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(captured);
        ToolIO.out = ps;
        ToolIO.err = ps;
        TLCGlobals.warn = true;
        TLCGlobals.tool = false;
        MP.resetMessageControl();
    }

    @After
    public void tearDown() {
        ToolIO.out = savedOut;
        ToolIO.err = savedErr;
        TLCGlobals.warn = true;
    }

    @Test
    public void testFirstWarningIsPrinted() {
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "first-warning");
        String output = captured.toString();
        assertTrue("First warning must appear in output",
                output.contains("first-warning"));
    }

    @Test
    public void testDuplicateWarningIsSuppressed() {
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "dup-test");
        captured.reset();

        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "dup-test");
        String secondOutput = captured.toString();
        assertTrue("Duplicate warning must produce no output",
                secondOutput.isEmpty());
    }

    @Test
    public void testDistinctWarningsAreBothPrinted() {
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "warning-A");
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "warning-B");
        String output = captured.toString();
        assertTrue("First distinct warning must appear", output.contains("warning-A"));
        assertTrue("Second distinct warning must appear", output.contains("warning-B"));
    }

    @Test
    public void testThirdDuplicateIsSuppressed() {
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "triple");
        captured.reset();
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "triple");
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "triple");
        assertTrue("All repeats after first must be suppressed",
                captured.toString().isEmpty());
    }

    @Test
    public void testNowarningHintOnFirstWarningOnly() {
        // In non-tool mode, the first warning (when warningHistory is empty)
        // should include the -nowarning hint. Subsequent warnings should not.
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "hint-first");
        String first = captured.toString();
        assertTrue("First warning should include -nowarning hint",
                first.contains("-nowarning"));

        captured.reset();
        MP.printWarning(EC.TLC_FEATURE_UNSUPPORTED, "hint-second");
        String second = captured.toString();
        assertFalse("Second warning should NOT include -nowarning hint",
                second.contains("-nowarning"));
    }
}
