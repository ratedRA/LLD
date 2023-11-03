package com.ratedra.thread;


// to print from 1 to n numbers in a loop
// two thread - odd thread and the other is even thread
// sequential - odd then even then odd - wait and notify
public class OddEvenThread {
    int n = 20;
    int ind = 1;
    boolean printOdd = true;

    private void printOdd(){
        synchronized (this){
            while(ind < 20){

                if(!printOdd){
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                System.out.println("Odd thread: "+ ind);
                ind++;
                printOdd = false;
                notify();
            }
        }
    }

    private void printEven(){
        synchronized (this){
            while(ind < 20){

                if(printOdd){
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                System.out.println("Even Thread: "+ ind);
                ind++;
                printOdd = true;
                notify();
            }
        }
    }

    public static void main(String[] args) {
        OddEvenThread oddEvenThread = new OddEvenThread();
        Thread oddThread = new Thread(() -> {
            oddEvenThread.printOdd();
        });

        Thread evenThread = new Thread(() -> {
            oddEvenThread.printEven();
        });
        oddThread.start();
        evenThread.start();
    }
}
