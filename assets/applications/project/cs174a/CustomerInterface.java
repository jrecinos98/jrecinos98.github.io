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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;


public class CustomerInterface extends JPanel{

	public enum CustomerActions {
		LOG_IN,
		UPDATE_PIN,
		LOG_OUT,
		ACTIONS_PAGE,

		DEPOSIT,
		TOP_UP,
		WITHDRAWAL,
		PURCHASE,

		TRANSFER,
		COLLECT,
		WIRE,
		PAY_FRIEND,
		
	}



	private OracleConnection connection;
	private String user_id;

	private ArrayList<JButton> action_buttons;
	private Hashtable<CustomerActions, JPanel> panels;
	private InputForm form;
	private JPanel current_page;
	private JFrame parent_frame;
	

	public CustomerInterface(OracleConnection connection) {
		super(new GridLayout(3, 1));
		this.connection = connection;
		create_pages();
		//Initial Screen
		current_page= panels.get(CustomerActions.LOG_IN);
		this.form= (InputForm) current_page;
		add(current_page);
		parent_frame= Interface.main_frame;
	}

	/*Creates all the pages that will be used for the customer interface*/
	private void create_pages(){
		panels= new Hashtable<CustomerActions, JPanel>();
		panels.put(CustomerActions.LOG_IN, create_login_page());

		panels.put(CustomerActions.ACTIONS_PAGE, create_actions_page());
		panels.put(CustomerActions.UPDATE_PIN, create_page(new ArrayList<String> (Arrays.asList("Old PIN:"," New PIN:")), "Update PIN", CustomerActions.UPDATE_PIN));
		
		panels.put(CustomerActions.DEPOSIT, create_page(new ArrayList<String> (Arrays.asList("Account ID: ", " Amount: $")), "Make Deposit", CustomerActions.DEPOSIT));
		panels.put(CustomerActions.TOP_UP, create_page(new ArrayList<String> (Arrays.asList("Pocket Account ID: ", " Amount: $","Linked Account ID: " )), "Transfer", CustomerActions.TOP_UP));
		panels.put(CustomerActions.WITHDRAWAL, create_page(new ArrayList<String> (Arrays.asList("Account ID: "," Amount: $")), "Make Withdrawal", CustomerActions.WITHDRAWAL ));
		panels.put(CustomerActions.PURCHASE, create_page(new ArrayList<String> (Arrays.asList("Account ID: "," Amount: $")), "Make Purchase", CustomerActions.PURCHASE ));
		panels.put(CustomerActions.TRANSFER, create_page(new ArrayList<String> (Arrays.asList("Sending Account ID: ", " Amount: $","Receiving Account: ")), "Transfer", CustomerActions.TRANSFER ));
		panels.put(CustomerActions.PAY_FRIEND, create_page(new ArrayList<String> (Arrays.asList("Sending Pocket Account ID: "," Amount: $", "Friend's Pocket Account ID: ")), "Pay Friend", CustomerActions.PAY_FRIEND ));

		//Have Fees
		panels.put(CustomerActions.COLLECT, create_page(new ArrayList<String> (Arrays.asList("Pocket Account ID: ", " Amount: $","Linked Account ID: ")), "Transfer From Pocket", CustomerActions.COLLECT ));
		panels.put(CustomerActions.WIRE, create_page(new ArrayList<String> (Arrays.asList("Sending Account ID: ", " Amount: $", "Receiving Account ID: ")), "Wire Money", CustomerActions.WIRE ));
		
		
	}
	
	public void login(){
		String id = form.getInput(0);
		String pin= form.getInput(1);

		System.out.println("Customer ID: " + id);
		System.out.println("Customer PIN: " + pin);

		if(!Utilities.valid_id(id)){
			form.setLabel("Incorrect ID/PIN", Color.red);
			return;
		}
		if(!Utilities.valid_pin_format(pin)){
			form.setLabel("Invalid PIN format", Color.red);
			return;
		}
		Customer cust = Customer.login(id, pin, this.connection);
		if(cust == null){
			form.setLabel("Verification Failed.", Color.red);
			System.out.println("Verification failed... Are your id/PIN correct?");
			
		}else{
			this.user_id= id;
			System.out.println("User: " + cust.name + " logged in!");
			update_page(CustomerActions.ACTIONS_PAGE);
	
		}
	}

