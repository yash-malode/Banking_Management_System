/**
 * BANKING MANAGEMENT SYSTEM
 * A comprehensive core Java project demonstrating OOP concepts, file handling, 
 * exception handling, collections, and multithreading
 */

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.ReentrantLock;

// Custom Exceptions
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

class InvalidAccountException extends Exception {
    public InvalidAccountException(String message) {
        super(message);
    }
}

// Transaction class to store transaction history
class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String transactionId;
    private String type;
    private double amount;
    private Date timestamp;
    private double balanceAfter;
    
    public Transaction(String type, double amount, double balanceAfter) {
        this.transactionId = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = new Date();
    }
    
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return String.format("[%s] %s | Type: %s | Amount: %.2f | Balance: %.2f", 
            transactionId, sdf.format(timestamp), type, amount, balanceAfter);
    }
}

// Abstract Account class
abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String accountNumber;
    protected String customerName;
    protected double balance;
    protected List<Transaction> transactions;
    protected ReentrantLock lock;
    
    public Account(String customerName, double initialDeposit) {
        this.accountNumber = generateAccountNumber();
        this.customerName = customerName;
        this.balance = initialDeposit;
        this.transactions = new ArrayList<>();
        this.lock = new ReentrantLock();
        
        if (initialDeposit > 0) {
            transactions.add(new Transaction("OPENING", initialDeposit, balance));
        }
    }
    
    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() % 1000000;
    }
    
    public void deposit(double amount) throws IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        lock.lock();
        try {
            balance += amount;
            transactions.add(new Transaction("DEPOSIT", amount, balance));
            System.out.println("Deposit successful. New balance: " + balance);
        } finally {
            lock.unlock();
        }
    }
    
    public void withdraw(double amount) throws InsufficientFundsException, IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        lock.lock();
        try {
            if (amount > balance) {
                throw new InsufficientFundsException("Insufficient funds. Current balance: " + balance);
            }
            balance -= amount;
            transactions.add(new Transaction("WITHDRAWAL", amount, balance));
            System.out.println("Withdrawal successful. New balance: " + balance);
        } finally {
            lock.unlock();
        }
    }
    
    public void transfer(Account recipient, double amount) throws InsufficientFundsException, IllegalArgumentException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        lock.lock();
        recipient.lock.lock();
        try {
            if (amount > balance) {
                throw new InsufficientFundsException("Insufficient funds for transfer");
            }
            
            this.balance -= amount;
            recipient.balance += amount;
            
            this.transactions.add(new Transaction("TRANSFER_OUT", amount, this.balance));
            recipient.transactions.add(new Transaction("TRANSFER_IN", amount, recipient.balance));
            
            System.out.println("Transfer successful!");
        } finally {
            recipient.lock.unlock();
            lock.unlock();
        }
    }
    
    public void displayTransactionHistory() {
        System.out.println("\n=== Transaction History for Account: " + accountNumber + " ===");
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (Transaction t : transactions) {
                System.out.println(t);
            }
        }
    }
    
    public abstract void displayAccountInfo();
    
    // Getters
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerName() { return customerName; }
    public double getBalance() { return balance; }
}

// Savings Account
class SavingsAccount extends Account {
    private static final double MIN_BALANCE = 500.0;
    private static final double INTEREST_RATE = 0.04; // 4% annual
    
    public SavingsAccount(String customerName, double initialDeposit) {
        super(customerName, initialDeposit);
    }
    
    @Override
    public void withdraw(double amount) throws InsufficientFundsException, IllegalArgumentException {
        lock.lock();
        try {
            if (balance - amount < MIN_BALANCE) {
                throw new InsufficientFundsException("Minimum balance of " + MIN_BALANCE + " must be maintained");
            }
            super.withdraw(amount);
        } finally {
            lock.unlock();
        }
    }
    
    public void calculateInterest() {
        lock.lock();
        try {
            double interest = balance * INTEREST_RATE / 12; // Monthly interest
            balance += interest;
            transactions.add(new Transaction("INTEREST", interest, balance));
            System.out.println("Interest credited: " + interest);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void displayAccountInfo() {
        System.out.println("\n=== Savings Account Information ===");
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Customer Name: " + customerName);
        System.out.println("Account Type: SAVINGS");
        System.out.println("Current Balance: " + balance);
        System.out.println("Minimum Balance: " + MIN_BALANCE);
        System.out.println("Interest Rate: " + (INTEREST_RATE * 100) + "% per annum");
    }
}

// Current Account
class CurrentAccount extends Account {
    private static final double OVERDRAFT_LIMIT = 10000.0;
    
    public CurrentAccount(String customerName, double initialDeposit) {
        super(customerName, initialDeposit);
    }
    
    @Override
    public void withdraw(double amount) throws InsufficientFundsException, IllegalArgumentException {
        lock.lock();
        try {
            if (amount > balance + OVERDRAFT_LIMIT) {
                throw new InsufficientFundsException("Exceeds overdraft limit");
            }
            balance -= amount;
            transactions.add(new Transaction("WITHDRAWAL", amount, balance));
            System.out.println("Withdrawal successful. New balance: " + balance);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void displayAccountInfo() {
        System.out.println("\n=== Current Account Information ===");
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Customer Name: " + customerName);
        System.out.println("Account Type: CURRENT");
        System.out.println("Current Balance: " + balance);
        System.out.println("Overdraft Limit: " + OVERDRAFT_LIMIT);
    }
}

// Bank class to manage all accounts
class Bank implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Account> accounts;
    private String bankName;
    
    public Bank(String bankName) {
        this.bankName = bankName;
        this.accounts = new HashMap<>();
    }
    
    public void createAccount(int type, String customerName, double initialDeposit) {
        Account account;
        if (type == 1) {
            account = new SavingsAccount(customerName, initialDeposit);
        } else {
            account = new CurrentAccount(customerName, initialDeposit);
        }
        
        accounts.put(account.getAccountNumber(), account);
        System.out.println("\nAccount created successfully!");
        System.out.println("Account Number: " + account.getAccountNumber());
        System.out.println("Please note down your account number.");
    }
    
    public Account getAccount(String accountNumber) throws InvalidAccountException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new InvalidAccountException("Account not found: " + accountNumber);
        }
        return account;
    }
    
    public void displayAllAccounts() {
        System.out.println("\n=== All Accounts in " + bankName + " ===");
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
        } else {
            for (Account account : accounts.values()) {
                System.out.println("Account: " + account.getAccountNumber() + 
                                 " | Name: " + account.getCustomerName() + 
                                 " | Balance: " + account.getBalance());
            }
        }
    }
    
