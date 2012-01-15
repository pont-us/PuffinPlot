package net.talvi.puffinplot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * This class collects miscellaneous, general-purpose utility functions
 * which are useful to PuffinPlot.
 * 
 * @author pont
 */
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
    
    /**
     * <p>Converts a string specification of number ranges to a corresponding
     * {@link BitSet}. The specification is of the form commonly encountered
     * in Print dialog boxes: a sequence of comma-separated units. Each unit
     * can be either an integer (meaning that the corresponding item should
     * be selected) or two integers separated by a hyphen (-) character
     * (meaning that all items in the corresponding range should be 
     * selected). Example inputs and outputs are shown below.</p>
     * 
     * <table>
<tr><td> 1 </td><td> {@code 1} </td></tr>
<tr><td> 1,3 </td><td> {@code 101} </td></tr>
<tr><td> 4-6 </td><td> {@code 000111} </td></tr>
<tr><td> 4-6,8-10,10,11,15-16 </td><td> {@code 0001110111100011} </td></tr>
<tr><td> 1-4,3-5,10,12-14,17 </td><td> {@code 11111000010111001} </td></tr>
</table>
     * 
     * <p>Note that the range specifications are one-based (the first
     * item is specified by 1, not 0) but the {@link BitSet} output
     * is zero-based.</p>
     * 
     * <p>Since an error in the specification string might result in
     * an impractically large bitset, this method also takes a limit
     * argument; no bits will be set beyond the specified limit.</p>
     * 
     * @param input a specification string
     * @param limit upper limit for bits to set; {@code limit-1} will be the 
     * highest possible set bit
     * @return a bit-set representation of the specified range
     */
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
                result.set(start, end+1);
            }
        }
        return result;
    }
}
