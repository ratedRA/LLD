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

    private String id;
    List<User> users;
    List<Expense> expenses;

    public Group(List<User> users) {
        this.users = users;
    }

    public String getId() {
        return id;
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

    public Expense() {
    }

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
        }else{
            throw new RuntimeException("Error adding expense");
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
        userIds = userIds.stream().distinct().collect(Collectors.toList());
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

    public void setId(String id) {
        this.id = id;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setIndividualIds(List<String> individualIds) {
        this.individualIds = individualIds;
    }

    public void setSplits(List<Split> splits) {
        this.splits = splits;
    }
}

class EqualExpense extends Expense{
    public EqualExpense() {
    }

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
    public ExactExpense() {
    }

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

            double totalIndividualSplitAmount = 0;
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

class PercentExpense extends Expense{
    public PercentExpense() {
    }

    public PercentExpense(String id, Double amount, String addedBy, Group group, List<Split> splits) {
        super(id, amount, addedBy, group, splits);
    }

    public PercentExpense(String id, Double amount, String addedBy, List<String> individualIds, List<Split> splits) {
        super(id, amount, addedBy, individualIds, splits);
    }

    @Override
    public ExpenseType getExpenseType() {
        return ExpenseType.PERCENT;
    }

    @Override
    public boolean validateAndSplitExpense(List<Split> splits) {
        boolean splitsAddedSuccessfully = true;
        try{
            List<String> userIds = getUserIds();
            boolean invalidSplitArgumets = splits.stream().anyMatch(s -> s.getPercent() == null || s.getExactAmount() != null);

            double totalIndividualSplitPercent = 0;
            for(Split split : splits){
                totalIndividualSplitPercent += split.getPercent();
            }
            boolean userIdsDontMatch = splits.stream().anyMatch(split -> !userIds.contains(split.getUserId()));

            if(invalidSplitArgumets || totalIndividualSplitPercent != this.getAmount() || userIdsDontMatch){
                throw new RuntimeException("invalid split provided");
            }

            for(Split split : splits){
                Integer percent = split.getPercent();
                this.getSplitByUserMap().put(split.getUserId(), calculatePercent(percent));
            }

        } catch (Exception ex){
            splitsAddedSuccessfully = false;
        }

        return splitsAddedSuccessfully;
    }

    private Double calculatePercent(Integer percent){
        assert (this.getAmount() != null);

        Double exactAmount = null;
        exactAmount = (percent * 100)/this.getAmount();

        return exactAmount;
    }
}

class ExpenseFactory{

    private static Map<ExpenseType, Expense> expenseByTypeMap = new HashMap<>();
    static {
        expenseByTypeMap.put(ExpenseType.EQUAL, new EqualExpense());
        expenseByTypeMap.put(ExpenseType.EXACT, new ExactExpense());
        expenseByTypeMap.put(ExpenseType.PERCENT, new PercentExpense());
    }

    private ExpenseFactory(){}

    private static ExpenseFactory INSTANCE;

    public static ExpenseFactory getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new ExpenseFactory();
        }
        return INSTANCE;
    }

    public Expense getExpense(ExpenseType expenseType){
        return expenseByTypeMap.get(expenseType);
    }
}

class ExpenseManager{
    private ExpenseDao expenseDao;
    private ExpenseManager() {
        expenseDao = ExpenseDao.getInstance();
    }
    private static ExpenseManager INSTANCE;

    public static ExpenseManager getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new ExpenseManager();
        }
        return INSTANCE;
    }

    public void addUser(User user){
        expenseDao.addUser(user);
    }

    public void addGroup(Group group){
        expenseDao.addGroup(group);
    }

    public void addExpense(Expense expense){
        expense.validateAndSplitExpense(expense.getSplits());
        expenseDao.addExpense(expense);
    }

    public void showBalance(String userId){
        Map<String, Map<String, Double>> balanceSheet = expenseDao.getBalanceSheet();
        Map<String, Double> userBalance = balanceSheet.get(userId);

        for(Map.Entry<String, Double> balance : userBalance.entrySet()){
            if(balance.getValue() == 0.0){
                continue;
            }
            if(balance.getValue() > 0.0){
                System.out.println(balance.getKey() + " owes " + userId + " " + balance.getValue());
            } else {
                System.out.println(userId + " owes " + balance.getKey() + " " + -1*balance.getValue());
            }
        }
    }

}

