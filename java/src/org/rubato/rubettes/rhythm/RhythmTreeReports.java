/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package org.rubato.rubettes.rhythm;

import java.util.ArrayList;
import java.util.List;

import org.rubato.io.ScoreNote;
import org.rubato.math.rhythm.Fraction;
import org.rubato.math.rhythm.RhythmLayout;
import org.rubato.math.rhythm.RhythmNotation;
import org.rubato.math.rhythm.RhythmTree;
import org.rubato.math.rhythm.RhythmTreeDenotators;
import org.rubato.math.yoneda.Denotator;

/**
 * Renders a rhythm tree as a Score and describes the result as text
 * (sources/RHYTHM-TREES-SPEC.md section 2 item 3). Each leaf becomes a note
 * (D6) at its exact onset and length rounded to doubles (D7), with one
 * pitch, loudness and voice for all; identical notes merge because a Score
 * is a set (D9). The text lists every node's exact span and is the
 * alternative for {@link RhythmSpanPanel}'s drawing.
 */
public final class RhythmTreeReports {

    /** Figure 2 of Flieder, arXiv 2512.04090 (p. 56). */
    public static final String DEFAULT_TREE = "9.5 (2 2.5 (1 (2 1) 1 1) 3 (1.5 2))";

    /** The rubette's properties. */
    public static final class Settings {
        public final String tree;
        public final String timing;
        public final String start;
        public final int pitch;
        public final int loudness;
        public final int voice;

        public Settings(String tree, String timing, String start, int pitch, int loudness, int voice) {
            this.tree = tree;
            this.timing = timing;
            this.start = start;
            this.pitch = pitch;
            this.loudness = loudness;
            this.voice = voice;
        }

        /** Figure 2, proportional timing, from 0, middle C at loudness 80 in voice 0. */
        public static Settings defaults() {
            return new Settings(DEFAULT_TREE, RhythmLayout.Timing.PROPORTIONAL.word(), "0", 60, 80, 0);
        }

        public Settings withTiming(String t) {
            return new Settings(tree, t, start, pitch, loudness, voice);
        }
    }

    /** A rendered tree. */
    public static final class Report {
        private final RhythmLayout layout;
        private final Denotator score;
        private final Denotator treeDenotator;
        private final String text;
        private final String summary;

        Report(RhythmLayout layout, Denotator score, Denotator treeDenotator, String text, String summary) {
            this.layout = layout;
            this.score = score;
            this.treeDenotator = treeDenotator;
            this.text = text;
            this.summary = summary;
        }

        public RhythmLayout layout() {
            return layout;
        }

        /** The Score, output 0. */
        public Denotator score() {
            return score;
        }

        /** The tree as a RhythmTree denotator, output 1. */
        public Denotator treeDenotator() {
            return treeDenotator;
        }

        public String text() {
            return text;
        }

        /** One line for the rubette's info. */
        public String summary() {
            return summary;
        }
    }

    private RhythmTreeReports() { /* static only */ }

