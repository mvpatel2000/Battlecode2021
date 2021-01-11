package scoutplayer;

public class List<T> {

    class ListNode {

        T val;
        ListNode next;
    
        ListNode(T val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    int length;
    ListNode first;
    ListNode curr;

    public List() {
        length = 0;
        first = null;
        curr = null;
    }

    public void add(T val) {
        ListNode second = first;
        first = new ListNode(val, second);
    }
    
    public T next() {
        curr = curr.next;
        return curr.val;
    }
    
    public void resetIter() {
        curr = first;
    }

    public boolean hasNext() {
        return (curr.next != null);
    }
}