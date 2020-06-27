package cs174a;


// JDBC Imports
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.swing.*;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import javax.swing.text.BadLocationException;



import java.awt.*;
import java.awt.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.*;

import java.text.*;

public class BankTellerInterface extends JPanel{
	
	public static String[] ACCT_TYPES= {"Student Checking", "Interest Checking", "Savings", "Pocket"};
	public enum BankTellerActions{
		
		CREATE_ACCOUNT,
		POCKET_ACCOUNT,
		CHECK_TRANSACTION,
		
		CUSTOMER_REPORT,
		LIST_CLOSED,

		MONTHLY_STATEMENT,
		DTER,

		DELETE_TRANSACTIONS,
		ADD_INTEREST,
		DELETE_CLOSED,


		ACTIONS_PAGE,
		SEARCH,
		SEARCH_CLOSED,
		SEARCH_DTER,
		SEARCH_REPORT,
		SEARCH_MONTHLY_STATEMENT
	}

	private OracleConnection connection;
	private Hashtable<BankTellerActions, JButton> action_buttons;
	private Hashtable<BankTellerActions, JPanel> panels;
	private InputForm form;
	private JPanel current_page;
	private JFrame parent_frame;


	public BankTellerInterface(OracleConnection connection){
		//super(new GridLayout(4, 4));
		this.connection = connection;
		create_pages();
		//Initial Screen
		current_page= panels.get(BankTellerActions.ACTIONS_PAGE);
		add(current_page);
		parent_frame= Interface.main_frame;
	}

