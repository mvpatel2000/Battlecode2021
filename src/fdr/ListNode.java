package fdr;

public class ListNode<T> {

    T val;
    ListNode<T> next;

    public ListNode(T val, ListNode<T> next) {
        this.val = val;
        this.next = next;
    }
}