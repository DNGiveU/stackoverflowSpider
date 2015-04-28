package spider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Context extends Thread {

	private String tag = null;

	public void setTag(String string) {
		tag = string;
	}

	public String getTag() {
		return tag;
	}

	//�ؼ���
	private String keyWord = null;

	public String getKeyWord() {
		return keyWord;
	}

	public void setKeyWord(String keyWord) {
		this.keyWord = keyWord;
	}

	//����ļ�·��
	private String outputFilePath = null;

	public String getOutputFilePath() {
		return outputFilePath;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	//����߳���
	private int maxThreadNumber = 0;

	public void setMaxThreadNumber(int value) {
		this.maxThreadNumber = value;
	}

	public int getMaxThreadNumber() {
		return maxThreadNumber;
	}

	//���ݿ����ӳس�ʼ��������
	private int dataBaseInitialSize = 10;

	public int getDataBaseInitialSize() {
		return dataBaseInitialSize;
	}

	public void setDataBaseInitialSize(int dataBaseInitialSize) {
		this.dataBaseInitialSize = dataBaseInitialSize;
	}

	//���ݿ����ӳ����������
	private int dataBaseMaxConnectionSize = 50;

	public int getDataBaseMaxConnectionSize() {
		return dataBaseMaxConnectionSize;
	}

	public void setDataBaseMaxConnectionSize(int dataBaseMaxConnectionSize) {
		this.dataBaseMaxConnectionSize = dataBaseMaxConnectionSize;
	}

	private BasicDataSource ds = null;

	/**
	 * ��ʼ�����ݿ����ӳ�
	 */
	private void initDataBaseConnectionPool() {
		ds = new BasicDataSource();
		ds.setDriverClassName(Constant.dataBaseDriverClassName);
		ds.setUrl(Constant.dataBaseUrl);
		ds.setUsername(Constant.dataBaseUserName);
		ds.setPassword(Constant.dataBasePassWord);
		ds.setInitialSize(10);
		ds.setMaxIdle(dataBaseMaxConnectionSize);
		ds.setMinIdle(20);
	}

	public synchronized BasicDataSource getDataBase() {
		return ds;
	}

	//HttpClient����
	private CloseableHttpClient httpClient = null;

	public HttpClient getHttpClient() {
		return httpClient;
	}

	//�̳߳ض���
	private ExecutorService threadPool = null;

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	//��������
	private ThreadList resolveList = null;
	//�������
	private ThreadList saveList = null;
	//�ϲ�����
	private ThreadList combateList = null;

	public synchronized ThreadList getResolveList() {
		return resolveList;
	}

	public synchronized ThreadList getSaveList() {
		return saveList;
	}

	public synchronized ThreadList getCombateList() {
		return combateList;
	}

	//����
	private int nextIndex = 0;

	public synchronized void increaseNextIndex() {
		nextIndex++;
	}

	private boolean runFlag = true;
	//��ʼ����
	private boolean isRunned = false;

	public void onStartRunning() {
		threadPool = Executors.newCachedThreadPool();
		saveList = new ThreadList(this, ThreadList.ListType.SAVE_LIST);
		resolveList = new ThreadList(this, ThreadList.ListType.RESOLVE_LIST);
		combateList = new ThreadList(this, ThreadList.ListType.COMBATE_LIST);
		//�����Ƿ����й���ʼ������
		if (isRunned) {
			initListFromSQL();
		} else {
			String url = "http://stackoverflow.com/questions/tagged/" + keyWord
					+ "?sort=" + tag + "&pagesize=50";
			new SaveThread(url.hashCode(),
					url,
					ThreadInfo.ThreadType.UNSAVE,
					ThreadInfo.ContentType.INDEX,
					this);
		}

		runFlag = true;
		start();
	}

	private void initListFromSQL() {
		// TODO
		saveList.initContent(ThreadInfo.ThreadType.UNSAVE,
				20);
		resolveList.initContent(ThreadInfo.ThreadType.SAVED, 20);
		combateList.initContent(ThreadInfo.ThreadType.RESOLVED, 20);
	}

	//ֹͣ����
	public void onStopRunning() {
		runFlag = false;


		saveList.clear();
		resolveList.clear();
		combateList.clear();

		//TODO �ȴ��߳���������ڹر�
		threadPool.shutdown();
		stop();
		//TODO �������й�����Ϣ
	}

	public Context() {
		httpClient = HttpClients.createDefault();
		initDataBaseConnectionPool();
	}

	@Override
	public void run() {
		while (runFlag) {
			int listEmptyFlag = 0;
			int leftThreadNum = maxThreadNumber - saveList.getRunningCount();
			ThreadInfo threadInfo = null;
			if (leftThreadNum > 0) {

				for (int i = 0; i < leftThreadNum / 3; i++) {
					if (null != (threadInfo = saveList.getThread())) {
						threadPool.execute(threadInfo);
					}else{
						listEmptyFlag |= 1;
					}
					if (null != (threadInfo = resolveList.getThread())) {
						threadPool.execute(threadInfo);
					}else{
						listEmptyFlag |= 2;
					}
					if (null != (threadInfo = combateList.getThread())) {
						threadPool.execute(threadInfo);
					}else{
						listEmptyFlag |= 4;
					}

					if(listEmptyFlag == 7){
						try {
							Thread.sleep(1 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
							try {
								Thread.sleep(1 * 1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