	public void change_pin(){	

		//String id= form.getInput(0);
		String old= form.getInput(0);
		String _new= form.getInput(1);

		System.out.println("Customer ID: " + this.user_id);
		
		System.out.println("Old PIN: " + old);

		System.out.println("New PIN: " + _new);
		//Check that pin format is valid 
		if (!Utilities.valid_pin_format(old)){
			form.setLabel("Invalid old PIN", Color.red);
			return;
		}
		//Check that pin format is valid 
		if (!Utilities.valid_pin_format(_new)){
			form.setLabel("Invalid new PIN", Color.red);
			return;
		}
		if(old.equals(_new)){
			form.setLabel("New PIN cannot be old PIN", Color.red);
			return;
		}

		if(Customer.update_pin(this.user_id, old, _new, this.connection)){
			System.out.println("PIN updated!");
			Customer cust = Customer.get_cust_by_id(this.user_id, this.connection);
			if(cust != null){
				//Pop up saying that operation was a success.
				form.setLabel("Successfully updated PIN", Color.red);	
				System.out.println("Successfully set pin to " + cust.encrypted_pin);
				update_page(CustomerActions.ACTIONS_PAGE);
				return;
			}
		}
		else{
			form.setLabel("Operation Failed. Check old pin is correct.", Color.red);
			System.out.println("Failed to set pin");
					
		}
	}

