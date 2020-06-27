package cs174a;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

import java.util.Scanner;
import java.util.ArrayList;

public class Tester{
	private OracleConnection connection;
	private int id = 30;
	private ManagerOperations mops = new ManagerOperations();

	public String get_next_id(){
		String next = "" + id;
		this.id++;
		return next;
	}

	public String result(String method, int status){
		String output = method;
		if(status == 0){
			output += " " + "PASS";
		}else if (status == 1){
			output += " " + "FAIL";
		}else{
			output += " " + "ERROR";
		}
		return output;
	}
	public int pass(){ return 0; }
	public int fail(){ return 1; }
	public int error(){ return 2; }

	public void setup(OracleConnection connection){
		// Initialize your system.  Probably setting up the DB connection.
		this.connection = connection;

		if(DBSystem.execute_queries_from_file("./scripts/destroy_db.sql", this.connection)){
			System.out.println("Successfully destroyed tables");
		}else{
			System.out.println("Error destroying tables");
		}

		if(DBSystem.execute_queries_from_file("./scripts/create_db.sql", this.connection)){
			System.out.println("Successfully created tables");
		}else{
			System.out.println("Error creating tables");
		}

		Bank.bank_set_up(this.connection);
		Bank.set_date("1997", "1", "2", this.connection);
	}

