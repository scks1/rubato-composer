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

package org.rubato.math.harmony.test;

/**
 * The numbers of Mazzola, <i>The Topos of Music IV</i>, App. P.1.1
 * (pp. 1525&ndash;1526), one row per TI orbit of seven-element scales in Z12.
 * Columns: class number of App. N.1 (pp. 1502&ndash;1505); that class's
 * five-element representative, whose complement is the scale; the number of
 * minimal cadence sets of the triadic interpretation; the number of quanta;
 * the number of quantized modulations; "!" where some transposition p has no
 * quantized modulation.
 */
final class ToposP11 {

    private ToposP11() { }

    static final String[][] ROWS = {
        { "38", "0,1,2,3,4", "9", "42", "54", "!" },
        { "38.1", "0,2,4,7,9", "5", "20", "26", "" },
        { "39", "0,1,2,3,5", "9", "29", "93", "" },
        { "39.1", "0,1,3,5,10", "6", "23", "55", "" },
        { "40", "0,1,2,3,6", "10", "24", "108", "" },
        { "40.1", "0,1,3,6,8", "7", "26", "72", "" },
        { "41", "0,1,2,3,7", "7", "25", "75", "" },
        { "41.1", "0,1,2,5,7", "6", "21", "53", "" },
        { "42", "0,1,2,4,5", "6", "22", "54", "" },
        { "42.1", "0,1,3,5,8", "7", "28", "74", "" },
        { "43", "0,1,2,4,6", "6", "22", "57", "" },
        { "43.1", "0,1,3,5,7", "7", "26", "72", "" },
        { "44", "0,1,2,4,7", "9", "23", "89", "" },
        { "45", "0,1,2,4,8", "7", "21", "63", "" },
        { "45.1", "0,1,4,6,8", "10", "21", "105", "" },
        { "46", "0,1,2,4,9", "6", "26", "56", "" },
        { "47", "0,1,2,4,10", "6", "28", "30", "" },
        { "47.1", "0,2,4,6,9", "15", "66", "114", "" },
        { "48", "0,1,2,5,6", "10", "23", "109", "" },
        { "48.1", "0,1,3,7,8", "7", "28", "68", "" },
        { "49", "0,1,2,5,8", "7", "21", "71", "" },
        { "49.1", "0,1,3,8,9", "7", "26", "74", "" },
        { "50", "0,1,2,5,9", "7", "34", "42", "" },
        { "50.1", "0,1,3,4,8", "6", "36", "46", "" },
        { "51", "0,1,2,6,7", "9", "13", "86", "" },
        { "52", "0,1,2,6,8", "5", "24", "24", "!" },
        { "53", "0,1,3,4,6", "7", "27", "67", "" },
        { "53.1", "0,1,3,6,10", "9", "25", "91", "" },
        { "54", "0,1,3,4,7", "7", "32", "71", "" },
        { "54.1", "0,1,4,6,9", "21", "32", "226", "" },
        { "55", "0,1,3,5,6", "6", "30", "32", "!" },
        { "56", "0,1,3,5,9", "7", "24", "70", "" },
        { "57", "0,1,3,6,7", "8", "21", "71", "" },
        { "58", "0,1,3,6,9", "18", "17", "185", "" },
        { "59", "0,1,3,7,9", "11", "22", "101", "" },
        { "60", "0,1,4,5,8", "6", "21", "60", "" },
        { "61", "0,1,4,7,8", "10", "38", "62", "" },
        { "62", "0,2,4,6,8", "5", "24", "24", "!" },
    };

    /** The seven-element scale of a row: the complement of its representative. */
    static int[] scale(String[] row) {
        boolean[] five = new boolean[12];
        for (String s : row[1].split(",")) {
            five[Integer.parseInt(s)] = true;
        }
        int[] out = new int[7];
        int k = 0;
        for (int i = 0; i < 12; i++) {
            if (!five[i]) {
                out[k++] = i;
            }
        }
        return out;
    }
}
