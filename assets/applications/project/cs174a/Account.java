package cs174a;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Scanner;
import java.util.ArrayList;



public class Account{
	public String a_id;
	public String owner_id;
	public String account_type;
	public String bank_branch;
	public double balance;
	public boolean is_open;
	public String interest_date;

	public static Account create_account(Testable.AccountType accountType, String id, double initialBalance,
										 String tin, String name, String address, OracleConnection connection){

		// Check that initial balance is >= 1000
		if(accountType == Testable.AccountType.STUDENT_CHECKING ||
		   accountType == Testable.AccountType.INTEREST_CHECKING ||
		   accountType == Testable.AccountType.SAVINGS){
			if(initialBalance < 1000){
				System.err.println("Initial balance < $1000 -- Don't create account");
				return null;
			}
		}

		Account acct = null;

		Account account_exists_check = Account.get_account_by_id(id, connection);
		if(account_exists_check != null){
			// Account already exists
			return null;
		}

		// Determine if customer exists
		Customer cust = Customer.get_cust_by_id(tin, connection);
		
		if(cust == null){  // Customer doesn't exist -- create them
			cust = Customer.create_customer(tin, name, address, connection);
			if(cust == null){
				return null;
			}
		}

		// Attempt to create the new account
		String query = String.format("INSERT INTO accounts (a_id, owner_id, account_type, bank_branch, balance, is_open, interest_date) " +
	    							 "VALUES ('%s', '%s', '%s', '%s', %f, %d, '%s')"
	    							 , id, tin, "" + accountType, "DEFAULT", initialBalance, 1, "");
		try( Statement statement = connection.createStatement() ) {
			try{
				int updates = statement.executeUpdate( query );
				if(updates == 0){
					return null;
				}
				acct = new Account(id, tin, "" + accountType, "", initialBalance, true, "");
			}catch(SQLException e){
				e.printStackTrace();
			}
		}catch(SQLException e){
			e.printStackTrace();
		}

		// Add an ownership record of this account with given customer
		boolean owner_create_success = Account.create_acct_ownership(id, tin, connection);
		if(!owner_create_success){
			System.out.println("Error creating cust-acct link");
			return null;
		}

		// Create Transaction of initial deposit
		if(initialBalance != 0){
			Transaction transaction = Transaction.create_transaction(
				id, "", tin, Bank.get_date(connection), "" + Transaction.TransactionType.DEPOSIT, initialBalance, connection
			);

			if(transaction == null){
				System.out.println("Error creating initial deposit transaction");
				return null;
			}
		}

		return acct;

	}

	public static Account create_pocket_account(String id, String linkedId, double initialTopUp,
											    String tin, OracleConnection connection){
		// Check account exists
		Account linked = Account.get_account_by_id(linkedId, connection);
		if(linked == null){
			System.err.println("Need an existing account for a pocket account");
			return null;
		}

		// Check account has at least initialTopUp + $5
		if(linked.balance < initialTopUp + 5){
			System.err.println("Not enough money to create pocket account!");
			return null;
		}

		// Check initial topup is proper amount
		if(initialTopUp <= 0.01){
			System.err.println("Need an initial top up of greater than .01");
			return null;
		}

		// Check account is open
		if(!linked.is_open){
			System.err.println("Account must be open to allow a pocket account to be created");
			return null;
		}
		// Check account is not another pocket account
		if(linked.account_type.equals("" + Testable.AccountType.POCKET)){
			System.err.println("Linked account cannot be another pocket account");
			return null;
		}
		Customer cust = Customer.get_cust_by_id(tin, connection);
		// Check customer exists
		if(cust == null){
			System.err.println("Need an existing customer for a pocket account");
			return null;
		}
		// Check customer owns account 
		ArrayList<String> owners = get_account_owners(linkedId, connection);
		if(owners == null){
			System.err.println("Error retrieving owners");
			return null;
		}
		boolean owns_account = false;
		for(int i = 0; i < owners.size(); i++){
			if(cust.c_id.equals(owners.get(i))){
				owns_account = true;
			}
		}
		if(!owns_account){
			System.err.println("Customer must own linked account to create a pocket account: " + linkedId);
			return null;
		}
		// Create pocket account
		Account pock_account = Account.create_account(Testable.AccountType.POCKET, id, 0.0,
										 tin,"", "", connection);
		if(pock_account == null){
			System.err.println("Could not create account for pocket account");
			return null;
		}

		// Transfer initial topup from linked account
		if(initialTopUp != 0 && !Transaction.transfer_money(id, linkedId, initialTopUp, connection)){
			System.err.println("Initial topup failed!");
			return null;
		}
		
		Transaction top_up = Transaction.create_transaction(id, linkedId, tin,
									 Bank.get_date(connection), "" + Transaction.TransactionType.TOP_UP, initialTopUp,
									  connection);

		if(top_up == null){
			System.err.println("Could not perform initial top up");
			return null;
		}

		Transaction fee = Transaction.create_transaction("", linkedId, tin,
									 Bank.get_date(connection), "" + Transaction.TransactionType.FTM_FEE, 5.00,
									  connection);	
		if(fee == null){
			System.err.println("Could not create fee transaction");
			return null;
		}

		// Transfer initial topup from linked account
		if(initialTopUp != 0 && !Transaction.transfer_money("", linkedId, 5, connection)){
			System.err.println("Fee failed!");
			return null;
		}

		// Create link to a checkings/savings account
		if(!create_pock_link(id, linkedId, connection)){
			System.err.println("Pocket linking failed");
			return null;
		}

		return pock_account;
	}