    public void saveToFile(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
            System.out.println("Bank data saved successfully!");
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
    
    public static Bank loadFromFile(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Bank) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing data found. Starting fresh.");
            return null;
        }
    }
}

// Main Application Class
public class BankingManagementSystem {
    private static Scanner scanner = new Scanner(System.in);
    private static Bank bank;
    private static final String DATA_FILE = "bank_data.ser";
    
    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("   BANKING MANAGEMENT SYSTEM");
        System.out.println("====================================");
        
        // Load existing data or create new bank
        bank = Bank.loadFromFile(DATA_FILE);
        if (bank == null) {
            bank = new Bank("Core Java Bank");
        }
        
        // Add shutdown hook to save data
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bank.saveToFile(DATA_FILE);
        }));
        
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1:
                    createNewAccount();
                    break;
                case 2:
                    performDeposit();
                    break;
                case 3:
                    performWithdrawal();
                    break;
                case 4:
                    performTransfer();
                    break;
                case 5:
                    checkBalance();
                    break;
                case 6:
                    viewTransactionHistory();
                    break;
                case 7:
                    viewAccountDetails();
                    break;
                case 8:
                    calculateInterest();
                    break;
                case 9:
                    bank.displayAllAccounts();
                    break;
                case 10:
                    bank.saveToFile(DATA_FILE);
                    break;
                case 0:
                    running = false;
                    System.out.println("Thank you for using Banking Management System!");
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
        
        scanner.close();
    }
    
    private static void displayMenu() {
        System.out.println("\n========== MAIN MENU ==========");
        System.out.println("1. Create New Account");
        System.out.println("2. Deposit Money");
        System.out.println("3. Withdraw Money");
        System.out.println("4. Transfer Money");
        System.out.println("5. Check Balance");
        System.out.println("6. View Transaction History");
        System.out.println("7. View Account Details");
        System.out.println("8. Calculate Interest (Savings Only)");
        System.out.println("9. Display All Accounts");
        System.out.println("10. Save Data");
        System.out.println("0. Exit");
        System.out.println("================================");
    }
    
    private static void createNewAccount() {
        System.out.println("\n=== Create New Account ===");
        System.out.println("1. Savings Account");
        System.out.println("2. Current Account");
        
        int type = getIntInput("Select account type: ");
        if (type != 1 && type != 2) {
            System.out.println("Invalid account type!");
            return;
        }
        
        System.out.print("Enter customer name: ");
        String name = scanner.nextLine();
        
        double deposit = getDoubleInput("Enter initial deposit: ");
        
        if (type == 1 && deposit < 500) {
            System.out.println("Minimum deposit for Savings Account is 500");
            return;
        }
        
        bank.createAccount(type, name, deposit);
    }
    
    private static void performDeposit() {
        try {
            String accNum = getAccountNumber();
            Account account = bank.getAccount(accNum);
            double amount = getDoubleInput("Enter deposit amount: ");
            account.deposit(amount);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void performWithdrawal() {
        try {
            String accNum = getAccountNumber();
            Account account = bank.getAccount(accNum);
            double amount = getDoubleInput("Enter withdrawal amount: ");
            account.withdraw(amount);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void performTransfer() {
        try {
            System.out.println("\n=== Money Transfer ===");
            String fromAcc = getAccountNumber("Enter sender account number: ");
            String toAcc = getAccountNumber("Enter recipient account number: ");
            
            Account sender = bank.getAccount(fromAcc);
            Account recipient = bank.getAccount(toAcc);
            
            double amount = getDoubleInput("Enter transfer amount: ");
            sender.transfer(recipient, amount);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void checkBalance() {
        try {
            String accNum = getAccountNumber();
            Account account = bank.getAccount(accNum);
            System.out.println("Current Balance: " + account.getBalance());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void viewTransactionHistory() {
        try {
            String accNum = getAccountNumber();
            Account account = bank.getAccount(accNum);
            account.displayTransactionHistory();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void viewAccountDetails() {
        try {
            String accNum = getAccountNumber();
            Account account = bank.getAccount(accNum);
            account.displayAccountInfo();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void calculateInterest() {
        try {
            String accNum = getAccountNumber();
            Account account = bank.getAccount(accNum);
            
            if (account instanceof SavingsAccount) {
                ((SavingsAccount) account).calculateInterest();
            } else {
                System.out.println("Interest calculation is only available for Savings Accounts");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static String getAccountNumber() {
        return getAccountNumber("Enter account number: ");
    }
    
    private static String getAccountNumber(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    
    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input! Please enter a number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return value;
    }
    
    private static double getDoubleInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextDouble()) {
            System.out.print("Invalid input! Please enter a valid amount: ");
            scanner.next();
        }
        double value = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        return value;
    }
}