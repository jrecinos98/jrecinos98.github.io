package cs174a;

/*
 * Customer is basically a struct to store data
 * returned from the database pertaining to
 * the customer table
 */
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


public class Customer {
	public String c_id;
	public String encrypted_pin;
	public String address;
	public String name;


	/*** STATICS ***/

	// Encrypts the given string using some encrpytion scheme
	// So that pins are stored in an encrypted form on db
	public static String encrypt_pin(String unencrypted) throws Exception{
		if(unencrypted.length() != 4){
			System.out.println("Bad pin: " + unencrypted);
			throw new Exception("Pin must be 4 characters got " + unencrypted.length());
		}

		// Get integer value of each digit
		int d0 = Integer.parseInt("" + unencrypted.charAt(0));
		int d1 = Integer.parseInt("" + unencrypted.charAt(1));
		int d2 = Integer.parseInt("" + unencrypted.charAt(2));
		int d3 = Integer.parseInt("" + unencrypted.charAt(3));

		// Encrypt each digit making sure still single integer
		d0 = (d0 + 1) % 10;
		d1 = (d1 + 1) % 10;
		d2 = (d2 + 1) % 10;
		d3 = (d3 + 1) % 10;

		return "" + d0 + d1 + d2 + d3;
	}

	// If a customer with given c_id and pin is found, return that customer, else return null
	public static Customer login(String c_id, String unencrypted_pin, OracleConnection connection){
		Customer cust = null;
		ResultSet result = null;
		PreparedStatement stmt = null;
		try{
			String encrypted = Customer.encrypt_pin(unencrypted_pin);

			// Do database lookup on customers table
			try {
				String query = String.format("SELECT * " + 
			    							 "FROM customers C " +
			    							 "WHERE C.c_id = '%s' " + 
			    							 "AND C.encrypted_pin = '%s'", c_id, encrypted);
			    stmt = connection.prepareStatement(query);
			    
			    result = stmt.executeQuery();

			    if(result.next()) {
				    cust = new Customer(
				    	result.getString("c_id"),
				    	result.getString("encrypted_pin"),
				    	result.getString("address"),
				    	result.getString("c_name")
				    );
				}
			}
			catch (SQLException e){
			    e.printStackTrace();
			}
			finally {
		        try { result.close(); } catch (Exception e) { /* ignored */ }
			    try { stmt.close(); } catch (Exception e) { /* ignored */ }
			}
		}catch(Exception e){
			System.out.println("Error logging in the customer" + c_id);
			e.printStackTrace();
		}
		return cust;
	}


