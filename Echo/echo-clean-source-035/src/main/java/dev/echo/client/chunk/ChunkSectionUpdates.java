package dev.echo.client.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Arrays;

/**
 * Complete face-generation source candidate for SchemaController. The original
 * normal key/control domain and direct/cached member bindings remain separately
 * gated. Padded occupancy columns and packed quad/state pairs retain their layout.
 */
public class ChunkSectionUpdates {
    private static final int PADDED_SIZE = 18;
    private static final int PADDED_PLANE = PADDED_SIZE * PADDED_SIZE;

    public ChunkSectionUpdates() {}

    // Origin: hack/SchemaController.handleFrame(J,ValueBuilder,ResponseProcessor[],IntArrayList,int[]).
    static void generateFaces(ChunkSectionScratch scratch, ChunkSectionData[] neighbors,
                              IntArrayList faces, int[] faceCounts) {
        faces.clear();
        short[] states = scratch.expandedStates;
        int[] occupancy = scratch.occupancyColumns;
        int[] surfaces = scratch.faceColumns;
        int[] plane = scratch.facePlane;
        Arrays.fill(occupancy, 0);
        for (int index = 0; index < 4096; index++) {
            if (states[index] == 0) {
                continue;
            }
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = (index >> 8) & 15;
            occupancy[(z + 1) * PADDED_SIZE + x + 1] |= 1 << (y + 1);
            occupancy[PADDED_PLANE + (y + 1) * PADDED_SIZE + z + 1] |= 1 << (x + 1);
            occupancy[2 * PADDED_PLANE + (y + 1) * PADDED_SIZE + x + 1] |= 1 << (z + 1);
        }
        seedNeighborColumns(occupancy, neighbors);
        for (int axis = 0; axis < 3; axis++) {
            for (int index = 0; index < PADDED_PLANE; index++) {
                int column = occupancy[axis * PADDED_PLANE + index];
                surfaces[axis * 2 * PADDED_PLANE + index] = column & ~(column << 1);
                surfaces[(axis * 2 + 1) * PADDED_PLANE + index] = column & ~(column >>> 1);
            }
        }
        for (int face = 0; face < 6; face++) {
            int axis = face / 2;
            Arrays.fill(plane, 0);
            for (int first = 0; first < 16; first++) {
                for (int second = 0; second < 16; second++) {
                    int bits = (surfaces[face * PADDED_PLANE + (second + 1) * PADDED_SIZE + first + 1] >> 1) & 65535;
                    while (bits != 0) {
                        int depth = Integer.numberOfTrailingZeros(bits);
                        bits &= bits - 1;
                        int row = axis == 1 ? second : first;
                        int column = axis == 1 ? first : second;
                        plane[(depth << 4) | row] |= 1 << column;
                    }
                }
            }
            int quadCount = 0;
            for (int depth = 0; depth < 16; depth++) {
                quadCount += meshPlane(states, plane, face, depth, faces);
            }
            faceCounts[face] = quadCount;
        }
    }

    // Origin: hack/SchemaController.addRoute(J[I[Lhack/ResponseProcessor;)V.
    static void seedNeighborColumns(int[] columns, ChunkSectionData[] neighbors) {
        for (int first = 0; first < 16; first++) {
            for (int second = 0; second < 16; second++) {
                ChunkSectionData down = neighbors[0];
                if (down != null && down.isOccupied(0xf00 | second << 4 | first)) {
                    columns[(second + 1) * 18 + first + 1] |= 1;
                }
                ChunkSectionData up = neighbors[1];
                if (up != null && up.isOccupied(second << 4 | first)) {
                    columns[(second + 1) * 18 + first + 1] |= 0x20000;
                }
                ChunkSectionData west = neighbors[2];
                if (west != null && west.isOccupied(first << 8 | second << 4 | 15)) {
                    columns[324 + (first + 1) * 18 + second + 1] |= 1;
                }
                ChunkSectionData east = neighbors[3];
                if (east != null && east.isOccupied(first << 8 | second << 4)) {
                    columns[324 + (first + 1) * 18 + second + 1] |= 0x20000;
                }
                ChunkSectionData north = neighbors[4];
                if (north != null && north.isOccupied(first << 8 | 0xf0 | second)) {
                    columns[648 + (first + 1) * 18 + second + 1] |= 1;
                }
                ChunkSectionData south = neighbors[5];
                if (south != null && south.isOccupied(first << 8 | second)) {
                    columns[648 + (first + 1) * 18 + second + 1] |= 0x20000;
                }
            }
        }
    }

    // Origin: hack/SchemaController.resetStore([SICCIIII)I.
    // Requires nonzero control state and the negative low key component branch.
    static int stateAtFace(short[] states, int face, int depth, int first, int second) {
        if (face < 2) {
            return states[depth << 8 | second << 4 | first] & 0xffff;
        }
        if (face < 4) {
            return states[first << 8 | second << 4 | depth] & 0xffff;
        }
        return states[second << 8 | depth << 4 | first] & 0xffff;
    }

    /** Width-first greedy rectangles, emitted as packed geometry then state ID. */
    // Origin: hack/SchemaController.selectOption(J[S[IIILit/unimi/dsi/fastutil/ints/IntArrayList;)I.
    private static int meshPlane(short[] states, int[] plane, int face, int depth, IntArrayList faces) {
        int base = depth << 4;
        int quadCount = 0;
        for (int row = 0; row < 16; row++) {
            int column = 0;
            while (column < 16) {
                int remaining = plane[base + row] >>> column;
                if (remaining == 0) {
                    break;
                }
                column += Integer.numberOfTrailingZeros(remaining);
                if (column >= 16) {
                    break;
                }
                int state = stateAtFace(states, face, depth, row, column);
                int width = 1;
                while (column + width < 16
                        && (plane[base + row] & (1 << (column + width))) != 0
                        && stateAtFace(states, face, depth, row, column + width) == state) {
                    width++;
                }
                int runMask = ((1 << width) - 1) << column;
                int height = 1;
                growHeight:
                while (row + height < 16) {
                    if ((plane[base + row + height] & runMask) != runMask) {
                        break;
                    }
                    for (int offset = 0; offset < width; offset++) {
                        if (stateAtFace(states, face, depth, row + height, column + offset) != state) {
                            break growHeight;
                        }
                    }
                    // The original consumes accepted subsequent rows as it grows.
                    // Its first row stays intact; advancing column skips that run.
                    plane[base + row + height] &= ~runMask;
                    height++;
                }
                int x;
                int y;
                int z;
                if (face < 2) {
                    x = row;
                    y = depth;
                    z = column;
                } else if (face < 4) {
                    x = depth;
                    y = row;
                    z = column;
                } else {
                    x = row;
                    y = column;
                    z = depth;
                }
                int packed = x | (y << 4) | (z << 8) | ((height - 1) << 12)
                        | ((width - 1) << 16) | (face << 20);
                faces.add(packed);
                faces.add(state);
                quadCount++;
                column += width;
            }
        }
        return quadCount;
    }
}