	public void deposit(){
		
		String to_acct = form.getInput(0);
		String amount= form.getInput(1);
		//If time permits obtain all accounts for the user and have a drop down menu
		if(!Utilities.valid_id(to_acct)){
			form.setLabel("Enter a valid account", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}
		System.out.println("Account: "+ to_acct);
		System.out.println("Amount: " + amount);

		String date = Bank.get_date(connection);
		Transaction.TransactionType type = Transaction.TransactionType.DEPOSIT;
		
		boolean success = Transaction.deposit(to_acct, this.user_id, date, type, Double.parseDouble(amount), connection);
		if(!success){
			form.setLabel("Deposit failed", Color.red);
			System.err.println("Deposit failed");
		}else{
			form.setLabel("Deposit successful", Color.green);
			System.out.println("Deposit success!");
			update_page(CustomerActions.ACTIONS_PAGE);

		}
	}

	public void top_up(){

		String to_acct = form.getInput(0);
		String amount= form.getInput(1);
		String from_acct= form.getInput(2);	
		if(!Utilities.valid_id(to_acct)){
			form.setLabel("Enter a valid pocket account", Color.red);
			return;
		}
		if(!Utilities.valid_id(from_acct)){
			form.setLabel("Enter a valid account", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}

		System.out.println("Pocket Account: "+ to_acct);
		System.out.println("Linked Account: "+ from_acct);
		System.out.println("Amount: " + amount);
		
		String date = Bank.get_date(connection);
		Transaction.TransactionType type = Transaction.TransactionType.TOP_UP;
		Transaction transaction = Transaction.top_up(to_acct, from_acct, date,  Double.parseDouble(amount), this.user_id, connection);
		if(transaction == null){
			form.setLabel("Transaction failed", Color.red);
			System.err.println("Top-Up failure");
		}else{
			form.setLabel("Transaction Successful", Color.green);
			System.out.println("Top-Up success!");
			update_page(CustomerActions.ACTIONS_PAGE);
		}
	}

	public void withdrawal(){
		String from_acct = form.getInput(0);
		String amount= form.getInput(1);

		if(!Utilities.valid_id(from_acct)){
			form.setLabel("Enter a valid account", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}

		System.out.println("Account: "+ from_acct);
		System.out.println("Amount: "+amount);
	
		String date = Bank.get_date(connection);
		Transaction.TransactionType type = Transaction.TransactionType.WITHDRAWAL;

		boolean success = Transaction.withdraw(from_acct, this.user_id, date, type, Double.parseDouble(amount), connection);
		if(!success){
			form.setLabel("Transaction Failed", Color.red);
			System.err.println("Withdrawal failed");
		}else{
			form.setLabel("Transaction Successful", Color.green);
			System.out.println("Withdrawal success!");
			update_page(CustomerActions.ACTIONS_PAGE);
		}
	}
	public void purchase(){
		String from_acct= form.getInput(0);
		String amount= form.getInput(1);
		if(!Utilities.valid_id(from_acct)){
			form.setLabel("Enter a valid account", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}
		
		String date = Bank.get_date(connection);
		Transaction.TransactionType type = Transaction.TransactionType.PURCHASE;

		boolean success = Transaction.withdraw(from_acct, this.user_id, date, type, Double.parseDouble(amount), connection);
		Transaction trans= Transaction.purchase(from_acct, date, Double.parseDouble(amount),this.user_id,connection);
		if(trans == null){
			form.setLabel("Purchase Failed", Color.red);
			System.err.println("Purchase failed");
		}else{
			form.setLabel("Purchase Successful", Color.green);
			System.out.println("Purchase success!");
			update_page(CustomerActions.ACTIONS_PAGE);
		}
		

	}
	public void transfer(){
		String from_acct= form.getInput(0);
		String amount= form.getInput(1);
		String to_acct= form.getInput(2);
		if(!Utilities.valid_id(from_acct)){
			form.setLabel("Enter a valid sending account", Color.red);
			return;
		}
		if(!Utilities.valid_id(to_acct)){
			form.setLabel("Enter a valid receiving account", Color.red);
			return;
		}
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}
		
		String date = Bank.get_date(connection);
		Transaction.TransactionType type = Transaction.TransactionType.TRANSFER;
		
		Transaction trans= Transaction.transfer(to_acct, from_acct, this.user_id, date, 
						 						type, Double.parseDouble(amount), this.connection);
		if(trans == null){
			form.setLabel("Transfer Failed", Color.red);
			System.err.println("Transfer failed");
		}else{
			form.setLabel("Transfer Successful", Color.green);
			System.out.println("Transfer success!");
			update_page(CustomerActions.ACTIONS_PAGE);
		}

	}
	public void collect(){
		//Show a dalog to make user confirm action (Make them type it)
		int i=JOptionPane.showConfirmDialog(parent_frame, "There is a 3% fee associated with this transaction. Want to proceed?");
        
        if(i==0){
			String from_pocket= form.getInput(0);
			String amount= form.getInput(1);
			String to_link= form.getInput(2);
			if(!Utilities.valid_id(from_pocket)){
				form.setLabel("Invalid sending pocket account", Color.red);
				return;
			}
			if(!Utilities.valid_money_input(amount)){
				form.setLabel("Enter a valid amount", Color.red);
				return;
			}
			if(!Utilities.valid_id(to_link)){
				form.setLabel("Invalid receiving pocket account", Color.red);
				return;
			}
		
			
			String date = Bank.get_date(connection);
			Transaction.TransactionType type = Transaction.TransactionType.PURCHASE;
			Transaction trans= Transaction.collect(to_link, from_pocket, this.user_id, date, 
							 						type, Double.parseDouble(amount),this.connection);
			if(trans == null){
				form.setLabel("Transfer Failed", Color.red);
				System.err.println("Transfer failed");
			}else{
				form.setLabel("Transfer Successful", Color.green);
				System.out.println("Transfer success!");
				update_page(CustomerActions.ACTIONS_PAGE);
			}
		}


	}
	public void wire(){
		//Show a dalog to make user confirm action (Make them type it)
		int i=JOptionPane.showConfirmDialog(parent_frame, "There is a 2% fee associated with this transaction. Want to proceed?");
        
        if(i==0){
			String from_acct= form.getInput(0);
			String amount= form.getInput(1);
			String to_acct= form.getInput(2);

			if(!Utilities.valid_id(from_acct)){
				form.setLabel("Enter a valid sending account", Color.red);
				return;
			}
			if(!Utilities.valid_id(to_acct)){
				form.setLabel("Enter a valid receiving account", Color.red);
				return;
			}
			if(!Utilities.valid_money_input(amount)){
				form.setLabel("Enter a valid amount", Color.red);
				return;
			}
			
			String date = Bank.get_date(connection);
			Transaction.TransactionType type = Transaction.TransactionType.PURCHASE;
			Transaction trans= Transaction.wire(to_acct, from_acct, this.user_id, date, 
							 					type, Double.parseDouble(amount), this.connection);
			if(trans == null){
				form.setLabel("Wire Transaction Failed", Color.red);
				System.err.println("Wire failed");
			}else{
				form.setLabel("Wire Transaction Successful", Color.green);
				System.out.println("Wire success!");
				update_page(CustomerActions.ACTIONS_PAGE);
			}
		}

	}
	public void pay_friend(){
		String from_acct= form.getInput(0);
		String amount= form.getInput(1);
		String to_acct= form.getInput(2);
		if(!Utilities.valid_id(from_acct)){
			form.setLabel("Enter a valid sending account", Color.red);
			return;
		}
		
		if(!Utilities.valid_money_input(amount)){
			form.setLabel("Enter a valid amount", Color.red);
			return;
		}
		if(!Utilities.valid_id(to_acct)){
			form.setLabel("Enter a valid receiving account", Color.red);
			return;
		}
		
		String date = Bank.get_date(connection);
		Transaction.TransactionType type = Transaction.TransactionType.PURCHASE;
		Transaction trans= Transaction.pay_friend(to_acct,from_acct, this.user_id, date, 
						 						  type, Double.parseDouble(amount),this.connection);
		if(trans == null){
			form.setLabel("Transaction Failed", Color.red);
			System.err.println("Pay Friend failed");
		}else{
			form.setLabel("Transaction Successful", Color.green);
			System.out.println("Pay Friend success!");
			update_page(CustomerActions.ACTIONS_PAGE);
		}


	}

	


	/*Creates the login page*/
	private JPanel create_login_page(){
		
		JButton button= new JButton("Submit PIN");
        button.addMouseListener(new ButtonListener(CustomerActions.LOG_IN));
        ArrayList<String> labels= new ArrayList<String> (
        						Arrays.asList("Customer ID:", "PIN"));
        InputForm form = new InputForm(labels, button);
        return form;
	}

	/*Creates a page with labels and textfields (depends on size of ArrayList)*/
	private JPanel create_page(ArrayList<String> labels, String b_label, CustomerActions action){
		JButton button= new JButton(b_label);
		button.addMouseListener(new ButtonListener(action));
		InputForm form = new InputForm(labels, button);
		JPanel holder= new JPanel();
		//holder.add(form);
		return form;
	}
	/* Creates the page with user actions*/
	private JPanel create_actions_page(){
		JPanel holder= new JPanel(new GridLayout(2, 1));
		create_render_buttons();
		for(int i=0; i< action_buttons.size();i++){
			holder.add(action_buttons.get(i));
		}	
		return holder;
	}
	/* Changes from one page to another*/
	private void update_page(CustomerActions page){
		this.remove(current_page);
		this.revalidate();
		this.repaint();
		try{
			current_page= panels.get(page);
			//Actions page has no form associated with it
			if(page != CustomerActions.ACTIONS_PAGE){
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
	/*resets user info and loads sign in page*/
	private void sign_out(){
		this.user_id=null;
	 	update_page(CustomerActions.LOG_IN);
	}
	//These buttons are used to render new pages from the ACTIONS_PAGE.
	//They perform no operations other than rendering a new JPanel.
	private void create_render_buttons(){
		this.action_buttons= new ArrayList<JButton>(10);
		
		JButton t= new JButton("Deposit");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.DEPOSIT);
            }
		});		
		this.action_buttons.add(t);

		t=new JButton("Withdrawal");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.WITHDRAWAL);
            }
		});
		this.action_buttons.add(t);

		t= new JButton("Purchase");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.PURCHASE);
            }
		});
		this.action_buttons.add( t);

		t=  new JButton("Top Up");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.TOP_UP);
            }
		});
		this.action_buttons.add(t);

		t= new JButton("Transfer");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.TRANSFER);
            }
		});
		this.action_buttons.add( t);

		t= new JButton("Collect");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.COLLECT);
            }
		});
		this.action_buttons.add( t);

		t= new JButton("Wire");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.WIRE);
            }
		});
		this.action_buttons.add(t);

		t= new JButton("Pay Friend");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.PAY_FRIEND);
            }
		});
		this.action_buttons.add( t);

		t= new JButton("Update PIN");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                update_page(CustomerActions.UPDATE_PIN);
            }
		});
		this.action_buttons.add(t);

		t= new JButton("Sign Out");
		t.addMouseListener(new MouseAdapter() { 
			private int action;
			public void mouseClicked(MouseEvent e) {
                sign_out();
            }
		});
		this.action_buttons.add(t);

	}

	class ButtonListener extends MouseAdapter{
		private CustomerInterface.CustomerActions action;
		public ButtonListener(CustomerInterface.CustomerActions action){
			super();
			this.action= action;
		}
		public void mouseClicked(MouseEvent e){
			//If left clicked
			if(SwingUtilities.isLeftMouseButton(e)){

				//Reset label/error message upon button click. Not needed but playing it safe
				form.resetLabel();

				switch(this.action){
					case LOG_IN:
						login();
						break;
					case UPDATE_PIN:
						change_pin();
						break;
					case DEPOSIT:
						deposit();
						break;
					case TOP_UP:
						top_up();
						break;
					case WITHDRAWAL:
						withdrawal();
						break;
					case PURCHASE:
						purchase();
						break;
					case TRANSFER:
						transfer();
						break;	
					case COLLECT:
						collect();
						break;	
					case WIRE:
						wire();
						break;
					case PAY_FRIEND:
						pay_friend();
						break;		
					case LOG_OUT:
						sign_out();
						break;	

				}

			}
			else if(SwingUtilities.isRightMouseButton(e)){
				//Essentially a back key
				update_page(CustomerActions.ACTIONS_PAGE);
			}
		}
	}

}