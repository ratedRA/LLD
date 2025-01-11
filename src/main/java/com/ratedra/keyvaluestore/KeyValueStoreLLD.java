package com.ratedra.keyvaluestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    public List<Tuple<K,V>> getAllTuples(){
        List<Tuple<K,V>> tuples = new ArrayList<>();
        Node<K, V> itr = head;
        while(itr != null){
          tuples.add(itr.data);
        }
        return tuples;
    }
}

class KeyValueStore<K, V>{
    private static final Integer DEFAULT_INITIAL_CAPACITY = 10;
    private static final Double DEFAULT_LOAD_FACTOR = 0.75;
    private int size;
    private int capacity = DEFAULT_INITIAL_CAPACITY;

    private ArrayList<CustomLinkedList<K, V>> buckets;

    public KeyValueStore() {
        this.buckets = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            buckets.add(null);
        }
    }

    void put(K key, V value){
        int bucketIndex = getBucketIndex(key);
        CustomLinkedList<K, V> currentBucket = buckets.get(bucketIndex);
        if(currentBucket == null){
            CustomLinkedList<K, V> bucket = new CustomLinkedList<>();
            bucket.push(new Tuple<>(key, value));
            buckets.set(bucketIndex, bucket);
        } else{
            currentBucket.push(new Tuple<>(key, value));
        }
        size+=1;
        if(shouldResize()){
            CompletableFuture.runAsync(() -> resize());
        }
    }

    void resize(){
        int newCapacity = buckets.size()*2;
        ArrayList<CustomLinkedList<K,V>> newBuckets = new ArrayList<>(newCapacity);

        // initialize with null
        for (int i = 0; i < newCapacity; i++) {
            newBuckets.add(null);
        }

        for(CustomLinkedList<K, V> bucket : buckets){
            if(bucket != null){
                List<Tuple<K,V>> allTuples = bucket.getAllTuples();
                for(Tuple<K,V> tuple : allTuples){
                    int bucketIndex = getBucketIndexForResize(tuple.key, newCapacity);
                    CustomLinkedList<K, V> currentBucket = newBuckets.get(bucketIndex);
                    if(currentBucket == null){
                        CustomLinkedList<K, V> newBucket = new CustomLinkedList<>();
                        newBucket.push(new Tuple<>(tuple.key, tuple.value));
                        newBuckets.set(bucketIndex, bucket);
                    } else{
                        currentBucket.push(new Tuple<>(tuple.key, tuple.value));
                    }
                }
            }
        }
        buckets = newBuckets;
        capacity = newCapacity;
    }

    private boolean shouldResize(){
        double loadFactor = ((double) size)/buckets.size();
        return loadFactor>DEFAULT_LOAD_FACTOR;
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

        return Math.abs(key.hashCode()) % capacity;
    }

    private int getBucketIndexForResize(K key, int newCapacity){
        assert key != null;

        return Math.abs(key.hashCode()) % newCapacity;
    }
}

public class KeyValueStoreLLD {
    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<>();
    }
}
