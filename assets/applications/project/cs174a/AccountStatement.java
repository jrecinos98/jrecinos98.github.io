package cs174a;

import java.util.ArrayList;

class AccountStatement {
	public String a_id;
	public ArrayList<String> owners;
	public ArrayList<Transaction> transactions;
	public double final_balance;
	public double initial_balance;
	public boolean insurance_limit_reached;

	public AccountStatement(String a_id, ArrayList<String> owners, ArrayList<Transaction> transactions,
							double final_balance, double initial_balance, boolean insurance_limit_reached){
		this.a_id = a_id;
		this.owners = owners;
		this.transactions = transactions;
		this.final_balance = final_balance;
		this.initial_balance = initial_balance;
		this.insurance_limit_reached = insurance_limit_reached;
	}
}