package dev.echo.client.chunk;

/**
 * Analysis candidate for CacheService, not production source.
 * Requires proof of all 30 dispatch sites and the zero-valued control-state
 * invariant; see needs.json. This preserves recursive compression and rank ties.
 */
final class SectionDisjointSets {
    final byte[] ranks = new byte[4096];
    final int[] parents = new int[4096];

    // Origin: hack/CacheService.<init>(J)V, normal branch in source lines 29-71.
    SectionDisjointSets() {
        for (int index = 0; index < 4096; index++) {
            parents[index] = index;
        }
    }

    // Origin: hack/CacheService.clearRule(CCII)I, source lines 82-177.
    int findRoot(int index) {
        int parent = parents[index];
        if (parents[parent] != parent) {
            return parents[index] = findRoot(parent);
        }
        return parent;
    }

    // Origin: hack/CacheService.selectEvent(JII)V, source lines 190-360.
    void union(int first, int second) {
        int firstRoot = findRoot(first);
        int secondRoot = findRoot(second);
        if (firstRoot == secondRoot) {
            return;
        }
        if (ranks[firstRoot] < ranks[secondRoot]) {
            parents[firstRoot] = secondRoot;
        } else if (ranks[secondRoot] < ranks[firstRoot]) {
            parents[secondRoot] = firstRoot;
        } else {
            parents[secondRoot] = firstRoot;
            ranks[firstRoot]++;
        }
    }
}
