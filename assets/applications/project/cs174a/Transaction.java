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



public class Transaction {
	public String to_acct;
	public String from_acct;
	public String cust_id;
	public String date;
	public String transaction_type;
	public double amount;
	public int t_id = -1;

	public enum TransactionType {
		DEPOSIT,
		WITHDRAWAL,
		TOP_UP,
		PURCHASE,
		PAY_FRIEND,
		FTM_FEE,
		PCT_FEE,
		TRANSFER,
		COLLECT,
		WIRE,
		WRITE_CHECK,
		ACCRUE_INTEREST,
	}

	public static Transaction create_transaction(String to_acct, String from_acct, String cust_id,
									 String date, String transaction_type, double amount, OracleConnection connection){
		
		Transaction transaction = null;
		int t_id = -1;
		String query = String.format("INSERT INTO transactions (to_acct, from_acct, cust_id, t_date, t_type, amount) " +
	    							 "VALUES ('%s', '%s', '%s', '%s', '%s', %f)",
	    							  to_acct, from_acct, cust_id, date, transaction_type, amount);

		try( PreparedStatement statement = connection.prepareStatement(query,new String[]{"t_id"}) ) {
			try{
				int updates = statement.executeUpdate();
				if(updates == 0){
					return null;
				}
				try(ResultSet rs = statement.getGeneratedKeys()){
					if(rs.next()){
				    	t_id = Integer.parseInt(rs.getString(1)); 
				    }	
				}catch(SQLException e){
					e.printStackTrace();
				}
			    
				transaction = new Transaction(t_id, to_acct, from_acct, cust_id, date, transaction_type, amount);
			}catch(SQLException e){
				e.printStackTrace();
			}

		}catch(SQLException e){
			e.printStackTrace();
		}
		return transaction;
	}

	public static Transaction create_transaction_and_transfer(String to_acct, String from_acct, String cust_id,
									 String date, String transaction_type, double amount, OracleConnection connection){
		// Transfer money into the account
		if(!Transaction.transfer_money(to_acct, from_acct, amount, connection)){
			System.err.println("Could not transfer money");
			return null;
		}

		// Create a transaction record
		Transaction transaction = Transaction.create_transaction(to_acct, from_acct, cust_id, date, transaction_type,
																 amount, connection);
		return transaction;
	}

