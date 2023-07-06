package com.ratedra.keyvaluestore;

import java.util.ArrayList;

class Tuple<K, V>{
    K key;
    V value;

    public Tuple(K key) {
        this.key = key;
    }

    public Tuple(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

class Node<K, V>{
    Tuple<K, V> data;
    Node<K, V> next;

    public Node(Tuple<K, V> data) {
        this.data = data;
        this.next = null;
    }
}

class CustomLinkedList<K, V>{
    Node<K, V> head;

    public CustomLinkedList() {
        this.head = null;
    }

    public void push(Tuple<K, V> data){
        if(head == null){
            head = new Node<>(data);
        } else{
            Node<K, V> temp = new Node(data);
            temp.next = head;
            head = temp;
        }
    }

    public Tuple<K, V> search(K key){
        Node<K, V> itr = head;
        while(itr != null){
            /**
             * Tuple key type should define their own equals method
             */
            if(itr.data.key.equals(key)){
                return itr.data;
            }
            itr = itr.next;
        }
        return null;
    }
}

class KeyValueStore<K, V>{
    private static final Integer DEFAULT_INITIAL_CAPACITY = 10;

    private ArrayList<CustomLinkedList<K, V>> buckets;

    public KeyValueStore() {
        this.buckets = new ArrayList<>(DEFAULT_INITIAL_CAPACITY);
        for (int i = 0; i < DEFAULT_INITIAL_CAPACITY; i++) {
            buckets.add(null);
        }
    }

    void put(K key, V value){
        int bucketIndex = getBucketIndex(key);
        CustomLinkedList<K, V> currentBucket = buckets.get(bucketIndex);
        if(currentBucket == null){
            CustomLinkedList<K, V> bucket = new CustomLinkedList<>();
            bucket.push(new Tuple<>(key, value));
            buckets.add(bucketIndex, bucket);
        } else{
            currentBucket.push(new Tuple<>(key, value));
        }
    }

    V get(K key){
        int bucketIndex = getBucketIndex(key);
        CustomLinkedList<K, V> bucket = buckets.get(bucketIndex);
        if(bucket != null){
            Tuple<K, V> foundTuple = bucket.search(key);
            return foundTuple.value;
        }
        return null;
    }

    private int getBucketIndex(K key){
        assert key != null;

        return Math.abs(key.hashCode()) % buckets.size();
    }
}

public class KeyValueStoreLLD {
    public static void main(String[] args) {

    }
}
