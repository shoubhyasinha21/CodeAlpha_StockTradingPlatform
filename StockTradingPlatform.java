import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
public class StockTradingPlatform {
    private static final String PORTFOLIO_FILE = "portfolio.csv";
    private static final String TRANSACTIONS_FILE = "transactions.csv";
    private static final String VALUE_HISTORY_FILE = "value_history.csv";
    public static void main(String[] args) {
        Market market = new Market();
        User user = new User("InternTrader", 10000.00); // starting cash
        
        Persistence.loadPortfolio(user, PORTFOLIO_FILE);
        Persistence.loadTransactions(user, TRANSACTIONS_FILE);
        Persistence.loadValueHistory(user, VALUE_HISTORY_FILE);
        Scanner sc = new Scanner(System.in);
        boolean running = true;
        System.out.println("Welcome to the Stock Trading Platform (Console)");

        while (running) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1) Show market data");
            System.out.println("2) Refresh market prices");
            System.out.println("3) Buy stock");
            System.out.println("4) Sell stock");
            System.out.println("5) View portfolio");
            System.out.println("6) View transactions");
            System.out.println("7) Save and exit");
            System.out.println("8) Show portfolio performance history");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    market.printMarketData();
                    break;
                case "2":
                    market.stepMarket();
                    System.out.println("Market refreshed.");
                    break;
                case "3":
                    buyFlow(sc, market, user);
                    break;
                case "4":
                    sellFlow(sc, market, user);
                    break;
                case "5":
                    user.printPortfolio(market);
                    break;
                case "6":
                    user.printTransactions();
                    break;
                case "7":
                    // Save and exit
                    Persistence.savePortfolio(user, PORTFOLIO_FILE);
                    Persistence.saveTransactions(user, TRANSACTIONS_FILE);
                    Persistence.saveValueHistory(user, VALUE_HISTORY_FILE);
                    System.out.println("Saved. Goodbye!");
                    running = false;
                    break;
                case "8":
                    user.printValueHistory();
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
            double portfolioValue = user.getTotalValue(market);
            user.recordValueSnapshot(portfolioValue);
        }
        sc.close();
    }
    private static void buyFlow(Scanner sc, Market market, User user) {
        market.printMarketData();
        System.out.print("Enter ticker to buy: ");
        String ticker = sc.nextLine().trim().toUpperCase();
        Stock s = market.getStock(ticker);
        if (s == null) {
            System.out.println("Ticker not found.");
            return;
        }
        System.out.println("Price: " + Utils.fmt(s.getPrice()));
        System.out.print("Enter quantity to buy: ");
        try {
            int qty = Integer.parseInt(sc.nextLine().trim());
            if (qty <= 0) {
                System.out.println("Quantity must be > 0");
                return;
            }
            boolean ok = user.buy(s, qty);
            if (ok) System.out.println("Bought " + qty + " of " + ticker);
            else System.out.println("Insufficient cash.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }
    private static void sellFlow(Scanner sc, Market market, User user) {
        user.printPortfolio(market);
        System.out.print("Enter ticker to sell: ");
        String ticker = sc.nextLine().trim().toUpperCase();
        Stock s = market.getStock(ticker);
        if (s == null) {
            System.out.println("Ticker not found.");
            return;
        }
        System.out.println("Price: " + Utils.fmt(s.getPrice()));
        System.out.print("Enter quantity to sell: ");
        try {
            int qty = Integer.parseInt(sc.nextLine().trim());
            if (qty <= 0) {
                System.out.println("Quantity must be > 0");
                return;
            }
            boolean ok = user.sell(s, qty);
            if (ok) System.out.println("Sold " + qty + " of " + ticker);
            else System.out.println("Insufficient shares.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }
}
class Utils {
    static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    static String now() {
        return LocalDateTime.now().format(DT_FMT);
    }
    static String fmt(double v) {
        return String.format("%.2f", v);
    }
}
class Stock {
    private final String ticker;
    private final String name;
    private double price;
    public Stock(String ticker, String name, double price) {
        this.ticker = ticker;
        this.name = name;
        this.price = price;
    }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = Math.max(0.01, price); }
    @Override
    public String toString() {
        return String.format("%s (%s) : %s", ticker, name, Utils.fmt(price));
    }
}
class Market {
    private final Map<String, Stock> stocks = new HashMap<>();
    private final Random rnd = new Random();
    public Market() {
        addStock(new Stock("INFY", "Infosys", 1500.0));
        addStock(new Stock("TCS", "Tata Consultancy Services", 3000.0));
        addStock(new Stock("RELI", "Reliance Industries", 2600.0));
        addStock(new Stock("HDFC", "HDFC Bank", 1400.0));
        addStock(new Stock("SBIN", "State Bank of India", 700.0));
    }
    public void addStock(Stock s) { stocks.put(s.getTicker(), s); }
    public Stock getStock(String ticker) { return stocks.get(ticker); }
    public Collection<Stock> listStocks() { return stocks.values(); }

    public void stepMarket() {
        for (Stock s : stocks.values()) {
            double changePct = (rnd.nextDouble() * 2 - 1) * 0.05; // +/-5%
            double newPrice = s.getPrice() * (1 + changePct);
            s.setPrice(newPrice);
        }
    }
    public void printMarketData() {
        System.out.println("\n--- Market Data (Ticker - Name - Price) ---");
        for (Stock s : stocks.values()) {
            System.out.println(s);
        }
    }
}
class PortfolioEntry {
    private final String ticker;
    private int quantity;
    private double avgCost; 
    public PortfolioEntry(String ticker, int quantity, double avgCost) {
        this.ticker = ticker;
        this.quantity = quantity;
        this.avgCost = avgCost;
    }
    public String getTicker() { return ticker; }
    public int getQuantity() { return quantity; }
    public double getAvgCost() { return avgCost; }

    public void addShares(int qty, double price) {
        double totalCost = this.avgCost * this.quantity + price * qty;
        this.quantity += qty;
        this.avgCost = totalCost / this.quantity;
    }

    public boolean removeShares(int qty) {
        if (qty > quantity) return false;
        quantity -= qty;
        return true;
    }
}
class Transaction {
    enum Type { BUY, SELL }
    private final Type type;
    private final String ticker;
    private final int qty;
    private final double price;
    private final String time;
    public Transaction(Type type, String ticker, int qty, double price) {
        this.type = type; this.ticker = ticker; this.qty = qty; this.price = price; this.time = Utils.now();
    }
    public Type getType() { return type; }
    public String getTicker() { return ticker; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }
    public String getTime() { return time; }
    @Override
    public String toString() {
        return String.format("%s,%s,%d,%.2f,%s", type, ticker, qty, price, time);
    }
}
class User {
    private final String name;
    private double cash;
    private final Map<String, PortfolioEntry> portfolio = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<ValueSnapshot> history = new ArrayList<>();
    public User(String name, double startingCash) {
        this.name = name; this.cash = startingCash;
    }
    public double getCash() { return cash; }
    public boolean buy(Stock s, int qty) {
        double cost = s.getPrice() * qty;
        if (cost > cash) return false;
        cash -= cost;
        PortfolioEntry e = portfolio.get(s.getTicker());
        if (e == null) portfolio.put(s.getTicker(), new PortfolioEntry(s.getTicker(), qty, s.getPrice()));
        else e.addShares(qty, s.getPrice());
        Transaction t = new Transaction(Transaction.Type.BUY, s.getTicker(), qty, s.getPrice());
        transactions.add(t);
        return true;
    }
    public boolean sell(Stock s, int qty) {
        PortfolioEntry e = portfolio.get(s.getTicker());
        if (e == null || e.getQuantity() < qty) return false;
        double revenue = s.getPrice() * qty;
        boolean removed = e.removeShares(qty);
        if (!removed) return false;
        cash += revenue;
        if (e.getQuantity() == 0) portfolio.remove(s.getTicker());
        Transaction t = new Transaction(Transaction.Type.SELL, s.getTicker(), qty, s.getPrice());
        transactions.add(t);
        return true;
    }

    public void printPortfolio(Market market) {
        System.out.println("\n--- Portfolio for " + name + " ---");
        System.out.println("Cash: " + Utils.fmt(cash));
        if (portfolio.isEmpty()) System.out.println("(No holdings)");
        else {
            System.out.printf("%10s %8s %10s %12s %12s\n", "Ticker","Qty","AvgCost","Price","MarketValue");
            for (PortfolioEntry e : portfolio.values()) {
                Stock s = market.getStock(e.getTicker());
                double price = (s == null) ? 0.0 : s.getPrice();
                double mv = price * e.getQuantity();
                System.out.printf("%10s %8d %10s %12s %12s\n",
                        e.getTicker(), e.getQuantity(), Utils.fmt(e.getAvgCost()), Utils.fmt(price), Utils.fmt(mv));
            }
            System.out.println("Total value (cash + holdings): " + Utils.fmt(getTotalValue(market)));
        }
    }
    public double getTotalValue(Market market) {
        double total = cash;
        for (PortfolioEntry e : portfolio.values()) {
            Stock s = market.getStock(e.getTicker());
            double price = (s == null) ? 0.0 : s.getPrice();
            total += price * e.getQuantity();
        }
        return total;
    }

    public void printTransactions() {
        System.out.println("\n--- Transactions ---");
        if (transactions.isEmpty()) System.out.println("(No transactions)");
        else {
            System.out.printf("%6s %8s %6s %10s %20s\n", "Type","Ticker","Qty","Price","Time");
            for (Transaction t : transactions) {
                System.out.printf("%6s %8s %6d %10.2f %20s\n", t.getType(), t.getTicker(), t.getQty(), t.getPrice(), t.getTime());
            }
        }
    }
    public Map<String, PortfolioEntry> getPortfolio() { return portfolio; }
    public List<Transaction> getTransactions() { return transactions; }
    public void recordValueSnapshot(double value) {
        if (history.isEmpty() || history.get(history.size()-1).value != value) {
            history.add(new ValueSnapshot(LocalDateTime.now(), value));
        }
    }
    public void printValueHistory() {
        System.out.println("\n--- Portfolio Value History ---");
        if (history.isEmpty()) { System.out.println("(no history)"); return; }
        System.out.printf("%20s %12s\n", "Time","Value");
        for (ValueSnapshot vs : history) {
            System.out.printf("%20s %12s\n", vs.time.format(Utils.DT_FMT), Utils.fmt(vs.value));
        }
        double start = history.get(0).value;
        double end = history.get(history.size()-1).value;
        double pct = (end - start) / start * 100.0;
        System.out.println("\nStart: " + Utils.fmt(start) + "  End: " + Utils.fmt(end) + "  Return: " + Utils.fmt(pct) + "%");
    }
    public List<ValueSnapshot> getHistory() { return history; }
}
class ValueSnapshot {
    public final LocalDateTime time;
    public final double value;
    public ValueSnapshot(LocalDateTime time, double value) { this.time = time; this.value = value; }
}
class Persistence {
    public static void savePortfolio(User user, String path) {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(path)))) {
            pw.println("ticker,quantity,avgCost");
            for (PortfolioEntry e : user.getPortfolio().values()) {
                pw.printf("%s,%d,%.6f\n", e.getTicker(), e.getQuantity(), e.getAvgCost());
            }
        } catch (IOException e) {
            System.out.println("Failed to save portfolio: " + e.getMessage());
        }
    }
    public static void loadPortfolio(User user, String path) {
        Path p = Paths.get(path);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String t = parts[0];
                int qty = Integer.parseInt(parts[1]);
                double avg = Double.parseDouble(parts[2]);
                user.getPortfolio().put(t, new PortfolioEntry(t, qty, avg));
            }
            System.out.println("Loaded portfolio from " + path);
        } catch (IOException e) {
            System.out.println("Failed to load portfolio: " + e.getMessage());
        }
    }
    public static void saveTransactions(User user, String path) {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(path)))) {
            pw.println("type,ticker,qty,price,time");
            for (Transaction t : user.getTransactions()) pw.println(t.toString());
        } catch (IOException e) {
            System.out.println("Failed to save transactions: " + e.getMessage());
        }
    }
    public static void loadTransactions(User user, String path) {
        Path p = Paths.get(path);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                Transaction.Type type = Transaction.Type.valueOf(parts[0]);
                String ticker = parts[1];
                int qty = Integer.parseInt(parts[2]);
                double price = Double.parseDouble(parts[3]);
                Transaction t = new Transaction(type, ticker, qty, price);
                user.getTransactions().add(t);
            }
            System.out.println("Loaded transactions from " + path);
        } catch (IOException e) {
            System.out.println("Failed to load transactions: " + e.getMessage());
        }
    }
    public static void saveValueHistory(User user, String path) {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(path)))) {
            pw.println("time,value");
            for (ValueSnapshot vs : user.getHistory()) {
                pw.printf("%s,%.6f\n", vs.time.format(Utils.DT_FMT), vs.value);
            }
        } catch (IOException e) {
            System.out.println("Failed to save value history: " + e.getMessage());
        }
    }
    public static void loadValueHistory(User user, String path) {
        Path p = Paths.get(path);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                LocalDateTime t = LocalDateTime.parse(parts[0], Utils.DT_FMT);
                double value = Double.parseDouble(parts[1]);
                user.getHistory().add(new ValueSnapshot(t, value));
            }
            System.out.println("Loaded value history from " + path);
        } catch (IOException e) {
            System.out.println("Failed to load value history: " + e.getMessage());
        }
    }
}