class ExpenseDao{
    private Map<String, User> users;
    private Map<String, Expense> expenses;
    private Map<String, Group> groups;
    private Map<String, Map<String, Double>> balanceSheet;

    private ExpenseDao(){
        users = new HashMap<>();
        expenses = new HashMap<>();
        groups = new HashMap<>();
        balanceSheet = new HashMap<>();
    }

    private static ExpenseDao INSTANCE;

    public static ExpenseDao getInstance(){
        if(INSTANCE == null){
            return INSTANCE = new ExpenseDao();
        }
        return INSTANCE;
    }

    public void addUser(User user){
        users.put(user.getId(), user);
    }

    public void addGroup(Group group){
        groups.put(group.getId(), group);
    }

    public void addExpense(Expense expense){
        expenses.put(expense.getId(), expense);
        String paidBy = expense.getAddedBy();
        for(Map.Entry<String, Double> paidTo : expense.getSplitByUserMap().entrySet()){
            updateBalanceSheet(paidBy, paidTo.getKey(), paidTo.getValue());
        }
    }

    public void updateBalanceSheet(String paidBy, String paidTo, Double amount){
        Map<String, Double> paidByBalance = balanceSheet.get(paidBy);
        if(paidByBalance == null){
            paidByBalance = new HashMap<>();
        }
        if(!paidByBalance.containsKey(paidTo)){
            paidByBalance.put(paidTo, 0.0);
        }
        paidByBalance.put(paidTo, paidByBalance.get(paidTo) + amount);

        balanceSheet.put(paidBy, paidByBalance);

        Map<String, Double> paidToBalance = balanceSheet.get(paidTo);
        if(paidToBalance == null){
            paidToBalance = new HashMap<>();
        }
        if(!paidToBalance.containsKey(paidBy)){
            paidToBalance.put(paidBy, 0.0);
        }
        paidToBalance.put(paidBy, paidToBalance.get(paidBy) - amount);

        balanceSheet.put(paidTo, paidToBalance);

    }

    public Map<String, User> getUsers() {
        return users;
    }

    public Map<String, Expense> getExpenses() {
        return expenses;
    }

    public Map<String, Group> getGroups() {
        return groups;
    }

    public Map<String, Map<String, Double>> getBalanceSheet() {
        return balanceSheet;
    }
}

public class SplitwiseLLD {
    public static void main(String[] args) {
        User user1 = new User("1");
        User user2 = new User("2");
        User user3 = new User("3");
        User user4 = new User("4");

        ExpenseManager expenseManager = ExpenseManager.getInstance();
        expenseManager.addUser(user1);
        expenseManager.addUser(user2);
        expenseManager.addUser(user3);
        expenseManager.addUser(user4);

        ExpenseFactory expenseFactory = ExpenseFactory.getInstance();

        Expense expense = expenseFactory.getExpense(ExpenseType.EXACT);
        expense.setId("1");
        expense.setAmount(100.2);
        List<String> paidToIds = new ArrayList<>();
        paidToIds.add("1");
        paidToIds.add("2");
        paidToIds.add("3");
        expense.setIndividualIds(paidToIds);
        expense.setAddedBy("4");

        Split firstSplit = new Split("1", 50.2);
        Split secondSplit = new Split("2", 20.0);
        Split thirdSplit = new Split("3", 30.0);
        expense.setSplits(Arrays.asList(firstSplit, secondSplit, thirdSplit));

        expenseManager.addExpense(expense);

        Expense secondExpense = expenseFactory.getExpense(ExpenseType.EQUAL);
        secondExpense.setId("1");
        secondExpense.setAmount(600.3);
        List<String> secondPaidToIds = new ArrayList<>();
        secondPaidToIds.add("1");
        secondPaidToIds.add("3");
        secondPaidToIds.add("4");
        secondExpense.setIndividualIds(secondPaidToIds);
        secondExpense.setAddedBy("2");

        expenseManager.addExpense(secondExpense);

        expenseManager.showBalance("4");
        expenseManager.showBalance("2");
    }
}
