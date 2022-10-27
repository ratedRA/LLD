package com.ratedra.splitwise;

import java.util.*;
import java.util.stream.Collectors;

class User{
    private String id;
    private String email;
    private String phoneNumber;

    public User(String id, String email, String phoneNumber) {
        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public User(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}

class Group{
    List<User> users;
    List<Expense> expenses;

    public Group(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }
}

enum ExpenseType{
    EXACT,
    EQUAL,
    PERCENT;
}

class Split{
    private String userId;
    private Double ExactAmount;
    private Integer percent;

    public Split(String userId, Double exactAmount) {
        this.userId = userId;
        ExactAmount = exactAmount;
    }

    public Split(String userId, Integer percent) {
        this.userId = userId;
        this.percent = percent;
    }

    public String getUserId() {
        return userId;
    }

    public Double getExactAmount() {
        return ExactAmount;
    }

    public Integer getPercent() {
        return percent;
    }
}

abstract class Expense{
    private String id;
    private Double amount;
    private String addedBy;
    private Group group;
    private List<String> individualIds;
    private List<Split> splits;

    private Map<String, Double> splitByUserMap = new HashMap<>();

    public Expense(String id, Double amount, String addedBy, Group group, List<Split> splits) {
        this.id = id;
        this.amount = amount;
        this.addedBy = addedBy;
        this.group = group;
        this.splits = splits;
        this.individualIds = null;

        if(this.validateAndSplitExpense(splits)){
            System.out.println("Expense Successfully Added;");
        } else{
            throw new RuntimeException("Error adding expense");
        }
    }

    public Expense(String id, Double amount, String addedBy, List<String> individualIds, List<Split> splits) {
        this(id, amount, addedBy, (Group) null, splits);
        this.individualIds = individualIds;

        if(this.validateAndSplitExpense(splits)){
            System.out.println("Expense Successfully Added;");
        }
    }

    public abstract ExpenseType getExpenseType();

    public abstract boolean validateAndSplitExpense(List<Split> splits);

    public boolean isGroupExpense(){
        return group != null;
    }

    public List<String> getUserIds(){
        List<String> userIds = new ArrayList<>();
        if (isGroupExpense()) {
            List<User> users = this.getGroup().getUsers();
            userIds = users.stream().map(user -> user.getId()).collect(Collectors.toList());
        } else {
            userIds = this.getIndividualIds();

            // adding current user too to add in the split
            userIds.add(this.getAddedBy());
        }
        return userIds;
    }

    public String getId() {
        return id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public Group getGroup() {
        return group;
    }

    public List<String> getIndividualIds() {
        return individualIds;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public Map<String, Double> getSplitByUserMap() {
        return splitByUserMap;
    }
}

class EqualExpense extends Expense{
    public EqualExpense(String id, Double amount, String addedBy, Group group, List<Split> splits) {
        super(id, amount, addedBy, group, splits);
    }

    public EqualExpense(String id, Double amount, String addedBy, List<String> individualIds, List<Split> splits) {
        super(id, amount, addedBy, individualIds, splits);
    }

    @Override
    public ExpenseType getExpenseType() {
        return ExpenseType.EQUAL;
    }

    @Override
    public boolean validateAndSplitExpense(List<Split> splits) {
        boolean splitsAddedSuccessfully = true;
        try {
            List<String> userIds = getUserIds();

            assert (userIds != null && userIds.size() > 0);

            double equalSplitAmount = this.getAmount() / userIds.size();
            for (String userId : userIds) {
                this.getSplitByUserMap().put(userId, equalSplitAmount);
            }
        } catch (Exception ex){
            splitsAddedSuccessfully = false;
        }
        return splitsAddedSuccessfully;
    }
}

class ExactExpense extends  Expense{
    public ExactExpense(String id, Double amount, String addedBy, Group group, List<Split> splits) {
        super(id, amount, addedBy, group, splits);
    }

    public ExactExpense(String id, Double amount, String addedBy, List<String> individualIds, List<Split> splits) {
        super(id, amount, addedBy, individualIds, splits);
    }

    @Override
    public ExpenseType getExpenseType() {
        return ExpenseType.EXACT;
    }

    @Override
    public boolean validateAndSplitExpense(List<Split> splits) {
        boolean splitsAddedSuccessfully = true;
        try{
            List<String> userIds = getUserIds();
            boolean invalidSplitArgumets = splits.stream().anyMatch(s -> s.getExactAmount() == null || s.getPercent() != null);

            int totalIndividualSplitAmount = 0;
            for(Split split : splits){
                totalIndividualSplitAmount += split.getExactAmount();
            }
            boolean userIdsDontMatch = splits.stream().anyMatch(split -> !userIds.contains(split.getUserId()));

            if(invalidSplitArgumets || totalIndividualSplitAmount != this.getAmount() || userIdsDontMatch){
                throw new RuntimeException("invalid split provided");
            }

            for(Split split : splits){
                this.getSplitByUserMap().put(split.getUserId(), split.getExactAmount());
            }

        } catch (Exception ex){
            splitsAddedSuccessfully = false;
        }

        return splitsAddedSuccessfully;
    }
}

public class SplitwiseLLD {

}