	public static boolean cust_insurance_limit_reached(String c_id, OracleConnection connection){
		String query = String.format("SELECT SUM(A.balance) FROM accounts A WHERE A.owner_id = '%s' ", c_id);
		try( Statement statement = connection.createStatement()){
			try( ResultSet rs = statement.executeQuery(query)){
				if(rs.next()){
					double balance = rs.getDouble(1);
					if(balance > 100000){
						return true;
					}
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return false;
	}

	public static ArrayList<AccountStatement> generate_monthly_statement(String c_id, OracleConnection connection){
		ArrayList<AccountStatement> statements = new ArrayList<AccountStatement>(); // Return statements
		ArrayList<String> accounts = Account.get_cust_accounts(c_id, connection);
		if(accounts.size() < 1){
			return statements;
		}
		accounts = Account.get_all_cust_accounts(c_id, connection);

		// Loop over all the accounts they are primary owner of
		for(int i = 0; i < accounts.size(); i++){
			ArrayList<String> owners = new ArrayList<String>();
			ArrayList<Transaction> transactions = new ArrayList<Transaction>();
			double final_balance;
			double initial_balance;
			boolean insurance_limit_reached;

			owners = Account.get_account_owners_name_address(accounts.get(i), connection);
			transactions = Transaction.get_acct_transactions_this_month(accounts.get(i), connection);
			final_balance = Account.get_account_balance(accounts.get(i), connection);
			initial_balance = final_balance;
			// Unwind transactions to find the initial balance at the start of the month
			for(int j = 0; j < transactions.size(); j++){
				if(transactions.get(j).to_acct != null && transactions.get(j).to_acct.equals(accounts.get(i))){
					// Subtract amount
					initial_balance -= transactions.get(j).amount;
				}else if(transactions.get(j).from_acct != null && transactions.get(j).from_acct.equals(accounts.get(i))){
					// Add amount
					initial_balance += transactions.get(j).amount;
				}
			}
			// Check if reached insurance limit
			insurance_limit_reached = Customer.cust_insurance_limit_reached(c_id, connection);

			// Add Account statement to customer's report
			statements.add(new AccountStatement(accounts.get(i), owners, transactions, final_balance,
												initial_balance, insurance_limit_reached));
		}

		return statements;
	}

	public static ArrayList<String> get_all_customers(OracleConnection connection){
		ArrayList<String> customers = new ArrayList<String>();
		String query = String.format("SELECT C.c_id FROM customers C ");
		try( Statement statement = connection.createStatement()){
			try( ResultSet rs = statement.executeQuery(query)){
				while(rs.next()){
					customers.add(rs.getString("c_id"));
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return customers;
	}

	public static Customer create_customer(String tin, String name, String address, OracleConnection connection){
		Customer cust = null;
		String new_encrypted_pin = "0000";
		Customer check_cust = Customer.get_cust_by_id(tin, connection);
		if(check_cust != null){
			// Customer already exists
			return null;
		}

		// Fix strings if have '
		name = name.replace("'", "''");
		address = address.replace("'", "''");


		try{
			new_encrypted_pin = Customer.encrypt_pin("1717");
		}catch(Exception e){ return null; }
		String query = String.format("INSERT INTO customers (c_id, c_name, address, encrypted_pin) " +
	    							 "VALUES ('%s', '%s', '%s', '%s')", tin, name, address, new_encrypted_pin);
		try( Statement statement = connection.createStatement() ) {
			try{
				int updates = statement.executeUpdate( query );
				if(updates == 0){
					return null;
				}
				try{
					cust = new Customer(tin, new_encrypted_pin, address, name);
				}catch(Exception e){ e.printStackTrace(); }
			}catch(SQLException e){
				e.printStackTrace();
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return cust;
	}

	public static boolean update_pin(String t_id, String old_pin, String new_pin, OracleConnection connection){
		Customer cust = Customer.get_cust_by_id(t_id, connection);
		if(cust != null){  // 	Customer exists
			try{
				if(cust.encrypted_pin.substring(0,4).equals(Customer.encrypt_pin(old_pin))){
					String new_enc = Customer.encrypt_pin(new_pin);
					String query = String.format("UPDATE customers C SET C.encrypted_pin = '%s'"
												+ " WHERE C.c_id = %s ", new_enc, t_id);
					try( Statement statement = connection.createStatement()){
						int rs = statement.executeUpdate(query);
						if(rs == 0){
							System.err.println("PIN update failed");
							return false;
						}
						return true;
					} catch (SQLException e){
						e.printStackTrace();
					}
				}else{
					System.out.println("PIN's did not match!");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return false;
	}

	public static Customer get_cust_by_id(String c_id, OracleConnection connection){
		Customer cust = null;
		String query = String.format("SELECT * FROM customers C WHERE C.c_id = '%s' ", c_id);
		try( Statement statement = connection.createStatement()){
			try( ResultSet rs = statement.executeQuery(query)){
				if(rs.next()){
					cust = new Customer(
				    	rs.getString("c_id"),
				    	rs.getString("encrypted_pin"),
				    	rs.getString("address"),
				    	rs.getString("c_name")
				    );
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return cust;
	}

	public static boolean del_cust_by_id(String c_id, OracleConnection connection){
		String query = String.format("DELETE FROM customers C WHERE C.c_id = %s ", c_id);
		try( Statement statement = connection.createStatement()){
			int result = statement.executeUpdate(query);
			if(result == 1){
				return true;
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return false;
	}


	/*** MEMBER FUNCTIONS ***/


	// Create a customer and throw exception if error in pin encountered
	public Customer(String c_id, String encrypted_pin, String address, String name){
		this.c_id = c_id;
		this.encrypted_pin = encrypted_pin;
		this.address = address;
		this.name = name;
	}

}