    /**
     * Renders the input tree, or the settings' tree text when input is null.
     *
     * @throws IllegalArgumentException for a refused input, tree text, timing
     *         or start onset
     */
    public static Report render(Denotator input, Settings s) {
        RhythmLayout.Timing timing = RhythmLayout.Timing.of(s.timing == null ? "" : s.timing);
        if (timing == null) {
            throw new IllegalArgumentException("unknown timing \"" + s.timing + "\"; use proportional or label");
        }
        Fraction start;
        try {
            start = Fraction.parse(s.start == null ? "" : s.start.trim());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("the start onset must be a number such as 0, 2.5 or 19/2, not \"" + s.start + "\"");
        }
        if (start.signum() < 0) {
            throw new IllegalArgumentException("the start onset must be 0 or more, not " + start);
        }
        RhythmTree tree;
        String source;
        if (input != null) {
            tree = RhythmTreeDenotators.fromDenotator(input);
            source = "from the input";
        }
        else {
            tree = RhythmNotation.parse(s.tree);
            source = "from the tree property";
        }
        RhythmLayout layout = RhythmLayout.layout(tree, timing, start);
        Denotator treeDenotator = RhythmTreeDenotators.toDenotator(tree);

        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        Fraction last = start;
        for (RhythmLayout.Span sp : layout.leaves()) {
            notes.add(new ScoreNote(sp.onset.toDouble(), s.pitch, s.loudness, sp.length.toDouble(), s.voice));
            if (sp.end().compareTo(last) > 0) {
                last = sp.end();
            }
        }
        Denotator score = ScoreNote.toScore(notes);
        int inScore = ScoreNote.fromScore(score).size();
        int merged = notes.size() - inScore;

        StringBuilder b = new StringBuilder();
        b.append("Rhythm tree (").append(source).append("): ").append(tree.toNotation()).append('\n');
        if (timing == RhythmLayout.Timing.PROPORTIONAL) {
            b.append("Timing: proportional (spec D1). The root lasts its label; a child lasts its parent's\n"
                     + "length times its factor over the sum of the factors. Own labels after @ are not used.\n");
        }
        else {
            b.append("Timing: label (spec D2). Every node lasts its own label; the factors are not used.\n");
        }
        b.append(tree.nodeCount()).append(" nodes, depth ").append(tree.depth()).append(", ")
         .append(layout.leaves().size()).append(" leaves, start ").append(start).append(".\n\n");

        String[] head = { "Node", "Label", "Factors", "Onset", "Length", "End", "" };
        List<String[]> rows = new ArrayList<String[]>();
        rows.add(head);
        for (RhythmLayout.Span sp : layout.spans()) {
            StringBuilder fs = new StringBuilder();
            for (Fraction r : sp.node.getFactors()) {
                fs.append(fs.length() == 0 ? "" : " ").append(r);
            }
            rows.add(new String[] {
                indent(sp.depth) + (sp.path.isEmpty() ? "root" : sp.path),
                sp.node.getLabel().toString(),
                fs.length() == 0 ? "-" : fs.toString(),
                sp.onset.toString(), sp.length.toString(), sp.end().toString(),
                sp.isLeaf() ? "note" : "" });
        }
        int[] w = new int[head.length];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                w[i] = Math.max(w[i], row[i].length());
            }
        }
        for (String[] row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                line.append(row[i]);
                if (i < row.length - 1) {
                    for (int k = row[i].length(); k < w[i] + 2; k++) {
                        line.append(' ');
                    }
                }
            }
            b.append(line.toString().replaceAll("\\s+$", "")).append('\n');
        }

        if (!layout.mismatches().isEmpty()) {
            b.append("\nNodes whose children do not fill them:\n");
            for (RhythmLayout.Span sp : layout.mismatches()) {
                Fraction diff = sp.childrenTotal.subtract(sp.length);
                b.append("  ").append(sp.path.isEmpty() ? "root" : sp.path)
                 .append(": the children last ").append(sp.childrenTotal)
                 .append(", the node ").append(sp.length).append(" (")
                 .append(diff.signum() < 0 ? diff.multiply(Fraction.of(-1)) + " short" : diff + " over").append(")\n");
            }
        }

        b.append("\nScore: ").append(inScore).append(inScore == 1 ? " note" : " notes").append(", pitch ").append(s.pitch)
         .append(", loudness ").append(s.loudness).append(", voice ").append(s.voice)
         .append(". Onsets and durations are the exact values above rounded to doubles (spec D7).\n");
        if (merged > 0) {
            b.append(merged).append(merged == 1 ? " identical note merged" : " identical notes merged")
             .append(": leaves with the same onset and length give the same note, and a Score is a set (spec D9).\n");
        }

        String summary = inScore + (inScore == 1 ? " note, " : " notes, ") + timing.word() + " timing, last note ends at " + last;
        return new Report(layout, score, treeDenotator, b.toString(), summary);
    }

    private static String indent(int depth) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            b.append("  ");
        }
        return b.toString();
    }
}