	public void teardown(){
		try{
			this.connection.close();
			System.out.println("Closing connection ...");
			System.out.println("Tests finished running");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	public int test_create_checking_acct(){
		try{
			Account acct = Account.create_account(Testable.AccountType.INTEREST_CHECKING, "1", 1000.00,
										 "111222111", "james", "sample_address", this.connection);
			if(acct == null){
				return fail();
			}

			Customer owner = Customer.get_cust_by_id("111222111", this.connection);
			if(owner == null){
				return fail();
			}

			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();

	}

	public int test_create_pocket_acct(){
		try{
			Account linked = Account.get_account_by_id("1", this.connection);
			double balance = linked.balance;
			Account acct = Account.create_pocket_account("2", "1", 50.00,
												    "111222111", this.connection);
			linked = Account.get_account_by_id("1", this.connection);
			if(linked.balance != balance - 50.00 - 5.00){
				return fail();
			}

			if(acct == null){
				return fail();
			}

			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
		
	}

	public int test_close_acct(){
		try{
			return fail();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_add_cust_to_acct(){
		try{
			return fail();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_pocket_acct_topup(){
		try{
			return fail();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_acct_transfer(){
		try{
			Account to_acct = Account.get_account_by_id("1", this.connection);
			Account other_acct = Account.create_account(Testable.AccountType.SAVINGS, "3", 1500.00,
										 "111222111", "", "", this.connection);
			Account not_owned_acct = Account.create_account(Testable.AccountType.SAVINGS, "4", 6900.00,
										 "222444222", "True Slav", "Mother Russia", this.connection);
			if(other_acct == null || not_owned_acct == null){
				System.err.println("Could not create account 3/4");
				return error();
			}

			double balance_to_acct = to_acct.balance;
			double balance_other_acct = other_acct.balance;
			double balance_not_owned_acct = not_owned_acct.balance;

			Transaction transact = Transaction.transfer(to_acct.a_id, other_acct.a_id, "111222111",
			 Bank.get_date(this.connection), Transaction.TransactionType.TRANSFER, 350, this.connection);
			if(transact == null){
				return fail();
			}
			ArrayList<Transaction> transactions = Transaction.get_acct_transactions_this_month(to_acct.a_id, this.connection);
			if(transactions.size() != 4){
				return fail();
			}

			double new_balance_to_acct = Account.get_account_by_id("1", this.connection).balance;
			double new_balance_other_acct = Account.get_account_by_id("3", this.connection).balance;

			if(new_balance_to_acct != balance_to_acct + 350 || new_balance_other_acct != balance_other_acct - 350){
				return fail();
			}

			transact = Transaction.transfer(not_owned_acct.a_id, other_acct.a_id, "111222111",
			Bank.get_date(this.connection), Transaction.TransactionType.TRANSFER, 350, this.connection);
			if(transact != null){
				return fail();
			}


			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_acct_wire(){
		try{
			Account to_acct = Account.create_account(Testable.AccountType.SAVINGS, this.get_next_id(), 1500.00,
										 "567567567", "Steve Irwin", "Australia", this.connection);
			Account other_acct = Account.create_account(Testable.AccountType.SAVINGS, this.get_next_id(), 1500.00,
										 "111222456", "Eric Cartman", "South Park", this.connection);

			double balance_to_acct = to_acct.balance;
			double balance_other_acct = other_acct.balance;

			Transaction transact = Transaction.wire(to_acct.a_id, other_acct.a_id, "111222456",
			 Bank.get_date(this.connection), Transaction.TransactionType.WIRE, 350, this.connection);
			if(transact == null){
				return fail();
			}

			double new_balance_to_acct = Account.get_account_by_id(to_acct.a_id, this.connection).balance;
			double new_balance_other_acct = Account.get_account_by_id(other_acct.a_id, this.connection).balance;

			if(new_balance_to_acct != balance_to_acct + 350 || new_balance_other_acct != balance_other_acct - 350 - (350 * .02)){
				return fail();
			}

			transact = Transaction.wire(other_acct.a_id, to_acct.a_id, "111222456",
			Bank.get_date(this.connection), Transaction.TransactionType.WIRE, 350, this.connection);
			if(transact != null){
				return fail();
			}


			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_acct_accrue_interest(){
		try{
			// Start date at Nov 1
			Bank.set_date("2019", "11", "1", this.connection);
			Account some_account = Account.create_account(Testable.AccountType.SAVINGS, this.get_next_id(), 1500.00,
										 "595959595", "Guy", "California", this.connection);
			Bank.set_date("2019", "11", "5", this.connection);
			Transaction.withdraw(some_account.a_id, "595959595", Bank.get_date(this.connection), 
						 Transaction.TransactionType.WITHDRAWAL, 500, this.connection);
			// Bank.set_date("2019", "11", "12", this.connection);
			// Transaction.deposit(some_account.a_id, "595959595", Bank.get_date(this.connection), 
			// 			 Transaction.TransactionType.WITHDRAWAL, 2500, this.connection);
			// Bank.set_date("2019", "11", "25", this.connection);
			// Transaction.withdraw(some_account.a_id, "595959595", Bank.get_date(this.connection), 
			// 			 Transaction.TransactionType.WITHDRAWAL, 1200, this.connection);
			// Bank.set_date("2019", "11", "30", this.connection);
			boolean transaction = Transaction.accrue_interest(some_account.a_id, this.connection);

			Account after_interest = Account.get_account_by_id(some_account.a_id, this.connection);
			
			if(!transaction){
				return fail();
			}

			if(!(Math.abs(after_interest.balance - 1004.2666666666667) < 0.001)){
				return fail();
			}

			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_pocket_acct_purchase(){
		try{
			Account pocket = Account.get_account_by_id("2", this.connection);
			double balance = pocket.balance;

			// Make sure this fails
			Transaction tran = Transaction.purchase("2", Bank.get_date(this.connection), 222, 
										"111222111", this.connection);
			if(tran != null){
				return fail();
			}


			pocket = Account.get_account_by_id("2", this.connection);
			if(pocket.balance != balance){
				return fail();
			}

			tran = Transaction.purchase("2", Bank.get_date(this.connection), 25, 
										"111222111", this.connection);

			pocket = Account.get_account_by_id("2", this.connection);
			if(tran == null || pocket.balance != balance - 25.0){
				return fail();
			}
			pocket = Account.get_account_by_id("2", this.connection);

			// Should fail as not a pocket account
			tran = Transaction.purchase("1", Bank.get_date(this.connection), 25, 
										"111222111", this.connection);
			if(tran != null){
				return fail();
			}

			// Test closing account as transfer out remaining 25
			tran = Transaction.purchase("2", Bank.get_date(this.connection), 25, 
										"111222111", this.connection);

			pocket = Account.get_account_by_id("2", this.connection);

			if(tran == null || pocket.is_open){
				return fail();
			}

			// !!!!!Test setting date forward a month and -5 dollars!!!!!
			
			return pass();

		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
		
	}

	public int test_pocket_acct_collect(){
		try{
			// Create two accounts for testing
			Account acct_linked = Account.create_account(Testable.AccountType.SAVINGS, "5", 1000.00,
										 "408466367", "Boi", "Yes", this.connection);
			Account pocket = Account.create_pocket_account("6", "5", 150.00,
												    "408466367", this.connection);
			double acct_linked_balance = Account.get_account_balance("5", this.connection);
			double pocket_balance = Account.get_account_balance("6", this.connection);

			Transaction collect_trans = Transaction.collect(acct_linked.a_id, pocket.a_id, "408466367", 
				Bank.get_date(this.connection), Transaction.TransactionType.COLLECT, 100, this.connection);
			
			double new_acct_linked_balance = Account.get_account_balance("5", this.connection);
			double new_pocket_balance = Account.get_account_balance("6", this.connection);

			if(new_acct_linked_balance != acct_linked_balance + 100 ||
				new_pocket_balance != pocket_balance - 103){
				return fail();
			}

			if(collect_trans == null){
				return fail();
			}

			collect_trans = Transaction.collect( pocket.a_id, acct_linked.a_id, "408466367", 
				Bank.get_date(this.connection), Transaction.TransactionType.COLLECT, 100, this.connection);

			if(collect_trans != null){
				return fail();
			}

			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}

	public int test_pocket_acct_pay_friend(){
		try{
			Account acct_linked = Account.create_account(Testable.AccountType.SAVINGS, "7", 1000.00,
										 "555777666", "Shrek", "Swamp", this.connection);
			Account pocket = Account.create_pocket_account("8", "7", 150.00,
												    "555777666", this.connection);

			Account acct_linked_2 = Account.create_account(Testable.AccountType.SAVINGS, "9", 1000.00,
										 "141516178", "Shrek", "Swamp", this.connection);
			Account pocket_2 = Account.create_pocket_account("10", "9", 150.00,
												    "141516178", this.connection);

			Transaction pay = Transaction.pay_friend(pocket_2.a_id, pocket.a_id, "555777666", Bank.get_date(this.connection), 
						 Transaction.TransactionType.PAY_FRIEND, 100, connection);
			
			if(pay == null){
				return fail();
			}

			double new_pocket_balance = Account.get_account_balance("8", this.connection);
			double new_pocket_2_balance = Account.get_account_balance("10", this.connection);

			if(new_pocket_balance != 50 || new_pocket_2_balance != 250.0){
				return fail();
			}

			Transaction pay_2 = Transaction.pay_friend("7", "9", "141516178", Bank.get_date(this.connection), 
						 Transaction.TransactionType.PAY_FRIEND, 100, connection);

			if(pay_2 != null){
				System.err.println("HERE2");
				return fail();
			}

			return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}
	public int test_overdrawn_transaction(){
		System.out.println("Overdrawn\n");
		try{
			
			Account acct_1= Account.create_account(Testable.AccountType.INTEREST_CHECKING, this.get_next_id(), 1510.00,
										 "696969696", "Michael Jackson", "Wonderland", this.connection);
			String m_id = acct_1.owner_id;
			//Topup = $5 and initial fee $5 = so total in account after creation is $1,500
			Account pocket_1= Account.create_pocket_account("707070707", acct_1.a_id, 5.00,
												    m_id, this.connection);
			
			Account acct_2= Account.create_account(Testable.AccountType.SAVINGS,this.get_next_id(), 2000.00,
										 "969696969" , "Bill Cosby", "Prison", this.connection);
			String cosby_id = acct_2.owner_id;
			Account pocket_2= Account.create_pocket_account("070707070", acct_2.a_id, 5.00,
												    cosby_id, this.connection);
			//Create a second account owned by Michael
			Account acct_3= Account.create_account(Testable.AccountType.SAVINGS,"699669969" , 5000.00,
										 m_id, "Michael Jackson", "Wonderland", this.connection);
			//Withdraw more than the account balance should fail for all transactions
			boolean withdraw= Transaction.withdraw(acct_1.a_id, m_id, Bank.get_date(this.connection), 
						 Transaction.TransactionType.WITHDRAWAL, 1500.01, this.connection);
			if(withdraw){
				System.err.print("Withdraw not null. Balance: "+Double.toString(acct_1.balance)+"\n");
				return fail();
			}

			Transaction transfer= Transaction.transfer(acct_3.a_id, acct_1.a_id, m_id,
			 					  Bank.get_date(this.connection), Transaction.TransactionType.TRANSFER, 1500.01, this.connection);
			if(transfer != null){
				System.err.print("Transfer not null\n");
				return fail();
			}
			Transaction wire = Transaction.wire(acct_2.a_id, acct_1.a_id, m_id,
			 				   Bank.get_date(this.connection), Transaction.TransactionType.WIRE, 1500.01, this.connection);
			if(wire != null){
				System.err.print("Wire not null\n");
				return fail();
			}
			Transaction check = Transaction.write_check(acct_1.a_id, m_id, Bank.get_date(this.connection), 
						 Transaction.TransactionType.WRITE_CHECK, 1500.01, this.connection);
			if(check != null){
				System.err.print("Check not null\n");
				return fail();
			}
			Transaction top_up= Transaction.top_up(pocket_1.a_id, acct_1.a_id, Bank.get_date(this.connection),
								 1500.01, m_id, this.connection);
			if(top_up != null){
				System.err.print("Top up not null\n");
				return fail();
			}

			//Check pocket account transactions are safe guarded
			//Pocket account should still only have the initial $5 since previous transactions shoud fail
			Transaction purchase = Transaction.purchase(pocket_1.a_id, Bank.get_date(this.connection), 5.01, 
										m_id, this.connection);
			if(purchase != null){
				System.err.print("Purchase not null\n");
				return fail();
			}
			Transaction collect = Transaction.collect(acct_1.a_id, pocket_1.a_id, m_id, Bank.get_date(this.connection), 
						 Transaction.TransactionType.COLLECT, 5.01, this.connection);
			if(collect != null){
				System.err.print("Collect not null\n");
				return fail();
			}
			Transaction pay_friend= Transaction.pay_friend(pocket_2.a_id, pocket_1.a_id, m_id, Bank.get_date(this.connection), 
						 Transaction.TransactionType.PAY_FRIEND, 5.01, this.connection);
			if(pay_friend != null){
				System.err.print("Pay Friend not null\n");
				return fail();
			}
		}catch(Exception e){
			e.printStackTrace();
			return error();
		}
		return pass();
	}

	public int test_monthly_statement(){
		try{
			Account acct = Account.create_account(Testable.AccountType.SAVINGS, this.get_next_id(), 1000.00,
										 "408466365", "Person", "Address", this.connection);
			 ArrayList<CustomerMonthlyStatement> mstmt = ManagerOperations.generate_monthly_statement(this.connection);
			 for(int i = 0; i < mstmt.size(); i++){
			 	CustomerMonthlyStatement m = mstmt.get(i);
			 	System.out.println("Customer: " + m.c_id);
			 	for(int j = 0; j < mstmt.get(i).statements.size(); j++){
					AccountStatement a = mstmt.get(i).statements.get(j);
					System.out.println("Account: " + a.a_id);
					System.out.print("Owners: ");
					for(int k = 0; k < a.owners.size(); k++){
						System.out.print(a.owners.get(k) + " ");
					}
					System.out.println();
					System.out.print("Transactions: ");
					for(int k = 0; k < a.transactions.size(); k++){
						System.out.print(a.transactions.get(k).t_id + " ");
					}
					System.out.println();
					String final_balance = String.format("Final Balance: %.2f", a.final_balance);
					String initial_balance = String.format("Initial Balance: %.2f", a.initial_balance);
					System.out.println(initial_balance);
					System.out.println(final_balance);
					if(a.insurance_limit_reached){
						System.out.println("Insurance limit has been reached!");
					}
					System.out.println();
			 	}

			 }
			 return pass();
		}catch(Exception e){
			e.printStackTrace();
		}
		return error();
	}


	public void run_tests(OracleConnection connection){
		// TODO: CHECK THAT ALL TRANSACTIONS CANNOT BE DONE BY A NON-OWNER
		// TODO: WRITE TESTS TO CHECK EVERY ACCOUNT GETS CLOSED ON 0 OR .01 BALANCE
		// TODO: TEST CREATING A POCKET ACCOUNT W/ INIT TOPUP THAT WOULD CLOSE LINKED
		this.setup(connection);

		ArrayList<String> results = new ArrayList<String>();
		results.add(result("test_create_checking_acct():", this.test_create_checking_acct()));
		results.add(result("test_create_pocket_acct():", this.test_create_pocket_acct()));
		results.add(result("test_pocket_acct_purchase():", this.test_pocket_acct_purchase()));
		results.add(result("test_pocket_acct_topup():", this.test_pocket_acct_topup()));
		results.add(result("test_add_cust_to_acct():", this.test_add_cust_to_acct()));
		results.add(result("test_close_acct():", this.test_close_acct()));
		results.add(result("test_acct_transfer():", this.test_acct_transfer()));
		results.add(result("test_pocket_acct_collect():", this.test_pocket_acct_collect()));
		results.add(result("test_pocket_acct_pay_friend():", this.test_pocket_acct_pay_friend()));
		results.add(result("test_acct_wire():", this.test_acct_wire()));
		//results.add(result("test_acct_write_check():", this.test_acct_write_check()));
		results.add(result("test_overdrawn_transaction():",this.test_overdrawn_transaction()));
		results.add(result("test_monthly_statement():", this.test_monthly_statement()));
		results.add(result("test_acct_accrue_interest():", this.test_acct_accrue_interest()));
		//results.add(result("test_testable_app():", this.test_testable_app()));
		//results.add(result("test_sample_data():", this.test_sample_data()));
		results.add(result("test_monthly_statement():", this.test_monthly_statement()));


		System.err.println("\n----- RESULTS -----");
		for(int i = 0; i < results.size(); i++){
			System.err.println(results.get(i));
		}

		this.teardown();
	}

	public void test_app(App app){
		System.out.println("START");

		// WRITE TESTS HERE
		String result; 
		result = app.initializeSystem();
		if(result.equals("1")){
			System.err.println("ERROR INIT");
		}
		result = app.dropTables();
		if(result.equals("1")){
			System.err.println("ERROR DROP");
		}
	
		result = app.createTables();
		if(result.equals("1")){
			System.err.println("ERROR CREATE");
		}

		result = app.setDate(2011, 3, 1);
		if(result.equals("1")){
			System.err.println("ERROR SETDATE");
		}

		result = app. createCheckingSavingsAccount( Testable.AccountType.SAVINGS, "12241",
		 			1250, "111000111", "George Washington", "White House");
		if(!result.equals("0 12241 SAVINGS 1250.00 111000111")){
			System.err.println("ERROR ACCOUNT CREATE 1");
		}

		result = app. createCheckingSavingsAccount( Testable.AccountType.INTEREST_CHECKING, "22241",
		 			5000, "222111222", "Abraham Lincoln", "White House");
		if(!result.equals("0 22241 INTEREST_CHECKING 5000.00 222111222")){
			System.err.println("ERROR ACCOUNT CREATE 2");
		}

		result = app. createCheckingSavingsAccount( Testable.AccountType.STUDENT_CHECKING, "33341",
		 			1010, "333222333", "Thomas Jefferson", "White House");
		if(!result.equals("0 33341 STUDENT_CHECKING 1010.00 333222333")){
			System.err.println("ERROR ACCOUNT CREATE 3");
		}

		result = app. createCheckingSavingsAccount( Testable.AccountType.INTEREST_CHECKING, "55541",
		 			200, "444333444", "Benjamin Franklin", "Paris");
		if(!result.equals("1")){
			System.err.println("ERROR ACCOUNT CREATE 4");
		}

		// Too much money
		result = app.createPocketAccount("66651", "12241", 1249, "111000111");
		if(!result.equals("1")){
			System.err.println("ERROR P_ACCOUNT CREATE 1");
		}

		// Cust doesn't exist
		result = app.createPocketAccount("66651", "12241", 50, "111010111");
		if(!result.equals("1")){
			System.err.println("ERROR P_ACCOUNT CREATE 2");
		}

		// Linked doesn't exist
		result = app.createPocketAccount("66651", "12243", 50, "111000111");
		if(!result.equals("1")){
			System.err.println("ERROR P_ACCOUNT CREATE 3");
		}

		// Should be ok
		result = app.createPocketAccount("66651", "12241", 50, "111000111");
		if(!result.equals("0 66651 POCKET 50.00 111000111")){
			System.err.println("ERROR P_ACCOUNT CREATE 4");
		}

		// Can't recreate same account
		result = app.createPocketAccount("66651", "12241", 50, "111000111");
		if(!result.equals("1")){
			System.err.println("ERROR P_ACCOUNT CREATE 5");
		}

		// Should be ok
		result = app.createPocketAccount("77751", "22241", 100, "222111222");
		if(!result.equals("0 77751 POCKET 100.00 222111222")){
			System.err.println("ERROR P_ACCOUNT CREATE 6");
		}


		// Should succeed
		result = app.createCustomer("33341", "121212121", "James Madison", "Pennsylvania");
		if(!result.equals("0")){
			System.err.println("ERROR CREATECUSTOMER 1");
		}

		// Should fail account doesnt exist
		result = app.createCustomer("33345", "121212121", "James Madison", "Pennsylvania");
		if(!result.equals("1")){
			System.err.println("ERROR CREATECUSTOMER 2");
		}

		// Should fail, customer already exists
		result = app.createCustomer("33341", "121212121", "James Madison", "Pennsylvania");
		if(!result.equals("1")){
			System.err.println("ERROR CREATECUSTOMER 3");
		}		

		result = app.setDate(2011, 3, 5);
		if(!result.equals("0 2011-03-05")){
			System.err.println("ERROR SETDATE 2");
		}

		result = app.deposit("12241", 5);
		if(!result.equals("0 1195.00 1200.00")){
			System.err.println("ERROR DEPOSIT 1");
		}

		// Should fail as deposit not work on pocket acct
		result = app.deposit("66651", 2);
		if(!result.equals("1")){
			System.err.println("ERROR DEPOSIT 2");
		}

		// Should fail as nonexistant account
		result = app.showBalance("66661");
		if(!result.equals("1")){
			System.err.println("ERROR SHOWBALANCE 1");
		}

		// Should succeed
		result = app.showBalance("22241");
		if(!result.equals("0 4895.00")){
			System.err.println("ERROR SHOWBALANCE 2");
		}

		result = app.topUp("66651", 50);
		if(!result.equals("0 1150.00 100.00")){
			System.err.println("ERROR TOPUP 1");
		}

		// Should fail, not enough money
		result = app.topUp("66651", 520000);
		if(!result.equals("1")){
			System.err.println("ERROR TOPUP 3");
		}

		// Nonexistant account, should fail
		result = app.topUp("66661", 50);
		if(!result.equals("1")){
			System.err.println("ERROR TOPUP 2");
		}

		// Should fail, more money than in account
		result = app.payFriend("77751", "66651", 50000);
		if(!result.equals("1")){
			System.err.println("ERROR PAYFRIEND 3");
		}		

		result = app.payFriend("77751", "66651", 100);
		if(!result.equals("0 0.00 200.00")){
			System.err.println("ERROR PAYFRIEND 1");
		}

		// Should fail, pocket is closed
		result = app.payFriend("66651", "77752", 20);
		if(!result.equals("1")){
			System.err.println("ERROR PAYFRIEND 2");
		}

		result = app.topUp("66651", 1150);
		if(!result.equals("0 0.00 1350.00")){
			System.err.println("ERROR TOPUP 3");
		}

		result = app.listClosedAccounts();
		if(!result.equals("1")){
			System.err.println("Should be closed: 66651 77751 12241");
			System.err.println("Closed accounts:" + result);
		}else{
			System.err.println("ERROR LISTCLOSEDACCT");
		}

		app.close_connection();
	}
}