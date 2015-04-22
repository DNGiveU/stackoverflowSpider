package spider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ThreadList{
	
	//�߳���
	private static int runningCount = 0;
	//�߳�����
	private static Object runningCountLock = new Object();
	
	/**
	 * ����߳���
	 * @return
	 */
	public static int getRunningCount() {
		synchronized(runningCountLock){
			return runningCount;
		}
	}
	
	/**
	 * �߳�������
	 */
	public static void increseRunningCount() {
		synchronized(runningCountLock){
			runningCount ++;
		}
	}
	
	/**
	 * �߳�������
	 */
	public static void decreseRunningCount() {
		synchronized(runningCountLock){
			runningCount --;
		}
	}
	
	//List����
	List<ThreadInfo> list = null;
	//�߳���
	private Object listLock = new Object();
	
	Context context = null;
	public ThreadList(Context context){
		synchronized (listLock) {
			list = new ArrayList<ThreadInfo>();
		}
		this.context = context;
	}
	
	public void addThread(ThreadInfo threadInfo){
		synchronized(listLock){
			list.add(threadInfo);
		}
		System.out.println("����" + this.toString() 
				+ "���" + "\t����hashcode:" + threadInfo.hashCode + 
				"\t·����" + threadInfo.path);
	}

	/**
	 * ���Thread����
	 */
	public void clear(){
		synchronized (listLock) {
			System.out.println(list.size());
			list.clear();
		}
	}

	/**
	 * ��ȡ�߳�
	 */
	public ThreadInfo getThread(int threadType) {
	
		synchronized (listLock) {
			ThreadInfo threadInfo = null;
			if (list.size() <= 0) {
				initContent(threadType, 20);
			}
			increseRunningCount();
			if (list.size() > 0) {
				threadInfo =  list.get(0);
				list.remove(0);
				System.out.println("ListType\t" + threadType + "\tList ��С\t" + list.size());
			}
			return threadInfo;
		}
	}

	public void initContent(int threadType,int size) {
		//�����ݿ��г�ʼ���߳���Ϣ
		try {
			Connection connection = context.getDataBase().getConnection();
			Statement stmt = (Statement) connection.createStatement();
			String sqlStr = 
					"SELECT path,threadtype,hashcode FROM thread WHERE threadtype = '" + threadType +"' LIMIT " + size;
			ResultSet rs = stmt.executeQuery(sqlStr);
			if(rs.wasNull() || rs.first() == false){
				System.out.println("ThreadType" + threadType + "Ϊ��");
			}
			
			while(rs.next()){
				ThreadInfo threadInfo = null;
				switch(threadType){
				case ThreadInfo.ThreadType.UNSAVE:
					threadInfo = new SaveThread(rs.getLong("hashcode"),
							rs.getString("path"),
							rs.getInt("threadtype"),
							context);
					context.getSaveList().addThread(threadInfo);
					break;
				case ThreadInfo.ThreadType.SAVED:
					threadInfo = new ResolveThread(rs.getLong("hashcode"),
							rs.getString("path"),
							rs.getInt("threadtype"),
							context);
					context.getResolveList().addThread(threadInfo);
					break;
				case ThreadInfo.ThreadType.COMBATED:
					threadInfo = new CombateThread(rs.getLong("hashcode"),
							rs.getString("path"),
							rs.getInt("threadtype"),
							context);
					context.getCombateList().addThread(threadInfo);
					break;
				}
			}
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
