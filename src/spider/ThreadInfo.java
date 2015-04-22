package spider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ThreadInfo implements Runnable{
	//�߳��Ƿ�����
	protected boolean isRunned = true;
	//hashCode
	protected long hashCode = -1L;
	//�߳�����
	protected int threadType = 0;
	//path
	protected String path = null;
	//������
	protected Context context = null;

	public static final class ThreadType{
		public static final int UNSAVE = 0;
		public static final int SAVED = 1;
		public static final int UNSAVE_NEXT = 4;
		public static final int SAVED_NEXT = 5;
		public static final int RESOLVED = 2;
		public static final int COMBATED = 3;
	}
	
	
	
	public ThreadInfo(long hashCode,String path,int threadType,Context context){
		this.hashCode = hashCode;
		
		this.path = path;
		this.threadType = threadType;
		this.context = context;
		
		printThreadInfo("����");
		insertToSQL();
	}
	
	protected void printThreadInfo(String extendMsg){
		System.out.println("HashCodeֵ" + hashCode
				+"\t·��" + path
				+ "\t�߳�����" + threadType + "\t" + extendMsg);
	}
	
	protected void printThreadInfoError(String extendsMsg,Exception e){
		printThreadInfo(extendsMsg);
		System.out.println("HashCodeֵ" + hashCode
				+"\t·��" + path
				+ "\t�߳�����" + threadType 
				+ "\t" + e.getCause() + "\t" + e.getMessage());
	}
	
	private void insertToSQL() {
		try {
			Connection connection = context.getDataBase().getConnection();
			Statement stmt = (Statement) connection.createStatement();
			String sqlStr = "INSERT INTO thread (path,threadtype,hashcode) VALUES('" + path +"','" + threadType +"','" + hashCode +"')";
			stmt.execute(sqlStr);
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void updateInfoStr() {
		printThreadInfo("����");
		
		try {
			Connection connection = context.getDataBase().getConnection();
			Statement stmt = (Statement) connection.createStatement();
			String sqlStr = "UPDATE thread SET path = '" + path +"',threadtype = '"+ ++threadType + "' WHERE hashcode= '"+ hashCode + "';";
			stmt.execute(sqlStr);
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