	public static boolean cust_owns_acct(String a_id, String c_id, OracleConnection connection){
		String query = String.format("SELECT * FROM custaccounts WHERE c_id = '%s' AND a_id = '%s'",
											c_id, a_id);
		// Check customer owns the account
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query ) ){
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

	public static Transaction top_up(String to_pocket, String from_link, String date,
								 double amount, String cust_id, OracleConnection connection){
		// Check that we have a link between the two accounts
		if(!Account.accounts_are_linked(to_pocket, from_link, connection)){
			System.err.println("Top up failed -- Accounts are not linked or do not exist");
			return null;
		}
		Account pock_acc = Account.get_account_by_id(to_pocket, connection);
		if(!pock_acc.owner_id.equals(cust_id)){
			System.err.println("Top up failed -- Customer does not own this pocket account");
			return null;
		}

		// Check that owner account has enough money
		Account link_acc = Account.get_account_by_id(from_link, connection);

		// Check if it's the first transaction of the month
		if(Transaction.is_ftm(to_pocket, connection)){
			if(link_acc == null || link_acc.balance - amount - 5.00 < 0){
				System.err.println("Top up failed -- not enough money");
				return null;
			}
		}else{
			if(link_acc == null || link_acc.balance - amount < 0){
				System.err.println("Top up failed -- not enough money");
				return null;
			}
		}

		if(!Transaction.transfer_money(to_pocket, from_link, amount, connection)){
			System.err.println("Top up failed -- Could not transfer money to the pocket account");
			return null;
		}
		Transaction top_up_trans = Transaction.create_transaction(to_pocket, from_link, cust_id,
									 date, "" + Transaction.TransactionType.TOP_UP, amount, connection);
		if(top_up_trans == null){
			System.err.println("Top up failed -- Could not create transaction");
			return null;
		}

		// Charge $5 fee
		if(Transaction.is_ftm(to_pocket, connection)){
			Transaction fee = Transaction.create_transaction("", from_link, cust_id,
									 date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(!Transaction.transfer_money("", from_link, 5, connection)){
				System.err.println("Collect failed -- Could not transfer money from the link account");
				return null;
			}
		}

		return top_up_trans;
	}

	public static Transaction top_up_no_owner_check(String to_pocket, String from_link, String date,
								 double amount, OracleConnection connection){
		// Check that we have a link between the two accounts
		if(!Account.accounts_are_linked(to_pocket, from_link, connection)){
			System.err.println("Top up failed -- Accounts are not linked or do not exist");
			return null;
		}
		// Account pock_acc = Account.get_account_by_id(to_pocket, connection);
		// if(!pock_acc.owner_id.equals(cust_id)){
		// 	System.err.println("Top up failed -- Customer does not own this pocket account");
		// 	return null;
		// }

		// Check that owner account has enough money
		Account link_acc = Account.get_account_by_id(from_link, connection);

		// Check if it's the first transaction of the month
		if(Transaction.is_ftm(to_pocket, connection)){
			if(link_acc == null || link_acc.balance - amount - 5.00 < 0){
				System.err.println("Top up failed -- not enough money");
				return null;
			}
		}else{
			if(link_acc == null || link_acc.balance - amount < 0){
				System.err.println("Top up failed -- not enough money");
				return null;
			}
		}

		if(!Transaction.transfer_money(to_pocket, from_link, amount, connection)){
			System.err.println("Top up failed -- Could not transfer money to the pocket account");
			return null;
		}
		Transaction top_up_trans = Transaction.create_transaction(to_pocket, from_link, "",
									 date, "" + Transaction.TransactionType.TOP_UP, amount, connection);
		if(top_up_trans == null){
			System.err.println("Top up failed -- Could not create transaction");
			return null;
		}

		// Charge $5 fee
		if(Transaction.is_ftm(to_pocket, connection)){
			Transaction fee = Transaction.create_transaction("", from_link, "",
									 date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(!Transaction.transfer_money("", from_link, 5, connection)){
				System.err.println("Collect failed -- Could not transfer money from the link account");
				return null;
			}
		}

		return top_up_trans;
	}


	public static Transaction purchase(String from_pocket, String date, double amount, 
										String cust_id, OracleConnection connection){
		Transaction transact = null;
		Account pock_acc = Account.get_account_by_id(from_pocket, connection);

		if(pock_acc == null || Transaction.is_ftm(from_pocket, connection)){
			if(pock_acc.balance - amount - 5 < 0){
				System.err.println("Purchase failed -- not enough money for pruchase + fee");
				return null;
			}
		}

		// Check customer owns account
		if(!Transaction.cust_owns_acct(from_pocket, cust_id, connection)){
			System.err.println("Purchase failed -- customer doesn't own account");
			return null;
		}

		if(!pock_acc.owner_id.equals(cust_id)){
			System.err.println("Purchase failed -- Customer does not own this pocket account");
			return null;
		}

		if(!pock_acc.account_type.equals("" + Testable.AccountType.POCKET)){
			System.err.println("Purchase failed -- Must be pocket account");
			return null;
		}

		if(!Transaction.transfer_money("", from_pocket, amount, connection)){
			System.err.println("Purchase failed -- Could not transfer money to the pocket account");
			return null;
		}

		transact = Transaction.create_transaction("", from_pocket, cust_id,
									 date, "" + Transaction.TransactionType.PURCHASE, amount, connection);

		// Charge $5 fee
		if(Transaction.is_ftm(from_pocket, connection)){
			if(!Transaction.transfer_money("", from_pocket, 5, connection)){
				System.err.println("Purchase failed -- Could not transfer fee");
				return null;
			}
			Transaction fee = Transaction.create_transaction("", from_pocket, cust_id,
									 date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(fee == null){
				System.err.println("Could not create fee transaction");
				return null;
			}
		}

		return transact;
	}



	public static boolean withdraw(String from_acct, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		// Check customer exists
		if(Customer.get_cust_by_id(cust_id, connection) == null){
			System.err.println("Withdraw failed -- customer doesn't exist");
			return false;
		}

		// Check customer owns account
		if(!Transaction.cust_owns_acct(from_acct, cust_id, connection)){
			System.err.println("Withdraw failed -- customer doesn't own account");
			return false;
		}

		// Make sure account is NOT a pocket account
		Account account = Account.get_account_by_id(from_acct, connection);
		if(account != null && account.account_type.equals("" + Testable.AccountType.POCKET)){
			System.err.println("Withdraw failed -- cannot withdraw to pocket account");
			return false;
		}

		// Transfer money into the account
		if(!Transaction.transfer_money("", from_acct, amount, connection)){
			return false;
		}

		// Create a transaction record
		Transaction transaction = Transaction.create_transaction("", from_acct, cust_id, date,"" + type, amount, connection);
		if(transaction == null){
			System.err.println("Withdraw failed -- could not create transaction");
			return false;
		}

		return true;
	}

	public static boolean deposit(String to_acct, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		
		// Check customer exists
		if(Customer.get_cust_by_id(cust_id, connection) == null){
			System.err.println("Deposit failed -- customer doesn't exist");
			return false;
		}

		// Check customer owns account
		if(!Transaction.cust_owns_acct(to_acct, cust_id, connection)){
			System.err.println("Deposit failed -- customer doesn't own account");
			return false;
		}

		// Make sure account is NOT a pocket account
		Account account = Account.get_account_by_id(to_acct, connection);
		if(account != null && account.account_type.equals("" + Testable.AccountType.POCKET)){
			System.err.println("Deposit failed -- cannot deposit to pocket account");
			return false;
		}

		// Transfer money into the account
		if(!Transaction.transfer_money(to_acct, "", amount, connection)){
			return false;
		}

		// Create a transaction record
		Transaction transaction = Transaction.create_transaction(to_acct, "", cust_id, date,"" + type, amount, connection);
		if(transaction == null){
			System.err.println("Deposit failed -- could not create transaction");
			return false;
		}

		return true;
	}

	public static boolean deposit_no_owner_check(String to_acct, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		
		// Check customer exists
		// if(Customer.get_cust_by_id(cust_id, connection) == null){
			// System.err.println("Deposit failed -- customer doesn't exist");
			// return false;
		// }

		// Check customer owns account
		// if(!Transaction.cust_owns_acct(to_acct, cust_id, connection)){
			// System.err.println("Deposit failed -- customer doesn't own account");
			// return false;
		// }

		// Make sure account is NOT a pocket account
		Account account = Account.get_account_by_id(to_acct, connection);
		if(account != null && account.account_type.equals("" + Testable.AccountType.POCKET)){
			System.err.println("Deposit failed -- cannot deposit to pocket account");
			return false;
		}

		// Transfer money into the account
		if(!Transaction.transfer_money(to_acct, "", amount, connection)){
			return false;
		}

		// Create a transaction record
		Transaction transaction = Transaction.create_transaction(to_acct, "", "", date,"" + type, amount, connection);
		if(transaction == null){
			System.err.println("Deposit failed -- could not create transaction");
			return false;
		}

		return true;
	}


	public static Transaction transfer(String to_acct, String from_acct, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		
		// Check customer is an owner on both accounts
		ArrayList<String> to_acct_owners = Account.get_account_owners(to_acct, connection);
		ArrayList<String> from_acct_owners = Account.get_account_owners(from_acct, connection);
		boolean owns_to_acct = false;
		boolean owns_from_acct = false;
		if(to_acct_owners == null || from_acct_owners == null){
			System.err.println("Transfer failed Could not find account owners, does accnt exist?");
			return null;
		}
		for(int i = 0; i < to_acct_owners.size(); i++){
			if(to_acct_owners.get(i).equals(cust_id)){
				owns_to_acct = true;
				break;
			}
		}
		for(int i = 0; i < from_acct_owners.size(); i++){
			if(from_acct_owners.get(i).equals(cust_id)){
				owns_from_acct = true;
				break;
			}
		}

		if(!(owns_to_acct && owns_from_acct)){
			System.err.println("Transfer failed -- customer does not own both accounts");
			return null;
		}

		// Ensure that neither account is a pocket account
		if(Account.get_account_type(to_acct, connection).equals("" + Testable.AccountType.POCKET) ||
			Account.get_account_type(from_acct, connection).equals("" + Testable.AccountType.POCKET)){
			System.err.println("Transfer failed -- cannot transfer on a pocket account");
			return null;
		}

		// Check not transferring over 2000
		if(amount > 2000){
			System.err.println("Transfer failed -- cannot transfer >2000");
			return null;
		}

		// Make sure from account has >= amount
		if(Account.get_account_balance(from_acct, connection) < amount){
			System.err.println("Transfer failed -- from account balance insufficient");
			return null;
		}

		// Try to transfer money
		if(!Transaction.transfer_money(to_acct, from_acct, amount, connection)){
			System.err.println("Transaction failed -- could not transfer money");
			return null;
		}

		// Create a transaction
		Transaction transact = Transaction.create_transaction(to_acct, from_acct, cust_id,
									 Bank.get_date(connection), "" + Transaction.TransactionType.TRANSFER
									 , amount, connection);

		if(transact == null){
			System.err.println("Transfer failed -- could not create transaction");
			return null;
		}

		return transact;
	}


	public static Transaction collect(String to_link, String from_pocket, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		// Make sure to and from are linked and to is not the pocket
		// Check that we have a link between the two accounts
		if(!Account.accounts_are_linked(from_pocket, to_link, connection)){
			System.err.println("Collect failed -- Accounts are not linked or do not exist");
			return null;
		}
		Account pock_acc = Account.get_account_by_id(from_pocket, connection);
		if(!pock_acc.owner_id.equals(cust_id)){
			System.err.println("Collect failed -- Customer does not own this pocket account");
			return null;
		}

		// Check that pocket account has enough money
		Account link_acc = Account.get_account_by_id(to_link, connection);

		// Check if it's the first transaction of the month
		if(Transaction.is_ftm(from_pocket, connection)){
			if(pock_acc.balance - amount - 5.00 - (0.03 * amount) < 0){
				System.err.println("Collect failed -- not enough money");
				return null;
			}
		}else{
			if(pock_acc.balance - amount - (0.03 * amount) < 0){
				System.err.println("Collect failed -- not enough money");
				return null;
			}
		}

		if(!Transaction.transfer_money(to_link, from_pocket, amount, connection)){
			System.err.println("Collect failed -- Could not transfer money from the pocket account");
			return null;
		}
		Transaction collect_trans = Transaction.create_transaction(to_link, from_pocket, cust_id,
									 date, "" + Transaction.TransactionType.COLLECT, amount, connection);
		if(collect_trans == null){
			System.err.println("Collect failed -- Could not create transaction");
			return null;
		}

		// Charge $5 fee
		if(Transaction.is_ftm(from_pocket, connection)){
			if(!Transaction.transfer_money("", from_pocket, 5, connection)){
				System.err.println("Collect failed -- Could not transfer money from the pocket account");
				return null;
			}

			Transaction fee = Transaction.create_transaction("", from_pocket, cust_id,
									 date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
		}

		// Charge 3% fee
		if(!Transaction.transfer_money("", from_pocket, amount * 0.03, connection)){
			System.err.println("Collect failed -- Could not transfer money from the pocket account");
			return null;
		}
		Transaction pct_fee = Transaction.create_transaction("", from_pocket, cust_id,
									 date, "" + Transaction.TransactionType.PCT_FEE, amount * 0.03, connection);
		if(pct_fee == null){
			System.err.println("Collect failed -- Could not create fee transaction");
		}
		return collect_trans;


	}


	public static Transaction pay_friend(String to_acct, String from_acct, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		// Check both accounts are pocket accounts
		Account to_pock_acct = Account.get_account_by_id(to_acct, connection);
		Account from_pock_acct = Account.get_account_by_id(from_acct, connection);
		
		// Check accounts not null
		if(to_pock_acct == null || from_pock_acct == null){
			System.err.println("Pay_Friend failed -- one of the accounts doesn't exist");
			return null;
		}

		if(!to_pock_acct.account_type.equals("" + Testable.AccountType.POCKET) ||
		   !from_pock_acct.account_type.equals("" + Testable.AccountType.POCKET) ){
			System.err.println("Pay_Friend failed -- not a pocket account");
			return null;
		}

		// Check if customer owns the from account
		if(from_pock_acct.owner_id == cust_id){
			System.err.println("Pay_Friend failed -- the customer doesn't own the from account");
			return null;
		}
		
		// Check if it's first transaction of the month for either account
		if(Transaction.is_ftm(to_acct, connection)){
			double balance = Account.get_account_balance(to_acct, connection);
			if(balance < 5){
				System.err.println("Pay_Friend failed -- to acct can't pay FTM");
				return null;
			}
		}

		if(Transaction.is_ftm(from_acct, connection)){
			double balance = Account.get_account_balance(from_acct, connection);
			if(balance < amount + 5){
				System.err.println("Pay_Friend failed -- from acct can't pay FTM");
				return null;
			}
		}

		// Do transfer between accounts
		if(!Transaction.transfer_money(to_acct, from_acct, amount, connection)){
			System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
			return null;
		}

		// Create transaction
		Transaction pay_friend = Transaction.create_transaction(to_acct, from_acct, cust_id,
									 date, "" + Transaction.TransactionType.PAY_FRIEND, amount, connection);
		if(pay_friend == null){
			System.err.println("Pay_Friend failed -- could not create transaction");
			return null;
		}

		// Do FTM transfer / transaction for to_acct
		if(Transaction.is_ftm(to_acct, connection)){
			if(!Transaction.transfer_money("", to_acct, 5, connection)){
				System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
				return null;
			}
			Transaction pay_friend_ftm = Transaction.create_transaction("", to_acct, cust_id,
									 date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(pay_friend_ftm == null){
				System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
				return null;
			}
		}

		// Do FTM transfer / transaction for from_acct
		if(Transaction.is_ftm(from_acct, connection)){
			Transaction pay_friend_ftm_2 = Transaction.create_transaction_and_transfer("", from_acct, cust_id,
										date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(pay_friend_ftm_2 == null){
				System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
				return null;
			}
		}

		return pay_friend;
	}

	public static Transaction pay_friend_no_owner_check(String to_acct, String from_acct, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		// Check both accounts are pocket accounts
		Account to_pock_acct = Account.get_account_by_id(to_acct, connection);
		Account from_pock_acct = Account.get_account_by_id(from_acct, connection);
		
		// Check accounts not null
		if(to_pock_acct == null || from_pock_acct == null){
			System.err.println("Pay_Friend failed -- one of the accounts doesn't exist");
			return null;
		}

		if(!to_pock_acct.account_type.equals("" + Testable.AccountType.POCKET) ||
		   !from_pock_acct.account_type.equals("" + Testable.AccountType.POCKET) ){
			System.err.println("Pay_Friend failed -- not a pocket account");
			return null;
		}

		// // Check if customer owns the from account
		// if(from_pock_acct.owner_id == cust_id){
		// 	System.err.println("Pay_Friend failed -- the customer doesn't own the from account");
		// 	return null;
		// }
		
		// Check if it's first transaction of the month for either account
		if(Transaction.is_ftm(to_acct, connection)){
			double balance = Account.get_account_balance(to_acct, connection);
			if(balance < 5){
				System.err.println("Pay_Friend failed -- to acct can't pay FTM");
				return null;
			}
		}

		if(Transaction.is_ftm(from_acct, connection)){
			double balance = Account.get_account_balance(from_acct, connection);
			if(balance < amount + 5){
				System.err.println("Pay_Friend failed -- from acct can't pay FTM");
				return null;
			}
		}

		// Do transfer between accounts
		if(!Transaction.transfer_money(to_acct, from_acct, amount, connection)){
			System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
			return null;
		}

		// Create transaction
		Transaction pay_friend = Transaction.create_transaction(to_acct, from_acct, "",
									 date, "" + Transaction.TransactionType.PAY_FRIEND, amount, connection);
		if(pay_friend == null){
			System.err.println("Pay_Friend failed -- could not create transaction");
			return null;
		}

		// Do FTM transfer / transaction for to_acct
		if(Transaction.is_ftm(to_acct, connection)){
			if(!Transaction.transfer_money("", to_acct, 5, connection)){
				System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
				return null;
			}
			Transaction pay_friend_ftm = Transaction.create_transaction("", to_acct, "",
									 date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(pay_friend_ftm == null){
				System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
				return null;
			}
		}

		// Do FTM transfer / transaction for from_acct
		if(Transaction.is_ftm(from_acct, connection)){
			Transaction pay_friend_ftm_2 = Transaction.create_transaction_and_transfer("", from_acct, "",
										date, "" + Transaction.TransactionType.FTM_FEE, 5, connection);
			if(pay_friend_ftm_2 == null){
				System.err.println("Pay_Friend failed -- Could not transfer money from the pocket account");
				return null;
			}
		}

		return pay_friend;
	}

	public static Transaction wire(String to_acct, String from_acct, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		// Check customer is an owner on both accounts
		Account to_account_obj = Account.get_account_by_id(to_acct, connection);
		if(to_account_obj == null){
			System.err.println("Wire failed Could not find account owners, does accnt exist?");
			return null;
		}
		
		if(!(Transaction.cust_owns_acct(from_acct, cust_id, connection))){
			System.err.println("Wire failed -- customer does not own account");
			return null;
		}

		// Ensure that neither account is a pocket account
		if(Account.get_account_type(to_acct, connection).equals("" + Testable.AccountType.POCKET) ||
			Account.get_account_type(from_acct, connection).equals("" + Testable.AccountType.POCKET)){
			System.err.println("Wire failed -- cannot transfer on a pocket account");
			return null;
		}

		// Make sure from account has >= amount
		if(Account.get_account_balance(from_acct, connection) < amount + (0.02 * amount)){
			System.err.println("Wire failed -- from account balance insufficient");
			return null;
		}

		// Create a transaction
		Transaction transact = Transaction.create_transaction_and_transfer(to_acct, from_acct, cust_id,
									 date, "" + Transaction.TransactionType.WIRE, amount, connection);

		if(transact == null){
			System.err.println("Wire failed -- could not create transaction");
			return null;
		}

		Transaction fee = Transaction.create_transaction_and_transfer("", from_acct, cust_id, date,
									"" + Transaction.TransactionType.PCT_FEE, amount * 0.02, connection);
		if(fee == null){
			System.err.println("Wire failed -- could not create transaction fee");
		}
		return transact;
	}

	public static Transaction write_check(String from_acct, String cust_id, String date, 
						 Transaction.TransactionType type, double amount, OracleConnection connection){
		// Check customer exists
		if(Customer.get_cust_by_id(cust_id, connection) == null){
			System.err.println("Write_check failed -- customer doesn't exist");
			return null;
		}

		// Check customer owns account
		if(!Transaction.cust_owns_acct(from_acct, cust_id, connection)){
			System.err.println("Write_check failed -- customer doesn't own account");
			return null;
		}

		// Make sure account is NOT a pocket account
		Account account = Account.get_account_by_id(from_acct, connection);
		if(account != null && account.account_type.equals("" + Testable.AccountType.POCKET)){
			System.err.println("Write_check failed -- cannot withdraw from pocket account");
			return null;
		}
		
		// Make sure not a savings account
		if(account != null && account.account_type.equals("" + Testable.AccountType.SAVINGS)){
			System.err.println("Write_check failed -- cannot withdraw from savings account");
			return null;
		}

		// Transfer money into the account
		if(!Transaction.transfer_money("", from_acct, amount, connection)){
			return null;
		}

		// Create a transaction record
		Transaction transaction = Transaction.create_transaction("", from_acct, cust_id, date,
								"" + Transaction.TransactionType.WRITE_CHECK, amount, connection);
		if(transaction == null){
			System.err.println("Write_check failed -- could not create transaction");
			return null;
		}

		return transaction;
	}

	public static boolean accrue_interest(String a_id, OracleConnection connection){
		Account account = Account.get_account_by_id(a_id, connection);
		if(account == null){
			System.err.println("Accrue_interest failed --Error, could not get account");
			return false;
		}

		// Don't accrue interest on a pocket account or a student checking
		if(account.account_type.equals("" + Testable.AccountType.POCKET) || 
			account.account_type.equals("" + Testable.AccountType.STUDENT_CHECKING)){
			return true;
		}

		// Don't accrue interest on closed accounts
		if(!account.is_open){
			return true;
		}

		double daily_balance = account.balance;

		ArrayList<Transaction> transactions = Transaction.get_acct_transactions_this_month(a_id, connection);
		if(transactions == null){
			System.err.println("Accrue_interest failed -- could not get account transactions");
			return false;
		}

		double interest_rate;
		if(account.account_type.equals("" + Testable.AccountType.INTEREST_CHECKING)){
			interest_rate = Bank.get_interest_rate(Testable.AccountType.INTEREST_CHECKING, connection);
		}else{
			interest_rate = Bank.get_interest_rate(Testable.AccountType.SAVINGS, connection);
		}

		double interest_amount = 0;
		for(int i = Bank.get_days_in_current_month(connection); i > 0; i--){
			// Compute weighted avg with the day's daily balance
			interest_amount += (daily_balance / Bank.get_days_in_current_month(connection));

			// Adjust previous days balance by undoing transactions on current day
			for(int j = 0; j < transactions.size(); j++){

				// Check if transaction occurred on this day
				if(Integer.parseInt(transactions.get(j).date.split("-")[2]) == i){
					if(transactions.get(j).to_acct != null && transactions.get(j).to_acct.equals(a_id)){
						// Money transferred to the account, reverse to get previous days balance
						daily_balance -= transactions.get(j).amount;
					}else if(transactions.get(j).from_acct != null && transactions.get(j).from_acct.equals(a_id)){
						//Money transferred from the account, reverse to get the previous days balance
						daily_balance += transactions.get(j).amount;
					}
				}
			}
		}

		// Multiply by monthly interest rate
		interest_amount *= ((interest_rate / 12)/100);

		Transaction transaction = Transaction.create_transaction_and_transfer(a_id, "", "", Bank.get_date(connection), 
			"" + Transaction.TransactionType.ACCRUE_INTEREST, interest_amount, connection);

		if(transaction == null){
			System.err.println("Accrue_interest failed -- could not create transaction");
			return false;
		}
		return true;
	}

	public static boolean is_ftm(String a_id, OracleConnection connection){
		if(Transaction.get_acct_transactions_this_month(a_id, connection) != null){
			return Transaction.get_acct_transactions_this_month(a_id, connection).size() == 0;
		}
		return true;
	}

	// Transfer money between account(s) if they exist and are not closed
	public static boolean transfer_money(String to_acct, String from_acct, double amount, OracleConnection connection){
		// Make sure at least one of to or from is specified
		if(to_acct.equals("") && from_acct.equals("")){
			return false;
		}

		// Get accounts, returning null on ""
		Account from_acct_temp = Account.get_account_by_id(from_acct, connection);
		Account to_acct_temp = Account.get_account_by_id(to_acct, connection);

		// Check required accounts exist
		if(!to_acct.equals("") && to_acct_temp == null ||
			!from_acct.equals("") && from_acct_temp == null){
			System.err.println("One or both accounts supplied do not exist");
			return false;
		}

		// Check account is open
		if(!from_acct.equals("") && from_acct_temp != null && !from_acct_temp.is_open ||
			!to_acct.equals("") && to_acct_temp != null && !to_acct_temp.is_open){
			System.err.println("A transaction cannot be done on a closed acct -- failed");
			return false;
		}

		if(!from_acct.equals("")){
			// Check transaction won't result in negative balance
			if(from_acct_temp.balance - amount < 0){
				System.err.println("Transaction would result in a negative balance -- failed");
				return false;
			}

			// Subtract amount from this account
			String query = String.format("UPDATE accounts SET balance = %s WHERE a_id = '%s'",
											from_acct_temp.balance - amount, from_acct);
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

			// If balance is in 0 < x < 0.01 -- close account
			if( from_acct_temp.balance - amount <= 0.01 && 
				from_acct_temp.balance >= 0){
				if(!Account.close_account_by_id(from_acct, connection)){
					return false;
				}
				if(!Account.close_pocket_accounts_by_owner_id(from_acct, connection)){
					return false;
				}
			}
		}

		if(!to_acct.equals("")){
			// Add amount to this account
			String query = String.format("UPDATE accounts SET balance = %s WHERE a_id = '%s'",
											to_acct_temp.balance + amount, to_acct);
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
		}

		return true;
	}

	public static ArrayList<Transaction> get_acct_transactions_this_month(String a_id, OracleConnection connection){
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();
		String month = Bank.get_month(connection);

		// Find transactions where account sent or received money
		String query = String.format("SELECT * FROM transactions T WHERE " +
									"(T.to_acct = '%s' OR T.from_acct = '%s')", a_id, a_id);
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				while(rs.next()){
					String t_date = rs.getString("t_date");
					if(!t_date.equals("xxxx-xx-xx")){
						if(t_date.split("-")[1].equals(month)){
							transactions.add(
								new Transaction(
									rs.getInt("t_id"), rs.getString("to_acct"),
									rs.getString("from_acct"),rs.getString("cust_id"),
									rs.getString("t_date"), rs.getString("t_type"), rs.getDouble("amount")
								)
							);
						}
					}
				}
			}catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
		return transactions;
	}

	public Transaction(String to_acct, String from_acct, String cust_id,
						String date, String transaction_type, double amount){
		this.to_acct = to_acct;
		this.from_acct = from_acct;
		this.cust_id = cust_id;
		this.date = date;
		this.transaction_type = transaction_type;
		this.amount = amount;
	}

	public Transaction(int t_id, String to_acct, String from_acct, String cust_id,
						String date, String transaction_type, double amount){
		this.to_acct = to_acct;
		this.from_acct = from_acct;
		this.cust_id = cust_id;
		this.date = date;
		this.transaction_type = transaction_type;
		this.amount = amount;
		this.t_id = t_id;
	}

}