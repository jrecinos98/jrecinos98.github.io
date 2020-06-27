package cs174a;                                             // THE BASE PACKAGE FOR YOUR APP MUST BE THIS ONE.  But you may add subpackages.

// You may have as many imports as you need.
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

import java.util.Scanner;
import java.util.ArrayList;
/**
 * The most important class for your application.
 * DO NOT CHANGE ITS SIGNATURE.
 */
public class App implements Testable
{
	private OracleConnection connection;                   // Example connection object to your DB.
	private Interface gui;
	// connection descriptor.
	final static String DB_URL= "jdbc:oracle:thin:@cs174a.cs.ucsb.edu:1521/orcl";
	final static String DB_USER = "c##ncduncan";
	final static String DB_PASSWORD = "3937679";
	/**
	 * Default constructor.
	 * DO NOT REMOVE.
	 */
	App() {
		// TODO: Any actions you need.

	}

	/**
	 * This is an example access operation to the DB.
	 */
	void exampleAccessToDB()
	{
		// Statement and ResultSet are AutoCloseable and closed automatically.
		try( Statement statement = this.connection.createStatement() )
		{
			try( ResultSet resultSet = statement.executeQuery( "select owner, table_name from all_tables" ) )
			{
				while( resultSet.next() )
					System.out.println( resultSet.getString( 1 ) + " " + resultSet.getString( 2 ) + " " );
			}
		}
		catch( SQLException e )
		{
			System.err.println( e.getMessage() );
		}
	}

	////////////////////////////// Implement all of the methods given in the interface /////////////////////////////////
	// Check the Testable.java interface for the function signatures and descriptions.

