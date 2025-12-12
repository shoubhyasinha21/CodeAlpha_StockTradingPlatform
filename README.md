A console-based Stock Trading Simulator built using Object-Oriented Programming (OOP) in Java.
This project demonstrates market simulation, portfolio tracking, buying/selling operations, and file-based data persistence — ideal for internship and academic use.

🚀 Features
1. Market Simulation

Displays real-time (simulated) stock prices

Prices update using a random-walk algorithm

Preloaded stocks (INFY, TCS, RELI, HDFC, SBIN)

 2. Buy / Sell Operations

Users can purchase stocks based on current market price

Selling allowed only if sufficient shares exist

Automatic average cost calculation

3. Portfolio Management

Shows all holdings:

Ticker

Quantity

Average cost

Current price

Market value

Shows total portfolio value (cash + holdings)

4. Transaction History

Logs every BUY/SELL with:

Type

Ticker

Quantity

Price

Timestamp

 5. Portfolio Value Tracking

Tracks total portfolio value after every action

Shows performance history over time

Calculates percentage return

6. File Persistence (CSV)

Technologies Used

Java (OOP Concepts)

Collections Framework (HashMap, ArrayList)

File I/O (CSV Persistence)

Scanner for Console Input

Random Price Simulation

Date & Time API

Stores data in files so that progress is not lost:

portfolio.csv

transactions.csv

value_history.csv
