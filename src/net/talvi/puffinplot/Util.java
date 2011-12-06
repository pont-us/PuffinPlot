/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Util {
    
    /**
     * Takes an integer, reduces it by one, and ensures it lies in the
     * range 0 <= i < upperLimit.
     */
    private static int constrainInt(int i, int upperLimit) {
        i -= 1;
        if (i<0) i = 0;
        if (i>= upperLimit) i = upperLimit-1;
        return i;
    }
    
    public static BitSet numberRangeStringToBitSet(String input, int limit) {
        Scanner sc = new Scanner(input);
        sc.useLocale(Locale.ENGLISH);
        sc.useDelimiter(", *");
        BitSet result = new BitSet();
        List<String> rangeExprs = new ArrayList<String>();
        while (sc.hasNext()) {
            if (sc.hasNextInt()) {
                final int val = constrainInt(sc.nextInt(), limit);
                result.set(val);
            } else {
                rangeExprs.add(sc.next());
            }
        }
        for (String rangeExpr: rangeExprs) {
            int start = -1;
            int end = -1;
            Scanner sc2 = new Scanner(rangeExpr);
            sc2.useLocale(Locale.ENGLISH);
            sc2.useDelimiter("-");
            if (sc2.hasNextInt()) start = constrainInt(sc2.nextInt(), limit);
            if (sc2.hasNextInt()) end = constrainInt(sc2.nextInt(), limit);
            if (start>-1 && end>-1) {
                result.set(start, end+1); // TODO check fenceposts!
            }
        }
        return result;
    }
}
