package com.ratedra.elevator;

import java.util.*;

// assuming just one elevator in the building
class Elevator implements Observer { // observer
    public static final Integer TOPMOST_FLOOR = 5;
    public static final Integer BOTTOMMOST_FLOOR = 0;

    private int currentFloor;
    private Direction currentDirection;
    private Queue<Integer> upQueue;
    private Queue<Integer> downQueue;

    private ElevatorManager elevatorManager;

    public Elevator() {
        this.currentFloor = 0;
        this.currentDirection = Direction.IDLE;
        this.upQueue = new PriorityQueue<>();
        this.elevatorManager = ElevatorManager.getInstance();
        this.downQueue = new PriorityQueue<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
    }

    @Override
    public void update() {
        elevatorManager.run(this);
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(Direction currentDirection) {
        this.currentDirection = currentDirection;
    }

    public Queue<Integer> getUpQueue() {
        return upQueue;
    }

    public void setUpQueue(Queue<Integer> upQueue) {
        this.upQueue = upQueue;
    }

    public Queue<Integer> getDownQueue() {
        return downQueue;
    }

    public void setDownQueue(Queue<Integer> downQueue) {
        this.downQueue = downQueue;
    }
}

interface Observer{
    public void update();
}

abstract class Observable{
    private List<Observer> observers = new ArrayList<>();

    public void addObservers(Observer elevator){
        observers.add(elevator);
    }

    public void notifyObservers(){
        observers.stream().forEach(o -> o.update());
    }
}

abstract class Request extends Observable{ // observable
    private static Integer requestCounter = 0;

    private List<Elevator> observers = new ArrayList<>();

    public abstract Direction getDirection();

    public abstract boolean addToQueue(Elevator elevator, int floor);
}

class UpRequest extends Request{

    @Override
    public Direction getDirection() {
        return Direction.UP;
    }

    @Override
    public boolean addToQueue(Elevator elevator, int floor) {
        try {
            assert (floor >= Elevator.BOTTOMMOST_FLOOR && floor <= Elevator.TOPMOST_FLOOR);
            Queue<Integer> upQueue = elevator.getUpQueue();
            if (!upQueue.contains(floor)) {
                upQueue.offer(floor);
                super.notifyObservers();
            }
        } catch (Exception exception){
            return false;
        }
        return true;
    }
}

class DownRequest extends Request{

    @Override
    public Direction getDirection() {
        return Direction.DOWN;
    }

    @Override
    public boolean addToQueue(Elevator elevator, int floor) {
        try {
            assert (floor >= Elevator.BOTTOMMOST_FLOOR && floor <= Elevator.TOPMOST_FLOOR);
            Queue<Integer> downQueue = elevator.getDownQueue();
            if (!downQueue.contains(floor)) {
                downQueue.offer(floor);
                notifyObservers();
            }
        } catch (Exception e){
            return false;
        }
        return true;
    }
}

class RequestFactory{
    private static Map<Direction, Request> requestByDirection = new HashMap<>();
    static {
        requestByDirection.put(Direction.UP, new UpRequest());
        requestByDirection.put(Direction.DOWN, new DownRequest());
    }

    private static RequestFactory INSTANCE;

    private RequestFactory(){}

    public static RequestFactory getInstance(){
        if(INSTANCE == null){
            INSTANCE = new RequestFactory();
        }
        return INSTANCE;
    }

    public Request getRequest(Direction direction){
        return requestByDirection.get(direction);
    }
}

enum Direction{
    UP,
    DOWN,
    IDLE;
}

class ElevatorManager{

    private static ElevatorManager INSTANCE;
    private ElevatorManager(){};

    public static ElevatorManager getInstance(){
        if(INSTANCE == null){
            INSTANCE = new ElevatorManager();
        }

        return INSTANCE;
    }
    public void addRequest(Elevator elevator, Direction direction, int floor){
        RequestFactory requestFactory =  RequestFactory.getInstance();
        Request request = requestFactory.getRequest(direction);
        request.addObservers(elevator);
        boolean success = request.addToQueue(elevator, floor);
        if(success){
            System.out.println("Request was added successfully");
        } else{
            System.out.println("failed to add Request");
        }
    }

    public void run(Elevator elevator){
        Direction currentDirection = elevator.getCurrentDirection();
        if(currentDirection == Direction.IDLE){
            moveUp(elevator);
            moveDown(elevator);
            elevator.setCurrentDirection(Direction.IDLE);
        }
    }

    private void moveUp(Elevator elevator){
        Queue<Integer> upQueue = elevator.getUpQueue();
        elevator.setCurrentDirection(Direction.UP);
        while (!upQueue.isEmpty()){
            Integer nextFloor = upQueue.poll();
            if(elevator.getCurrentFloor() < nextFloor) {
                elevator.setCurrentFloor(nextFloor);
                System.out.println("Elevator reached " + nextFloor + " floor");
            } else {
                System.out.println("It is actually a move down request");
                addRequest(elevator, Direction.DOWN, nextFloor);
            }
        }
    }

    private void moveDown(Elevator elevator){
        Queue<Integer> downQueue = elevator.getDownQueue();
        elevator.setCurrentDirection(Direction.DOWN);
        while (!downQueue.isEmpty()){
            Integer nextFloor = downQueue.poll();
            if(elevator.getCurrentFloor() > nextFloor) {
                elevator.setCurrentFloor(nextFloor);
                System.out.println("Elevator reached " + nextFloor + " floor");
            } else {
                System.out.println("It is actually a move up request");
                addRequest(elevator, Direction.UP, nextFloor);
            }
        }
    }

}


public class ElevatorLLD {
    public static void main(String[] args) {
        Elevator elevator = new Elevator();

        ElevatorManager elevatorManager = ElevatorManager.getInstance();
        elevatorManager.addRequest(elevator, Direction.UP, 3);
        elevatorManager.addRequest(elevator, Direction.DOWN, 1);
        elevatorManager.addRequest(elevator, Direction.DOWN, 2);
        elevatorManager.addRequest(elevator, Direction.UP, 4);
    }
}