	/*Creates all the pages that will be used for the bankTeller interface*/
	private void create_pages(){
		panels= new Hashtable<BankTellerActions, JPanel>();
		//panels.put(CustomerActions.LOG_IN, create_login_page());

		panels.put(BankTellerActions.ACTIONS_PAGE, create_actions_page());
		
		panels.put(BankTellerActions.CREATE_ACCOUNT, create_account_page(true));
		panels.put(BankTellerActions.POCKET_ACCOUNT, create_account_page(false));
		panels.put(BankTellerActions.CHECK_TRANSACTION, create_page(new ArrayList<String> (Arrays.asList("Customer ID: ", " Amount: $","Account ID: " )), "Write Check", BankTellerActions.CHECK_TRANSACTION));	
		
		//No input needed just display info
		panels.put(BankTellerActions.LIST_CLOSED, create_closed_page());
		panels.put(BankTellerActions.DTER, create_dter_page());
		
		//Need input but Input form doesn't cut it
		panels.put(BankTellerActions.MONTHLY_STATEMENT, create_monthly_statement_page());
		panels.put(BankTellerActions.CUSTOMER_REPORT, create_customer_report_page());
		
		
	}
	public void create_acct(){
		//Get JComboBox and find selected Index
		int a_type=0;
		try{
			a_type= ((JComboBox)form.getCustomComponent()).getSelectedIndex();
			//If none selected or error
			if(a_type < 0){
				//Set to default value (Student Checkings)
				a_type= 0;
			}
		}
		catch(Exception e){
			System.err.println(e);
		}

		Testable.AccountType type = Testable.AccountType.values()[a_type];
		String a_id= form.getInput(0);
		String c_id= form.getInput(2);
		String name= form.getInput(3);
		String address= form.getInput(4);
		String initial_balance= form.getInput(5);

		if(!Utilities.valid_id(a_id)){
			form.setLabel("Enter a valid account ID", Color.red);
			return;
		}
		if(!Utilities.valid_id(c_id)){
			form.setLabel("Enter a valid customer ID", Color.red);
			return;
		}
		if(name.equals("")){
			form.setLabel("Enter a valid customer name", Color.red);
			return;
		}
		if(address.equals("")){
			form.setLabel("Enter a valid customer address", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(initial_balance)){
			form.setLabel("Enter a valid deposit amount", Color.red);
			return;
		}
		if(Double.parseDouble(initial_balance) < 1000){
			form.setLabel("Initial deposit too low", Color.red);
			return;
		}
		Account acct = Account.create_account(type, a_id,Double.parseDouble(initial_balance),
										 c_id, name,address, this.connection);
		if(acct == null){
			form.setLabel("Account creation failed", Color.red);
			System.out.println("Account creation failed... ");
		}else{
			form.setLabel("Account created successfully", Color.green);
			System.out.println("Account: " + acct.a_id + " created!");
			update_page(BankTellerActions.ACTIONS_PAGE);
		}
	}


	public void create_pocket_acct(){
		//Get JComboBox and find selected Index
		int a_type=3;
		try{
			a_type= ((JComboBox)form.getCustomComponent()).getSelectedIndex();
			//If none selected or error
			if(a_type < 0){
				//Set to default value (Student Checkings)
				a_type= 3;
			}
		}
		catch(Exception e){
			System.err.println(e);
		}

		Testable.AccountType type = Testable.AccountType.values()[a_type];
		String id= form.getInput(0);
		String linkedId= form.getInput(2);
		String tin= form.getInput(3);
		String initialTopUp= form.getInput(4);

		if(!Utilities.valid_id(id)){
			form.setLabel("Enter a valid account ID", Color.red);
			return;
		}
		if(!Utilities.valid_id(linkedId)){
			form.setLabel("Enter a valid linked account ID", Color.red);
			return;
		}
		if(!Utilities.valid_id(tin)){
			form.setLabel("Enter a valid customer ID", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(initialTopUp)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}
		/*
		if(Double.parseDouble(initial_balance) < 1000){
			form.setLabel("Initial deposit too low", Color.red);
			return;
		}*/

		//Create the pocket account
		if(ManagerOperations.create_pocket_account(id, linkedId,Double.parseDouble(initialTopUp), tin, this.connection)){
			form.setLabel("Account creation failed", Color.red);
			System.err.println("Error: could not create pocket acct");
		}else{
			form.setLabel("Account created successfully", Color.green);
			System.out.println("Successfully created pocket acct!");
		}
	}
	public void check_transaction(){
		String cust_id= form.getInput(0);
		String amount= form.getInput(1);
		String a_id= form.getInput(2);

		if(!Utilities.valid_id(cust_id)){
			form.setLabel("Enter a valid customer ID", Color.red);
			return;
		}
		if(!Utilities.valid_id(a_id)){
			form.setLabel("Enter a valid account ID", Color.red);
			return;
		}
		
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}
		String checkID= ManagerOperations.enter_check_transaction(a_id,cust_id, Double.parseDouble(amount), this.connection);
		if(checkID.equals("-1")){
			form.setLabel("Check transaction failed", Color.red);
			System.out.println("Check transaction failed");
			return;
		}
		else{
			//May have to do something with check num.
			form.setLabel(String.format("	Check ID: %s 	",checkID), Color.green);
			System.out.println("Check transaction success");
			return;
		}

	}
	public void show_closed(){
		ListPage p = (ListPage)panels.get(BankTellerActions.LIST_CLOSED);
		String[] col = {"Account ID"};
		ArrayList< ArrayList<String> > a_list= new ArrayList<ArrayList<String>>();
		ArrayList<String> accounts = Account.get_closed_accounts(this.connection);
		if(accounts == null || accounts.size() == 0){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"No accounts were found");
			System.out.println("No accounts found");
			return;
		}
		for(int i = 0; i < accounts.size(); i++){
			System.out.println("a_id: " + accounts.get(i));
		}
		System.out.println("Successfully got closed accounts!");
		a_list.add(accounts);
		p.createTable(col, a_list);
		//p.removeSearch();
		update_page(BankTellerActions.LIST_CLOSED);



		//form.setLabel("IN PROGRESS", Color.red);
	}
	
	public void dter(){
		ListPage p = (ListPage)panels.get(BankTellerActions.DTER);
		String[] col= {"Customer ID"};
		ArrayList< ArrayList<String> > c_list= new ArrayList<ArrayList<String>>();
		ArrayList<String> customers = ManagerOperations.generate_dter(this.connection);
		String date= Bank.get_date(this.connection);
		String[] s= date.split("-");
		if(Integer.parseInt(s[2]) != (Bank.get_days_in_month(s[1], this.connection))){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"Action can only be performed at the end of the month","Inane warning",JOptionPane.WARNING_MESSAGE);
			return;
		}

