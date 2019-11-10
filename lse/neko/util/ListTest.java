package lse.neko.util;

// java imports:
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ListTest {

    public static void main(String[] args) {
        List l1 = new ArrayList();
        SparseArrayList l2 = new SparseArrayList();
        ListOperation l1InvariantChecker =
            new ListInvariantChecker();
        ListOperation l2InvariantChecker =
            new SparseArrayListInvariantChecker();

        ListOperation[] listOperations = {
            new ListOperation() {
                public void doIt(List list) {
                    list.addAll(Arrays.asList(new Object[]{
                        new Integer(2), new Integer(3)}));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.add(new Integer(1));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.remove(1);
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.set(0, null);
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.set(0, null);
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.set(0, new Integer(5));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.set(0, new Integer(4));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.addAll(1, Collections.nCopies(230, new Integer(13)));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.addAll(14, Collections.nCopies(230, null));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.subList(16, 400).clear();
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.addAll(32, Collections.nCopies(230, null));
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.remove(1);
                }
            },
            new ListOperation() {
                public void doIt(List list) {
                    list.add(new Integer(1));
                }
            }
        };

        int i = -1;
        boolean comparing = true;
        try {
            compareLists(l1, l2);
            for (i = 0; i < listOperations.length; i++) {
                comparing = false;
                ListOperation op = listOperations[i];
                op.doIt(l1);
                op.doIt(l2);
                comparing = true;
                compareLists(l1, l2);
                //System.out.println("Lists after operation #"+i+": "
                //                   +Util.toString(l1.toArray()));
                l1InvariantChecker.doIt(l1);
                l2InvariantChecker.doIt(l2);
            }
        } catch (Exception ex) {
            if (i < 0) {
                System.out.println("Error while comparing empty lists");
            } else if (comparing) {
                System.out.println("Error while comparing lists "
                                   + "after operation #" + i);
            } else {
                System.out.println("Error while executing operation #" + i);
            }
            if (ex instanceof ListCompareException) {
                System.out.println("Cause: " + ex);
            } else {
                ex.printStackTrace();
            }
            System.out.println("List #1: " + Util.toString(l1.toArray()));
            System.out.println("List #2: " + Util.toString(l2.toArray()));
            System.exit(1);
        }
        System.out.println("Test successful");
        System.exit(0);
    }

    public static class ListCompareException
        extends Exception
    {
        ListCompareException(String s) {
            super(s);
        }

        ListCompareException() {
            super();
        }
    }

    public static void compareLists(List left, List right)
        throws ListCompareException
    {
        if (left.size() != right.size()) {
            throw new ListCompareException("Different sizes: " + left.size()
                                           + " and " + right.size());
        }
        for (int i = 0; i < left.size(); i++) {
            Object oLeft = left.get(i);
            Object oRight = right.get(i);
            if ((oLeft != null && oRight == null)
                || (oLeft == null && oRight != null)
                || (oLeft != null && oRight != null
                    && !oLeft.equals(oRight)))
            {
                throw new ListCompareException("Elements #" + i + " mismatch: "
                                               + oLeft + " and " + oRight);
            }
        }
    }

    public interface ListOperation {
        void doIt(List list);
    }

    public static class ListInvariantChecker
        implements ListOperation
    {
        public void doIt(List list) {
            boolean ok = false;
            try {
                list.get(list.size());
            } catch (IndexOutOfBoundsException ex) {
                ok = true;
            }
            if (!ok) {
                throw new RuntimeException("No IndexOutOfBoundsException "
                                           + "when reading "
                                           + "beyond the list size!");
            }
        }
    }

    public static class SparseArrayListInvariantChecker
        extends ListInvariantChecker
    {
        public void doIt(List list) {
            super.doIt(list);
            int nonNullCount = 0;
            for (int i = 0; i < list.size(); i++) {
                if (null != list.get(i)) {
                    nonNullCount++;
                }
            }
            SparseArrayList l = (SparseArrayList) list;
            if (nonNullCount != l.getNonNullCount()) {
                throw new RuntimeException("Non-null objects: "
                                           + nonNullCount
                                           + " counted by the list: "
                                           + l.getNonNullCount());
            }
        }
    }

}


