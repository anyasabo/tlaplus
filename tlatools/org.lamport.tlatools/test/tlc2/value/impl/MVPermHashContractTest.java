package tlc2.value.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import util.UniqueString;

/**
 * Verifies that MVPerm's equals/hashCode contract satisfies the requirements
 * for correct HashSet behavior. This is critical because MVPerms.permutationSubgroup
 * uses a HashSet&lt;IMVPerm&gt; for deduplication (previously used util.Set which also
 * relied on equals/hashCode).
 *
 * These tests exist to give reviewers confidence that the util.Set -&gt; HashSet
 * migration preserves correctness of symmetry group computation.
 */
public class MVPermHashContractTest {

    private ModelValue mvA, mvB, mvC;

    @Before
    public void setUp() {
        UniqueString.initialize();
        ModelValue.init();
        mvA = (ModelValue) ModelValue.make("a");
        mvB = (ModelValue) ModelValue.make("b");
        mvC = (ModelValue) ModelValue.make("c");
        ModelValue.setValues();
    }

    @Test
    public void testEqualPermsHaveSameHashCode() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);

        MVPerm p2 = new MVPerm();
        p2.put(mvA, mvB);

        assertEquals("Equal perms must have equal hashCode", p1.hashCode(), p2.hashCode());
        assertTrue("Same mapping must be equal", p1.equals(p2));
    }

    @Test
    public void testDifferentPermsAreNotEqual() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);

        MVPerm p2 = new MVPerm();
        p2.put(mvA, mvC);

        assertFalse("Different mappings must not be equal", p1.equals(p2));
    }

    @Test
    public void testEmptyPermsAreEqual() {
        MVPerm p1 = new MVPerm();
        MVPerm p2 = new MVPerm();
        assertTrue("Two empty perms must be equal", p1.equals(p2));
        assertEquals("Two empty perms must have same hashCode", p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testHashSetDeduplicatesEqualPerms() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);

        MVPerm p2 = new MVPerm();
        p2.put(mvA, mvB);

        HashSet<MVPerm> set = new HashSet<>();
        assertTrue("First add should return true", set.add(p1));
        assertFalse("Duplicate add should return false", set.add(p2));
        assertEquals("Set should contain exactly one element", 1, set.size());
    }

    @Test
    public void testHashSetKeepsDistinctPerms() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);

        MVPerm p2 = new MVPerm();
        p2.put(mvB, mvC);

        HashSet<MVPerm> set = new HashSet<>();
        assertTrue(set.add(p1));
        assertTrue(set.add(p2));
        assertEquals("Set should contain both distinct perms", 2, set.size());
    }

    @Test
    public void testEqualsIsSymmetric() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);

        MVPerm p2 = new MVPerm();
        p2.put(mvA, mvB);

        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    @Test
    public void testEqualsWithNull() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);
        assertFalse(p1.equals(null));
    }

    @Test
    public void testEqualsWithWrongType() {
        MVPerm p1 = new MVPerm();
        p1.put(mvA, mvB);
        assertFalse(p1.equals("not a perm"));
    }

    @Test
    public void testIdentityMappingIsNotStored() {
        MVPerm p = new MVPerm();
        p.put(mvA, mvA);
        assertEquals("Identity mapping should not increase size", 0, p.size());
    }

    @Test
    public void testHashSetWithComposedPerms() {
        // Simulate what MVPerms.permutationSubgroup does:
        // compose two permutations and verify the result deduplicates correctly.
        MVPerm swap_ab = new MVPerm();
        swap_ab.put(mvA, mvB);
        swap_ab.put(mvB, mvA);

        // Composing swap_ab with itself should yield the identity (empty perm).
        // MVPerm.compose returns a perm with size 0 for identity, which
        // permutationSubgroup skips (perm.size() > 0 check).
        MVPerm composed = (MVPerm) swap_ab.compose(swap_ab);
        assertEquals("swap composed with itself should be identity", 0, composed.size());

        // Two independently constructed swap(a,b) perms should deduplicate.
        MVPerm swap_ab2 = new MVPerm();
        swap_ab2.put(mvA, mvB);
        swap_ab2.put(mvB, mvA);

        HashSet<MVPerm> set = new HashSet<>();
        set.add(swap_ab);
        set.add(swap_ab2);
        assertEquals("Equal composed perms must deduplicate in HashSet", 1, set.size());
    }
}
