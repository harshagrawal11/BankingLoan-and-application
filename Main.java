import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
//OBSERVER
interface LoanObserver {
    void update(Loan request, String approver);
}
class AuditDepartment implements LoanObserver {
    @Override
    public void update(Loan request, String approver) {
        System.out.println("[Audit Department] Loan logged for ₹" + request.getAmount() + " approved by " + approver + ".");
    }}
class AccountsDepartment implements LoanObserver {
    @Override
    public void update(Loan request, String approver) {
        System.out.println("[Accounts Department] Disbursement and risk allocation checked for ₹" + request.getAmount() + ".");
    }}
class LoanCoordinator {
    private static LoanCoordinator instance;
    private List<LoanObserver> observers = new ArrayList<>();
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DB_USER = "system";
    private static final String DB_PASS = "system";
    private LoanCoordinator() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            Statement stmt = conn.createStatement();
            try {
                stmt.execute("CREATE SEQUENCE loan_seq START WITH 1 INCREMENT BY 1");
            } catch (Exception e) {}
            try {
                stmt.execute("CREATE TABLE employee (id NUMBER PRIMARY KEY, name VARCHAR2(255))");
            } catch (Exception e) {}
            try {
                stmt.execute("CREATE TABLE loan_requests (" +
                        "id NUMBER PRIMARY KEY, " +
                        "loan_type VARCHAR2(255), " +
                        "amount NUMBER(15, 2), " +
                        "approver VARCHAR2(255))");
            } catch (Exception e) {}
            conn.close();
        } catch (Exception e) {
            System.out.println("Oracle 11g DB Initialization Error: " + e.getMessage());
        }}
    public static LoanCoordinator getInstance() {
        if (instance == null) instance = new LoanCoordinator();
        return instance;
    }
    public void registerObserver(LoanObserver o) {
        observers.add(o);
    }
    public void processApproval(Loan request, String approver) {
        String msg = "Your " + request.getLoanType() + " application for ₹" + request.getAmount() + " has been approved by " + approver + ".";
        request.getNotifier().sendNotification(msg);
        for (LoanObserver o : observers) {
            o.update(request, approver);
        }
        String sql = "INSERT INTO loan_requests (id, loan_type, amount, approver) VALUES (loan_seq.NEXTVAL, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getLoanType());
            pstmt.setDouble(2, request.getAmount());
            pstmt.setString(3, approver);
            pstmt.executeUpdate();
            System.out.println("[Database] Loan request saved successfully to Oracle 11g 'loan_requests' table.");
        } catch (Exception e) {
            System.out.println("[Database Error] " + e.getMessage());
        }}}
// BRIDGE
interface NotificationChannel {
    void sendNotification(String message);
}
class EmailNotification implements NotificationChannel {
    @Override
    public void sendNotification(String message) {
        System.out.println("[Email Sent] " + message);
    }}
class SMSNotification implements NotificationChannel {
    @Override
    public void sendNotification(String message) {
        System.out.println("[SMS Sent] " + message);
    }}
class MobileAppNotification implements NotificationChannel {
    @Override
    public void sendNotification(String message) {
        System.out.println("[Mobile App Push] " + message);
    }}
// ABSTRACT FACTORY PATTERN
abstract class Loan {
    protected double amount;
    protected NotificationChannel notifier;
    public Loan(double amount, NotificationChannel notifier) {
        this.amount = amount;
        this.notifier = notifier;
    }
    public double getAmount() {
        return amount;
    }
    public NotificationChannel getNotifier() {
        return notifier;
    }
    public abstract String getLoanType();
    public void notifyStatus(String approver) {
        LoanCoordinator.getInstance().processApproval(this, approver);
    }}
class SalariedPersonalLoan extends Loan {
    public SalariedPersonalLoan(double amount, NotificationChannel notifier) { super(amount, notifier); }
    @Override public String getLoanType() { return "Salaried Personal Loan"; }
}
class SalariedHousingLoan extends Loan {
    public SalariedHousingLoan(double amount, NotificationChannel notifier) { super(amount, notifier); }
    @Override public String getLoanType() { return "Salaried Housing Loan"; }
}
class SalariedVehicleLoan extends Loan {
    public SalariedVehicleLoan(double amount, NotificationChannel notifier) { super(amount, notifier); }
    @Override public String getLoanType() { return "Salaried Vehicle Loan"; }
}
class SelfEmployedPersonalLoan extends Loan {
    public SelfEmployedPersonalLoan(double amount, NotificationChannel notifier) { super(amount, notifier); }
    @Override public String getLoanType() { return "Self-Employed Personal Loan"; }
}
class SelfEmployedHousingLoan extends Loan {
    public SelfEmployedHousingLoan(double amount, NotificationChannel notifier) { super(amount, notifier); }
    @Override public String getLoanType() { return "Self-Employed Housing Loan"; }
}
class SelfEmployedVehicleLoan extends Loan {
    public SelfEmployedVehicleLoan(double amount, NotificationChannel notifier) { super(amount, notifier); }
    @Override public String getLoanType() { return "Self-Employed Vehicle Loan"; }
}
abstract class LoanFactory {
    public abstract Loan createLoan(String type, double amount, NotificationChannel notifier);
}
class SalariedLoanFactory extends LoanFactory {
    @Override
    public Loan createLoan(String type, double amount, NotificationChannel notifier) {
        if (type.equalsIgnoreCase("Personal")) return new SalariedPersonalLoan(amount, notifier);
        if (type.equalsIgnoreCase("Housing")) return new SalariedHousingLoan(amount, notifier);
        if (type.equalsIgnoreCase("Vehicle")) return new SalariedVehicleLoan(amount, notifier);
        throw new IllegalArgumentException("Invalid Loan Type.");
    }}
