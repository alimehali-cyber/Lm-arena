#!/usr/bin/env python3
"""Model of the corridor-mesh index arithmetic, run where Kotlin cannot be built.

This is a line-by-line translation of

  * VertexMesh.gridIndices  (the shipped generator, both meshes),
  * VertexMesh.of           (the validation gate),
  * the pre-fix emitters    (hand-written, straight into Canvas.drawVertices),
  * the classic off-by-one emitter (one index past the vertex count),

so the numbers in the report are reproductions of the logic rather than assertions about it.
Each case is checked against the invariant that matters: every index < vertexCount.
"""
SHORT_MAX = 32767


def grid_vertex_count(cols, rows):
    return 0 if cols < 0 or rows < 0 else (cols + 1) * rows


def grid_indices(cols, rows):
    """VertexMesh.gridIndices."""
    if cols < 1 or rows < 2:
        return []
    vertex_count = grid_vertex_count(cols, rows)
    if vertex_count > SHORT_MAX:
        return []
    out = []
    for i in range(cols):
        row_a = i * rows
        row_b = row_a + rows
        for j in range(rows - 1):
            a = row_a + j
            b = row_b + j
            c = a + 1
            d = b + 1
            out += [a, b, c, c, b, d]
    return out


def prefx_emitter(cols, rows):
    """The emitter as it shipped (identical arithmetic, written out by hand)."""
    out = []
    for i in range(cols):
        for j in range(rows - 1):
            a = i * rows + j
            b = (i + 1) * rows + j
            c = i * rows + j + 1
            d = (i + 1) * rows + j + 1
            out += [a, b, c, c, b, d]
    return out


def offbyone_emitter(cols, rows):
    """The shape the tombstone implies: one ring too many in the index loop."""
    out = []
    for i in range(cols + 1):          # <-- the bug: one ring past the last vertex ring
        for j in range(rows - 1):
            a = i * rows + j
            b = (i + 1) * rows + j
            c = i * rows + j + 1
            d = (i + 1) * rows + j + 1
            out += [a, b, c, c, b, d]
    return out


def gate(vertex_count, indices):
    """VertexMesh.of, reduced to what the invariants care about."""
    kept, dropped, worst = [], 0, -1
    finite = [True] * vertex_count
    k = 0
    while k + 2 < len(indices):
        tri = indices[k:k + 3]
        iff = all(0 <= v < vertex_count and finite[v] for v in tri)
        if iff:
            kept += tri
        else:
            dropped += 1
            worst = max([worst] + [v & 0xFFFF for v in tri])
        k += 3
    if k != len(indices):
        dropped += 1
    return kept, dropped, worst


def check(name, cols, rows, emitter):
    vc = grid_vertex_count(cols, rows)
    idx = emitter(cols, rows)
    worst = max(idx) if idx else -1
    oob = [v for v in idx if not (0 <= v < vc)]
    kept, dropped, gate_worst = gate(vc, idx)
    print(
        f"{name:<10} {cols:>3}x{rows:<3} verts={vc:>6} idx={len(idx):>7} "
        f"max={worst:>6} vertexCount-1={vc - 1:>6} out_of_range={len(oob):<5} "
        f"gate_dropped={dropped:<4} gate_kept={len(kept):<7} gate_max={(max(kept) if kept else -1):>6}"
    )
    return vc, idx, oob, kept


print("=== the sizes the tests cover (cols, rows -> vertex count) ===")
shapes = [(0, 0), (0, 1), (1, 1), (2, 1), (1, 2), (0, 5), (1, 3),
          (84, 3), (63, 4), (0, 257), (99, 3), (12, 2)]
counts = set()
ok = True
for cols, rows in shapes:
    counts.add(grid_vertex_count(cols, rows))
    vc, idx, oob, kept = check("shipped", cols, rows, grid_indices)
    ok = ok and not oob and (not idx or max(idx) == vc - 1)
print("vertex counts covered:", sorted(counts))
print("shipped generator: every index < vertexCount and max == vertexCount-1 ->", ok)

print()
print("=== the same shapes through the pre-fix hand-written emitter ===")
same = all(prefx_emitter(c, r) == grid_indices(c, r) for c, r in shapes)
print("pre-fix emitter output identical to the shipped generator ->", same)

print()
print("=== the shipped river grids ===")
for name, (cols, rows) in {"corridor": (480, 49), "water": (640, 35)}.items():
    check(name, cols, rows, grid_indices)

print()
print("=== the off-by-one emitter (what the tombstone's 256 implies) ===")
cols, rows = 63, 4
vc, idx, oob, kept = check("offbyone", cols, rows, offbyone_emitter)
print(f"    vertexCount={vc}  offending indices={sorted(set(oob))}  "
      f"fault byte offset for element {vc} = {vc * 16 + 12} (tombstone: 4108)")
print(f"    gate dropped {vc and (len(idx) - len(kept)) // 3} triangle(s); "
      f"kept max index = {max(kept)} < {vc} -> nothing out of range reaches Skia")