	public static ArrayList<String> get_account_owners(String a_id, OracleConnection connection){
		ArrayList<String> owners = new ArrayList<String>();
		String query = String.format("SELECT c_id FROM custaccounts A WHERE A.a_id = '%s'", a_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					owners.add(rs.getString("c_id"));
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return owners;
	}

	public static ArrayList<String> get_account_owners_name_address(String a_id, OracleConnection connection){
		ArrayList<String> owners = new ArrayList<String>();
		String query = String.format("SELECT C.c_name, C.address " +
									 "FROM custaccounts A, Customers C " +
									 "WHERE A.a_id = '%s' " +
									 "AND A.c_id = C.c_id", a_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					String owner = "name: ";
					owner += rs.getString("c_name");
					owner += " , address: ";
					owner += rs.getString("address");
					owners.add(owner);
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return owners;
	}

	public static ArrayList<String> get_all_accounts(OracleConnection connection){
		ArrayList<String> accounts = new ArrayList<String>();
		String query = String.format("SELECT a_id FROM accounts");
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					accounts.add(rs.getString("a_id"));
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return accounts;
	}

	// Create an instance in custaccounts linking customer + account
	public static boolean create_acct_ownership(String a_id, String c_id, OracleConnection connection){
		String query = String.format("INSERT INTO custaccounts (c_id, a_id) " +
	    							 "VALUES ('%s', '%s')"
	    							 , c_id, a_id);
		try( Statement statement = connection.createStatement() ) {
			try{
				int updates = statement.executeUpdate( query );
				if(updates == 0){
					return false;
				}
			}catch(SQLException e){
				e.printStackTrace();
				return false;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static ArrayList<String> get_cust_accounts(String c_id, OracleConnection connection){
		ArrayList<String> accts = new ArrayList<String>();
		String query = String.format("SELECT A.a_id " +
									 "FROM Accounts A " +
									 "WHERE A.owner_id = '%s'", c_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					accts.add(rs.getString("a_id"));
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return accts;
	}

	public static ArrayList<String> get_cust_accounts_and_status(String c_id, OracleConnection connection){
		ArrayList<String> acct_statuses = new ArrayList<String>();
		String query = String.format("SELECT A.a_id, A.is_open " +
									 "FROM custaccounts C, Accounts A " +
									 "WHERE C.a_id = A.a_id AND C.c_id = '%s'", c_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					String acct_status = "Account id: " + rs.getString("a_id") +" /";
					acct_status += " Status: ";
					if(rs.getInt("is_open") == 1){
						acct_status += "open";
					}else{
						acct_status += "closed";
					}
					acct_statuses.add(acct_status);
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return acct_statuses;
	}

	public static ArrayList<String> get_all_cust_accounts(String c_id, OracleConnection connection){
		ArrayList<String> accts = new ArrayList<String>();
		String query = String.format("SELECT C.a_id " +
									 "FROM custaccounts C " +
									 "WHERE C.c_id = '%s'", c_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					accts.add(rs.getString("a_id"));
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return accts;
	}

	public static boolean create_pock_link(String a_id, String link_id, OracleConnection connection){
		String query = String.format("INSERT INTO pocketlinks (pocket_id, link_id) " +
	    							 "VALUES ('%s', '%s')"
	    							 , a_id, link_id);
		try( Statement statement = connection.createStatement() ) {
			try{
				int updates = statement.executeUpdate( query );
				if(updates == 0){
					return false;
				}
			}catch(SQLException e){
				e.printStackTrace();
				return false;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static String get_linked(String pocket_id, OracleConnection connection){
		String query = String.format("SELECT P.link_id FROM pocketlinks P WHERE P.pocket_id = '%s'",
										pocket_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				if(rs.next()){
					return rs.getString("link_id");
				}
			}catch(SQLException e){
				e.printStackTrace();
				return "";
			}
		}catch(SQLException e){
			e.printStackTrace();
			return "";
		}
		return "";
	}	

	public static boolean accounts_are_linked(String pocket_id, String linked_id, OracleConnection connection){
		String query = String.format("SELECT * FROM pocketlinks P WHERE P.pocket_id = '%s' AND P.link_id = '%s'",
										pocket_id, linked_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				if(rs.next()){
					return true;
				}
			}catch(SQLException e){
				e.printStackTrace();
				return false;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public static ArrayList<String> get_closed_accounts(OracleConnection connection){
		ArrayList<String> accounts = new ArrayList<String>();
		String query = String.format("SELECT a_id FROM accounts A WHERE A.is_open = 0");
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					accounts.add(rs.getString("a_id"));
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return accounts;
	}

	public static Account get_account_by_id(String a_id, OracleConnection connection){
		Account acct = null;
		String query = String.format("SELECT * FROM accounts A WHERE A.a_id = '%s'", a_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				if(rs.next()){
					acct = new Account(
						rs.getString("a_id"),
						rs.getString("owner_id"),
						rs.getString("account_type"),
						rs.getString("bank_branch"),
						rs.getDouble("balance"),
						rs.getInt("is_open") == 1,
						rs.getString("interest_date")
					);
				}else{
					return null;
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return acct;
	}

	public static boolean close_account_by_id(String a_id, OracleConnection connection){
		String query = String.format("UPDATE accounts SET is_open = 0 WHERE a_id = '%s'", a_id);
		try( Statement statement = connection.createStatement() ) {
			try{
				int updates = statement.executeUpdate( query );
				if(updates == 0){
					return false;
				}

				// Close any attached pocket accounts
				Account.close_pocket_accounts_by_owner_id(a_id, connection);
			}catch(SQLException e){
				e.printStackTrace();
				return false;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean close_pocket_accounts_by_owner_id(String from_acct, OracleConnection connection){
		String query = String.format( "UPDATE accounts A " +
									  "SET A.is_open = 0 " +
									  "WHERE A.a_id in ( " +
    								  	"SELECT P.pocket_id " +
    								  	"FROM pocketlinks P " +
    								  	"WHERE P.link_id = '%s' " +
									  ")", from_acct );
		try( Statement statement = connection.createStatement() ) {
			try{
				int updates = statement.executeUpdate( query );
			}catch(SQLException e){
				e.printStackTrace();
				return false;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// Negative amount means error
	public static double get_account_balance(String a_id, OracleConnection connection){
		Account account = Account.get_account_by_id(a_id, connection);
		if(account == null){
			return -1;
		}
		return account.balance;
	}

	public static String get_account_type(String a_id, OracleConnection connection){
		Account account = Account.get_account_by_id(a_id, connection);
		if(account == null){
			return "";
		}
		return account.account_type;
	}

	public Account(String a_id, String owner_id, String account_type, String bank_branch,
					double balance, boolean is_open, String interest_date){
		// Leave default
		this.a_id = a_id;
		this.owner_id = owner_id;
		this.account_type = account_type;
		this.bank_branch = bank_branch;
		this.balance = balance;
		this.is_open = is_open;
		this.interest_date = interest_date;
	}

}