class SelfEmployedLoanFactory extends LoanFactory {
    @Override
    public Loan createLoan(String type, double amount, NotificationChannel notifier) {
        if (type.equalsIgnoreCase("Personal")) return new SelfEmployedPersonalLoan(amount, notifier);
        if (type.equalsIgnoreCase("Housing")) return new SelfEmployedHousingLoan(amount, notifier);
        if (type.equalsIgnoreCase("Vehicle")) return new SelfEmployedVehicleLoan(amount, notifier);
        throw new IllegalArgumentException("Invalid Loan Type.");
    }
}
// CHAIN OF RESPONSIBILITY
abstract class LoanHandler {
    protected LoanHandler nextHandler;
    public void setNextHandler(LoanHandler nextHandler) { this.nextHandler = nextHandler; }
    public abstract void handleRequest(Loan request);
}
class BranchManager extends LoanHandler {
    @Override
    public void handleRequest(Loan request) {
        if (request.getAmount() <= 50000) {
            request.notifyStatus("Branch Manager");
        } else if (nextHandler != null) {
            System.out.println("Branch Manager forwarding to Regional Head...");
            nextHandler.handleRequest(request);
        }}}
class RegionalHead extends LoanHandler {
    @Override
    public void handleRequest(Loan request) {
        if (request.getAmount() <= 200000) {
            request.notifyStatus("Regional Head");
        } else if (nextHandler != null) {
            System.out.println("Regional Head forwarding to Risk Committee...");
            nextHandler.handleRequest(request);
        }}}
class RiskCommittee extends LoanHandler {
    @Override
    public void handleRequest(Loan request) {
        if (request.getAmount() <= 500000) {
            request.notifyStatus("Risk Committee");
        } else {
            System.out.println("[REJECTED] Amount ₹" + request.getAmount() + " exceeds maximum Risk Committee limit of ₹5,00,000.");
        }}}
// PROXY PATTERN
interface LoanManagementService {
    void processLoan(LoanHandler handler, Loan request);
}
class LoanManagementSystem implements LoanManagementService {
    private static LoanManagementSystem instance;
    private LoanManagementSystem() {}
    public static LoanManagementSystem getInstance() {
        if (instance == null) instance = new LoanManagementSystem();
        return instance;
    }
    @Override
    public void processLoan(LoanHandler handler, Loan request) {
        handler.handleRequest(request);
    }}
class ProxyLoanManagementService implements LoanManagementService {
    private LoanManagementSystem realSystem;
    private String username;
    private String password;
    public ProxyLoanManagementService(String username, String password) {
        this.username = username;
        this.password = password;
    }
    private boolean authenticate() {
        String[] knownIds = {"EMP101", "EMP102", "EMP103"};
        String[] knownPass = {"pass101", "pass102", "pass103"};
        for (int i = 0; i < knownIds.length; i++) {
            if (knownIds[i].equalsIgnoreCase(username) && knownPass[i].equals(password)) return true;
        }
        return false;
    }
    @Override
    public void processLoan(LoanHandler handler, Loan request) {
        if (authenticate()) {
            System.out.println("Login successful for user: " + username);
            if (realSystem == null) realSystem = LoanManagementSystem.getInstance();
            realSystem.processLoan(handler, request);
        } else {
            System.out.println("Access Denied: Invalid credentials.");
        }}}
//MAIN
public class Main {
    public static void main(String[] args) {
        LoanCoordinator coordinator = LoanCoordinator.getInstance();
        coordinator.registerObserver(new AuditDepartment());
        coordinator.registerObserver(new AccountsDepartment());
        Scanner scanner = new Scanner(System.in);
        LoanHandler branchManager = new BranchManager();
        LoanHandler regionalHead = new RegionalHead();
        LoanHandler riskCommittee = new RiskCommittee();
        branchManager.setNextHandler(regionalHead);
        regionalHead.setNextHandler(riskCommittee);
        System.out.println("Banking Loan Application & Risk Pipeline System");
        System.out.print("Enter Login ID: ");
        String userId = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();
        System.out.print("Applicant Type (1 for Salaried, 2 for Self-Employed): ");
        String userType = scanner.nextLine();
        System.out.print("Notification Pref (1 for Email, 2 for SMS, 3 for Mobile App): ");
        String notifPref = scanner.nextLine();
        System.out.print("Enter Loan Type (Personal/Housing/Vehicle): ");
        String loanType = scanner.nextLine();
        System.out.print("Enter Loan Amount (in ₹): ");
        double amount = scanner.nextDouble();
        System.out.println("\nSubmission Result");
        try {
            LoanFactory factory = userType.equals("1") ?
                    new SalariedLoanFactory() : new SelfEmployedLoanFactory();
            NotificationChannel notifier;
            switch (notifPref) {
                case "2": notifier = new SMSNotification(); break;
                case "3": notifier = new MobileAppNotification(); break;
                default: notifier = new EmailNotification(); break;
            }
            Loan request = factory.createLoan(loanType, amount, notifier);
            LoanManagementService service = new ProxyLoanManagementService(userId, password);
            service.processLoan(branchManager, request);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        scanner.close();
    }
}