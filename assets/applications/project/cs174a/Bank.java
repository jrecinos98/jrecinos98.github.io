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

public class Bank{
	public static boolean bank_set_up(OracleConnection connection){
		String query = String.format("INSERT INTO bank (b_id, day, month, year, chk_int_intrst, sav_intrst, last_intrst_date) " +
	    							 "VALUES (1, 'xx', 'xx', 'xxxx', 3, 4.8, 'xxxx-xx-xx')");
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

	public static String get_date(OracleConnection connection){
		String date = "";
		String query = String.format("SELECT * FROM bank");
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				if(rs.next()){
					date = String.format("%s-%s-%s",rs.getString("year"),
					        rs.getString("month"), rs.getString("day"));
				}else{
					return "";
				}
			}catch(SQLException e){
				e.printStackTrace();
				return "";
			}
		}catch(SQLException e){
			e.printStackTrace();
			return "";
		}
		return date;
	}

	public static boolean set_date(String year, String month, String day, OracleConnection connection){
		int iyear = Integer.parseInt(year);
		int imonth = Integer.parseInt(month);
		int iday = Integer.parseInt(day);
		String query = String.format("UPDATE bank SET day = '%d', month = '%d', year = '%d'", iday, imonth, iyear);
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

	public static String get_last_interest_date(OracleConnection connection){
		String date = "";
		String query = String.format("SELECT last_intrst_date FROM bank");
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				if(rs.next()){
					date = String.format("%s",rs.getString("last_intrst_date"));
				}else{
					return "";
				}
			}catch(SQLException e){
				e.printStackTrace();
				return "";
			}
		}catch(SQLException e){
			e.printStackTrace();
			return "";
		}
		return date;
	}

	public static boolean set_last_interest_date(String date, OracleConnection connection){
		String query = String.format("UPDATE bank SET last_intrst_date = '%s'", date);
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

	public static String get_month(OracleConnection connection){
		String date = Bank.get_date(connection);
		if(date.equals("")){
			return "";
		}
		String[] day_month_year = date.split("-");
		return day_month_year[1];
	}

	public static String get_day(OracleConnection connection){
		String date = Bank.get_date(connection);
		if(date.equals("")){
			return "";
		}
		String[] day_month_year = date.split("-");
		return day_month_year[2];
	}

	public static String get_year(OracleConnection connection){
		String date = Bank.get_date(connection);
		if(date.equals("")){
			return "";
		}
		String[] day_month_year = date.split("-");
		return day_month_year[0];
	}

	public static boolean set_interest_rate(Testable.AccountType type, double rate, OracleConnection connection){
		if(type == Testable.AccountType.POCKET || type == Testable.AccountType.STUDENT_CHECKING){
			System.err.println("Cannot set interest rate on pocket or student account");
			return false;
		}
		if(rate < 0 || rate > 100){
			System.err.println("Interest rate out of bounds 0 - 100");
			return false;
		}
		String update_type = "";
		if(type == Testable.AccountType.INTEREST_CHECKING){
			update_type = "chk_int_intrst";
		}else{
			update_type = "sav_intrst";
		}
		String query = String.format("UPDATE bank SET %s = %f", update_type, rate);
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


	public static double get_interest_rate(Testable.AccountType type, OracleConnection connection){
		double interest_checking, savings;
		String query = String.format("SELECT * FROM bank");
		try( Statement statement = connection.createStatement() ) {
			try( ResultSet rs = statement.executeQuery( query )){
				if(rs.next()){
					interest_checking = rs.getDouble("chk_int_intrst");
					savings = rs.getDouble("sav_intrst");
				}else{
					return -1;
				}
			}catch(SQLException e){
				e.printStackTrace();
				return -1;
			}
		}catch(SQLException e){
			e.printStackTrace();
			return -1;
		}
		if(type == Testable.AccountType.POCKET || type == Testable.AccountType.STUDENT_CHECKING){
			return 0.0;
		}else if(type == Testable.AccountType.INTEREST_CHECKING){
			return interest_checking;
		}else{
			return savings;
		}
	}

	public static int get_days_in_month(String month, OracleConnection connection){
		int year = Integer.parseInt(Bank.get_year(connection));
		boolean leap_year = false;
		if (year % 4 != 0) {
		    leap_year = false;
		} else if (year % 400 == 0) {
		    leap_year = true;
		} else if (year % 100 == 0) {
		    leap_year = false;
		} else {
		    leap_year = true;
		}


		if(month.equals("1") || month.equals("01")){
			return 31;
		}else if(month.equals("2") || month.equals("02")){
			if(leap_year == true){
				return 29;
			}else{
				return 28;
			}
		}else if(month.equals("3") || month.equals("03")){
			return 31;
		}else if(month.equals("4") || month.equals("04")){
			return 30;
		}else if(month.equals("5") || month.equals("05")){
			return 31;
		}else if(month.equals("6") || month.equals("06")){
			return 30;
		}else if(month.equals("7") || month.equals("07")){
			return 31;
		}else if(month.equals("8") || month.equals("08")){
			return 31;
		}else if(month.equals("9") || month.equals("09")){
			return 30;
		}else if(month.equals("10")){
			return 31;
		}else if(month.equals("11")){
			return 30;
		}else if(month.equals("12")){
			return 31;
		}
		return -1;
	}

	public static int get_days_in_current_month(OracleConnection connection){
		return Bank.get_days_in_month(Bank.get_month(connection), connection);
	}

	public static String pretty_month(String month){
		if(month.length() == 1){
			return "0" + month;
		}else{
			return month;
		}
	}

	public static String pretty_day(String day){
		if(day.length() == 1){
			return "0" + day;
		}else{
			return day;
		}
	}
}