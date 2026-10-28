package dev.echo.client.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Arrays;

/**
 * Complete ordinary-source candidate for ResponseProcessor's section operations.
 * Direct targets, control-state/key branches and shared-cache applicability
 * remain separate proof obligations; this analysis candidate is not promoted.
 */
public class ChunkSectionData {
    private static final short[] EMPTY_STATES = new short[0];
    private final long[] occupied = new long[64];
    private short[] states = EMPTY_STATES;
    private int size;
    private boolean dirty = true;
    private final IntArrayList faces = new IntArrayList();
    private final int[] faceCounts = new int[6];
    private final ObjectList<BlockCluster> clusters = new ObjectArrayList<>();
    public final int sectionX;
    public final int sectionY;
    public final int sectionZ;

    // Origin: hack/ResponseProcessor.<init>(IIJI)V.
    public ChunkSectionData(int sectionX, int sectionY, int sectionZ) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
    }

    // Origin: hack/ResponseProcessor.buildJob(IIJI)I.
    public static int packIndex(int x, int y, int z) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) {
            return -1;
        }
        return y << 8 | z << 4 | x;
    }

    // Origin: hack/ResponseProcessor.processToken(JI)I.
    private int packedOffset(int index) {
        int word = index >> 6;
        int offset = Long.bitCount(occupied[word] & ((1L << (index & 63)) - 1L));
        for (int preceding = 0; preceding < word; preceding++) {
            offset += Long.bitCount(occupied[preceding]);
        }
        return offset;
    }

    // Origin: hack/ResponseProcessor.findSchema(IIJ)V.
    // The original fast path appends without sorting, duplicate checks or dirty writes.
    public void append(int index, int state) {
        if (size == states.length) {
            states = Arrays.copyOf(states, Math.max(16, states.length << 1));
        }
        occupied[index >> 6] |= 1L << (index & 63);
        states[size++] = (short) state;
    }

    // Origin: hack/ResponseProcessor.openModel(IIIIJ)V.
    public void set(int x, int y, int z, int state) {
        int index = packIndex(x, y, z);
        if (index == -1) {
            return;
        }
        boolean present = (occupied[index >> 6] & (1L << (index & 63))) != 0;
        if (state == 0) {
            if (!present) {
                return;
            }
            int offset = packedOffset(index);
            System.arraycopy(states, offset + 1, states, offset, size - offset - 1);
            size--;
            occupied[index >> 6] &= ~(1L << (index & 63));
            dirty = true;
        } else if (present) {
            int offset = packedOffset(index);
            if ((states[offset] & 0xffff) != state) {
                states[offset] = (short) state;
                dirty = true;
            }
        } else {
            if (size == states.length) {
                states = Arrays.copyOf(states, Math.max(16, states.length << 1));
            }
            int offset = packedOffset(index);
            System.arraycopy(states, offset, states, offset + 1, size - offset);
            states[offset] = (short) state;
            size++;
            occupied[index >> 6] |= 1L << (index & 63);
            dirty = true;
        }
    }

    // Origin: hack/ResponseProcessor.setContext(IJIBI)I.
    public int get(int x, int y, int z) {
        int index = packIndex(x, y, z);
        if (index == -1) {
            return -1;
        }
        if ((occupied[index >> 6] & (1L << (index & 63))) == 0) {
            return 0;
        }
        return states[packedOffset(index)] & 0xffff;
    }

    // Origin: hack/ResponseProcessor.decodeState(SIS)V.
    public void markDirty() {
        dirty = true;
    }

    // Origin: hack/ResponseProcessor.parseCommand(JI)Z.
    public boolean isOccupied(int index) {
        return (occupied[index >> 6] & (1L << (index & 63))) != 0;
    }

    /**
     * Rebuilds packed faces and cluster centroids using per-thread working arrays.
     * Neighbors are DOWN, UP, WEST, EAST, NORTH, SOUTH. The dirty flag is cleared
     * only after both rebuild operations return; exceptions retain partial output.
     */
    // Origin: hack/ResponseProcessor.createItem(CII[Lhack/ResponseProcessor;)V.
    public void rebuild(ChunkSectionData[] neighbors) {
        ChunkSectionScratch scratch = ChunkSectionScratch.current();
        expand(scratch.expandedStates);
        ChunkSectionUpdates.generateFaces(scratch, neighbors, faces, faceCounts);
        rebuildClusters(scratch);
        dirty = false;
    }

    // Origin: hack/ResponseProcessor.addSchema(J[S)V.
    void expand(short[] destination) {
        Arrays.fill(destination, (short) 0);
        int stateOffset = 0;
        for (int word = 0; word < 64; word++) {
            long bits = occupied[word];
            while (bits != 0) {
                int index = (word << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                destination[index] = states[stateOffset++];
            }
        }
    }

    // Origin: hack/ResponseProcessor.stopAccount(JLhack/ValueBuilder;)V.
    // Cluster centroids use positive-axis connectivity of identical short states.
    void rebuildClusters(ChunkSectionScratch scratch) {
        clusters.clear();
        short[] expanded = scratch.expandedStates;
        int[] parents = scratch.parents;
        for (int word = 0; word < 64; word++) {
            long bits = occupied[word];
            while (bits != 0) {
                int index = (word << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                parents[index] = index;
                scratch.sumX[index] = 0;
                scratch.sumY[index] = 0;
                scratch.sumZ[index] = 0;
                scratch.counts[index] = 0;
            }
        }
        for (int word = 0; word < 64; word++) {
            long bits = occupied[word];
            while (bits != 0) {
                int index = (word << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                short state = expanded[index];
                if ((index & 15) < 15 && expanded[index + 1] == state) {
                    union(parents, index, index + 1);
                }
                if (((index >> 4) & 15) < 15 && expanded[index + 16] == state) {
                    union(parents, index, index + 16);
                }
                if ((index >> 8) < 15 && expanded[index + 256] == state) {
                    union(parents, index, index + 256);
                }
            }
        }
        for (int word = 0; word < 64; word++) {
            long bits = occupied[word];
            while (bits != 0) {
                int index = (word << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                int root = findRoot(parents, index);
                scratch.sumX[root] += index & 15;
                scratch.sumZ[root] += (index >> 4) & 15;
                scratch.sumY[root] += (index >> 8) & 15;
                scratch.counts[root]++;
            }
        }
        for (int word = 0; word < 64; word++) {
            long bits = occupied[word];
            while (bits != 0) {
                int index = (word << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                int count = scratch.counts[index];
                if (count != 0) {
                    clusters.add(new BlockCluster(
                            (double) scratch.sumX[index] / count + 0.5,
                            (double) scratch.sumY[index] / count + 0.5,
                            (double) scratch.sumZ[index] / count + 0.5,
                            expanded[index] & 0xffff));
                }
            }
        }
    }

    // Origin: hack/ResponseProcessor.startProperty([IIJ)I; path halving, not recursion.
    private static int findRoot(int[] parents, int index) {
        while (parents[index] != index) {
            parents[index] = parents[parents[index]];
            index = parents[index];
        }
        return index;
    }

    // Origin: hack/ResponseProcessor.selectAction(J[III)V; no union-by-rank here.
    private static void union(int[] parents, int first, int second) {
        int firstRoot = findRoot(parents, first);
        int secondRoot = findRoot(parents, second);
        if (firstRoot != secondRoot) {
            parents[secondRoot] = firstRoot;
        }
    }

    // Origin: hack/ResponseProcessor.readMessage(J)I.
    public int size() { return size; }

    // Origin: hack/ResponseProcessor.stopResponse(IIC)Z.
    public boolean isDirty() { return dirty; }

    // Origin: hack/ResponseProcessor.decodeEvent(J)Lit/unimi/dsi/fastutil/ints/IntArrayList;
    public IntArrayList faces() { return faces; }

    // Origin: hack/ResponseProcessor.stopHandler(J)[I.
    public int[] faceCounts() { return faceCounts; }

    // Origin: hack/ResponseProcessor.decodeMessage(JC)Lit/unimi/dsi/fastutil/objects/ObjectList;
    public ObjectList<BlockCluster> clusters() { return clusters; }
}
