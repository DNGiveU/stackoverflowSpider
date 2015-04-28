package spider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public abstract class ThreadInfo implements Runnable{
	private int contentType;
	public int getContentType(){
		return contentType;
	}
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


	/**
	 * �����߳��������е���Ϣ
	 */
	public void updateThreadRunningInfo() {
		++threadType ;
		insertToSQL();
	}

	/**
	 * �����߳�����ʧ�ܵ���Ϣ
	 */
	public void updateThreadRunFailInfo(){
		--threadType;
		insertToSQL();
	}

	public static final class ThreadType{
		public static final int UNSAVE = 0;
		public static final int SAVING = 1;
		public static final int SAVED = 2;
		public static final int RESOLVING = 3;
		public static final int RESOLVED = 4;
		public static final int COMBATING = 5;
		public static final int COMBATED = 6;
	}
	
	
	
	public ThreadInfo(long hashCode,String path,int threadType,int contentType,Context context){
		this.hashCode = hashCode;
		
		this.path = path;
		this.threadType = threadType;
		this.contentType = contentType;
		this.context = context;
		
		printThreadInfo("����");
		insertToSQL();
	}



	/**
	 * ��ʽ�������뵽���ݿ��е��ַ�����ʹ�����SQL��ʽ
	 * @param string ��ת����sql
	 */
	private String formatStringToSQL(String string){

		Map<String,String> replaceMap = new HashMap<>();
		replaceMap.put("'","��");
		replaceMap.put("\"","��");
		replaceMap.put("\\","\\\\");
		replaceMap.put("%","�ٷֺ�");
		replaceMap.put("?","��");
		replaceMap.put("_", "�»���");
		Iterator iterator = replaceMap.keySet().iterator();
		String key = null;

		while(iterator.hasNext()){
			key = (String)iterator.next();
			string = string.replace(key, replaceMap.get(key));
		}

		return string;
	}
	protected void printThreadInfo(String extendMsg){
		String tag = null;
		switch(threadType){
			case ThreadType.RESOLVED:
				tag = "�ϲ��߳�";
				break;
			case ThreadType.SAVED:
				tag = "�����߳�";
				break;
			case ThreadType.UNSAVE:
				tag = "�����߳�";
				break;

		}
		System.out.println("HashCodeֵ" + hashCode
				+"\t·��" + path
				+ "\t��������" + (contentType==0?"����":"����")
				+ "\t�߳�����" + tag + "\t" + extendMsg);
	}
	
	protected void printThreadInfoError(String extendsMsg,Exception e){
		printThreadInfo(extendsMsg);
		System.out.println("HashCodeֵ" + hashCode
				+"\t·��" + path
				+ "\t�߳�����" + threadType 
				+ "\t" + e.getCause() + "\t" + e.getMessage());
	}
	
	public void insertToSQL() {
		try {
			Connection connection = context.getDataBase().getConnection();
			Statement stmt = (Statement) connection.createStatement();

			String sqlStr = "INSERT INTO thread (path,threadtype,contenttype,hashcode) VALUES('"
					+ formatStringToSQL(path) +"','" + threadType +"','" + contentType +"','" + hashCode +"')"
					+ " ON DUPLICATE KEY UPDATE path = '"+ formatStringToSQL(this.path) +"', threadtype = "+ this.threadType + ";";

			System.out.println("SQL�������" + sqlStr);
			stmt.execute(sqlStr);
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void updateInfoStr1() {

		try {
			Connection connection = context.getDataBase().getConnection();
			Statement stmt = (Statement) connection.createStatement();
			String sqlStr = "UPDATE thread SET path = '" + formatStringToSQL(path) +"',threadtype = '"+ ++threadType + "' WHERE hashcode= '"+ hashCode + "';";
			stmt.execute(sqlStr);
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �����������е��߳���Ŀ
	 */
	public void updateThreadCount(){
		ThreadList.decreseRunningCount();
	}

	public final static class ContentType {

		public static final int INDEX = 0;
		public static final int CONTENT = 1;
	}
}