		if(customers == null || customers.size() == 0){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"No customers were found");
			System.out.println("No customer found");
			return;
		}
		for(int i = 0; i < customers.size(); i++){
			System.out.println("c_id: " + customers.get(i));
		}
		System.out.println("Successfully got closed accounts!");
		c_list.add(customers);
		p.createTable(col, c_list);
		//p.removeSearch();
		update_page(BankTellerActions.DTER);

	}
	public void cust_report(){
		ListPage page = (ListPage) current_page;
		String cust_id= page.getInput();
		//Needed to prevent making unnecessary queries or multiple tables
		if(cust_id.equals(page.getSearch())){
			return;
		}
		if (!Utilities.valid_id(cust_id)){
			page.setLabel("Invalid customer ID", Color.red);
			return;
		}
		//Update search field
		page.setPrevious(cust_id);
		String [] col={"Account ID", "Status"};
		ArrayList<String> acct_status= ManagerOperations.customer_report(cust_id, this.connection);
		if(acct_status == null || acct_status.size() == 0){
			JOptionPane.showMessageDialog(parent_frame,"No accounts associated with the given customer ID","Inane warning",JOptionPane.WARNING_MESSAGE);
			//page.setLabel("No accounts associated with the given customer ID", Color.red);
			return;
		}
		ArrayList<String> acct = new ArrayList<String>();
		ArrayList<String> status= new ArrayList<String>();
		for(int i=0; i<acct_status.size();i++){

			String a_s= acct_status.get(i);
			//split account and status
			String[] s= a_s.split("/");
			//split account
			String acct_str= (s[0].split(":"))[1];
			//split status
			String status_str= (s[1].split(":"))[1];
		
			acct.add(acct_str);
			status.add(status_str);
		}
		ArrayList<ArrayList<String>> row_elements= new ArrayList<ArrayList<String>>();
		row_elements.add(acct);
		row_elements.add(status);
		
		page.createTable(col, row_elements);
		page.revalidate();
		//form.setLabel("IN PROGRESS", Color.red);

	}
	public void monthly_statement(){
		ListPage page = (ListPage) panels.get(BankTellerActions.MONTHLY_STATEMENT);

		String date= Bank.get_date(this.connection);
		String[] d= date.split("-");
		if(Integer.parseInt(d[2]) != (Bank.get_days_in_month(d[1], this.connection))){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"Action can only be performed at the end of the month","Inane warning",JOptionPane.WARNING_MESSAGE);
			return;
		}

		//Multiple entries in table for a customer if they are the primary owner for multiple accounts
		String [] col={"Owner", "Account", "Owners","Transactions", "Initial Balance", "Final Balance", "Insurance status"};
		ArrayList<CustomerMonthlyStatement> statement= ManagerOperations.generate_monthly_statement(this.connection);
		
		ArrayList<String> customers = new ArrayList<String>(); 
		ArrayList<String> accounts= new ArrayList<String>();

		ArrayList<String> owners= new ArrayList<String>();
		ArrayList<String> trans= new ArrayList<String>();
		//Single strings
		ArrayList<String> final_balance= new ArrayList<String>();
		ArrayList<String> initial_balance= new ArrayList<String>();
		ArrayList<String> insurance_limit= new ArrayList<String>();

		ArrayList<ArrayList<String>> row_elements= new ArrayList<ArrayList<String>>();
		DecimalFormat df = new DecimalFormat("#.###");
		for(int i=0; i < statement.size(); i++){
			ArrayList<AccountStatement> a_info= statement.get(i).statements;
			for(int j=0; j < statement.get(i).statements.size(); j++){
				customers.add(statement.get(i).c_id);
				//Add account id
				accounts.add(a_info.get(j).a_id);

				owners.add(Utilities.format_owners(a_info.get(j).owners));
				trans.add(Utilities.format_transactions(a_info.get(j).transactions));
				initial_balance.add(String.format("%.2f",Math.abs(a_info.get(j).initial_balance)));
				final_balance.add(String.format("%.2f", Math.abs(a_info.get(j).final_balance)));
				//Reverse so it makes more sense in table
				insurance_limit.add(Boolean.toString(!a_info.get(j).insurance_limit_reached));
			}

		}
		row_elements.add(customers);
		row_elements.add(accounts);
		row_elements.add(owners);
		row_elements.add(trans);
		row_elements.add(initial_balance);
		row_elements.add(final_balance);
		row_elements.add(insurance_limit);
		page.createTable(col, row_elements);
		
		page.maximizeTable(this.parent_frame);
		page.minimize(this.parent_frame);
		update_page(BankTellerActions.MONTHLY_STATEMENT);

		//Do command line too for extra security.
		System.out.println("\n\nDate: "+ Bank.get_date(this.connection)+"\n\n");
		for (int i =0; i < statement.size(); i++){
			String s="";
			System.out.println("Primary Owner: "+ statement.get(i).c_id+ " ");
			System.out.println("	Accounts: ");
			ArrayList<AccountStatement> a_info= statement.get(i).statements;
			for (int j=0; j< statement.get(i).statements.size(); j++){
				s+= "		Account: " + a_info.get(j).a_id+ "\n\n";

				s+= "			Owners:\n"+ Utilities.format_owners_cli(a_info.get(j).owners) + "\n";
				s+= "			Transactions:\n"+Utilities.format_transactions_cli(a_info.get(j).transactions)+"\n";
				s+= "			"+ String.format("Initial Balance: %.2f", Math.abs(a_info.get(j).initial_balance)) + "\n\n";
				s+= "			"+ String.format("Final Balance: %.2f", Math.abs(a_info.get(j).final_balance)) + "\n\n";
				s+= "			Insurance Limit Reached: "+ Boolean.toString(a_info.get(j).insurance_limit_reached)+ "\n\n";
			}
			System.out.println(s);
			System.out.println("______________________________________________________________________________________________________________________________");

		}
		if(statement == null || statement.size() == 0){
			JOptionPane.showMessageDialog(parent_frame,"Error: No reports","Inane warning",JOptionPane.WARNING_MESSAGE);
			//page.setLabel("No accounts associated with the given customer ID", Color.red);
			return;
		}

	}
	public void add_interest(){
		String date= Bank.get_date(this.connection);
		String[] s= date.split("-");
		//If the day isn't the last day in the month
		if(Integer.parseInt(s[2]) != (Bank.get_days_in_month(s[1], this.connection))){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"Action can only be performed at the end of the month","Inane warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		int i=JOptionPane.showConfirmDialog(parent_frame, "Sure you want to add interest?");
		
		

        if(i==0){
        	if(ManagerOperations.add_interest(this.connection)){
        		//Show dialog confirming
        		JOptionPane.showMessageDialog(parent_frame,"Interest added successfully");
			}
			else{

				//Show dialog stating failure
				JOptionPane.showMessageDialog(parent_frame,"An error occured","Inane warning",JOptionPane.WARNING_MESSAGE);
				System.out.println("Check transaction success");
				return;
			}
        }
	}
	public void delete_closed(){
		String date= Bank.get_date(this.connection);
		String[] s= date.split("-");
		if(Integer.parseInt(s[2]) != (Bank.get_days_in_month(s[1], this.connection))){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"Action can only be performed at the end of the month","Inane warning",JOptionPane.WARNING_MESSAGE);
			return;
		}

		//Show a dalog to make user confirm action (Make them type it)
		int i=JOptionPane.showConfirmDialog(parent_frame, "Sure you want to delete all closed accounts?");
        
        if(i==0){
        	if(ManagerOperations.delete_closed_acc_and_cust(this.connection)){
        		//Show dialog confirming
        		JOptionPane.showMessageDialog(parent_frame,"Closed accounts deleted successfully");
			}
			else{

				//Show dialog stating failure
				JOptionPane.showMessageDialog(parent_frame,"No closed accounts or an error occured","Inane warning",JOptionPane.WARNING_MESSAGE);
				System.out.println("Failed to delete closed accounts");
				return;
			}
        }
	}
	public void delete_transactions(){
		String date= Bank.get_date(this.connection);
		String[] s= date.split("-");
		if(Integer.parseInt(s[2]) != (Bank.get_days_in_month(s[1], this.connection))){
			//have a dialog saying that no closed accounts were found
			JOptionPane.showMessageDialog(parent_frame,"Action can only be performed at the end of the month","Inane warning",JOptionPane.WARNING_MESSAGE);
			return;
		}


		//Show a dalog to make user confirm action (Make them type it)
		int i=JOptionPane.showConfirmDialog(parent_frame, "Sure you want to delete all transactions?");
        if(i==0){
        	if(ManagerOperations.delete_transactions(this.connection)){
        		//Show dialog confirming
        		JOptionPane.showMessageDialog(parent_frame,"Transactions deleted successfully");
			}
			else{

				//Show dialog stating failure
				JOptionPane.showMessageDialog(parent_frame,"An error occurred","Inane warning",JOptionPane.WARNING_MESSAGE);
				System.out.println("Failed to delete transactions");
				return;
			}
        }
	}

	/* Changes from one page to another*/
	private void update_page(BankTellerActions page){
		this.remove(current_page);
		this.revalidate();
		this.repaint();
		try{
			current_page= panels.get(page);
			//Actions page has no form associated with it
			if(isForm(page)){
				//Clear old values from old form
				if(this.form != null)
					this.form.resetFields();
				//Change form to current page
				this.form= (InputForm) current_page;
				this.form.resetFields();
			}
			else{
				this.form= null;
			}
		}
		catch(Exception e){
			System.err.println(e);
		}
		add(current_page);
	}
	private boolean isForm(BankTellerActions page){
		return (page == BankTellerActions.CREATE_ACCOUNT
			 || page == BankTellerActions.POCKET_ACCOUNT
			 || page == BankTellerActions.CHECK_TRANSACTION);
	}
	/*Creates a page with labels and textfields (depends on size of ArrayList)*/
	private JPanel create_page(ArrayList<String> labels, String b_label, BankTellerActions action){
		
		JButton button= new JButton(b_label);
		button.addMouseListener(new ButtonListener(action));
		InputForm form = new InputForm(labels, button);
		JPanel holder= new JPanel();
		//holder.put(form);
		return form;
	}
	/*Creates a page with labels and textfields (depends on size of ArrayList)*/
	private JPanel create_custom_page(ArrayList<String> labels, String b_label, JComponent component, String c_label, BankTellerActions action){
		
		JButton button= new JButton(b_label);
		button.addMouseListener(new ButtonListener(action));
		InputForm form = new InputForm(labels, button,component,c_label);
		//JPanel holder= new JPanel();
		//holder.put(form);
		return form;
	}

	/* Creates the page with user actions*/
	private JPanel create_actions_page(){
		JPanel holder= new JPanel(new GridLayout(2, 1));
		create_render_buttons();
		ArrayList<BankTellerActions> keys = new ArrayList<BankTellerActions>(action_buttons.keySet());
		Collections.sort(keys);
		for(int i=0; i< keys.size();i++){
			holder.add(action_buttons.get(keys.get(i)));
		}	
		return holder;
	}
	private JPanel create_account_page(boolean regular_acct){
		ItemChangeListener item = new ItemChangeListener();
		
		if(regular_acct){
			JComboBox<String> acctList = new JComboBox<>(BankTellerInterface.ACCT_TYPES);
			acctList.addItemListener(item);
			return create_custom_page(new ArrayList<String> (Arrays.asList(" Account ID: ", "Bank Branch: ", " Customer ID: ", "Customer Name: ", " Customer Address: ", "Initial Balance: $")), "Create Account", acctList,"Account Type",BankTellerActions.CREATE_ACCOUNT );
	
		}
		else{
			JComboBox<String> acctList = new JComboBox<>(BankTellerInterface.ACCT_TYPES);
			acctList.addItemListener(item);
			return create_custom_page(new ArrayList<String> (Arrays.asList(" Account ID: ", "Bank Branch: ", " Linked Account ID: ", "Customer ID: ", " Initial Top Up: $")), "Create Pocket Account", acctList,"Account Type",BankTellerActions.POCKET_ACCOUNT );
		}
	}
	private JPanel create_closed_page(){
		JButton button= new JButton("Search");
		button.addMouseListener( new ListButtonListener(BankTellerActions.SEARCH_CLOSED));
		return (new ListPage(button, "Search by Account ID: "));
	}

	private JPanel create_dter_page(){
		JButton button= new JButton("Search");
		button.addMouseListener(new ListButtonListener(BankTellerActions.SEARCH_DTER));
		return (new ListPage(button, "Search by Customer ID: "));
		
	}
	private JPanel create_customer_report_page(){
		JButton button= new JButton("Search");
		button.addMouseListener( new ListButtonListener(BankTellerActions.SEARCH_REPORT));
		return (new ListPage(button, "Customer ID: "));
	}
	private JPanel create_monthly_statement_page(){
		JButton button= new JButton("Search");
		button.addMouseListener( new ListButtonListener(BankTellerActions.SEARCH_MONTHLY_STATEMENT));
		return (new ListPage(button, "Customer ID: "));
	}

	//These buttons are used to render new pages from the ACTIONS_PAGE.
	//They perform no operations other than rendering a new JPanel.
	private void create_render_buttons(){
		this.action_buttons= new Hashtable< BankTellerActions, JButton>(10);

		JButton t= new JButton("Create Account");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                update_page(BankTellerActions.CREATE_ACCOUNT);
            }
		});
		this.action_buttons.put(BankTellerActions.CREATE_ACCOUNT, t);

		t= new JButton("Write Check");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                update_page(BankTellerActions.CHECK_TRANSACTION);
            }
		});		
		this.action_buttons.put(BankTellerActions.CHECK_TRANSACTION,t);

		t= new JButton("Add Interest");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                add_interest();
            }
		});
		this.action_buttons.put(BankTellerActions.ADD_INTEREST, t);


		t= new JButton("Closed Accounts");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                show_closed();
            }
		});
		this.action_buttons.put(BankTellerActions.LIST_CLOSED, t);
	
		t= new JButton("Customer Report");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                update_page(BankTellerActions.CUSTOMER_REPORT);
            }
		});
		this.action_buttons.put(BankTellerActions.CUSTOMER_REPORT, t);

		t=new JButton("Monthly Statement");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                monthly_statement();
            }
		});
		this.action_buttons.put(BankTellerActions.MONTHLY_STATEMENT, t);
		

		t= new JButton("Delete Closed Accounts");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                delete_closed();
            }
		});
		this.action_buttons.put(BankTellerActions.DELETE_CLOSED, t);

		t= new JButton("Delete Transactions");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
                //update_page(BankTellerActions.DELETE_TRANSACTIONS);
                delete_transactions();
            }
		});
		this.action_buttons.put(BankTellerActions.DELETE_TRANSACTIONS, t);

		t=  new JButton("Generate DTER");
		t.addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) {
				dter();
            }
		});
		this.action_buttons.put(BankTellerActions.DTER, t);

	}
	class ButtonListener extends MouseAdapter{
		private BankTellerInterface.BankTellerActions action;
		public ButtonListener(BankTellerInterface.BankTellerActions action){
			super();
			this.action= action;
		}
		public void mouseClicked(MouseEvent e){
			//If left clicked
			if(SwingUtilities.isLeftMouseButton(e)){

				//Reset label/error message upon button click. Not needed but playing it safe
				
				form.resetLabel();
				switch(this.action){
					case CHECK_TRANSACTION:
						check_transaction();
						break;
					case MONTHLY_STATEMENT:
						monthly_statement();
						break;
					case CREATE_ACCOUNT:
						create_acct();
						break;		
					case POCKET_ACCOUNT:
						create_pocket_acct();
						break;
					case SEARCH:
					 	System.out.println("Searching");
						try{
							ListPage page= (ListPage) panels.get(current_page);
							//page.search();
						}catch(Exception err){
							System.err.println(err);
						}
						break;
				}

			}
			else if(SwingUtilities.isRightMouseButton(e)){
				//Essentially a back key
				update_page(BankTellerActions.ACTIONS_PAGE);
			}
		}
	}
	class ListButtonListener extends MouseAdapter{
		private BankTellerInterface.BankTellerActions action;
		public ListButtonListener(BankTellerInterface.BankTellerActions action){
			super();
			this.action= action;
		}
		public void mouseClicked(MouseEvent e){
			//If left clicked
			if(SwingUtilities.isLeftMouseButton(e)){
				switch(this.action){
					case SEARCH_CLOSED:
						//Table already created
						break;
					case SEARCH_DTER:
						//Table already created
						break;
					case SEARCH_REPORT:
						cust_report();
						break;
					case SEARCH_MONTHLY_STATEMENT:
						break;
				}

			}
			else if(SwingUtilities.isRightMouseButton(e)){
				//Essentially a back key
				update_page(BankTellerActions.ACTIONS_PAGE);
			}
		}
	}
	class ItemChangeListener implements ItemListener{
		private int index=0;
	    @Override
	    public void itemStateChanged(ItemEvent event) {
	       if (event.getStateChange() == ItemEvent.SELECTED) {
	          Object item = event.getItem();
	          int i=index;
	          try{
	          	i=((JComboBox)form.getCustomComponent()).getSelectedIndex();
	          }catch(Exception e){
	          	System.err.println("Could not cast to JComboBox");
	          }
	          if(i==3 && index != i){
	          	update_page(BankTellerActions.POCKET_ACCOUNT);
	          	((JComboBox)form.getCustomComponent()).setSelectedIndex(3);
	          }
	          //Condition wrong
	          else if (index == 3 && i!= 3){
	          	update_page(BankTellerActions.CREATE_ACCOUNT);
	          	((JComboBox)form.getCustomComponent()).setSelectedIndex(i);
	          }
	          index=i;

	       }
	    }       
	}
}