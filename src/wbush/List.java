package wbush;

/**
 * Bytecode-efficient linked list.
 * 
 * Proper usage:
 * 
 * List<T> L = new List<T>();
 * L.add(val);
 * L.add(val2);
 * ...
 * 
 * L.resetIter();
 * while (L.currNotNull()) {
 *     val = L.curr();
 *     if (decideToRemoveVal) {
 *         L.popStep();
 *     } else {
 *         L.step();
 *     }
 * }
 */

// TODO: @Mihir is there any more room for bytecode optimization?

public class List<T> {

    public class ListNode {

        T val;
        ListNode next;
    
        ListNode(T val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    int length;
    ListNode root;
    ListNode curr;
    ListNode prev;

    public List() {
        length = 0;
        root = new ListNode(null, null);
        curr = null;
        prev = root;
    }

    public void add(T val) {
        root.next = new ListNode(val, root.next);
        length++;
    }
    
    public T curr() {
        return curr.val;
    }

    /**
     * Return the current value and step forward.
     */
    public T step() {
        prev = curr;
        curr = curr.next;
        return prev.val;
    }

    /**
     * Pop the current value and step forward.
     */
    public T popStep() {
        T valToReturn = curr.val;
        prev.next = curr.next;
        curr = curr.next;
        length--;
        return valToReturn;
    }

    /**
     * Reset list iteration vars to start. Run before
     * iterating through the loop.
     */
    public void resetIter() {
        prev = root;
        curr = root.next;
    }

    /**
     * Returns true if the current ListNode exists.
     */
    public boolean currNotNull() {
        return curr != null;
    }
}