	@Override
	public String initializeSystem()
	{
		
		// Initialize your system.  Probably setting up the DB connection.
		Properties info = new Properties();
		info.put( OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER );
		info.put( OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD );
		info.put( OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20" );

		try
		{
			OracleDataSource ods = new OracleDataSource();
			ods.setURL( DB_URL );
			ods.setConnectionProperties( info );
			connection = (OracleConnection) ods.getConnection();
			return "0";
		}
		catch( SQLException e )
		{
			System.err.println( e.getMessage() );
			return "1";
		}
	}

	public void run_demo(){
		// Setup
		try{
			this.initializeSystem();
			this.dropTables();
			this.createTables();


			// Set date march 1, 2011
			Bank.set_date(""+ 2011, "" + 3, ""+1, this.connection);

			// Create accounts
			this.createCheckingSavingsAccount(Testable.AccountType.STUDENT_CHECKING, "17431", 1200, 
				"344151573", "Joe Pepsi", "3210 State St");
			Customer.update_pin("344151573", "1717", "3692", this.connection);

			this.createCheckingSavingsAccount(Testable.AccountType.STUDENT_CHECKING, "54321", 21000,
			 "212431965" , "Hurryson Ford", "678 State St");
			Customer.update_pin("212431965", "1717", "3532", this.connection);

			this.createCheckingSavingsAccount(Testable.AccountType.STUDENT_CHECKING, "12121", 1200,
			 "207843218" , "David Copperfill", "1357 State St");
			Customer.update_pin("207843218", "1717", "8582", this.connection);

			this.createCheckingSavingsAccount(Testable.AccountType.INTEREST_CHECKING, "41725", 15000,
			 "201674933" , "George Brush", "5346 Foothill Av");
			Customer.update_pin("201674933", "1717", "9824", this.connection);

			this.createCheckingSavingsAccount(Testable.AccountType.INTEREST_CHECKING, "93156", 2000000,
			 "209378521" , "Kelvin Costner", "Santa Cruz #3579");
			Customer.update_pin("209378521", "1717", "4659", this.connection);

			this.createPocketAccount("53027", "12121", 50, "207843218");

			this.createCheckingSavingsAccount(Testable.AccountType.SAVINGS, "43942", 1289,
			 "361721022" , "Alfred Hitchcock", "6667 El Colegio #40");
			Customer.update_pin("361721022", "1717", "1234", this.connection);

			this.createCustomer("43942", "400651982" , "Pit Wilson", "911 State St");
			Customer.update_pin("400651982", "1717", "1821", this.connection);

			this.createCheckingSavingsAccount(Testable.AccountType.SAVINGS, "29107", 34000,
			 "209378521" , "Kelvin Costner", "Santa Cruz #3579");

			this.createCustomer("29107", "212116070", "Li Kung", "2 People's Rd Beijing ");
			Customer.update_pin("212116070", "1717", "9173", this.connection);
			
			this.createCheckingSavingsAccount(Testable.AccountType.SAVINGS, "19023", 2300,
			 "412231856" , "Cindy Laugher", "7000 Hollister");
			Customer.update_pin("412231856", "1717", "3764", this.connection);

			this.createCustomer("19023", "401605312" , "Fatal Castro", "3756 La Cumbre Plaza");
			Customer.update_pin("401605312", "1717", "8193", this.connection);

			this.createPocketAccount("60413", "43942", 20, "400651982");

			this.createCheckingSavingsAccount(Testable.AccountType.SAVINGS, "32156", 1000,
			 "188212217" , "Magic Jordon", "3852 Court Rd");
			Customer.update_pin("188212217", "1717", "7351", this.connection);

			this.createCheckingSavingsAccount(Testable.AccountType.INTEREST_CHECKING, "76543", 8456,
			 "212116070" , "Li Kung", "2 People's Rd Beijing");

			this.createPocketAccount("43947", "29107", 30, "212116070");

			this.createPocketAccount("67521", "19023", 100, "401605312");


			// Create non-primary owners and link them
			this.createCustomer("41725", "231403227" , "Billy Clinton", "5777 Hollister");
			Customer.update_pin("231403227", "1717", "1468", this.connection);

			this.createCustomer("54321", "122219876" , "Elizabeth Sailor", "4321 State St");
			Customer.update_pin("122219876", "1717", "3856", this.connection);

			this.createCustomer("17431", "322175130" , "Ivan Lendme", "1235 Johnson Dr");
			Customer.update_pin("322175130", "1717", "8471", this.connection);

			this.createCustomer("54321", "203491209" , "Nam-Hoi Chung", "1997 People's St HK");
			Customer.update_pin("203491209", "1717", "5340", this.connection);

			this.createCustomer("93156", "210389768" , "Olive Stoner", "6689 El Colegio #151");
			Customer.update_pin("210389768", "1717", "8452", this.connection);

			// Add all other customer-account links
			Account.create_acct_ownership("17431", "412231856", this.connection);

			Account.create_acct_ownership("54321", "412231856", this.connection);

			Account.create_acct_ownership("41725", "401605312", this.connection);

			Account.create_acct_ownership("76543", "188212217", this.connection);

			Account.create_acct_ownership("93156", "188212217", this.connection);
			Account.create_acct_ownership("93156", "122219876", this.connection);
			Account.create_acct_ownership("93156", "203491209", this.connection);

			Account.create_acct_ownership("43942", "212431965", this.connection);
			Account.create_acct_ownership("43942", "322175130", this.connection);

			Account.create_acct_ownership("29107", "210389768", this.connection);

			Account.create_acct_ownership("19023", "201674933", this.connection);

			Account.create_acct_ownership("32156", "207843218", this.connection);
			Account.create_acct_ownership("32156", "122219876", this.connection);
			Account.create_acct_ownership("32156", "344151573", this.connection);
			Account.create_acct_ownership("32156", "203491209", this.connection);
			Account.create_acct_ownership("32156", "210389768", this.connection);

			// Other transactions
			Bank.set_date("" + 2011, "" + 3, "" + 2, this.connection);
			Transaction.deposit("17431", "344151573", Bank.get_date(this.connection), 
							 Transaction.TransactionType.DEPOSIT, 8800, this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 3, this.connection);
			Transaction.withdraw("54321", "122219876", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WITHDRAWAL, 3000, this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 5, this.connection);
			Transaction.withdraw("76543", "212116070", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WITHDRAWAL, 2000, this.connection);

			Transaction.purchase("53027", Bank.get_date(this.connection), 5, 
											"207843218", connection);

			Bank.set_date("" + 2011, "" + 3, "" + 6, this.connection);
			Transaction.withdraw("93156", "188212217", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WITHDRAWAL, 1000000, this.connection);

			Transaction.write_check("93156", "209378521", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WRITE_CHECK, 950000, this.connection);
			
			Transaction.withdraw("29107", "212116070", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WITHDRAWAL, 4000, this.connection);

			Transaction.collect("29107", "43947", "212116070", Bank.get_date(this.connection), 
							 Transaction.TransactionType.COLLECT, 10, this.connection);

			Transaction.top_up("43947", "29107", Bank.get_date(this.connection),
									 30, "212116070", this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 7, this.connection);
			Transaction.transfer("17431", "43942", "322175130", Bank.get_date(this.connection), 
							 Transaction.TransactionType.TRANSFER, 289, this.connection);

			Transaction.withdraw("43942", "400651982", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WITHDRAWAL, 289, this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 8, this.connection);
			Transaction.pay_friend("67521", "60413", "400651982", Bank.get_date(this.connection), 
							 Transaction.TransactionType.PAY_FRIEND, 10, this.connection);

			Transaction.deposit("93156", "210389768", Bank.get_date(this.connection), 
							 Transaction.TransactionType.DEPOSIT, 50000, this.connection);

			Transaction.write_check("12121", "207843218", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WRITE_CHECK, 200, this.connection);

			Transaction.transfer("19023",  "41725", "201674933", Bank.get_date(this.connection), 
							 Transaction.TransactionType.TRANSFER, 1000, this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 9, this.connection);
			Transaction.wire("32156", "41725", "401605312", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WIRE, 4000, this.connection);

			Transaction.pay_friend("60413", "53027", "207843218", Bank.get_date(this.connection), 
							 Transaction.TransactionType.PAY_FRIEND, 10, this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 10, this.connection);
			Transaction.purchase("60413", Bank.get_date(this.connection), 15, 
											"400651982", this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 12, this.connection);
			Transaction.withdraw("93156", "203491209", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WITHDRAWAL, 20000, this.connection);

			Transaction.write_check("76543", "188212217", Bank.get_date(this.connection), 
							 Transaction.TransactionType.WRITE_CHECK, 456, this.connection);

			Transaction.top_up("67521", "19023", Bank.get_date(this.connection),
									 50, "401605312", this.connection);

			Bank.set_date("" + 2011, "" + 3, "" + 14, this.connection);
			Transaction.pay_friend("53027", "67521", "401605312", Bank.get_date(this.connection), 
							 Transaction.TransactionType.PAY_FRIEND, 20, this.connection);

			Transaction.collect("29107", "43947", "212116070", Bank.get_date(this.connection), 
							 Transaction.TransactionType.COLLECT, 15, this.connection);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			this.close_connection();
		}
	}

	@Override
	public String listClosedAccounts()
	{
		try {
			ArrayList<String> closed_accts = Account.get_closed_accounts(this.connection);
			String closed = "0";
			if(closed_accts == null){
				return "1";
			}else{
				for(int i = 0; i < closed_accts.size(); i++){
					closed += " " + closed_accts.get(i);
				}
				return closed;
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String createCheckingSavingsAccount( AccountType accountType, String id, double initialBalance, String tin, String name, String address)
	{
		try{
			Account new_acct =  Account.create_account(accountType, id, initialBalance,
											 tin, name, address, this.connection);
			if(new_acct == null){
				return "1";
			}else{
				String response = String.format("0 %s %s %.2f %s", id, "" + accountType, initialBalance, tin);
				return response;
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String payFriend(String from, String to, double amount ){
		try{
			// Can't pay same account
			if(from.equals(to)){
				return "1";
			}
			if(amount <= 0){
				return "1";
			}
			Transaction transact = Transaction.pay_friend_no_owner_check(to, from, Bank.get_date(this.connection), 
							 Transaction.TransactionType.PAY_FRIEND, amount, connection);
			if(transact == null){
				return "1";
			}else{
				double fromNewBalance = Account.get_account_balance(from, this.connection);
				double toNewBalance = Account.get_account_balance(to, this.connection);
				String response = String.format("0 %.2f %.2f", Math.abs(fromNewBalance), Math.abs(toNewBalance));
				return response;
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String topUp( String accountId, double amount ){
		try{
			if(amount <= 0){
				return "1";
			}
			Account account = Account.get_account_by_id(accountId, this.connection);
			String linked_id = Account.get_linked(accountId, this.connection);
			if(linked_id != ""){
				Transaction transact = Transaction.top_up_no_owner_check(accountId, linked_id, Bank.get_date(this.connection),
							 amount, connection);
				if(transact != null){
					double pocket_balance = Account.get_account_balance(accountId, this.connection);
					double linked_balance = Account.get_account_balance(linked_id, this.connection);
					String resp = String.format("0 %.2f %.2f", Math.abs(linked_balance), Math.abs(pocket_balance));
					return resp;
				}			
			}
			return "1";
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String showBalance( String accountId ){
		try{
			double balance = Account.get_account_balance(accountId, this.connection);
			if(balance == -1){
				return "1";
			}else{
				String response = String.format("0 %.2f", Math.abs(balance));
				return response;
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String deposit( String accountId, double amount ){
		try{
			if(amount <= 0){
				return "1";
			}
			double old = Account.get_account_balance(accountId, this.connection);
			boolean transact = Transaction.deposit_no_owner_check(accountId, Bank.get_date(this.connection), 
							 Transaction.TransactionType.DEPOSIT, amount, this.connection);
			double new_b = Account.get_account_balance(accountId, this.connection);
			if(transact == false || old == -1 || new_b == -1){
				return "1";
			}else{
				String response = String.format("0 %.2f %.2f", Math.abs(old), Math.abs(new_b));
				return response;
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String createCustomer( String accountId, String tin, String name, String address ){
		try{
			Account account = Account.get_account_by_id(accountId, this.connection);
			if(account == null){
				return "1";
			}else{
				if(!account.is_open){
					// Customer cannot link to a closed account
					return "1";
				}
				Customer cust = Customer.create_customer(tin, name, address,this.connection);
				if(cust == null){
					return "1";
				}else{
					if(Account.create_acct_ownership(accountId, tin, this.connection)){
						return "0";
					}else{
						return "1";
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String createPocketAccount( String id, String linkedId, double initialTopUp, String tin ){
		try{
			Account account = Account.create_pocket_account(id, linkedId, initialTopUp,
												    tin, this.connection);
			if(account == null){
				return "1";
			}else{
				String resp = String.format("0 %s %s %.2f %s", id, "" + Testable.AccountType.POCKET, initialTopUp, tin);
				return resp;
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String setDate( int year, int month, int day ){
		try{
			boolean success = Bank.set_date( "" + year, "" + month, "" + day, this.connection);
			if(success){
				return "0 " + year + "-" + Bank.pretty_month("" + month) + "-" + Bank.pretty_day("" + day);
			}else{
				return "1";
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String dropTables(){
		try{
			if(DBSystem.execute_queries_from_file("./scripts/destroy_db.sql", this.connection)){
				return "0";
			}else{
				return "1";
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	@Override
	public String createTables(){
		try{
			if(DBSystem.execute_queries_from_file("./scripts/create_db.sql", this.connection)){
				Bank.bank_set_up(this.connection);
				return "0";
			}else{
				return "1";
			}
		}catch(Exception e){
			e.printStackTrace();
			return "1";
		}
	}

	public String close_connection(){
		try{
			this.connection.close();
			System.err.println("Connection closed!");
			return "0";
		}catch(SQLException e){
			e.printStackTrace();
			return "1";
		}
	}
	public void run_gui(){
		this.gui = new Interface(this.connection);
	}

	public void run_cli(){
		try{
			// Translate CLI to GUI
			Scanner in = new Scanner(System.in);
			System.out.println("Enter (1) for GUI or (2) for unit tests");
			String resp = in.nextLine();
			if(resp.equals("1")){
				Interface gui = new Interface(connection);
				gui.setVisible(true);
			}else if (resp.equals("2")){
				Tester tester = new Tester();
				tester.run_tests(connection);
				this.close_connection();
			}
			else{
				System.out.println("Did not recognize input -- should be 1 , 2, or 3");
			}
			
		} catch( Exception e ) {
			System.err.println( e.getMessage() );
			e.printStackTrace();
			this.close_connection();
		}

	}
}
