/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.rubato.io.musicxml;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Plans the tuplet brackets of one MusicXML voice in one measure
 * (sources/MUSICXML-TUPLETS-SPEC.md).
 * <p>
 * The input is the voice's timeline in the measure: segments (notes or
 * gaps) that cover the measure from the barline, in quarter notes from the
 * barline. The planner divides the measure along the meter (halves, beats,
 * then halves or thirds of a beat) and, for each span, compares three
 * spellings: written directly, which needs every boundary in the span on
 * the grid of 1024th notes; one bracket q:n over the span; or the span's
 * metrical parts, each planned the same way. The spelling with the lowest
 * cost wins: one per written note or rest, two per bracket and a half per
 * note or rest under a bracket. Brackets are not nested.
 * <p>
 * A bracket over a span of W quarter notes takes q, the odd part of the
 * lowest common denominator of the boundaries' positions as fractions of
 * W, and n, the largest power of 2 below q. Its normal note W / n must be a
 * plain or dotted value, q must not exceed {@link #MAX_ACTUAL}, and every
 * boundary's written position, its sounding position times q / n, must
 * fall on the grid of 1024th notes.
 */
final class TupletPlanner {

    /** The largest actual-notes number of a bracket. */
    static final int MAX_ACTUAL = 255;
    /** The written grid: a 1024th note is 1/256 quarter. */
    static final Frac GRID = Frac.of(1, 256);
    /** Spans are not divided below a 128th note. */
    private static final Frac SMALLEST_SPAN = Frac.of(1, 32);

    /** A note or a gap of the timeline, from and to in quarter notes from the barline. */
    static final class Segment {
        final Frac from;
        final Frac to;
        /** The index of the chord in the caller's list, or -1 for a gap. */
        final int chord;

        Segment(Frac from, Frac to, int chord) {
            this.from = from;
            this.to = to;
            this.chord = chord;
        }
    }

    /** A bracket: q notes in the time of n normal notes of value {@code normal}, over [from, from + span). */
    static final class Bracket {
        final int actual;
        final int normal;
        /** The index in {@link MusicXMLWriter}'s value table of the normal note. */
        final int normalValue;
        final Frac from;
        final Frac span;

        Bracket(int actual, int normal, int normalValue, Frac from, Frac span) {
            this.actual = actual;
            this.normal = normal;
            this.normalValue = normalValue;
            this.from = from;
            this.span = span;
        }

        /** The written length of a sounding length under this bracket. */
        Frac written(Frac sounding) {
            return sounding.multiply(Frac.of(this.actual, this.normal));
        }

        /** The sounding length of a written length under this bracket. */
        Frac sounding(Frac written) {
            return written.multiply(Frac.of(this.normal, this.actual));
        }
    }

    /** A part of a segment: [from, to) in quarter notes from the barline, under a bracket or none. */
    static final class Cell {
        final int segment;
        final Frac from;
        Frac to;
        final Bracket bracket;

        Cell(int segment, Frac from, Frac to, Bracket bracket) {
            this.segment = segment;
            this.from = from;
            this.to = to;
            this.bracket = bracket;
        }
    }

    private static final class Plan {
        final double cost;
        final List<Cell> cells;

        Plan(double cost, List<Cell> cells) {
            this.cost = cost;
            this.cells = cells;
        }
    }

    private final List<Segment> segments;
    private final List<Frac> marks;
    private final int beats;
    private final int beatType;

    /**
     * @param segments the timeline, in order, covering the measure
     * @param marks further positions that must fall on a written position (grace notes)
     */
    TupletPlanner(List<Segment> segments, List<Frac> marks, int beats, int beatType) {
        this.segments = segments;
        this.marks = marks;
        this.beats = beats;
        this.beatType = beatType;
    }

    static boolean onGrid(Frac f) {
        return f.divide(GRID).den.equals(BigInteger.ONE);
    }

    /** The cells of the cheapest plan with adjacent unbracketed cells of one segment merged, or null when no plan exists. */
    List<Cell> plan() {
        Frac measure = Frac.of(4L * this.beats, this.beatType);
        Plan p;
        if (this.beatType >= 8 && this.beats % 3 == 0 && this.beats > 3) {
            p = this.planSpan(Frac.ZERO, measure, this.beats / 3, Frac.of(3 * 4, this.beatType));
        }
        else {
            p = this.planSpan(Frac.ZERO, measure, this.beats, Frac.of(4, this.beatType));
        }
        if (p == null) {
            return null;
        }
        List<Cell> merged = new ArrayList<Cell>();
        for (Cell c : p.cells) {
            Cell last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && last.segment == c.segment && last.bracket == null && c.bracket == null && last.to.equals(c.from)) {
                last.to = c.to;
            }
            else {
                merged.add(new Cell(c.segment, c.from, c.to, c.bracket));
            }
        }
        return merged;
    }

    /** Positions strictly inside (a, b): segment boundaries and marks. */
    private List<Frac> inside(Frac a, Frac b) {
        List<Frac> out = new ArrayList<Frac>();
        for (Segment s : this.segments) {
            if (s.from.compareTo(a) > 0 && s.from.compareTo(b) < 0) {
                out.add(s.from);
            }
        }
        for (Frac m : this.marks) {
            if (m.compareTo(a) > 0 && m.compareTo(b) < 0) {
                out.add(m);
            }
        }
        return out;
    }

    /** The segments' parts in [a, b), as cells under the given bracket. */
    private List<Cell> clip(Frac a, Frac b, Bracket bracket) {
        List<Cell> out = new ArrayList<Cell>();
        for (int i = 0; i < this.segments.size(); i++) {
            Segment s = this.segments.get(i);
            Frac from = s.from.compareTo(a) > 0 ? s.from : a;
            Frac to = s.to.compareTo(b) < 0 ? s.to : b;
            if (from.compareTo(to) < 0) {
                out.add(new Cell(i, from, to, bracket));
            }
        }
        return out;
    }

    /**
     * Plans the span [a, a + w). {@code count} is the number of metrical units
     * of length {@code unit} the span holds above the beat level, or 1 at and
     * below the beat.
     */
    private Plan planSpan(Frac a, Frac w, int count, Frac unit) {
        Frac b = a.add(w);
        List<Frac> points = this.inside(a, b);
        Plan best = null;
        // directly
        boolean direct = onGrid(a) && onGrid(b);
        for (Frac p : points) {
            direct = direct && onGrid(p);
        }
        if (direct) {
            List<Cell> cells = this.clip(a, b, null);
            double cost = 0;
            for (Cell c : cells) {
                cost += this.pieces(c, c.to.subtract(c.from), c.from);
            }
            best = new Plan(cost, cells);
            if (points.isEmpty()) {
                return best;
            }
        }
        // one bracket over the span
        if (!direct && onGrid(a)) {
            Plan t = this.bracket(a, w, points);
            if (t != null && (best == null || t.cost < best.cost)) {
                best = t;
            }
        }
        // the span's metrical parts
        List<Frac[]> parts = new ArrayList<Frac[]>();
        int partCount = 1;
        Frac partUnit = unit;
        if (count > 1) {
            if (count % 2 == 0 && count > 2) {
                for (int i = 0; i < 2; i++) {
                    parts.add(new Frac[] { a.add(unit.multiply(Frac.of((long)i * count / 2, 1))), unit.multiply(Frac.of(count / 2, 1)) });
                }
                partCount = count / 2;
            }
            else {
                for (int i = 0; i < count; i++) {
                    parts.add(new Frac[] { a.add(unit.multiply(Frac.of(i, 1))), unit });
                }
            }
        }
        else {
            // a dotted span (3 times a power of 2) divides into thirds, any other into halves
            int k = w.num.equals(BigInteger.valueOf(3).shiftLeft(w.num.getLowestSetBit())) ? 3 : 2;
            Frac part = w.divide(Frac.of(k, 1));
            if (part.compareTo(SMALLEST_SPAN) >= 0) {
                for (int i = 0; i < k; i++) {
                    parts.add(new Frac[] { a.add(part.multiply(Frac.of(i, 1))), part });
                }
            }
            partUnit = part;
        }
        if (!parts.isEmpty() && !(best != null && best.cells.size() <= 1)) {
            double cost = 0;
            List<Cell> cells = new ArrayList<Cell>();
            for (Frac[] part : parts) {
                Plan p = this.planSpan(part[0], part[1], partCount > 1 ? partCount : 1, partCount > 1 ? unit : partUnit);
                if (p == null) {
                    cost = -1;
                    break;
                }
                cost += p.cost;
                cells.addAll(p.cells);
            }
            if (cost >= 0) {
                cost -= mergeSaving(cells);
                if (best == null || cost < best.cost) {
                    best = new Plan(cost, cells);
                }
            }
        }
        return best;
    }

    /** Pieces saved where adjacent unbracketed cells of one segment merge (an estimate: one per merge). */
    private static double mergeSaving(List<Cell> cells) {
        double saving = 0;
        for (int i = 1; i < cells.size(); i++) {
            Cell p = cells.get(i - 1), c = cells.get(i);
            if (p.segment == c.segment && p.bracket == null && c.bracket == null) {
                saving += 1;
            }
        }
        return saving;
    }

    /** The number of written values of a cell: a note as the writer splits notes, a gap as it splits rests. */
    private int pieces(Cell c, Frac written, Frac at) {
        List<Integer> values = this.segments.get(c.segment).chord < 0 ? MusicXMLWriter.alignedValues(written, at) : null;
        if (values == null) {
            values = MusicXMLWriter.greedyValues(written);
        }
        return values.size();
    }

    /** One bracket over [a, a + w), or null when no allowed bracket expresses the points. */
    private Plan bracket(Frac a, Frac w, List<Frac> points) {
        if (points.isEmpty()) {
            return null;
        }
        BigInteger lcd = BigInteger.ONE;
        for (Frac p : points) {
            BigInteger d = p.subtract(a).divide(w).den;
            lcd = lcd.divide(lcd.gcd(d)).multiply(d);
        }
        BigInteger odd = lcd.shiftRight(lcd.getLowestSetBit());
        if (odd.equals(BigInteger.ONE) || odd.compareTo(BigInteger.valueOf(MAX_ACTUAL)) > 0) {
            return null;
        }
        int q = odd.intValue();
        int n = Integer.highestOneBit(q);
        int normalValue = MusicXMLWriter.valueIndex(w.divide(Frac.of(n, 1)));
        if (normalValue < 0) {
            return null;
        }
        Bracket br = new Bracket(q, n, normalValue, a, w);
        for (Frac p : points) {
            if (!onGrid(br.written(p.subtract(a)))) {
                return null;
            }
        }
        List<Cell> cells = this.clip(a, a.add(w), br);
        double pieces = 0;
        for (Cell c : cells) {
            pieces += this.pieces(c, br.written(c.to.subtract(c.from)), br.written(c.from.subtract(a)));
        }
        return new Plan(pieces + 2 + 0.5 * pieces, cells);
    }
}
