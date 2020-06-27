package cs174a;
import java.util.Scanner;

import javax.swing.JFrame;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;

public class Utilities{
	public static String prompt(String p){
		System.out.println(p);
		Scanner in = new Scanner(System.in);
		String resp = in.nextLine();
		return resp;
	}
	public static void setWindow(JFrame frame){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
 	    int height = screenSize.height;
  	    int width = screenSize.width;
  	    frame.setSize(width/2, height/2);
  	    frame.setLocationRelativeTo(null);
  	    //frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
	}
	public static boolean valid_id(String id){
		if(id.equals("")){
			return false;
		}
		//Add more restrictions if any.
		return true;
	}
	public static boolean valid_pin_format(String pin){
		//If less or greater than 4 chars it is invalid
		if(pin.length()  != 4){
			System.out.println("Length invalid: "+ pin.length());
			return false;
		}
		//Check that all chars are digits
		for (int i=0; i < pin.length();i++){
			if (Character.isLetter(pin.charAt(i))){
				System.out.println("Invalid character: "+ pin.charAt(i));
				return false;
			}
		}
		return true;
	}
	public static boolean valid_rate(String rate){
		if(rate.equals("")){
			return false;
		}
		Double r;
		try{
			r=Double.parseDouble(rate);
		}
		catch(Exception e){
			return false;
		}
		if(r < 0){
			return false;
		}
		if (r > 100){
			return false;
		}
		return true;

	}
	public static boolean valid_money_input(String amount){
		double m;
		//Trim spaces and try to parse
		try{
			m=Double.parseDouble(amount.trim());
		}
		catch(NumberFormatException e){
			System.err.println(e);
			return false;
		}
		//Amount cant be negative
		if(m < 0.01){
			return false;
		}
		return true;
	}
	public static boolean valid_date(String d){
		if(d.equals("")){
			System.out.println("Empty date");
			return false;
		}
		String [] date= d.split("-");
		if(date.length != 3){
			System.out.println("Date: "+date);
			return false;
		}
		int year;
		int month;
		int day;
		try{
			//Parse Int strips leading 0. Error if it parses double
			year = Integer.parseInt(date[2]);
			month = Integer.parseInt(date[0]);
			day = Integer.parseInt(date[1]);
		}
		catch(NumberFormatException e){
			System.err.println(e);
			return false;
		}	
		System.out.println("Year: "+ year);	
		System.out.println("Month: "+ month);
		System.out.println("Day: "+ day);
		//If negative or unreasonably large
		if (year < 0 || year > 9999){
			return false;
		}
		if(month <=0 || month >12){
			return false;
		}
		if(day <=0 || day > 31){
			return false;
		}
		return true;
	}
	public static String format_owners_cli(ArrayList<String> o){
		String owners="";
		for (int i=0; i< o.size(); i++){
			owners+=" 				"+ o.get(i)+"\n";
		}
		return owners;
	}
	public static String format_transactions_cli(ArrayList<Transaction> transactions){
		String transaction="";
		for(int i=0; i < transactions.size(); i++){
			Transaction trans = transactions.get(i);

			transaction+= "				[T_ID: "+Integer.toString(trans.t_id)+"] ";
			if(trans.transaction_type.equals("FTM_FEE") || trans.transaction_type.equals("PCT_FEE")){
				transaction+= "$"+ Double.toString(trans.amount)+ " taken from account "
								+ trans.from_acct + " on " + trans.date;
			}
			else if(trans.transaction_type.equals("ACCRUE_INTEREST")){
				transaction+= "$"+ Double.toString(trans.amount)+ " added to account "
								+ trans.to_acct + " on " + trans.date;
			}
			if(trans.from_acct == null || trans.from_acct.equals("") ){
				transaction+= "$"+ Double.toString(trans.amount)+ " added to account "
								+ trans.to_acct + " on " + trans.date + " by customer: " + trans.cust_id;
			}
			//Witdraw
			else if(trans.to_acct != null && trans.to_acct.equals("") || trans.to_acct ==null){
				transaction+= "$"+ Double.toString(trans.amount)+ " taken from "
								+ trans.to_acct + " on " + trans.date + " by " + trans.cust_id;

			}
			else{
				transaction+= "$" + Double.toString(trans.amount)+ ", account " +trans.from_acct+ " -> account " + trans.to_acct
								+ " on " + trans.date + " by customer: " + trans.cust_id;
			}
			transaction+="\n";						   
		}
		//A big ass string
		return transaction;
	}
	public static String format_owners(ArrayList<String> o){
		String owners="";
		for (int i=0; i< o.size(); i++){
			//owners+=o.get(i)+"\n";
			String[] n_a= o.get(i).split(",");
			String n= (n_a[0].split(":"))[1];
			owners+=n;
			if(i != o.size()-1){
				owners+=", ";
			}
		}
		return owners;
	}
	public static String format_transactions(ArrayList<Transaction> transactions){
		String transaction="";
		for(int i=0; i < transactions.size(); i++){
			Transaction trans = transactions.get(i);
			transaction+= "\n[T_ID: "+Integer.toString(trans.t_id)+"] ";
			transaction+= trans.transaction_type + ": ";
			if(trans.transaction_type.equals("FTM_FEE") || trans.transaction_type.equals("PCT_FEE")){
				transaction+= "$"+ Double.toString(trans.amount)+ " taken from account "
								+ trans.from_acct + " on " + trans.date;

			}
			else if(trans.transaction_type.equals("ACCRUE_INTEREST")){
				transaction+= "$"+ Double.toString(trans.amount)+ " added to account "
								+ trans.to_acct + " on " + trans.date;
			}
			//From account empty means deposit1
			else if(trans.from_acct == null || trans.from_acct.equals("") ){
				transaction+= "$"+ Double.toString(trans.amount)+ " added to account "
								+ trans.to_acct + " on " + trans.date + " by customer: " + trans.cust_id;
			}
			//Witdraw
			else if(trans.to_acct ==null || trans.to_acct.equals("")){
				transaction+= "$"+ Double.toString(trans.amount)+ " taken from account "
								+ trans.from_acct + " on " + trans.date + " by customer: " + trans.cust_id;

			}
			else{
				transaction+= "$" + Double.toString(trans.amount)+ ", account " +trans.from_acct+ " -> account " + trans.to_acct
								+ " on " + trans.date + " by customer: " + trans.cust_id;
			}
			transaction+="\n";						   
		}
		//A big ass string
		return transaction;
	}
	
}